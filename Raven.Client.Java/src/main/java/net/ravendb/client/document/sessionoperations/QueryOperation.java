package net.ravendb.client.document.sessionoperations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJValue;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.client.connection.SerializationHelper;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;
import net.ravendb.client.exceptions.NonAuthoritativeInformationException;
import net.ravendb.client.shard.ShardReduceFunction;

import org.apache.commons.lang.StringUtils;


public class QueryOperation {

  private static final ILog log = LogManager.getCurrentClassLogger();
  private final InMemoryDocumentSessionOperations sessionOperations;
  private final String indexName;
  private final IndexQuery indexQuery;
  private final boolean waitForNonStaleResults;
  private boolean disableEntitiesTracking;
  private final long timeout;
  private final ShardReduceFunction transformResults;
  private final Set<String> includes;
  private QueryResult currentQueryResults;
  private final String[] projectionFields;
  private boolean firstRequest = true;

  private static final Pattern ID_ONLY =  Pattern.compile("^__document_id\\s*:\\s*([\\w_\\-/\\\\\\.]+)\\s*$");

  public QueryResult getCurrentQueryResults() {
    return currentQueryResults;
  }

  public String getIndexName() {
    return indexName;
  }

  public IndexQuery getIndexQuery() {
    return indexQuery;
  }

  private long spStart;
  private long spStop;

  public QueryOperation(InMemoryDocumentSessionOperations sessionOperations, String indexName, IndexQuery indexQuery,
      String[] projectionFields, boolean waitForNonStaleResults, long timeout,
      ShardReduceFunction transformResults,
      Set<String> includes, boolean disableEntitiesTracking) {
    this.indexQuery = indexQuery;
    this.waitForNonStaleResults = waitForNonStaleResults;
    this.timeout = timeout;
    this.transformResults = transformResults;
    this.includes = includes;
    this.projectionFields = projectionFields;
    this.sessionOperations = sessionOperations;
    this.indexName = indexName;
    this.disableEntitiesTracking = disableEntitiesTracking;

    assertNotQueryById();
  }

  private void assertNotQueryById() {
    // this applies to dynamic indexes only
    if (!indexName.toLowerCase().startsWith("dynamic/") && !StringUtils.equalsIgnoreCase(indexName, "dynamic")) {
      return ;
    }

    Matcher matcher = ID_ONLY.matcher(indexQuery.getQuery());
    if (!matcher.matches()) {
      return ;
    }

    if (sessionOperations.getConventions().isAllowQueriesOnId()) {
      return ;
    }

    String value = matcher.group(1);
    throw new IllegalStateException( "Attempt to query by id only is blocked, you should use call session.load(\"" + value + "\"); instead of session.query().where(x=>x.Id == \"" + value + "\");\n" +
        "You can turn this error off by specifying documentStore.getConventions().setAllowQueriesOnId(true);, but that is not recommend and provided for backward compatibility reasons only.");
  }

  private void startTiming() {
    spStart = new Date().getTime();
  }

  public void logQuery() {
    log.debug("Executing query '%s' on index '%s' in '%s'",
        indexQuery.getQuery(), indexName, sessionOperations.getStoreIdentifier());
  }

  public CleanCloseable enterQueryContext() {
    if (firstRequest) {
      startTiming();
      firstRequest = false;
    }

    if (waitForNonStaleResults == false) {
      return null;
    }

    return sessionOperations.getDocumentStore().disableAggressiveCaching();
  }

  @SuppressWarnings("boxing")
  public boolean shouldQueryAgain(Exception e) {
    if (e instanceof NonAuthoritativeInformationException == false) {
      return false;
    }
    return (new Date().getTime()  - spStart) <= sessionOperations.getNonAuthoritativeInformationTimeout();
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> complete(Class<T> clazz)
  {
    QueryResult queryResult = currentQueryResults.createSnapshot();
    for (JsonDocument include : SerializationHelper.ravenJObjectsToJsonDocuments(queryResult.getIncludes())) {
      sessionOperations.trackIncludedDocument(include);
    }

    List<T> list = new ArrayList<>();
    for (RavenJObject obj: queryResult.getResults()) {
      list.add(obj != null?deserialize(clazz, obj) : null);
    }

    List<RavenJObject> notNullResults = new ArrayList<>();
    for (RavenJObject obj : queryResult.getResults()) {
      if (obj != null) {
        notNullResults.add(obj);
      }
    }

    if (disableEntitiesTracking == false) {
      sessionOperations.registerMissingIncludes(notNullResults, includes);
    }

    if (transformResults == null) {
      return list;
    }

    return ((List<T>) transformResults.apply(indexQuery, (List<Object>) list));
  }

  public boolean isDisableEntitiesTracking() {
    return disableEntitiesTracking;
  }

  public void setDisableEntitiesTracking(boolean disableEntitiesTracking) {
    this.disableEntitiesTracking = disableEntitiesTracking;
  }

  @SuppressWarnings("unchecked")
  public <T> T deserialize(Class<T> clazz, RavenJObject result) {
    RavenJObject metadata = result.value(RavenJObject.class, "@metadata");
    if ((projectionFields == null || projectionFields.length <= 0) &&
        (metadata != null && StringUtils.isNotEmpty(metadata.value(String.class, "@id")))) {
      return (T) sessionOperations.trackEntity(clazz, metadata.value(String.class, "@id"), result, metadata, disableEntitiesTracking);
    }

    if (RavenJObject.class.equals(clazz)) {
      return (T)result;
    }

    String documentId = result.value(String.class, Constants.DOCUMENT_ID_FIELD_NAME); //check if the result contain the reserved name

    if (StringUtils.isNotEmpty(documentId) && String.class.equals(clazz) && // __document_id is present, and result type is a string
        projectionFields != null && projectionFields.length== 1 && // We are projecting one field only (although that could be derived from the
                                                                   // previous check, one could never be too careful
            hasSingleValidProperty(result, metadata) // there are no more props in the result object
        ) {
      return (T)documentId;
    }

    handleInternalMetadata(result);

    T deserializedResult = deserializedResult(clazz, result);

    if (StringUtils.isNotEmpty(documentId)) {
      // we need to make an additional check, since it is possible that a value was explicitly stated
      // for the identity property, in which case we don't want to override it.
      Field identityProperty = sessionOperations.getConventions().getIdentityProperty(clazz);
      if (identityProperty == null ||
          (!result.containsKey(identityProperty.getName()) ||
              result.get(identityProperty.getName()).getType() == JTokenType.NULL))  {
        sessionOperations.getGenerateEntityIdOnTheClient().trySetIdentity(deserializedResult, documentId);
      }
    }

    return deserializedResult;
  }

  private boolean hasSingleValidProperty(RavenJObject result, RavenJObject metadata) {
    if (metadata == null && result.getCount() == 1) {
      return true; // { Foo: val }
    }
    if (metadata != null && result.getCount() == 2) {
      return true; // { @metadata: {} , Foo: val}
    }
    if (metadata != null && result.getCount() == 3) {
      String entityName = metadata.value(String.class, Constants.RAVEN_ENTITY_NAME);
      String idPropName = sessionOperations.getConventions().getFindIdentityPropertyNameFromEntityName().find(entityName);

      if (result.containsKey(idPropName)) {
        // when we try to project the id by name
        RavenJToken token = result.value(RavenJToken.class, idPropName);

        if (token == null || token.getType() == JTokenType.NULL) {
          return true; // { @metadata: {}, Foo: val, Id: null }
        }
      }
    }
    return false;
  }

  private <T> T deserializedResult(Class<T> clazz, RavenJObject result) {
    if (String.class.equals(clazz) || Number.class.isAssignableFrom(clazz) || clazz.isEnum()) {
      if (projectionFields != null && projectionFields.length == 1) { // we only select a single field
        return result.value(clazz, projectionFields[0]);
      }

      switch (result.getCount()) {
        case 1:
          return result.value(clazz, projectionFields[0]);
        case 2:
          if (result.containsKey(Constants.METADATA)) {
            HashSet<String> keys = new HashSet<>(result.getKeys());
            keys.remove(Constants.METADATA);

            return result.value(clazz, keys.iterator().next());
          }
          break;
      }
    }

    try {
      return JsonExtensions.createDefaultJsonSerializer().readValue(result.toString(), clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void handleInternalMetadata(RavenJObject result) {
    // Implant a property with "id" value ... if not exists
    RavenJObject metadata = result.value(RavenJObject.class, "@metadata");
    if (metadata == null || StringUtils.isEmpty(metadata.value(String.class, "@id"))) {
      // if the item has metadata, then nested items will not have it, so we can skip recursing down

      for (Entry<String, RavenJToken> nestedTuple : result) {
        RavenJToken nested = nestedTuple.getValue();
        if (nested instanceof RavenJObject) {
          handleInternalMetadata((RavenJObject) nested);
        }
        if (nested instanceof RavenJArray) {
          RavenJArray array = (RavenJArray) nested;
          for (RavenJToken token : array) {
            if (token instanceof RavenJObject) {
              handleInternalMetadata((RavenJObject) token);
            }
          }
        }
      }
      return;

    }

    String entityName = metadata.value(String.class, Constants.RAVEN_ENTITY_NAME);

    String idPropName = sessionOperations.getConventions().getFindIdentityPropertyNameFromEntityName().find(entityName);

    if (result.containsKey(idPropName)) {
      return;
    }
    result.add(idPropName, new RavenJValue(metadata.value(String.class, "@id")));
  }

  public void forceResult(QueryResult result)
  {
    currentQueryResults = result;
    currentQueryResults.ensureSnapshot();
  }

  @SuppressWarnings("boxing")
  public boolean isAcceptable(QueryResult result) {
    if (!sessionOperations.isAllowNonAuthoritativeInformation() &&
        result.isNonAuthoritativeInformation()) {
      if ((new Date().getTime() - spStart) > sessionOperations.getNonAuthoritativeInformationTimeout()) {
        spStop = new Date().getTime();
        throw new RuntimeException( String.format("Waited for %dms for the query to return authoritative result.",
            (spStop - spStart)));
      }
      log.debug(
          "Non authoritative query results on authoritative query '%s' on index '%s' in '%s', query will be retried, index etag is: %s",
          indexQuery.getQuery(),
          indexName,
          sessionOperations.getStoreIdentifier(),
          result.getIndexEtag());
      return false;
    }
    if (waitForNonStaleResults && result.isStale()) {
      if ((new Date().getTime() - spStart) > timeout) {
        spStop = new Date().getTime();
        throw new RuntimeException(
            String.format("Waited for %sms for the query to return non stale result.",
                (spStop - spStart)));
      }
      log.debug(
          "Stale query results on non stale query '%s' on index '%s' in '%s', query will be retried, index etag is: %s",
          indexQuery.getQuery(),
          indexName,
          sessionOperations.getStoreIdentifier(),
          result.getIndexEtag());
      return false;
    }
    currentQueryResults = result;
    currentQueryResults.ensureSnapshot();
    log.debug("Query returned %d/%d %sresults", result.getResults().size(),
        result.getTotalResults(),(result.isStale() ? "stale " : ""));
    return true;
  }

}
