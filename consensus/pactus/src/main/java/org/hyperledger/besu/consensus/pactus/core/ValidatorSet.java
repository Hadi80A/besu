package org.hyperledger.besu.consensus.pactus.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the set of validators participating in Pactus consensus.
 * Handles stake updates, sortition, and committee rotation.
 */
public class ValidatorSet {

  // Map of validator ID to Validator object
  private final Map<String, Validator> validatorMap = new ConcurrentHashMap<>();

  public void addOrUpdateValidator(Validator validator) {
    validatorMap.put(validator.getId(), validator);
  }

  public Optional<Validator> getValidator(String id) {
    return Optional.ofNullable(validatorMap.get(id));
  }

  public Collection<Validator> getAllValidators() {
    return validatorMap.values();
  }

  public List<Validator> getCommitteeValidators() {
    return validatorMap.values().stream()
        .filter(Validator::isInCommittee)
        .collect(Collectors.toList());
  }

  public List<Validator> getNonCommitteeValidators() {
    return validatorMap.values().stream()
        .filter(v -> !v.isInCommittee())
        .collect(Collectors.toList());
  }

  public List<Validator> getEligibleForSortition(long currentHeight) {
    return getNonCommitteeValidators().stream()
        .filter(v -> v.canRunSortition(currentHeight))
        .collect(Collectors.toList());
  }

  public Optional<Validator> getOldestCommitteeMember(long currentHeight) {
    return getCommitteeValidators().stream()
        .filter(v -> v.shouldBeRemoved(currentHeight))
        .min(Comparator.comparingLong(Validator::getLastJoinedHeight));
  }

  public boolean isInCommittee(String id) {
    Validator v = validatorMap.get(id);
    return v != null && v.isInCommittee();
  }

  public void markAsJoined(String id, long currentHeight) {
    Validator v = validatorMap.get(id);
    if (v != null) {
      v.setInCommittee(true);
      v.setLastJoinedHeight(currentHeight);
    }
  }

  public void markAsRemoved(String id) {
    Validator v = validatorMap.get(id);
    if (v != null) {
      v.setInCommittee(false);
    }
  }

  public void updateStake(String id, long amount, long height) {
    Validator v = validatorMap.get(id);
    if (v != null) {
      v.getStakeInfo().setStakedAmount(amount);
      v.getStakeInfo().setActivationHeight(height);
    }
  }

  public int committeeSize() {
    return (int) validatorMap.values().stream().filter(Validator::isInCommittee).count();
  }

  public int totalSize() {
    return validatorMap.size();
  }
}
