package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.MiscParameters;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt64;

public class MiscParametersData implements MiscParameters {

  @JsonProperty("SHARD_COUNT")
  private String SHARD_COUNT;
  @JsonProperty("TARGET_COMMITTEE_SIZE")
  private String TARGET_COMMITTEE_SIZE;
  @JsonProperty("MAX_BALANCE_CHURN_QUOTIENT")
  private String MAX_BALANCE_CHURN_QUOTIENT;
  @JsonProperty("BEACON_CHAIN_SHARD_NUMBER")
  private String BEACON_CHAIN_SHARD_NUMBER;
  @JsonProperty("MAX_INDICES_PER_ATTESTATION")
  private String MAX_INDICES_PER_ATTESTATION;
  @JsonProperty("MIN_PER_EPOCH_CHURN_LIMIT")
  private String MIN_PER_EPOCH_CHURN_LIMIT;
  @JsonProperty("CHURN_LIMIT_QUOTIENT")
  private String CHURN_LIMIT_QUOTIENT;
  @JsonProperty("BASE_REWARDS_PER_EPOCH")
  private String BASE_REWARDS_PER_EPOCH;
  @JsonProperty("MAX_EXIT_DEQUEUES_PER_EPOCH")
  private String MAX_EXIT_DEQUEUES_PER_EPOCH;

  @Override
  @JsonIgnore
  public ShardNumber getShardCount() {
    return new ShardNumber(UInt64.valueOf(getSHARD_COUNT()));
  }

  @Override
  @JsonIgnore
  public ValidatorIndex getTargetCommitteeSize() {
    return new ValidatorIndex(UInt64.valueOf(getTARGET_COMMITTEE_SIZE()));
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxBalanceChurnQuotient() {
    return UInt64.valueOf(getMAX_BALANCE_CHURN_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public ShardNumber getBeaconChainShardNumber() {
    return ShardNumber.of(UInt64.valueOf(getBEACON_CHAIN_SHARD_NUMBER()));
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxIndicesPerAttestation() {
    return UInt64.valueOf(getMAX_INDICES_PER_ATTESTATION());
  }

  @Override
  @JsonIgnore
  public UInt64 getMinPerEpochChurnLimit() {
    return UInt64.valueOf(getMIN_PER_EPOCH_CHURN_LIMIT());
  }

  @Override
  @JsonIgnore
  public UInt64 getChurnLimitQuotient() {
    return UInt64.valueOf(getCHURN_LIMIT_QUOTIENT());
  }

  @Override
  public UInt64 getBaseRewardsPerEpoch() {
    return UInt64.valueOf(getBASE_REWARDS_PER_EPOCH());
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxExitDequesPerEpoch() {
    return UInt64.valueOf(getMAX_EXIT_DEQUEUES_PER_EPOCH());
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSHARD_COUNT() {
    return SHARD_COUNT;
  }

  public void setSHARD_COUNT(String SHARD_COUNT) {
    this.SHARD_COUNT = SHARD_COUNT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getTARGET_COMMITTEE_SIZE() {
    return TARGET_COMMITTEE_SIZE;
  }

  public void setTARGET_COMMITTEE_SIZE(String TARGET_COMMITTEE_SIZE) {
    this.TARGET_COMMITTEE_SIZE = TARGET_COMMITTEE_SIZE;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_BALANCE_CHURN_QUOTIENT() {
    return MAX_BALANCE_CHURN_QUOTIENT;
  }

  public void setMAX_BALANCE_CHURN_QUOTIENT(String MAX_BALANCE_CHURN_QUOTIENT) {
    this.MAX_BALANCE_CHURN_QUOTIENT = MAX_BALANCE_CHURN_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getBEACON_CHAIN_SHARD_NUMBER() {
    return BEACON_CHAIN_SHARD_NUMBER;
  }

  public void setBEACON_CHAIN_SHARD_NUMBER(String BEACON_CHAIN_SHARD_NUMBER) {
    this.BEACON_CHAIN_SHARD_NUMBER = BEACON_CHAIN_SHARD_NUMBER;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_INDICES_PER_ATTESTATION() {
    return MAX_INDICES_PER_ATTESTATION;
  }

  public void setMAX_INDICES_PER_ATTESTATION(String MAX_INDICES_PER_ATTESTATION) {
    this.MAX_INDICES_PER_ATTESTATION = MAX_INDICES_PER_ATTESTATION;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_PER_EPOCH_CHURN_LIMIT() {
    return MIN_PER_EPOCH_CHURN_LIMIT;
  }

  public void setMIN_PER_EPOCH_CHURN_LIMIT(String MIN_PER_EPOCH_CHURN_LIMIT) {
    this.MIN_PER_EPOCH_CHURN_LIMIT = MIN_PER_EPOCH_CHURN_LIMIT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getCHURN_LIMIT_QUOTIENT() {
    return CHURN_LIMIT_QUOTIENT;
  }

  public void setCHURN_LIMIT_QUOTIENT(String CHURN_LIMIT_QUOTIENT) {
    this.CHURN_LIMIT_QUOTIENT = CHURN_LIMIT_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getBASE_REWARDS_PER_EPOCH() {
    return BASE_REWARDS_PER_EPOCH;
  }

  public void setBASE_REWARDS_PER_EPOCH(String BASE_REWARDS_PER_EPOCH) {
    this.BASE_REWARDS_PER_EPOCH = BASE_REWARDS_PER_EPOCH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_EXIT_DEQUEUES_PER_EPOCH() {
    return MAX_EXIT_DEQUEUES_PER_EPOCH;
  }

  public void setMAX_EXIT_DEQUEUES_PER_EPOCH(String MAX_EXIT_DEQUEUES_PER_EPOCH) {
    this.MAX_EXIT_DEQUEUES_PER_EPOCH = MAX_EXIT_DEQUEUES_PER_EPOCH;
  }
}
