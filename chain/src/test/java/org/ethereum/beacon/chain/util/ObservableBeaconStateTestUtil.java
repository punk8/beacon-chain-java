package org.ethereum.beacon.chain.util;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.consensus.ChainStart;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class ObservableBeaconStateTestUtil {

  public static ObservableBeaconState createInitialState(Random random, BeaconChainSpec spec) {
    return createInitialState(
        random, spec, PendingOperationsTestUtil.createEmptyPendingOperations());
  }

  public static ObservableBeaconState createInitialState(
      Random random, BeaconChainSpec spec, SlotNumber slotNumber) {
    ObservableBeaconState originalState =
        createInitialState(
            random, spec, PendingOperationsTestUtil.createEmptyPendingOperations());

    MutableBeaconState modifiedState = originalState.getLatestSlotState().createMutableCopy();
    modifiedState.setSlot(slotNumber);

    BeaconBlock modifiedHead =
        BeaconBlock.Builder.fromBlock(originalState.getHead()).withSlot(slotNumber).build();
    return new ObservableBeaconState(
        modifiedHead, Mockito.spy(new BeaconStateExImpl(modifiedState)), originalState.getPendingOperations());
  }

  public static ObservableBeaconState createInitialState(
      Random random, BeaconChainSpec spec, PendingOperations operations) {
    BeaconBlock genesis = spec.get_empty_block();
    ChainStart chainStart =
        new ChainStart(
            Time.ZERO,
            new Eth1Data(Hash32.random(random), UInt64.ZERO, Hash32.random(random)),
            Collections.emptyList());
    InitialStateTransition stateTransition = new InitialStateTransition(chainStart, spec);

    BeaconStateEx state = stateTransition.apply(genesis);
    state = ExtendedSlotTransition.create(spec).apply(state);
    return new ObservableBeaconState(genesis, state, operations);
  }
}
