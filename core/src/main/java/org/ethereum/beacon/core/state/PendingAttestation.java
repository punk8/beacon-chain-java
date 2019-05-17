package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * An attestation data that have not been processed yet.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#pendingattestation">PendingAttestation
 *     in the spec</a>
 */
@SSZSerializable
public class PendingAttestation {

  /** Attester aggregation bitfield. */
  @SSZ private final Bitfield aggregationBitfield;
  /** Signed data. */
  @SSZ private final AttestationData data;
  /** Slot in which it was included. */
  @SSZ private final SlotNumber inclusionDelay;
  /** Proposer index. */
  @SSZ private final ValidatorIndex proposerIndex;

  public PendingAttestation(
      Bitfield aggregationBitfield,
      AttestationData data,
      SlotNumber inclusionDelay,
      ValidatorIndex proposerIndex) {
    this.aggregationBitfield = aggregationBitfield;
    this.data = data;
    this.inclusionDelay = inclusionDelay;
    this.proposerIndex = proposerIndex;
  }

  public Bitfield getAggregationBitfield() {
    return aggregationBitfield;
  }

  public AttestationData getData() {
    return data;
  }

  public ValidatorIndex getProposerIndex() {
    return proposerIndex;
  }

  public SlotNumber getInclusionDelay() {
    return inclusionDelay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PendingAttestation that = (PendingAttestation) o;
    return Objects.equal(data, that.data)
        && Objects.equal(aggregationBitfield, that.aggregationBitfield)
        && Objects.equal(proposerIndex, that.proposerIndex)
        && Objects.equal(inclusionDelay, that.inclusionDelay);
  }

  private String getSignerIndices() {
    return aggregationBitfield.getBits().stream().map(i -> "" + i).collect(Collectors.joining("+"));
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "Attestation["
        + data.toString(spec, beaconStart)
        + ", attesters=" + getSignerIndices()
        + ", proposerIndex=" + getProposerIndex()
        + ", inclusionDelay=#" + getInclusionDelay().toStringNumber(spec)
        + "]";
  }

  public String toStringShort(@Nullable SpecConstants spec) {
    return "epoch=" + getData().getTargetEpoch().toString(spec) + "/"
        + "delay=" + getInclusionDelay().toStringNumber(spec) + "/"
        + getData().getShard().toString() + "/"
        + getData().getBeaconBlockRoot().toStringShort() + "/"
        + getSignerIndices();
  }
}