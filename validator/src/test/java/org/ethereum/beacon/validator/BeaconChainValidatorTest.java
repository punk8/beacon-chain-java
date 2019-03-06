package org.ethereum.beacon.validator;

import static org.mockito.ArgumentMatchers.any;

import java.util.Random;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.util.ValidatorServiceTestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BeaconChainValidatorTest {

  @Test
  public void recentStateIsKept() {
    Random random = new Random();
    Schedulers schedulers = Schedulers.createDefault();
    SpecHelpers specHelpers =
        Mockito.spy(SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT, schedulers::getCurrentTime));

    BeaconChainValidator validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, specHelpers);
    Mockito.doReturn(true).when(specHelpers).is_current_slot(any());
    Mockito.doNothing().when(validator).runTasks(any());

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, currentSlot);

    // state was kept
    validator.onNewState(currentSlotState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());

    ObservableBeaconState updatedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, currentSlot);

    // state was updated
    validator.onNewState(updatedState);
    Assert.assertEquals(updatedState, validator.getRecentState());
  }

  @Test
  public void outboundRecentStateIsIgnored() {
    Random random = new Random();
    Schedulers schedulers = Schedulers.createDefault();
    SpecHelpers specHelpers =
        Mockito.spy(SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT, schedulers::getCurrentTime));

    BeaconChainValidator validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, specHelpers);
    Mockito.doNothing().when(validator).runTasks(any());

    ObservableBeaconState outdatedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, SlotNumber.ZERO);

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, currentSlot);

    Mockito.doReturn(true).when(specHelpers).is_current_slot(currentSlotState.getLatestSlotState());

    // state wasn't kept
    validator.onNewState(outdatedState);
    Assert.assertNull(validator.getRecentState());

    // state was kept
    validator.onNewState(currentSlotState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());

    // state wasn't updated
    validator.onNewState(outdatedState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());
  }

  @Test
  public void initService() {
    Random random = new Random();
    Schedulers schedulers = Schedulers.createDefault();
    SpecHelpers specHelpers =
        Mockito.spy(SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT, schedulers::getCurrentTime));

    BeaconChainValidator validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, specHelpers);

    ValidatorIndex validatorIndex = ValidatorIndex.of(Math.abs(random.nextInt()) % 10 + 10);

    ObservableBeaconState outdatedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, SlotNumber.ZERO);
    validator.onNewState(outdatedState);
    Assert.assertNull(validator.getRecentState());

    Mockito.verify(validator, Mockito.never()).init(any());
    Mockito.verify(validator, Mockito.never()).runTasks(any());

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, currentSlot);

    Mockito.doReturn(true).when(specHelpers).is_current_slot(any());
    Mockito.doReturn(validatorIndex).when(specHelpers).get_validator_index_by_pubkey(any(), any());
    Mockito.doNothing().when(validator).runTasks(any());

    validator.onNewState(currentSlotState);

    // init was called
    Mockito.verify(validator, Mockito.times(1)).init(currentSlotState.getLatestSlotState());

    // validatorIndex is set
    Assert.assertEquals(validatorIndex, validator.getValidatorIndex());

    // init is not triggered twice
    validator.onNewState(currentSlotState);
    Mockito.verify(validator, Mockito.times(1)).init(currentSlotState.getLatestSlotState());
  }

  @Test
  public void runValidatorTasks() {
    Random random = new Random();
    Schedulers schedulers = Schedulers.createDefault();
    SpecHelpers specHelpers =
        Mockito.spy(SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT, schedulers::getCurrentTime));

    BeaconChainValidator validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, specHelpers);

    ValidatorIndex validatorIndex = ValidatorIndex.of(Math.abs(random.nextInt()) % 10 + 10);

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState initialState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, currentSlot);
    ObservableBeaconState updatedState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, specHelpers, currentSlot.increment());
    ObservableBeaconState sameSlotState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, specHelpers, currentSlot.increment());
    ObservableBeaconState nextSlotState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, specHelpers, currentSlot.increment().increment());

    Mockito.doReturn(true).when(specHelpers).is_current_slot(any());
    Mockito.doReturn(validatorIndex).when(specHelpers).get_validator_index_by_pubkey(any(), any());
    Mockito.doNothing().when(validator).runTasks(any());

    validator.onNewState(initialState);
    // validatorIndex is set
    Assert.assertEquals(validatorIndex, validator.getValidatorIndex());

    // runTasks was called on a new state
    validator.onNewState(updatedState);
    Mockito.verify(validator, Mockito.times(1)).runTasks(updatedState);
    Mockito.verify(validator, Mockito.times(2)).runTasks(any());

    // runTasks was not called for a state belonging to the same slot
    validator.onNewState(sameSlotState);
    Mockito.verify(validator, Mockito.times(2)).runTasks(any());

    // runTasks was called again when a state for a new slot came
    validator.onNewState(nextSlotState);
    Mockito.verify(validator, Mockito.times(3)).runTasks(any());
  }
}
