package net.ravendb.tests.issues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentSession;
import net.ravendb.client.document.DocumentStore;

import org.junit.Test;


public class RavenDB_1533 extends RemoteClientTest {
  public static class Developer {
    private String nick;
    private int id;

    public String getNick() {
      return nick;
    }

    public void setNick(String nick) {
      this.nick = nick;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

  }

  @SuppressWarnings("boxing")
  @Test
  public void canDeleteObjectByKeyOnSyncSession() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      String developerId;
      Developer developer = new Developer();
      developer.setNick("ayende");

      try (IDocumentSession session = store.openSession()) {
        session.store(developer);
        session.saveChanges();
        developerId = store.getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(developer.getId(), Developer.class, false);
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        assertFalse(session.isDeleted(developerId));
        session.delete(developerId);
        assertTrue(session.isDeleted(developerId));
        session.saveChanges();
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        assertNull(session.load(Developer.class, developer.getId()));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void canDeleteObjectByTypeAndIdOnSyncSession() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      String developerId;
      Developer developer = new Developer();
      developer.setNick("ayende");

      try (IDocumentSession session = store.openSession()) {
        session.store(developer);
        session.saveChanges();
        developerId = store.getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(developer.getId(), Developer.class, false);
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        assertFalse(session.isDeleted(developerId));
        session.delete(Developer.class, developer.getId());
        assertTrue(session.isDeleted(developerId));
        session.saveChanges();
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        assertNull(session.load(Developer.class, developer.getId()));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldNotThrowWhenDeletingUnchangedLoadedObject() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      String developerId;
      Developer developer = new Developer();
      developer.setNick("ayende");

      try (IDocumentSession session = store.openSession()) {
        session.store(developer);
        session.saveChanges();
        developerId = store.getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(developer.getId(), Developer.class, false);
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        session.load(Developer.class, developer.getId());
        assertTrue(session.isLoaded(developerId));
        session.delete(Developer.class, developer.getId());
        assertFalse(session.isLoaded(developerId));
        session.saveChanges();
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenDeletingChangedLoadedObject() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      Developer developer = new Developer();
      developer.setNick("ayende");

      try (IDocumentSession session = store.openSession()) {
        session.store(developer);
        session.saveChanges();
      }

      try (DocumentSession session = (DocumentSession) store.openSession()) {
        Developer newDev = session.load(Developer.class, developer.getId());
        //modify object
        newDev.setNick("newNick");
        session.delete(Developer.class, developer.getId());
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenDeletingNewlyCreatedEntity() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      Developer developer = new Developer();
      developer.setNick("ayende");

      try (IDocumentSession session = store.openSession()) {
        session.store(developer);
        store.getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(developer.getId(), Developer.class, false);
        session.delete(Developer.class, developer.getId());
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenStoringJustDeletedIdentifier() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      Developer developer = new Developer();
      developer.setNick("ayende");
      developer.setId(11);

      try (IDocumentSession session = store.openSession()) {
        session.delete(Developer.class, 11);
        session.store(developer);
      }
    }
  }


}
