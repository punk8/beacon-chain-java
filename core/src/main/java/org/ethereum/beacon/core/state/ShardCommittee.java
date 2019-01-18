package org.ethereum.beacon.core.state;

import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.List;

/** Validator committee assigned to a certain shard. */
public class ShardCommittee {

  /** Validator indices. */
  private final List<UInt24> committee;
  /** Shard number. */
  private final UInt64 shard;

  public ShardCommittee(List<UInt24> committee, UInt64 shard) {
    this.shard = shard;
    this.committee = committee;
  }

  public List<UInt24> getCommittee() {
    return committee;
  }

  public UInt64 getShard() {
    return shard;
  }
}
