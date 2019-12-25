package org.ethereum.beacon.validator.proposer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlock.Builder;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.ValidatorService;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An implementation of beacon chain proposer.
 *
 * @see BeaconChainProposer
 * @see ValidatorService
 */
public class BeaconChainProposerImpl implements BeaconChainProposer {
  /** The spec. */
  private BeaconChainSpec spec;
  /** Per-block state transition. */
  private BlockTransition<BeaconStateEx> perBlockTransition;
  /** Eth1 deposit contract. */
  private DepositContract depositContract;

  private final Function<Collection<Attestation>, List<Attestation>> aggregator;
  private final BiFunction<Attestation, BeaconState, Gwei> profitEvaluator;

  public BeaconChainProposerImpl(
      BeaconChainSpec spec,
      BlockTransition<BeaconStateEx> perBlockTransition,
      DepositContract depositContract) {
    this.spec = spec;
    this.perBlockTransition = perBlockTransition;
    this.depositContract = depositContract;
    this.aggregator = new SequentialAggregator(spec);
    this.profitEvaluator = new ProfitEvaluator(spec);
  }

  @Override
  public BeaconBlock propose(ObservableBeaconState observableState, BLSSignature randaoReveal) {
    BeaconStateEx state = observableState.getLatestSlotState();

    Hash32 parentRoot = spec.hash_tree_root(observableState.getHead());
    Eth1Data eth1Data = getEth1Data(state);
    BeaconBlockBody blockBody =
        getBlockBody(state, observableState.getPendingOperations(), randaoReveal, eth1Data);

    // create new block
    Builder builder = BeaconBlock.Builder.createEmpty();
    builder
        .withSlot(state.getSlot())
        .withParentRoot(parentRoot)
        .withStateRoot(Hash32.ZERO)
        .withBody(blockBody);

    // calculate state_root
    BeaconBlock newBlock = builder.build();
    BeaconState newState = perBlockTransition.apply(state, newBlock);
    builder.withStateRoot(spec.hash_tree_root(newState));

    return builder.build();
  }

  /**
   * Returns Eth1 data vote.
   *
   * @param state state at the slot of proposing block.
   * @return voted eth1 data.
   */
  private Eth1Data getEth1Data(BeaconState state) {
    UInt64 previousEth1Distance = UInt64.ZERO;
    return spec.get_eth1_vote(
        state,
        previousEth1Distance,
        distance -> depositContract.getLatestEth1Data().orElse(state.getEth1Data()));
  }

  /**
   * Creates block body for new block.
   *
   * @param state state at the slot of created block.
   * @param operations pending operations instance.
   * @return {@link BeaconBlockBody} for new block.
   * @see PendingOperations
   */
  private BeaconBlockBody getBlockBody(
      BeaconState state,
      PendingOperations operations,
      BLSSignature randaoReveal,
      Eth1Data eth1Data) {
    List<ProposerSlashing> proposerSlashings =
        operations.peekProposerSlashings(spec.getConstants().getMaxProposerSlashings());
    List<AttesterSlashing> attesterSlashings =
        operations.peekAttesterSlashings(spec.getConstants().getMaxAttesterSlashings());

    List<Attestation> aggregates = aggregator.apply(operations.getAttestations());
    List<Attestation> attestations;
    // include most profitable aggregates beyond block limit
    if (aggregates.size() <= spec.getConstants().getMaxAttestations()) {
      attestations = aggregates;
    } else {
      attestations =
          aggregates.stream()
              .map(attestation -> Pair.with(profitEvaluator.apply(attestation, state), attestation))
              .sorted(
                  Comparator.<Pair<Gwei, Attestation>, Gwei>comparing(Pair::getValue0).reversed())
              .map(Pair::getValue1)
              .collect(Collectors.toList());
    }

    List<SignedVoluntaryExit> voluntaryExits =
        operations.peekExits(spec.getConstants().getMaxVoluntaryExits());

    Eth1Data latestProcessedDeposit = null; // TODO wait for spec update to include this to state
    List<Deposit> deposits =
        depositContract
            .peekDeposits(
                spec.getConstants().getMaxDeposits(), latestProcessedDeposit, state.getEth1Data())
            .stream()
            .map(DepositInfo::getDeposit)
            .collect(Collectors.toList());
    Bytes32 graffiti = getGraffiti();

    return new BeaconBlockBody(
        randaoReveal,
        eth1Data,
        graffiti,
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        spec.getConstants());
  }

  private Bytes32 getGraffiti() {
    return Bytes32.ZERO;
  }
}
