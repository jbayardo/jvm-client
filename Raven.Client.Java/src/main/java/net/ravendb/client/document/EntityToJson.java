package net.ravendb.client.document;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJValue;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.listeners.IDocumentConversionListener;

public class EntityToJson {
  private final IDocumentStore documentStore;

  //All the listeners for this session
  private DocumentSessionListeners listeners;

  private final IdentityHashMap<Object, Map<String, RavenJToken>> missingDictionary = new IdentityHashMap<>();

  protected IdentityHashMap<Object, RavenJObject> cachedJsonDocs;

  public IdentityHashMap<Object, RavenJObject> getCachedJsonDocs() {
    return cachedJsonDocs;
  }

  public IdentityHashMap<Object, Map<String, RavenJToken>> getMissingDictionary() {
    return missingDictionary;
  }

  public DocumentSessionListeners getListeners() {
    return listeners;
  }

  public EntityToJson(IDocumentStore documentStore, DocumentSessionListeners listeners) {
    this.documentStore = documentStore;
    this.listeners = listeners;
  }


  public RavenJObject convertEntityToJson(String key, Object entity, RavenJObject metadata) {
    for (IDocumentConversionListener extendedDocumentConversionListener : listeners.getConversionListeners()) {
      extendedDocumentConversionListener.beforeConversionToDocument(key, entity, metadata);
    }

    Class< ? > entityType = entity.getClass();
    Field identityProperty = documentStore.getConventions().getIdentityProperty(entityType);

    RavenJObject objectAsJson = getObjectAsJson(entity);
    if (identityProperty != null) {
      objectAsJson.remove(identityProperty.getName());
    }

    setJavaClass(entityType, metadata);

    for (IDocumentConversionListener extendedDocumentConversionListener: listeners.getConversionListeners()) {
      extendedDocumentConversionListener.afterConversionToDocument(key, entity, objectAsJson, metadata);
    }

    return objectAsJson;
  }



  private RavenJObject getObjectAsJson(Object entity) {
    if (entity instanceof RavenJObject) {
      return ((RavenJObject) entity).cloneToken();
    }

    if (cachedJsonDocs != null && cachedJsonDocs.containsKey(entity)) {
      return cachedJsonDocs.get(entity).createSnapshot();
    }

    RavenJObject jObject = RavenJObject.fromObject(entity, documentStore.getConventions().createSerializer());
    jObject.setTag(entity);
    if (missingDictionary.containsKey(entity)) {
      Map<String, RavenJToken> value = missingDictionary.get(entity);
      for (Map.Entry<String, RavenJToken> item: value.entrySet()) {
        jObject.add(item.getKey(), item.getValue());
      }
    }

    trySimplifyingJson(jObject);

    if (cachedJsonDocs != null) {
      jObject.ensureCannotBeChangeAndEnableShapshotting();
      cachedJsonDocs.put(entity, jObject);
      return jObject.createSnapshot();
    }
    return jObject;
  }

  private void setJavaClass(Class<?> entityType, RavenJObject metadata) {
    if (RavenJObject.class.equals(entityType)) {
      return ; // do not overwrite the value
    }
    metadata.add(Constants.RAVEN_JAVA_CLASS, new RavenJValue(documentStore.getConventions().getJavaClassName(entityType)));
  }


  /**
   * All calls to convert an entity to a json object would be cache
   * This is used inside the SaveChanges() action, where we need to access the entities json
   * in several disparate places.
   *
   * Note: This assumes that no modifications can happen during the SaveChanges. This is naturally true
   * Note: for SaveChanges (and multi threaded access will cause undefined behavior anyway).
   */
  public CleanCloseable entitiesToJsonCachingScope() {
    cachedJsonDocs = new IdentityHashMap<>();
    return new DisposeCachedJsonDocs();
  }

  protected class DisposeCachedJsonDocs implements CleanCloseable {
    @Override
    public void close() {
      cachedJsonDocs = null;
    }
  }

  @SuppressWarnings("unused")
  private static void trySimplifyingJson(RavenJObject jObject) {
    // empty for now - register custom clean up methods
  }

}
