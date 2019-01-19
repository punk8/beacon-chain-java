package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Misc beacon chain constants.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#misc">Misc</a>
 *     in the spec.
 */
public interface MiscParameters {

  UInt64 SHARD_COUNT = UInt64.valueOf(1 << 10); // 1024 shards
  UInt24 TARGET_COMMITTEE_SIZE = UInt24.valueOf(1 << 7); // 128 validators
  Ether EJECTION_BALANCE = Ether.valueOf(1 << 4); // 16 ETH
  UInt64 MAX_BALANCE_CHURN_QUOTIENT = UInt64.valueOf(1 << 5); // 32
  UInt64 BEACON_CHAIN_SHARD_NUMBER = UInt64.MAX_VALUE; // (1 << 64) - 1
  int MAX_CASPER_VOTES = 1 << 10; // 1024 votes
  UInt64 LATEST_BLOCK_ROOTS_LENGTH = UInt64.valueOf(1 << 13); // 8192 block roots
  UInt64 LATEST_RANDAO_MIXES_LENGTH = UInt64.valueOf(1 << 13); // 8192 randao mixes
  UInt64 LATEST_PENALIZED_EXIT_LENGTH = UInt64.valueOf(1 << 13); // 8192 epochs
  UInt64 MAX_WITHDRAWALS_PER_EPOCH = UInt64.valueOf(1 << 2); // 4

  /* Values defined in the spec. */

  UInt64 getShardCount();

  UInt24 getTargetCommitteeSize();

  Ether getEjectionBalance();

  UInt64 getMaxBalanceChurnQuotient();

  UInt64 getBeaconChainShardNumber();

  int getMaxCasperVotes();

  UInt64 getLatestBlockRootsLength();

  UInt64 getLatestRandaoMixesLength();

  UInt64 getLatestPenalizedExitLength();

  UInt64 getMaxWithdrawalsPerEpoch();
}
