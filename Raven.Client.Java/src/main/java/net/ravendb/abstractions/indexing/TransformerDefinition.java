package net.ravendb.abstractions.indexing;

public class TransformerDefinition implements Cloneable {
  private String transformResults;
  private int indexId;
  private String name;
  private TransformerLockMode lockMode;

  /**
   * Transformer identifier (internal).
   */
  public int getIndexId() {
    return indexId;
  }

  /**
   * Transformer identifier (internal).
   * @param indexId
   */
  public void setIndexId(int indexId) {
    this.indexId = indexId;
  }

  @Override
  public String toString() {
    if (name != null) {
      return name;
    }
    return transformResults;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    TransformerDefinition clone = new TransformerDefinition();
    clone.setTransformResults(transformResults);
    clone.setName(name);
    return clone;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((transformResults == null) ? 0 : transformResults.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TransformerDefinition other = (TransformerDefinition) obj;
    if (transformResults == null) {
      if (other.transformResults != null)
        return false;
    } else if (!transformResults.equals(other.transformResults))
      return false;
    return true;
  }

  /**
   * Projection function.
   */
  public String getTransformResults() {
    return transformResults;
  }

  /**
   * Projection function.
   * @param transformResults
   */
  public void setTransformResults(String transformResults) {
    this.transformResults = transformResults;
  }

  /**
   * Transformer name.
   */
  public String getName() {
    return name;
  }

  /**
   * Transformer name.
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }


  public TransformerLockMode getLockMode() {
    return lockMode;
  }

  public void setLockMode(TransformerLockMode lockMode) {
    this.lockMode = lockMode;
  }
}
