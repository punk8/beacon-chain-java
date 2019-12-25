package qa

import org.ethereum.beacon.bench.BenchmarkController
import org.ethereum.beacon.chain.BeaconTuple
import org.ethereum.beacon.chain.observer.ObservableBeaconState
import org.ethereum.beacon.chain.storage.BeaconBlockStorage
import org.ethereum.beacon.chain.storage.BeaconChainStorage
import org.ethereum.beacon.chain.storage.BeaconStateStorage
import org.ethereum.beacon.chain.storage.BeaconTupleStorage
import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory
import org.ethereum.beacon.chain.storage.impl.SerializerFactory
import org.ethereum.beacon.consensus.BeaconChainSpec
import org.ethereum.beacon.consensus.TransitionType
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl
import org.ethereum.beacon.consensus.util.PseudoBLSFunctions.pseudoSign
import org.ethereum.beacon.core.BeaconBlock
import org.ethereum.beacon.core.BeaconBlockBody
import org.ethereum.beacon.core.BeaconState
import org.ethereum.beacon.core.envelops.SignedBeaconBlock
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader
import org.ethereum.beacon.core.operations.Attestation
import org.ethereum.beacon.core.spec.SignatureDomains
import org.ethereum.beacon.core.state.Checkpoint
import org.ethereum.beacon.core.types.BLSSignature
import org.ethereum.beacon.core.types.EpochNumber
import org.ethereum.beacon.core.types.SlotNumber
import org.ethereum.beacon.core.types.ValidatorIndex
import org.ethereum.beacon.db.source.DataSource
import org.ethereum.beacon.db.source.SingleValueSource
import org.ethereum.beacon.emulator.config.main.Signer
import org.ethereum.beacon.emulator.config.main.ValidatorKeys
import org.ethereum.beacon.emulator.config.main.conract.EmulatorContract
import org.ethereum.beacon.node.ConfigUtils
import org.ethereum.beacon.qa.TestUtils
import org.ethereum.beacon.start.common.Launcher
import org.ethereum.beacon.start.common.util.BLS381MessageSignerFactory
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers
import org.ethereum.beacon.start.common.util.SimpleDepositContract
import org.ethereum.beacon.test.StateTestUtils
import org.ethereum.beacon.validator.BeaconBlockSigner
import org.ethereum.beacon.validator.RandaoGenerator
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl
import org.ethereum.beacon.validator.crypto.BLS381MessageSigner
import org.ethereum.beacon.wire.WireApiSub
import org.reactivestreams.Publisher
import qa.integration_tests.AttestationEvent
import qa.integration_tests.BlockEvent
import qa.integration_tests.CheckEvent
import qa.integration_tests.SlotEvent
import qa.integration_tests.TestEvent
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import tech.pegasys.artemis.ethereum.core.Hash32
import tech.pegasys.artemis.util.bytes.Bytes48
import tech.pegasys.artemis.util.collections.ReadList
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun createSigner(key: Bytes48) = BLS381MessageSigner { messageHash, domain -> BLSSignature.wrap(pseudoSign(key, messageHash, domain)) }

class TestWire() : WireApiSub {
  val blockProc = DirectProcessor.create<SignedBeaconBlock>()
  val blockProcSink = blockProc.sink()

  val attProc = DirectProcessor.create<Attestation>()
  val attProcSink = attProc.sink()

  override fun sendProposedBlock(block: SignedBeaconBlock) {}
  override fun sendAttestation(attestation: Attestation) {}
  override fun inboundBlocksStream(): Publisher<SignedBeaconBlock> {
    return blockProc
  }

  override fun inboundAttestationsStream(): Publisher<Attestation> {
    return attProc
  }
}

/** Dummy SignedBeaconBlock with absent signature (when it cannot be recovered).*/
class DummySignedBeaconBlock(block: BeaconBlock): SignedBeaconBlock(block, null) {
  override fun getSignature(): BLSSignature = throw IllegalStateException()
}

object ObservableStates {
  val data = hashMapOf<Launcher,ObservableBeaconState>()
}

class Tester(contract: EmulatorContract, val recordEvents: Boolean = true) {
  companion object {
    fun createContract(genesisTime: Long, validatorCount: Int): EmulatorContract {
      val contract = EmulatorContract()
      val interopKeys = ValidatorKeys.InteropKeys()
      interopKeys.count = validatorCount
      contract.keys = listOf<ValidatorKeys>(interopKeys)
      contract.genesisTime = Date(genesisTime)
      return contract
    }
  }

  val spec = TestUtils.getBeaconChainSpec()
  val mdcControlledSchedulers = MDCControlledSchedulers()

  init {
    mdcControlledSchedulers.currentTime = contract.genesisTime.time - 1000
  }

  val events = mutableListOf<TestEvent>()

  val wireApi = TestWire()
  var testLauncher = createTestLauncher(spec, contract, wireApi, mdcControlledSchedulers)

  private val baseState: BeaconState?
    get() = ObservableStates.data[testLauncher]?.latestSlotState

  var currentSlot: SlotNumber
    get() = spec.get_current_slot(baseState, mdcControlledSchedulers.currentTime)
    set(value) {
      mdcControlledSchedulers.currentTime = spec.get_slot_start_time(
          baseState, value).millis.value
      if (recordEvents)
        events.add(SlotEvent(value.intValue))
    }

  val currentEpoch: EpochNumber
    get() = spec.compute_epoch_at_slot(currentSlot)

  val chainStorage: BeaconChainStorage
    get() {
      if (testLauncher.beaconChainStorage != null)
        return testLauncher.beaconChainStorage
      else
        return object: BeaconChainStorage {
          override fun getJustifiedStorage(): SingleValueSource<Checkpoint> {
            return object : SingleValueSource<Checkpoint> {
              override fun get(): Optional<Checkpoint> {
                return Optional.ofNullable(testLauncher.store.justifiedCheckpoint)
              }

              override fun set(value: Checkpoint?) {
                testLauncher.store.justifiedCheckpoint = value
              }

              override fun remove() {
                testLauncher.store.justifiedCheckpoint = null
              }

            }
          }

          override fun getFinalizedStorage(): SingleValueSource<Checkpoint> {
            return object : SingleValueSource<Checkpoint> {
              override fun get(): Optional<Checkpoint> {
                return Optional.ofNullable(testLauncher.store.finalizedCheckpoint)
              }

              override fun set(value: Checkpoint?) {
                testLauncher.store.finalizedCheckpoint = value
              }

              override fun remove() {
                testLauncher.store.finalizedCheckpoint = null
              }

            }
          }

          override fun getBlockStorage(): BeaconBlockStorage {
            return object : BeaconBlockStorage {
              override fun getSlotBlocks(slot: SlotNumber?): MutableList<Hash32> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun getMaxSlot(): SlotNumber {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun put(item: SignedBeaconBlock?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun put(key: Hash32, value: SignedBeaconBlock) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun getChildren(parent: Hash32?, limit: Int): MutableList<SignedBeaconBlock> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun remove(key: Hash32) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

              override fun get(key: Hash32): Optional<SignedBeaconBlock> {
                return testLauncher.store.getBlock(key).map(::DummySignedBeaconBlock)
              }

              override fun flush() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
              }

            }
          }

          override fun getStateStorage(): BeaconStateStorage {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

          override fun getTupleStorage(): BeaconTupleStorage {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

          override fun getBestJustifiedStorage(): SingleValueSource<Checkpoint> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

          override fun commit() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

          override fun getBlockHeaderStorage(): DataSource<Hash32, SignedBeaconBlockHeader> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

        }
    }

  val blockStorage get() = chainStorage.blockStorage
  val stateStorage get() = chainStorage.stateStorage
  fun root(a: BeaconBlock?): Hash32 = spec.hash_tree_root(a)

  private var _genesisState: BeaconState? = null
  val genesisState get() = _genesisState!!

  private fun createTestLauncher(spec: BeaconChainSpec, contract: EmulatorContract, wireApi: TestWire, mdcControlledSchedulers: MDCControlledSchedulers): Launcher {
    val signerFactory =
        if (spec.isBlsVerifyProofOfPossession)
          BLS381MessageSignerFactory {
            createSigner(it.public.encodedBytes)
          }
        else null
    val chainStart = ConfigUtils.createChainStart(contract, spec, signerFactory)
    val storageFactory = SSZBeaconChainStorageFactory(
        spec.objectHasher, SerializerFactory.createSSZ(spec.constants))
    val signer = Signer.Insecure()
    signer.keys = contract.keys
    val credentials = ConfigUtils.createCredentials(signer, false)

    val schedulers = mdcControlledSchedulers.createNew("val1")
    val launcher = Launcher(
        spec,
        SimpleDepositContract(chainStart, schedulers),
        null,
        wireApi,
        storageFactory,
        schedulers,
        BenchmarkController.NO_BENCHES,
        true)
    Flux.from(launcher.observableStateStream)
        .publishOn(schedulers.events().toReactor())
        .subscribe {
          ObservableStates.data[launcher] = it
        }
    mdcControlledSchedulers.currentTime = chainStart.time.millis.value
    if (ObservableStates.data[launcher]?.latestSlotState?.slot != SlotNumber.ZERO)
      throw IllegalStateException("No genesis state")
    else
      _genesisState = ObservableStates.data[launcher]?.latestSlotState
    return launcher
  }
}


class Store(val storage: BeaconChainStorage) {
  val cache = HashMap<Hash32, BeaconBlock>()

  fun put(root: Hash32, block: BeaconBlock) {
    cache[root] = block
  }

  fun get(root: Hash32): Optional<BeaconBlock> {
    return if (root in cache) {
      Optional.ofNullable(cache[root])
    } else {
      storage.blockStorage.get(root).map { it.message }
    }
  }

  fun getAncestor(root: Hash32, slot: SlotNumber): Hash32 {
    val block = get(root)
    require(block.isPresent) { "No such block $root" }
    return when {
      block.get().slot.greater(slot) -> getAncestor(block.get().parentRoot, slot)
      block.get().slot == slot -> root
      else -> Hash32.ZERO
    }
  }
}

class TestChain(val tester: Tester) {
  val attestationCache = ArrayList<Attestation>()
  val attester = BeaconChainAttesterImpl(tester.spec)

  val head: BeaconTuple
    get() {
      val state = ObservableStates.data[tester.testLauncher] //tester.testLauncher.beaconChain.recentlyProcessed
      if (state != null)
        return BeaconTuple.of(DummySignedBeaconBlock(state.head), state.latestSlotState)
      else
        throw IllegalStateException("No state")
    }

  fun addCheck(name: String, goldenValue: Any) {
    if (tester.recordEvents)
      tester.events.add(CheckEvent(mapOf(name to goldenValue)))
  }

  fun addHeadCheck(block: BeaconBlock) {
    addBlockCheck("head", block)
  }

  fun addBlockCheck(name: String, block: BeaconBlock) {
    addCheck(name, tester.root(block).toString())
  }

  fun mkBlock(slot: Int, parent: BeaconTuple? = null, attestations: List<Attestation>? = null,
              postProcess: ((BeaconBlock) -> BeaconBlock)? = null): BeaconTuple {
    val parentTuple = parent ?: head
    val atts = attestations ?: attestationCache
    val pBlock = createValidBlock(tester.spec,
        parentTuple, slot.toLong(), atts, postProcess)
    if (attestations == null) {
      attestationCache.clear()
    }
    return pBlock
  }

  fun sendBlock(block: SignedBeaconBlock) {
    tester.wireApi.blockProcSink.next(block)
    if (tester.recordEvents)
      tester.events.add(BlockEvent(StateTestUtils.serializeSignedBlock(block)))
  }

  fun sendAttestation(attestation: Attestation) {
    tester.wireApi.attProcSink.next(attestation)
    if (tester.recordEvents)
      tester.events.add(AttestationEvent(StateTestUtils.serializeAttestation(attestation)))
  }

  fun sendAttestations(attestations: List<Attestation>) {
    for (a in attestations) {
      sendAttestation(a)
    }
  }

  fun proposeBlock(slot: Int, parent: BeaconTuple? = null, attestations: List<Attestation>? = null): BeaconTuple {
    val block = mkBlock(slot, parent, attestations)
    sendBlock(block.signedBlock)
    return block
  }

  fun gatherAttestations(head: BeaconTuple? = null, slot: Int? = null) {
    val result = mkAttestations(head, slot)
    attestationCache.addAll(result)
  }

  fun getAttesters(head: BeaconTuple? = this.head, slot: Int = this.tester.currentSlot.intValue): List<ValidatorIndex> {
    val slotNumber = SlotNumber(slot)
    val currState = processSlots(head!!.state, slotNumber)
    val committees = tester.spec.get_crosslink_committees_at_slot(
        currState, slotNumber)
    return committees.flatMap { it.committee }
  }

  fun processSlots(state: BeaconState, slotNumber: SlotNumber): BeaconState {
    val mutableState = state.createMutableCopy()
    tester.spec.process_slots(mutableState, slotNumber)
    return mutableState.createImmutable()
  }

  fun mkAttestations(
      head: BeaconTuple? = null, slot: Int? = null,
      validators: Collection<ValidatorIndex>? = null,
      postProcess: ((Attestation) -> Attestation)? = null,
      postSign: ((Attestation) -> Attestation)? = null): ArrayList<Attestation> {
    val tupleToAttest = head ?: this.head
    val slotNumber = SlotNumber(slot ?: tester.currentSlot.intValue)

    val currState = processSlots(tupleToAttest.state, slotNumber)

    val committees = tester.spec.get_crosslink_committees_at_slot(
        currState, slotNumber)
    val validatorsSet = HashSet(validators ?: committees.flatMap { it.committee })
    val result = ArrayList<Attestation>()
    for (committee in committees) {
      for (vi in committee.committee) {
        if (vi !in validatorsSet)
          continue
        val a = attester.attest(vi, committee.index, currState,
            tupleToAttest.signedBlock.message)

        val b = if (postProcess != null) postProcess(a) else a

        val sig = createSigner(currState.validators[vi].pubKey).sign(
            tester.spec.hash_tree_root(b.data),
            tester.spec.get_domain(currState, SignatureDomains.BEACON_ATTESTER, b.data.target.epoch))

        val c = b.withSignature(sig)

        val d = if (postSign != null) postSign(c) else c

        result.add(d)
      }
    }
    return result
  }

  fun attestBlock(head: BeaconTuple? = null, slot: Int? = null) {
    sendAttestations(mkAttestations(head, slot))
  }

  private fun createValidBlock(spec: BeaconChainSpec, parent: BeaconTuple, slot: Long,
                               attestations: List<Attestation> = Collections.emptyList(),
                               postProcess: ((BeaconBlock) -> BeaconBlock)? = null): BeaconTuple {
    val slotNumber = SlotNumber.of(slot)
    val parentRoot = spec.hash_tree_root(parent.signedBlock.message)

    val preState = parent.state.createMutableCopy()
    spec.process_slots(preState, slotNumber)
    val slotState = preState.createImmutable()

    val emptyBody = BeaconBlockBody.getEmpty(spec.constants)
    val proposerIndex = spec.get_beacon_proposer_index(slotState)

    val signer = createSigner(slotState.validators[proposerIndex].pubKey)
    val randaoReveal = RandaoGenerator.getInstance(spec, signer)
        .reveal(spec.get_current_epoch(slotState), slotState)

    val body = BeaconBlockBody(
        randaoReveal,
        emptyBody.eth1Data,
        emptyBody.graffiti,
        emptyBody.proposerSlashings,
        emptyBody.attesterSlashings,
        ReadList.wrap(attestations, { i -> i }, spec.constants.maxAttestations.toLong()),
        emptyBody.deposits,
        emptyBody.voluntaryExits,
        spec.constants
    )

    val block = BeaconBlock.Builder.fromBlock(spec._empty_block)
        .withSlot(slotNumber)
        .withParentRoot(parentRoot)
        .withBody(body)
        .build()

    val parentState = BeaconStateExImpl(spec.state_transition(
        slotState.createMutableCopy(),
        SignedBeaconBlock(block, BLSSignature.ZERO), false).createImmutable(), TransitionType.BLOCK)
    //println(parentState.toString())

    val newBlock = block.withStateRoot(spec.hash_tree_root(parentState))!!

    val processedBlock = postProcess?.invoke(newBlock) ?: newBlock

    val signedBlock = BeaconBlockSigner.getInstance(spec, signer).sign(processedBlock, slotState)

    return BeaconTuple.of(signedBlock, parentState)
  }
}