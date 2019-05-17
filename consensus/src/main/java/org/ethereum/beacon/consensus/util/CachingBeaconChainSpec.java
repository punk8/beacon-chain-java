package org.ethereum.beacon.consensus.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.ethereum.beacon.consensus.BeaconChainSpecImpl;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.util.cache.Cache;
import org.ethereum.beacon.util.cache.CacheFactory;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class CachingBeaconChainSpec extends BeaconChainSpecImpl {

  private final Cache<Pair<List<? extends UInt64>, Bytes32>, List<UInt64>> shufflerCache;
  private final Cache<Object, Hash32> hashTreeRootCache;
  private final Cache<Object, Hash32> signedRootCache;
  private final Cache<Triplet<Hash32, EpochNumber, ShardNumber>, List<ValidatorIndex>> crosslinkCommitteesCache;
  private final Cache<EpochNumber, List<ValidatorIndex>> activeValidatorsCache;
  private final Cache<EpochNumber, Gwei> totalActiveBalanceCache;
  private final Cache<Triplet<Hash32, AttestationData, Bitfield>, List<ValidatorIndex>> attestingIndicesCache;

  private ValidatorIndex maxCachedIndex = ValidatorIndex.ZERO;
  private final Map<BLSPubkey, ValidatorIndex> pubkeyToIndexCache = new ConcurrentHashMap<>();

  private final boolean cacheEnabled;

  public CachingBeaconChainSpec(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession,
      boolean cacheEnabled) {
    super(constants, hashFunction, objectHasher, blsVerify, blsVerifyProofOfPossession);
    this.cacheEnabled = cacheEnabled;

    CacheFactory factory = CacheFactory.create(cacheEnabled);
    this.shufflerCache = factory.createLRUCache(1024);
    this.hashTreeRootCache = factory.createLRUCache(1024);
    this.signedRootCache = factory.createLRUCache(1024);
    this.crosslinkCommitteesCache = factory.createLRUCache(128);
    this.activeValidatorsCache = factory.createLRUCache(32);
    this.totalActiveBalanceCache = factory.createLRUCache(32);
    this.attestingIndicesCache = factory.createLRUCache(1024);
  }

  public CachingBeaconChainSpec(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession) {
    this(constants, hashFunction, objectHasher, blsVerify, blsVerifyProofOfPossession, true);
  }

  @Override
  public List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    return shufflerCache.get(
        Pair.with(indices, seed),
        k -> super.get_permuted_list(k.getValue0(), k.getValue1()));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return hashTreeRootCache.get(object, super::hash_tree_root);
  }

  @Override
  public Hash32 signing_root(Object object) {
    return signedRootCache.get(object, super::signing_root);
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    if (!cacheEnabled) {
      return super.get_validator_index_by_pubkey(state, pubkey);
    }

    // relying on the fact that at index -> validator is invariant
    if (state.getValidatorRegistry().size().greater(maxCachedIndex)) {
      for (ValidatorIndex index : maxCachedIndex.iterateTo(state.getValidatorRegistry().size())) {
        pubkeyToIndexCache.put(state.getValidatorRegistry().get(index).getPubKey(), index);
      }
      maxCachedIndex = state.getValidatorRegistry().size();
    }
    return pubkeyToIndexCache.getOrDefault(pubkey, ValidatorIndex.MAX);
  }

  @Override
  public List<ValidatorIndex> get_crosslink_committee(BeaconState state, EpochNumber epoch, ShardNumber shard) {
    return crosslinkCommitteesCache.get(Triplet.with(hash(state), epoch, shard),
        s -> super.get_crosslink_committee(state, epoch, shard));
  }

  @Override
  public List<ValidatorIndex> get_active_validator_indices(BeaconState state, EpochNumber epoch) {
    return activeValidatorsCache.get(epoch, e -> super.get_active_validator_indices(state, epoch));
  }

  @Override
  public Gwei get_total_active_balance(BeaconState state) {
    return totalActiveBalanceCache.get(
        get_current_epoch(state), e -> super.get_total_active_balance(state));
  }

  @Override
  public List<ValidatorIndex> get_attesting_indices(
      BeaconState state, AttestationData attestation_data, Bitfield bitfield) {
    return attestingIndicesCache.get(
        Triplet.with(hash(state), attestation_data, bitfield),
        e -> super.get_attesting_indices(state, attestation_data, bitfield));
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  /**
   * Do not use {@link #hash_tree_root(Object)} directly as it causes false counts in benchmarks.
   */
  private Hash32 hash(Object object) {
    return hashTreeRootCache.get(object, (o) -> getObjectHasher().getHash(o));
  }
}