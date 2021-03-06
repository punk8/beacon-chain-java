package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class ResponseMessage extends Message {
  @SSZ
  private UInt64 id;
  @SSZ(type = "uint16")
  private final int responseCode;
  @SSZ
  private final BytesValue result;

  public ResponseMessage(int responseCode, BytesValue result) {
    this.responseCode = responseCode;
    this.result = result;
  }

  public ResponseMessage(UInt64 id, int responseCode,
      BytesValue result) {
    this.id = id;
    this.responseCode = responseCode;
    this.result = result;
  }

  public UInt64 getId() {
    return id;
  }

  public void setId(UInt64 id) {
    this.id = id;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public BytesValue getResult() {
    return result;
  }

  @Override
  public ResponseMessagePayload getPayload() {
    return null;
  }
}
