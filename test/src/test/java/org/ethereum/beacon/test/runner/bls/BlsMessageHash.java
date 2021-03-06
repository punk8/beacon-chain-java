package org.ethereum.beacon.test.runner.bls;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP2;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.bls.milagro.MilagroMessageMapper;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsMessageHashCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertLists;

/**
 * TestRunner for {@link BlsMessageHashCase}
 *
 * <p>Hash message as G2 point (uncompressed)
 * Test format description: <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/msg_hash_g2_uncompressed.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/msg_hash_g2_uncompressed.md</a>
 */
public class BlsMessageHash implements Runner {
  private BlsMessageHashCase testCase;
  private BeaconChainSpec spec;

  public BlsMessageHash(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsMessageHashCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsMessageHashCase as input!");
    }
    this.testCase = (BlsMessageHashCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {

    MessageParameters messageParameters =
        MessageParameters.create(
            Hash32.wrap(Bytes32.fromHexString(testCase.getInput().getMessage())),
            Bytes8.fromHexStringLenient(testCase.getInput().getDomain()));
    MilagroMessageMapper milagroMessageMapper = new MilagroMessageMapper();
    ECP2 point = milagroMessageMapper.map(messageParameters);

    Optional<String> compareX = assertLists(testCase.getOutput().get(0), serialize(point.getX()));
    Optional<String> compareY = assertLists(testCase.getOutput().get(1), serialize(point.getY()));
    Optional<String> compareZ = assertLists(testCase.getOutput().get(2), serialize(point.getz()));

    if (!compareX.isPresent() && !compareY.isPresent() && !compareZ.isPresent()) {
      return Optional.empty();
    }
    StringBuilder errors = new StringBuilder();
    if (compareX.isPresent()) {
      errors.append("X coordinate error:\n");
      errors.append(compareX.get());
      errors.append("\n");
    }
    if (compareY.isPresent()) {
      errors.append("Y coordinate error:\n");
      errors.append(compareY.get());
      errors.append("\n");
    }
    if (compareZ.isPresent()) {
      errors.append("Z coordinate error:\n");
      errors.append(compareZ.get());
      errors.append("\n");
    }

    return Optional.of(errors.toString());
  }

  private List<String> serialize(FP2 coordinate) {
    List<String> res = new ArrayList<>();
    res.add("0x" + coordinate.getA().toString());
    res.add("0x" + coordinate.getB().toString());

    return res;
  }
}
