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
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.util.cache.Cache;
import org.ethereum.beacon.util.cache.CacheFactory;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.uint.UInt64;

public class CachingBeaconChainSpec extends BeaconChainSpecImpl {

  private static class Caches {
    private Cache<Pair<List<? extends UInt64>, Bytes32>, List<UInt64>> shufflerCache;
    private Cache<Object, Hash32> hashTreeRootCache;
    private Cache<Hash32, List<ValidatorIndex>> activeValidatorsCache;
    private Cache<Hash32, List<ValidatorIndex>> crosslinkCommitteesCache;
    private Cache<Hash32, Gwei> totalActiveBalanceCache;
    private Cache<Hash32, List<ValidatorIndex>> attestingIndicesCache;

    private ValidatorIndex maxCachedIndex = ValidatorIndex.ZERO;
    private final Map<BLSPubkey, ValidatorIndex> pubkeyToIndexCache = new ConcurrentHashMap<>();

    private Caches(CacheFactory factory) {
      this.shufflerCache = factory.createLRUCache(128);
      this.hashTreeRootCache = factory.createLRUCache(32);
      this.crosslinkCommitteesCache = factory.createLRUCache(128);
      this.activeValidatorsCache = factory.createLRUCache(32);
      this.totalActiveBalanceCache = factory.createLRUCache(32);
      this.attestingIndicesCache = factory.createLRUCache(1024);
    }
  }
  
  protected Caches caches;

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
    this.caches = new Caches(factory);
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
    return caches.shufflerCache.get(
        Pair.with(indices, seed),
        k -> super.get_permuted_list(k.getValue0(), k.getValue1()));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return caches.hashTreeRootCache.get(object, super::hash_tree_root);
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    if (!cacheEnabled) {
      return super.get_validator_index_by_pubkey(state, pubkey);
    }

    // relying on the fact that at index -> validator is invariant
    if (state.getValidatorRegistry().size().greater(caches.maxCachedIndex)) {
      for (ValidatorIndex index : caches.maxCachedIndex.iterateTo(state.getValidatorRegistry().size())) {
        caches.pubkeyToIndexCache.put(state.getValidatorRegistry().get(index).getPubKey(), index);
      }
      caches.maxCachedIndex = state.getValidatorRegistry().size();
    }
    return caches.pubkeyToIndexCache.getOrDefault(pubkey, ValidatorIndex.MAX);
  }

  @Override
  public List<ValidatorIndex> get_crosslink_committee(BeaconState state, EpochNumber epoch, ShardNumber shard) {
    Hash32 digest = getDigest(
        objectHash(state.getValidatorRegistry()),
        objectHash(state.getLatestRandaoMixes()),
        epoch.toBytes8(),
        shard.toBytes8()
    );
    return caches.crosslinkCommitteesCache.get(
        digest, s -> super.get_crosslink_committee(state, epoch, shard));
  }

  @Override
  public List<ValidatorIndex> get_active_validator_indices(BeaconState state, EpochNumber epoch) {
    Hash32 digest = getDigest(objectHash(state.getValidatorRegistry()), epoch.toBytes8());
    return caches.activeValidatorsCache.get(
        digest, e -> super.get_active_validator_indices(state, epoch));
  }

  @Override
  public Gwei get_total_active_balance(BeaconState state) {
    Hash32 digest = getDigest(
        objectHash(state.getValidatorRegistry()), get_current_epoch(state).toBytes8());
    return caches.totalActiveBalanceCache.get(digest, e -> super.get_total_active_balance(state));
  }

  @Override
  public List<ValidatorIndex> get_attesting_indices(
      BeaconState state, AttestationData attestation_data, Bitfield bitfield) {
    Hash32 digest = getDigest(
        objectHash(state.getValidatorRegistry()),
        objectHash(state.getLatestRandaoMixes()),
        attestation_data.getTargetEpoch().toBytes8(),
        attestation_data.getShard().toBytes8(),
        bitfield
    );
    return caches.attestingIndicesCache.get(
        digest, e -> super.get_attesting_indices(state, attestation_data, bitfield));
  }

  /**
   * This function introduced in order to avoid {@link #hash_tree_root(Object)} calls not related to
   * state processing to keep benchmarks counting clean.
   *
   * @param object an object.
   * @return calculated hash.
   */
  private Hash32 objectHash(Object object) {
    return getObjectHasher().getHash(object);
  }

  private Hash32 getDigest(BytesValue... identity) {
    return Hashes.sha256(BytesValues.concatenate(identity));
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public Caches getCaches() {
    return caches;
  }
}
