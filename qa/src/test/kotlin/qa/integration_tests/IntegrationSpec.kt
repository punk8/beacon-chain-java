package qa.integration_tests

import io.kotlintest.Description
import io.kotlintest.TestCase
import io.kotlintest.specs.AnnotationSpec
import org.ethereum.beacon.core.operations.attestation.AttestationData
import org.ethereum.beacon.core.state.Checkpoint
import org.ethereum.beacon.core.types.CommitteeIndex
import org.ethereum.beacon.core.types.EpochNumber
import org.ethereum.beacon.core.types.SlotNumber
import org.ethereum.beacon.emulator.config.main.ValidatorKeys
import org.ethereum.beacon.emulator.config.main.conract.EmulatorContract
import tech.pegasys.artemis.ethereum.core.Hash32
import java.util.*


open class IntegrationSpec: AnnotationSpec() {
    override fun testCases(): List<TestCase> {
        return super.testCases().map {
            if (it.name.startsWith("f_")) {
                val name = "f:" + it.name.subSequence(2, it.name.length)
                TestCase(Description(it.description.parents, name), it.spec, it.test, it.source, it.type, it.config)
            } else {
                it
            }
        }
    }
    val genesisTime = Date(2019, 8, 24, 0, 0, 0).time - 10000

    fun AttestationData.withBeaconBlockRoot(root: Hash32) =
            AttestationData(slot, index, root, source, target)

  fun AttestationData.withSlot(slot: SlotNumber) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withIndex(index: CommitteeIndex) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withSource(source: Checkpoint) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withTarget(target: Checkpoint) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun Checkpoint.withEpoch(epoch: EpochNumber) = Checkpoint(epoch, root)
  fun Checkpoint.withRoot(root: Hash32) = Checkpoint(epoch, root)

  fun createContract(genesisTime: Long, validatorCount: Int): EmulatorContract {
        val contract = EmulatorContract()
        val interopKeys = ValidatorKeys.InteropKeys()
        interopKeys.count = validatorCount
        contract.keys = listOf<ValidatorKeys>(interopKeys)
        contract.genesisTime = Date(genesisTime)
        return contract
    }
}