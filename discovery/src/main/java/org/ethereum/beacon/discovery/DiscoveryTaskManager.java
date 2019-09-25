package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.enr.NodeStatus;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.schedulers.Scheduler;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.ethereum.beacon.discovery.NodeContext.DEFAULT_DISTANCE;
import static org.ethereum.beacon.discovery.enr.NodeRecordV5.NODE_ID_FUNCTION;

/** Manages recurrent node check task(s) */
public class DiscoveryTaskManager {
  private static final int MS_IN_SECOND = 1000;
  private static final int STATUS_EXPIRATION_SECONDS = 600;
  private static final int TASK_INTERVAL_SECONDS = 10;
  private static final int RETRY_TIMEOUT_SECONDS = 60;
  private static final int MAX_RETRIES = 10;
  private final Scheduler scheduler;
  private final Bytes32 homeNodeId;
  private final NodeConnectTasks nodeConnectTasks;
  private NodeTable nodeTable;
  /**
   * Checks whether {@link org.ethereum.beacon.discovery.enr.NodeRecord} is ready for alive status
   * check. Plus, marks records as DEAD if there were a lot of unsuccessful retries to get reply
   * from node.
   *
   * <p>We don't need to recheck the node if
   *
   * <ul>
   *   <li>Node is ACTIVE and last connection retry was not too much time ago
   *   <li>Number of unsuccessful retries exceeds settings
   *   <li>Node is not ACTIVE but last connection retry was "seconds ago"
   * </ul>
   *
   * <p>In all other cases method returns true, meaning node is ready for ping check
   */
  private final Predicate<NodeRecordInfo> READY_FOR_PING =
      nodeRecord -> {
        long currentTime = System.currentTimeMillis() / MS_IN_SECOND;
        if (nodeRecord.getStatus() == NodeStatus.ACTIVE
            && nodeRecord.getLastRetry() > currentTime - STATUS_EXPIRATION_SECONDS) {
          return false; // no need to rediscover
        }
        if (nodeRecord.getRetry() >= MAX_RETRIES) {
          nodeTable.save(
              new NodeRecordInfo(
                  nodeRecord.getNode(), nodeRecord.getLastRetry(), NodeStatus.DEAD, 0));
          return false;
        }
        if ((currentTime - nodeRecord.getLastRetry())
            < (nodeRecord.getRetry() * nodeRecord.getRetry())) {
          return false; // too early for retry
        }

        return true;
      };
  private boolean resetDead = false;

  /**
   * @param discoveryManager Discovery manager
   * @param nodeTable Ethereum node records storage
   * @param homeNode Home node
   * @param scheduler scheduler to run recurrent tasks on
   * @param resetDead Whether to reset dead status of the nodes. If set to true, resets its status
   *     at startup and sets number of used retries to 0
   */
  public DiscoveryTaskManager(
      DiscoveryManager discoveryManager,
      NodeTable nodeTable,
      NodeRecordV5 homeNode,
      Scheduler scheduler,
      boolean resetDead) {
    this.scheduler = scheduler;
    this.nodeTable = nodeTable;
    this.homeNodeId = NODE_ID_FUNCTION.apply(homeNode);
    this.nodeConnectTasks = new NodeConnectTasks(discoveryManager, scheduler, Duration.ofSeconds(RETRY_TIMEOUT_SECONDS));
    this.resetDead = resetDead;
  }

  public void start() {
    scheduler.executeAtFixedRate(
        Duration.ZERO, Duration.ofSeconds(TASK_INTERVAL_SECONDS), this::recurrentTask);
  }

  private void recurrentTask() {
    List<NodeRecordInfo> nodes = nodeTable.findClosestNodes(homeNodeId, DEFAULT_DISTANCE);
    Stream<NodeRecordInfo> closestNodes = nodes.stream();
    if (resetDead) {
      closestNodes =
          closestNodes.map(
              nodeRecordInfo ->
                  new NodeRecordInfo(
                      nodeRecordInfo.getNode(),
                      nodeRecordInfo.getLastRetry(),
                      NodeStatus.SLEEP,
                      0));
      resetDead = false;
    }
    closestNodes
        .filter(READY_FOR_PING)
        .forEach(
            nodeRecord ->
                nodeConnectTasks.add(
                    nodeRecord,
                    () ->
                        nodeTable.save(
                            new NodeRecordInfo(
                                nodeRecord.getNode(),
                                System.currentTimeMillis() / MS_IN_SECOND,
                                NodeStatus.ACTIVE,
                                0)),
                    () ->
                        nodeTable.save(
                            new NodeRecordInfo(
                                nodeRecord.getNode(),
                                System.currentTimeMillis() / MS_IN_SECOND,
                                NodeStatus.SLEEP,
                                (nodeRecord.getRetry() + 1)))));
  }
}