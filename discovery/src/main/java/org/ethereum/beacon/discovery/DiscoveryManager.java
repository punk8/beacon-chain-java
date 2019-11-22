package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecord;

import java.util.concurrent.CompletableFuture;

/**
 * Discovery Manager, top interface for peer discovery mechanism as described at <a
 * href="https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md">https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md</a>
 */
public interface DiscoveryManager {

  void start();

  void stop();

  /**
   * Initiates FINDNODE with node `nodeRecord`
   *
   * @param nodeRecord Ethereum Node record
   * @param distance Distance to search for
   * @return Future which is fired when reply is received or fails in timeout/not successful
   *     handshake/bad message exchange.
   */
  CompletableFuture<Void> findNodes(NodeRecord nodeRecord, int distance);

  /**
   * Initiates PING with node `nodeRecord`
   *
   * @param nodeRecord Ethereum Node record
   * @return Future which is fired when reply is received or fails in timeout/not successful
   *     handshake/bad message exchange.
   */
  CompletableFuture<Void> ping(NodeRecord nodeRecord);
}
