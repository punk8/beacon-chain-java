package org.ethereum.beacon.chain;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BeaconTupleDetails extends BeaconTuple {

  private final BeaconStateEx postSlotState;
  private final BeaconStateEx postBlockState;

  public BeaconTupleDetails(
      @Nonnull SignedBeaconBlock block,
      @Nullable BeaconStateEx postSlotState,
      @Nullable BeaconStateEx postBlockState,
      @Nonnull BeaconStateEx finalState) {

    super(block, finalState);
    this.postSlotState = postSlotState;
    this.postBlockState = postBlockState;
  }
  public BeaconTupleDetails(BeaconTuple tuple) {
    this(tuple.getSignedBlock(), null, null, tuple.getState());
  }

  public Optional<BeaconStateEx> getPostSlotState() {
    return Optional.ofNullable(postSlotState);
  }

  public Optional<BeaconStateEx> getPostBlockState() {
    return Optional.ofNullable(postBlockState);
  }

  public BeaconStateEx getFinalState() {
    return getState();
  }

  @Override
  public String toString() {
    return getFinalState().toString();
  }
}
