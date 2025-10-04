package org.hyperledger.besu.consensus.pos.core;

import org.hyperledger.besu.datatypes.Address;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the set of validators participating in Pactus consensus.
 * Handles stake updates, sortition, and committee rotation.
 */
public class NodeSet {

  // Map of validator ID to Node object
  private final Map<Address, Node> validatorMap = new ConcurrentHashMap<>();

  public void addOrUpdateNode(Node node) {
    validatorMap.put(node.getAddress(), node);
  }

  public Optional<Node> getNode(Address address) {
    return Optional.ofNullable(validatorMap.get(address));
  }

  public Collection<Node> getAllNodes() {
    return validatorMap.values();
  }

  public List<Node> getCommitteeNodes() {
    return validatorMap.values().stream()
        .filter(Node::isInCommittee)
        .collect(Collectors.toList());
  }

  public List<Node> getNonCommitteeNodes() {
    return validatorMap.values().stream()
        .filter(v -> !v.isInCommittee())
        .collect(Collectors.toList());
  }

  public Node getNodeByIndex(int index) {
      return new ArrayList<>(validatorMap.values()).get(index);
  }

//  public List<Node> getEligibleForSortition(long currentHeight) {
//    return getNonCommitteeNodes().stream()
//        .filter(v -> v.canRunSortition(currentHeight))
//        .collect(Collectors.toList());
//  }
//
//  public Optional<Node> getOldestCommitteeMember(long currentHeight) {
//    return getCommitteeNodes().stream()
//        .filter(v -> v.shouldBeRemoved(currentHeight))
//        .min(Comparator.comparingLong(Node::getLastJoinedHeight));
//  }

//  public boolean isInCommittee(String id) {
//    Node v = validatorMap.get(id);
//    return v != null && v.isInCommittee();
//  }
//
//  public void markAsJoined(String id, long currentHeight) {
//    Node v = validatorMap.get(id);
//    if (v != null) {
//      v.setInCommittee(true);
//      v.setLastJoinedHeight(currentHeight);
//    }
//  }

//  public void markAsRemoved(String id) {
//    Node v = validatorMap.get(id);
//    if (v != null) {
//      v.setInCommittee(false);
//    }
//  }

//  public void updateStake(String id, long amount, long height) {
//    Node v = validatorMap.get(id);
//    if (v != null) {
//      v.getStakeInfo().setStakedAmount(amount);
//      v.getStakeInfo().setActivationHeight(height);
//    }
//  }

  public int committeeSize() {
    return (int) validatorMap.values().stream().filter(Node::isInCommittee).count();
  }

  public int totalSize() {
    return validatorMap.size();
  }
}
