package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.ValidatorDutiesService;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;

public class AttestationController extends SyncRestController {
  private final ValidatorDutiesService service;
  private final BeaconChainSpec spec;
  private ObservableBeaconState observableBeaconState = null;

  public AttestationController(
      ObservableStateProcessor stateProcessor,
      ValidatorDutiesService service,
      SyncManager syncManager,
      BeaconChainSpec spec) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.observableBeaconState = observableBeaconState;
            });
    this.service = service;
    this.spec = spec;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processGetRequestImpl(this::produceAttestationResponse);
  }

  private Object produceAttestationResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(getParamString("slot", params)));
      BLSPubkey validatorPubkey =
          BLSPubkey.wrap(Bytes48.fromHexStringStrict(getParamString("validator_pubkey", params)));
      Long pocBit =
          Long.valueOf(
              getParamString("poc_bit", params)); // XXX: Proof of custody is a stub at Phase 0
      ShardNumber shard = ShardNumber.of(UInt64.valueOf(getParamString("shard", params)));

      ValidatorIndex validatorIndex =
          spec.get_validator_index_by_pubkey(
              observableBeaconState.getLatestSlotState().createMutableCopy(), validatorPubkey);
      Attestation attestation =
          service.prepareAttestation(slot, validatorIndex, shard, observableBeaconState);
      IndexedAttestation indexedAttestation =
          spec.convert_to_indexed(
              observableBeaconState.getLatestSlotState().createMutableCopy(), attestation);
      return BeaconBlockConverter.presentIndexedAttestation(indexedAttestation);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
