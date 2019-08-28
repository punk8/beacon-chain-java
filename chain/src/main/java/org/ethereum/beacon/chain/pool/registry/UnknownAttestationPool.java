package org.ethereum.beacon.chain.pool.registry;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.StatefulProcessor;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class UnknownAttestationPool implements AttestationRegistry, StatefulProcessor {

  /** prev + curr + lookahead */
  private final EpochNumber trackedEpochs;

  private final Queue queue;

  private final BeaconBlockStorage blockStorage;
  private final BeaconChainSpec spec;

  private EpochNumber currentBaseLine;

  public UnknownAttestationPool(
      BeaconBlockStorage blockStorage, BeaconChainSpec spec, EpochNumber lookahead, long size) {
    this.blockStorage = blockStorage;
    this.spec = spec;
    this.trackedEpochs = EpochNumber.of(2).plus(lookahead);
    this.queue = new Queue(trackedEpochs, size);
  }

  @Override
  public boolean add(ReceivedAttestation attestation) {
    assert isInitialized();

    AttestationData data = attestation.getMessage().getData();

    // beacon block has not yet been imported
    // it implies that source and target blocks might have not been imported too
    if (blockStorage.get(data.getBeaconBlockRoot()).isPresent()) {
      return false;
    } else {
      queue.add(data.getTarget().getEpoch(), data.getBeaconBlockRoot(), attestation);
      return true;
    }
  }

  public List<ReceivedAttestation> feedNewImportedBlock(BeaconBlock block) {
    EpochNumber blockEpoch = spec.compute_epoch_of_slot(block.getSlot());
    // blockEpoch < currentBaseLine || blockEpoch >= currentBaseLine + TRACKED_EPOCHS
    if (blockEpoch.less(currentBaseLine)
        || blockEpoch.greaterEqual(currentBaseLine.plus(trackedEpochs))) {
      return Collections.emptyList();
    }

    Hash32 blockRoot = spec.hash_tree_root(block);
    return queue.evict(blockRoot);
  }

  public void feedNewSlot(SlotNumber slotNumber) {
    EpochNumber currentEpoch = spec.compute_epoch_of_slot(slotNumber);
    EpochNumber baseLine =
        currentEpoch.equals(spec.getConstants().getGenesisEpoch())
            ? currentEpoch
            : currentEpoch.decrement();
    queue.moveBaseLine(baseLine);

    this.currentBaseLine = baseLine;
  }

  @Override
  public boolean isInitialized() {
    return currentBaseLine != null;
  }
}
