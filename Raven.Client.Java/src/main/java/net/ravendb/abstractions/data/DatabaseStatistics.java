package net.ravendb.abstractions.data;

import java.util.Collection;
import java.util.UUID;

public class DatabaseStatistics {

  public static class TriggerInfo {
    private String type;
    private String name;

    public String getName() {
      return name;
    }
    public String getType() {
      return type;
    }
    public void setName(String name) {
      this.name = name;
    }
    public void setType(String type) {
      this.type = type;
    }
  }

  private Etag lastDocEtag;
  private Etag lastAttachmentEtag;
  private int countOfIndexes;
  private int inMemoryIndexingQueueSize;
  private long approximateTaskCount;
  private long countOfDocuments;
  private long countOfAttachments;
  private String[] staleIndexes;
  private int currentNumberOfItemsToIndexInSingleBatch;
  private int currentNumberOfItemsToReduceInSingleBatch;
  private float databaseTransactionVersionSizeInMB;
  private IndexStats[] indexes;
  private ServerError[] errors;
  private TriggerInfo[] triggers;
  private Collection<ExtensionsLog> extensions;
  private IndexingBatchInfo[] indexingBatchInfo;
  private FutureBatchStats[] prefetches;
  private UUID databaseId;
  private boolean supportsDtc;


  public IndexingBatchInfo[] getIndexingBatchInfo() {
    return indexingBatchInfo;
  }

  public void setIndexingBatchInfo(IndexingBatchInfo[] indexingBatchInfo) {
    this.indexingBatchInfo = indexingBatchInfo;
  }

  public boolean isSupportsDtc() {
    return supportsDtc;
  }

  public void setSupportsDtc(boolean supportsDtc) {
    this.supportsDtc = supportsDtc;
  }
  public long getApproximateTaskCount() {
    return approximateTaskCount;
  }

  public long getCountOfAttachments() {
    return countOfAttachments;
  }

  public void setCountOfAttachments(long countOfAttachments) {
    this.countOfAttachments = countOfAttachments;
  }
  public long getCountOfDocuments() {
    return countOfDocuments;
  }
  public int getCountOfIndexes() {
    return countOfIndexes;
  }
  public int getCurrentNumberOfItemsToIndexInSingleBatch() {
    return currentNumberOfItemsToIndexInSingleBatch;
  }

  public int getCurrentNumberOfItemsToReduceInSingleBatch() {
    return currentNumberOfItemsToReduceInSingleBatch;
  }
  public UUID getDatabaseId() {
    return databaseId;
  }
  public float getDatabaseTransactionVersionSizeInMB() {
    return databaseTransactionVersionSizeInMB;
  }
  public ServerError[] getErrors() {
    return errors;
  }
  public Collection<ExtensionsLog> getExtensions() {
    return extensions;
  }
  public IndexStats[] getIndexes() {
    return indexes;
  }
  public int getInMemoryIndexingQueueSize() {
    return inMemoryIndexingQueueSize;
  }
  public Etag getLastAttachmentEtag() {
    return lastAttachmentEtag;
  }
  public Etag getLastDocEtag() {
    return lastDocEtag;
  }
  public FutureBatchStats[] getPrefetches() {
    return prefetches;
  }
  public String[] getStaleIndexes() {
    return staleIndexes;
  }
  public TriggerInfo[] getTriggers() {
    return triggers;
  }
  public void setApproximateTaskCount(long approximateTaskCount) {
    this.approximateTaskCount = approximateTaskCount;
  }
  public void setCountOfDocuments(long countOfDocuments) {
    this.countOfDocuments = countOfDocuments;
  }
  public void setCountOfIndexes(int countOfIndexes) {
    this.countOfIndexes = countOfIndexes;
  }
  public void setCurrentNumberOfItemsToIndexInSingleBatch(int currentNumberOfItemsToIndexInSingleBatch) {
    this.currentNumberOfItemsToIndexInSingleBatch = currentNumberOfItemsToIndexInSingleBatch;
  }
  public void setCurrentNumberOfItemsToReduceInSingleBatch(int currentNumberOfItemsToReduceInSingleBatch) {
    this.currentNumberOfItemsToReduceInSingleBatch = currentNumberOfItemsToReduceInSingleBatch;
  }
  public void setDatabaseId(UUID databaseId) {
    this.databaseId = databaseId;
  }
  public void setDatabaseTransactionVersionSizeInMB(float databaseTransactionVersionSizeInMB) {
    this.databaseTransactionVersionSizeInMB = databaseTransactionVersionSizeInMB;
  }
  public void setErrors(ServerError[] errors) {
    this.errors = errors;
  }
  public void setExtensions(Collection<ExtensionsLog> extensions) {
    this.extensions = extensions;
  }
  public void setIndexes(IndexStats[] indexes) {
    this.indexes = indexes;
  }
  public void setInMemoryIndexingQueueSize(int inMemoryIndexingQueueSize) {
    this.inMemoryIndexingQueueSize = inMemoryIndexingQueueSize;
  }

  public void setLastAttachmentEtag(Etag lastAttachmentEtag) {
    this.lastAttachmentEtag = lastAttachmentEtag;
  }

  public void setLastDocEtag(Etag lastDocEtag) {
    this.lastDocEtag = lastDocEtag;
  }

  public void setPrefetches(FutureBatchStats[] prefetches) {
    this.prefetches = prefetches;
  }

  public void setStaleIndexes(String[] staleIndexes) {
    this.staleIndexes = staleIndexes;
  }

  public void setTriggers(TriggerInfo[] triggers) {
    this.triggers = triggers;
  }



}
