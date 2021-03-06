package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
public enum UuidType {

  DOCUMENTS((byte)1),

  @Deprecated
  ATTACHMENTS((byte)2),
  DOCUMENTTRANSACTIONS((byte)3),
  MAPPEDRESULTS((byte)4),
  REDUCERESULTS((byte)5),
  SCHEDULEDREDUCTIONS((byte)6),
  QUEUE((byte)7),
  TASKS((byte)8),
  INDEXING((byte)9),
  DOCUMENT_REFERENCES((byte)11),
  SUBSCRIPTIONS((byte)12),
  TRANSFORMERS((byte)13);

  private byte value;

  private UuidType(byte value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public byte getValue() {
    return value;
  }


}
