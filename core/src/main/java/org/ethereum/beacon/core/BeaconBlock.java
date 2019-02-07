package org.ethereum.beacon.core;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Beacon chain block.
 *
 * <p>It consists of a header fields and {@link BeaconBlockBody}.
 *
 * @see BeaconBlockBody
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beaconblock">BeaconBlock
 *     in the spec</a>
 */
@SSZSerializable
public class BeaconBlock implements Hashable<Hash32> {

  /** Number of a slot that block does belong to. */
  @SSZ private final SlotNumber slot;
  /** A hash of parent block. */
  @SSZ private final Hash32 parentRoot;
  /** A hash of the state that is created by applying a block to the previous state. */
  @SSZ private final Hash32 stateRoot;
  /** RANDAO signature submitted by proposer. */
  @SSZ private final BLSSignature randaoReveal;
  /** Eth1 data that is observed by proposer. */
  @SSZ private final Eth1Data eth1Data;
  /** Proposer's signature. */
  @SSZ private final BLSSignature signature;

  /** Block body. */
  @SSZ private final BeaconBlockBody body;

  public BeaconBlock(
      SlotNumber slot,
      Hash32 parentRoot,
      Hash32 stateRoot,
      BLSSignature randaoReveal,
      Eth1Data eth1Data,
      BLSSignature signature,
      BeaconBlockBody body) {
    this.slot = slot;
    this.parentRoot = parentRoot;
    this.stateRoot = stateRoot;
    this.randaoReveal = randaoReveal;
    this.eth1Data = eth1Data;
    this.signature = signature;
    this.body = body;
  }

  public BeaconBlock withStateRoot(Hash32 stateRoot) {
    return new BeaconBlock(slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public Hash32 getParentRoot() {
    return parentRoot;
  }

  public Hash32 getStateRoot() {
    return stateRoot;
  }

  public BLSSignature getRandaoReveal() {
    return randaoReveal;
  }

  public Eth1Data getEth1Data() {
    return eth1Data;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  public BeaconBlockBody getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BeaconBlock block = (BeaconBlock) o;
    return Objects.equal(slot, block.slot) &&
        Objects.equal(parentRoot, block.parentRoot) &&
        Objects.equal(stateRoot, block.stateRoot) &&
        Objects.equal(randaoReveal, block.randaoReveal) &&
        Objects.equal(eth1Data, block.eth1Data) &&
        Objects.equal(signature, block.signature) &&
        Objects.equal(body, block.body);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);
  }

  public static class Builder {
    private SlotNumber slot;
    private Hash32 parentRoot;
    private Hash32 stateRoot;
    private BLSSignature randaoReveal;
    private Eth1Data eth1Data;
    private BLSSignature signature;
    private BeaconBlockBody body;

    private Builder() {}

    public static Builder createEmpty() {
      return new Builder();
    }

    public static Builder fromBlock(BeaconBlock block) {
      Builder builder = new Builder();

      builder.slot = block.slot;
      builder.parentRoot = block.parentRoot;
      builder.stateRoot = block.stateRoot;
      builder.randaoReveal = block.randaoReveal;
      builder.eth1Data = block.eth1Data;
      builder.signature = block.signature;
      builder.body = block.body;

      return builder;
    }

    public Builder withSlot(SlotNumber slot) {
      this.slot = slot;
      return this;
    }

    public Builder withParentRoot(Hash32 parentRoot) {
      this.parentRoot = parentRoot;
      return this;
    }

    public Builder withStateRoot(Hash32 stateRoot) {
      this.stateRoot = stateRoot;
      return this;
    }

    public Builder withRandaoReveal(BLSSignature randaoReveal) {
      this.randaoReveal = randaoReveal;
      return this;
    }

    public Builder withEth1Data(Eth1Data eth1Data) {
      this.eth1Data = eth1Data;
      return this;
    }

    public Builder withSignature(BLSSignature signature) {
      this.signature = signature;
      return this;
    }

    public Builder withBody(BeaconBlockBody body) {
      this.body = body;
      return this;
    }

    public BeaconBlock build() {
      assert slot != null;
      assert parentRoot != null;
      assert stateRoot != null;
      assert randaoReveal != null;
      assert eth1Data != null;
      assert signature != null;
      assert body != null;

      return new BeaconBlock(
          slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);
    }
  }
}
