package org.ethereum.beacon.core.state;

import java.util.function.Function;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.Serializer;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SSZSerializable
public class ImmutableBeaconStateImpl implements BeaconState, Hashable<Hash32> {

  /* Misc */

  @SSZ private final SlotNumber slot;
  @SSZ private final Time genesisTime;
  @SSZ private final Fork fork;

  /* Validator registry */

  @SSZ private final List<ValidatorRecord> validatorRegistryList;
  @SSZ private final List<Gwei> validatorBalancesList;
  @SSZ private final EpochNumber validatorRegistryUpdateEpoch;

  /* Randomness and committees */

  @SSZ private final List<Hash32> latestRandaoMixesList;
  @SSZ private final ShardNumber previousShufflingStartShard;
  @SSZ private final ShardNumber currentShufflingStartShard;
  @SSZ private final EpochNumber previousShufflingEpoch;
  @SSZ private final EpochNumber currentShufflingEpoch;
  @SSZ private final Hash32 previousShufflingSeed;
  @SSZ private final Hash32 currentShufflingSeed;

  /* Finality */

  @SSZ private List<PendingAttestation> previousEpochAttestationList = new ArrayList<>();
  @SSZ private List<PendingAttestation> currentEpochAttestationList = new ArrayList<>();
  @SSZ private EpochNumber previousJustifiedEpoch = EpochNumber.ZERO;
  @SSZ private EpochNumber currentJustifiedEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 previousJustifiedRoot = Hash32.ZERO;
  @SSZ private Hash32 currentJustifiedRoot = Hash32.ZERO;
  @SSZ private Bitfield64 justificationBitfield = Bitfield64.ZERO;
  @SSZ private EpochNumber finalizedEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 finalizedRoot = Hash32.ZERO;

  /* Recent state */

  @SSZ private List<Crosslink> previousEpochCrosslinksList = new ArrayList<>();
  @SSZ private List<Crosslink> currentEpochCrosslinksList = new ArrayList<>();
  @SSZ private List<Hash32> latestBlockRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestStateRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestActiveIndexRootsList = new ArrayList<>();
  @SSZ private List<Gwei> latestSlashedBalancesList = new ArrayList<>();
  @SSZ private BeaconBlockHeader latestBlockHeader = BeaconBlockHeader.EMPTY;
  @SSZ private List<Hash32> historicalRootList = new ArrayList<>();

  /* PoW receipt root */

  @SSZ private final Eth1Data latestEth1Data;
  @SSZ private final List<Eth1DataVote> eth1DataVotesList;
  @SSZ private final UInt64 depositIndex;

  private Hash32 hashCache = null;

  public ImmutableBeaconStateImpl(BeaconState state) {
    this.slot = state.getSlot();
    this.genesisTime = state.getGenesisTime();
    this.fork = state.getFork();

    this.validatorRegistryList = state.getValidatorRegistry().listCopy();
    this.validatorBalancesList = state.getValidatorBalances().listCopy();
    this.validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

    this.latestRandaoMixesList = state.getLatestRandaoMixes().listCopy();
    this.previousShufflingStartShard = state.getPreviousShufflingStartShard();
    this.currentShufflingStartShard = state.getCurrentShufflingStartShard();
    this.previousShufflingEpoch = state.getPreviousShufflingEpoch();
    this.currentShufflingEpoch = state.getCurrentShufflingEpoch();
    this.previousShufflingSeed = state.getPreviousShufflingSeed();
    this.currentShufflingSeed = state.getCurrentShufflingSeed();

    this.previousEpochAttestationList = state.getPreviousEpochAttestations().listCopy();
    this.currentEpochAttestationList = state.getCurrentEpochAttestations().listCopy();
    this.previousJustifiedEpoch = state.getPreviousJustifiedEpoch();
    this.currentJustifiedEpoch = state.getCurrentJustifiedEpoch();
    this.previousJustifiedRoot = state.getPreviousJustifiedRoot();
    this.currentJustifiedRoot = state.getCurrentJustifiedRoot();
    this.justificationBitfield = state.getJustificationBitfield();
    this.finalizedEpoch = state.getFinalizedEpoch();
    this.finalizedRoot = state.getFinalizedRoot();

    this.previousEpochCrosslinksList = state.getPreviousEpochCrosslinks().listCopy();
    this.currentEpochCrosslinksList = state.getCurrentEpochCrosslinks().listCopy();
    this.latestBlockRootsList = state.getLatestBlockRoots().listCopy();
    this.latestStateRootsList = state.getLatestStateRoots().listCopy();
    this.latestActiveIndexRootsList = state.getLatestActiveIndexRoots().listCopy();
    this.latestSlashedBalancesList = state.getLatestSlashedBalances().listCopy();
    this.latestBlockHeader = state.getLatestBlockHeader();
    this.historicalRootList = state.getHistoricalRoots().listCopy();

    this.latestEth1Data = state.getLatestEth1Data();
    this.eth1DataVotesList = state.getEth1DataVotes().listCopy();
    this.depositIndex = state.getDepositIndex();
  }

  /** ******* List Getters for serialization ********* */
  public List<ValidatorRecord> getValidatorRegistryList() {
    return validatorRegistryList;
  }

  public List<Gwei> getValidatorBalancesList() {
    return validatorBalancesList;
  }

  public List<Hash32> getLatestRandaoMixesList() {
    return new ArrayList<>(latestRandaoMixesList);
  }

  public List<Crosslink> getPreviousEpochCrosslinksList() {
    return previousEpochCrosslinksList;
  }

  public List<Crosslink> getCurrentEpochCrosslinksList() {
    return new ArrayList<>(currentEpochCrosslinksList);
  }

  public List<Hash32> getLatestBlockRootsList() {
    return new ArrayList<>(latestBlockRootsList);
  }

  public List<Eth1DataVote> getEth1DataVotesList() {
    return new ArrayList<>(eth1DataVotesList);
  }

  public List<Hash32> getLatestActiveIndexRootsList() {
    return new ArrayList<>(latestActiveIndexRootsList);
  }

  public List<Gwei> getLatestSlashedBalancesList() {
    return new ArrayList<>(latestSlashedBalancesList);
  }

  public List<PendingAttestation> getPreviousEpochAttestationList() {
    return new ArrayList<>(previousEpochAttestationList);
  }

  public List<PendingAttestation> getCurrentEpochAttestationList() {
    return new ArrayList<>(currentEpochAttestationList);
  }

  public List<Hash32> getLatestStateRootsList() {
    return new ArrayList<>(latestStateRootsList);
  }

  public List<Hash32> getHistoricalRootList() {
    return new ArrayList<>(historicalRootList);
  }

  @Override
  public SlotNumber getSlot() {
    return slot;
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public Fork getFork() {
    return fork;
  }

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return validatorRegistryUpdateEpoch;
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch;
  }

  @Override
  public EpochNumber getCurrentJustifiedEpoch() {
    return currentJustifiedEpoch;
  }

  @Override
  public Hash32 getPreviousJustifiedRoot() {
    return previousJustifiedRoot;
  }

  @Override
  public Hash32 getCurrentJustifiedRoot() {
    return currentJustifiedRoot;
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch;
  }

  @Override
  public Hash32 getFinalizedRoot() {
    return finalizedRoot;
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public ShardNumber getPreviousShufflingStartShard() {
    return previousShufflingStartShard;
  }

  @Override
  public ShardNumber getCurrentShufflingStartShard() {
    return currentShufflingStartShard;
  }

  @Override
  public EpochNumber getPreviousShufflingEpoch() {
    return previousShufflingEpoch;
  }

  @Override
  public EpochNumber getCurrentShufflingEpoch() {
    return currentShufflingEpoch;
  }

  @Override
  public Hash32 getPreviousShufflingSeed() {
    return previousShufflingSeed;
  }

  @Override
  public Hash32 getCurrentShufflingSeed() {
    return currentShufflingSeed;
  }

  @Override
  public ReadList<Integer, PendingAttestation> getPreviousEpochAttestations() {
    return ReadList.wrap(previousEpochAttestationList, Function.identity());
  }

  @Override
  public ReadList<Integer, PendingAttestation> getCurrentEpochAttestations() {
    return ReadList.wrap(currentEpochAttestationList, Function.identity());
  }

  @Override
  public ReadList<ShardNumber, Crosslink> getPreviousEpochCrosslinks() {
    return ReadList.wrap(previousEpochCrosslinksList, ShardNumber::of);
  }

  @Override
  public ReadList<ShardNumber, Crosslink> getCurrentEpochCrosslinks() {
    return ReadList.wrap(currentEpochCrosslinksList, ShardNumber::of);
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return ReadList.wrap(latestActiveIndexRootsList, EpochNumber::of);
  }

  @Override
  public ReadList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return ReadList.wrap(latestSlashedBalancesList, EpochNumber::of);
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex;
  }

  @Override
  public ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return ReadList.wrap(validatorRegistryList, ValidatorIndex::of);
  }

  @Override
  public ReadList<ValidatorIndex, Gwei> getValidatorBalances() {
    return ReadList.wrap(validatorBalancesList, ValidatorIndex::of);
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return ReadList.wrap(latestRandaoMixesList, EpochNumber::of);
  }

  @Override
  public ReadList<SlotNumber, Hash32> getLatestBlockRoots() {
    return ReadList.wrap(latestBlockRootsList, SlotNumber::of);
  }

  @Override
  public ReadList<SlotNumber, Hash32> getLatestStateRoots() {
    return ReadList.wrap(latestStateRootsList, SlotNumber::of);
  }

  @Override
  public BeaconBlockHeader getLatestBlockHeader() {
    return latestBlockHeader;
  }

  @Override
  public ReadList<Integer, Hash32> getHistoricalRoots() {
    return ReadList.wrap(historicalRootList, Function.identity());
  }

  @Override
  public ReadList<Integer, Eth1DataVote> getEth1DataVotes() {
    return ReadList.wrap(eth1DataVotesList, Integer::valueOf);
  }

  @Override
  public Optional<Hash32> getHash() {
    return Optional.ofNullable(hashCache);
  }

  @Override
  public void setHash(Hash32 hash) {
    this.hashCache = hash;
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object o) {
    Serializer serializer = Serializer.annotationSerializer();
    return serializer.encode2(this).equals(serializer.encode2(o));
  }

  @Override
  public String toString() {
    return toStringShort(null);
  }
}
