package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;

public interface SpecConstants
    extends NonConfigurableConstants,
        InitialValues,
        MiscParameters,
        StateListLengths,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters,
        GweiValues,
        ChainStartParameters {

  @Override
  default EpochNumber getGenesisEpoch() {
    return getGenesisSlot().dividedBy(getSlotsPerEpoch());
  }
}
