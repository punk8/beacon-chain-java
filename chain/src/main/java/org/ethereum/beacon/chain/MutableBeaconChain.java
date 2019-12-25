package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public interface MutableBeaconChain extends BeaconChain {

  enum ImportResult {
    OK,
    ExistingBlock,
    NoParent,
    ExpiredBlock,
    InvalidBlock,
    StateMismatch,
    UnexpectedError
  }

  /**
   * Inserts new block into a chain.
   *
   * @param block a block.
   * @return whether a block was inserted or not.
   */
  ImportResult insert(SignedBeaconBlock block);
}
