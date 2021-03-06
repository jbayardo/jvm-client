package net.ravendb.client.document.sessionoperations;

import java.lang.reflect.Array;
import java.util.*;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.client.connection.SerializationHelper;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Defaults;


public class MultiLoadOperation {
  private static final ILog log = LogManager.getCurrentClassLogger();

  private final InMemoryDocumentSessionOperations sessionOperations;
  protected Function0<CleanCloseable> disableAllCaching;
  private final String[] ids;
  private final Tuple<String, Class<?>>[] includes;
  boolean firstRequest = true;
  JsonDocument[] results;
  JsonDocument[] includeResults;

  private long spStart;

  public MultiLoadOperation(InMemoryDocumentSessionOperations sessionOperations, Function0<CleanCloseable> disableAllCaching, String[] ids, Tuple<String, Class<?>>[] includes) {
    this.sessionOperations = sessionOperations;
    this.disableAllCaching = disableAllCaching;
    this.ids = ids;
    this.includes = includes;
  }

  public void logOperation() {
    if (ids == null) {
      return;
    }
    log.debug("Bulk loading ids [%s] from %s", StringUtils.join(ids, ", "), sessionOperations.getStoreIdentifier());
  }

  public CleanCloseable enterMultiLoadContext() {
    if (firstRequest == false) { // if this is a repeated request, we mustn't use the cached result, but have to re-query the server
      return disableAllCaching.apply();
    }
    spStart = new Date().getTime();
    return null;
  }

  @SuppressWarnings("boxing")
  public boolean setResult(MultiLoadResult multiLoadResult) {
    firstRequest = false;
    includeResults = SerializationHelper.ravenJObjectsToJsonDocuments(multiLoadResult.getIncludes()).toArray(new JsonDocument[0]);
    results = SerializationHelper.ravenJObjectsToJsonDocuments(multiLoadResult.getResults()).toArray(new JsonDocument[0]);

    if (sessionOperations.isAllowNonAuthoritativeInformation()) {
      return false;
    }
    for (JsonDocument doc : results) {
      if (doc != null && !Boolean.TRUE.equals(doc.getNonAuthoritativeInformation())) {
        return false;
      }
    }
    if ( (new Date().getTime() - spStart ) < sessionOperations.getNonAuthoritativeInformationTimeout()) {
      return false;
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  public <T> T[] complete(Class<T> clazz) {
    for (int i = 0; i < includeResults.length; i++) {
      sessionOperations.trackIncludedDocument(includeResults[i]);
    }

    T[] finalResults = ids != null ? returnResultsById(clazz) : returnResults(clazz);

    for (int i = 0; i < finalResults.length; i++) {
      if (finalResults[i] == null) {
        sessionOperations.registerMissing(ids[i]);
      }
    }

    List<String> includePaths = null;
    if (this.includes != null) {
      includePaths = new ArrayList<>();
      for (Tuple<String, Class<?>> pair : this.includes) {
        includePaths.add(pair.getItem1());
      }
    }

    List<RavenJObject> missingInc = new ArrayList<>();
    for (JsonDocument doc: results) {
      if (doc != null) {
        missingInc.add(doc.getDataAsJson());
      }
    }
    sessionOperations.registerMissingIncludes(missingInc, includePaths);

    return finalResults;
  }

  private <T> T[] returnResults(Class<T> clazz) {

    T[] finalResults = (T[]) Array.newInstance(clazz, results.length);
    JsonDocument[] selectedResults = selectResults();

    for (int i = 0; i < selectedResults.length; i++) {
      if (results[i] != null) {
        finalResults[i] = (T) sessionOperations.trackEntity(clazz, selectedResults[i]);
      }
    }
    return finalResults;
  }

  private <T> T[] returnResultsById(Class<T> clazz) {
    T[] finalResults = (T[]) Array.newInstance(clazz, results.length);
    TreeMap<String, FinalResultPositionById> dic = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == null) {
        continue;
      }

      FinalResultPositionById position = dic.get(ids[i]);
      if (position == null) {
        FinalResultPositionById finalResultPositionById = new FinalResultPositionById();
        finalResultPositionById.singleReturn = i;
        dic.put(ids[i], finalResultPositionById);
      } else {
        if (position.singleReturn != null) {
          position.multipleReturns = new ArrayList<>(2);
          position.multipleReturns.add(position.singleReturn);
          position.singleReturn = null;
        }
        position.multipleReturns.add(i);
      }
    }

    for (JsonDocument jsonDocument : results) {
      if (jsonDocument == null) {
        continue;
      }

      String id = jsonDocument.getMetadata().value(String.class, "@id");
      if (id == null) {
        continue;
      }

      FinalResultPositionById position = dic.get(id);

      if (position != null) {
        if (position.singleReturn != null) {
          finalResults[position.singleReturn] = (T) sessionOperations.trackEntity(clazz, jsonDocument);
        } else if (position.multipleReturns != null) {
          T trackedEntity = (T) sessionOperations.trackEntity(clazz, jsonDocument);
          for (Integer pos : position.multipleReturns) {
            finalResults[pos] = trackedEntity;
          }
        }
      }
    }
    return finalResults;
  }

  private JsonDocument[] selectResults() {
    if (ids == null) {
      return results;
    }
    JsonDocument[] finalResult = new JsonDocument[ids.length];
    for (int i = 0; i < ids.length; i++) {
      String id = ids[i];
      for (JsonDocument doc: results) {
        if (doc != null && StringUtils.equalsIgnoreCase(id, doc.getMetadata().value(String.class, "@id"))) {
          finalResult[i] = doc;
          break;
        }
      }
    }

    return finalResult;
  }

  private static class FinalResultPositionById {
    public Integer singleReturn;
    public List<Integer> multipleReturns;
  }

}

