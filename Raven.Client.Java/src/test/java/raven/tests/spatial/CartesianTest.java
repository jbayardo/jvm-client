package raven.tests.spatial;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;

import raven.abstractions.indexing.FieldIndexing;
import raven.abstractions.indexing.FieldStorage;
import raven.abstractions.indexing.SpatialOptionsFactory;
import raven.abstractions.indexing.SpatialOptionsFactory.SpatialBounds;
import raven.abstractions.json.linq.RavenJObject;
import raven.client.IDocumentSession;
import raven.client.IDocumentStore;
import raven.client.RemoteClientTest;
import raven.client.document.DocumentQueryCustomizationFactory;
import raven.client.document.DocumentStore;
import raven.client.indexes.AbstractIndexCreationTask;


public class CartesianTest extends RemoteClientTest {

  @QueryEntity
  public static class SpatialDoc {
    private String id;
    private Object name;
    private String wkt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public Object getName() {
      return name;
    }

    public void setName(Object name) {
      this.name = name;
    }

    public String getWkt() {
      return wkt;
    }

    public void setWkt(String wkt) {
      this.wkt = wkt;
    }

  }

  public static class CartesianIndex extends AbstractIndexCreationTask {
    public CartesianIndex() {
      QCartesianTest_SpatialDoc x = QCartesianTest_SpatialDoc.spatialDoc;
      map = "from doc in docs.SpatialDocs select new { doc.Name, doc.Wkt }";
      index(x.name, FieldIndexing.ANALYZED);
      store(x.name, FieldStorage.YES);
      spatial(x.wkt, new SpatialOptionsFactory().getCartesian().quadPrefixTreeIndex(12, new SpatialBounds(0, 0, 2000, 2000)));
    }
  }

  @Test
  public void points() throws Exception {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      store.executeIndex(new CartesianIndex());
      try (IDocumentSession session = store.openSession()) {
        SpatialDoc doc1 = new SpatialDoc();
        doc1.setWkt("POINT (1950 1950)");
        Map<String, String> doc1Name = new HashMap<>();
        doc1Name.put("sdsdsd", "sdsds");
        doc1Name.put("sdsdsds", "sdsds");
        doc1.setName(doc1Name);

        session.store(doc1);

        SpatialDoc doc2 = new SpatialDoc();
        doc2.setWkt("POINT (50 1950)");
        doc2.setName("dog");
        session.store(doc2);

        SpatialDoc doc3 = new SpatialDoc();
        doc3.setWkt("POINT (1950 50)");
        doc3.setName("cat");
        session.store(doc3);

        SpatialDoc doc4 = new SpatialDoc();
        doc4.setWkt("POINT (50 50)");
        doc4.setName("dog");
        session.store(doc4);

        session.saveChanges();
      }

      waitForNonStaleIndexes(store.getDatabaseCommands());

      try (IDocumentSession session = store.openSession()) {
        boolean matched = session.query(RavenJObject.class, CartesianIndex.class)
          .customize(new DocumentQueryCustomizationFactory().withinRadiusOf("Wkt", 70,  1900, 1900))
          .any();
        assertTrue(matched);
      }
    }
  }

}
