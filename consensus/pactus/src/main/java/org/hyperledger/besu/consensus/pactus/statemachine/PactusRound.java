
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.PactusBlockImporter;
import org.hyperledger.besu.consensus.pactus.PactusProtocolSchedule;
import org.hyperledger.besu.consensus.pactus.core.*;
import org.hyperledger.besu.consensus.pactus.factory.PactusRoundFactory;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.PreCommit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Prepare;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Proposal;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.*;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.plugin.data.Signature;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pactus round. */
public class PactusRound {

  private static final Logger LOG = LoggerFactory.getLogger(PactusRound.class);


  /** The Round state. */
  protected final RoundState roundState;

  /** The Block creator. */
  protected final PactusBlockCreator blockCreator;

  /** The Protocol context. */
  protected final PactusBlockInterface blockInterface;

  /** The Protocol schedule. */
  protected final PactusProtocolSchedule protocolSchedule;

  private final NodeKey nodeKey;
  private final PactusRoundFactory.MessageFactory messageFactory; // used only to create stored local msgs
  private final PactusMessageTransmitter transmitter;
  private final PactusBlockHeader parentHeader;
  private final ValidatorSet validators;
  private final PactusProposerSelector proposerSelector;
  private Address currentProposer;

  /**
   * Instantiates a new Pactus round.
   *
   * @param roundState the round state
   * @param blockCreator the block creator
   * @param blockInterface the block interface
   * @param protocolSchedule the protocol schedule
   * @param nodeKey the node key
   * @param messageFactory the message factory
   * @param transmitter the transmitter
   * @param roundTimer the round timer
   * @param parentHeader the parent header
   */
  public PactusRound(
          final RoundState roundState,
          final PactusBlockCreator blockCreator,
          final PactusBlockInterface blockInterface,
          final PactusProtocolSchedule protocolSchedule,
          final NodeKey nodeKey,
          final PactusRoundFactory.MessageFactory messageFactory,
          final PactusMessageTransmitter transmitter,
          final RoundTimer roundTimer,
          final PactusBlockHeader parentHeader,
          final ValidatorSet validators,
          final PactusProposerSelector proposerSelector
          ) {
    this.roundState = roundState;
    this.blockCreator = blockCreator;
    this.blockInterface = blockInterface;
    this.protocolSchedule = protocolSchedule;
    this.nodeKey = nodeKey;
    this.messageFactory = messageFactory;
    this.transmitter = transmitter;
    this.parentHeader = parentHeader;
    this.validators=validators;
    this.proposerSelector=proposerSelector;
    roundTimer.startTimer(getRoundIdentifier());
  }

  /**
   * Gets round identifier.
   *
   * @return the round identifier
   */
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundState.getRoundIdentifier();
  }

  /**
   * Create a block
   *
   * @param headerTimeStampSeconds of the block
   * @return a Block
   */
  public PactusBlock createBlock(final long headerTimeStampSeconds) {
    LOG.debug("Creating proposed block. round={}", roundState.getRoundIdentifier());
    int roundNumber=roundState.getRoundIdentifier().getRoundNumber();
    if (roundState.getRoundIdentifier().getRoundNumber()>0){
      List<Validator> committers= validators.getCommitteeValidators();
      List<Validator> absentees= validators.getNonCommitteeValidators();
      int round = 0;
      int height = 0;
      if(roundNumber>1){
//        round=
      }
      PactusCertificate  certificate= createCertificate(round+1,height+1,committers,absentees);
    }
    return blockCreator.createBlock(headerTimeStampSeconds, this.parentHeader,proposerSelector.getCurrentProposer());
  }

  private PactusCertificate createCertificate(int round, int height, List<Validator> committers, List<Validator> absentees, Signature sign){
    return new PactusCertificate(height, round, committers, absentees, sign);
  }
  public void startRound(final long headerTimeStampSeconds){
//      currentProposer = proposerSelector.selectProposerForRound(roundState.getRoundIdentifier());

  }
  /**
   * Start round with.
   */
//  public void startRoundWith(
//          final RoundChangeArtifacts roundChangeArtifacts, final long headerTimestamp) {
//    final Optional<PreparedCertificate> bestPreparedCertificate =
//            roundChangeArtifacts.getBestPreparedPeer();
//
//    final PactusBlock blockToPublish;
//    if (bestPreparedCertificate.isEmpty()) {
//      LOG.debug("Sending proposal with new block. round={}", roundState.getRoundIdentifier());
//      blockToPublish = blockCreator.createBlock(headerTimestamp, this.parentHeader);
//    } else {
//      LOG.debug(
//              "Sending proposal from PreparedCertificate. round={}", roundState.getRoundIdentifier());
//      PactusBlock preparedBlock = bestPreparedCertificate.get().getBlock();
//      blockToPublish =
//              blockInterface.replaceRoundInBlock(
//                      preparedBlock, roundState.getRoundIdentifier().getRoundNumber());
//    }
//
//    LOG.debug(" proposal - new/prepared block hash : {}", blockToPublish.getHash());
//
//    updateStateWithProposalAndTransmit(
//            blockToPublish,
//            roundChangeArtifacts.getRoundChanges(),
//            bestPreparedCertificate.map(PreparedCertificate::getPrepares).orElse(emptyList()));
//  }

  private SignedData<ProposePayload> createProposePayload(PactusBlock block) {
    int round=0;
    int height=0;
    if(Objects.nonNull(block.getPactusCertificate())){
      round=block.getPactusCertificate().getRound();
      height=block.getPactusCertificate().getHeight();
    }
    ProposePayload proposePayload=messageFactory.createProposePayload(round,height);
    return createSignedData(proposePayload);

  }

  private<M extends Payload> SignedData<M> createSignedData(M payload){
    SECPSignature sign = nodeKey.sign(payload.hashForSignature());
    return SignedData.create(payload, sign);
  }
  /**
   * Update state with proposal and transmit.
   *
   * @param block the block
   */
  protected void createProposalAndTransmit(final PactusBlock block) {
    final Proposal proposal;
    try {
      var proposePayload =createProposePayload(block);

      proposal = messageFactory.createProposal(proposePayload,block);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed Proposal, waiting for next round.", e);
      return;
    }
    transmitter.multicastProposal(proposal);
  }

  /**
   * Handle proposal message.
   *
   * @param msg the msg
   */
  public void handleProposalMessage(final Proposal msg) {
    LOG.debug("Received a proposal message. round={}. author={}", roundState.getRoundIdentifier(),
            msg.getAuthor());
    final PactusBlock block = msg.getPactusBlock();
    if (validateProposer(msg.getSignedPayload())){
      LOG.debug("Valid a proposal message.");
      roundState.setProposalMessage(msg);
      sendPrepare(block);
    }else {
      LOG.debug("Invalid a proposal message.");
    }
  }
  private boolean validateProposer(SignedData<ProposePayload> payload){
      return payload.getAuthor().equals(proposerSelector.getCurrentProposer()) &&
              payload.getPayload().getRound() == roundState.getRound() &&
              payload.getPayload().getHeight() == roundState.getHeight();

  }

  private void sendPrepare(final PactusBlock block) {
    LOG.debug("Sending prepare message. round={}", roundState.getRoundIdentifier());
    try {
      PreparePayload unsigned= messageFactory.createPreparePayload(block);
      SignedData<PreparePayload> signedData= createSignedData(unsigned);
      final Prepare localPrepareMessage = messageFactory.createPrepare(signedData);
      roundState.addPrepareMessage(localPrepareMessage);
      transmitter.multicastPrepare(localPrepareMessage);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed Prepare; {}", e.getMessage());
    }
  }

  public void handlePrepareMessage(final Prepare msg) {
    LOG.debug("Received a prepare message. round={}. author={}", roundState.getRoundIdentifier(),
            msg.getAuthor());
    roundState.addPrepareMessage(msg);
    if(validateBlockHash(msg.getSignedPayload().getPayload().getBlockHash()) &&
            checkThreshold(roundState.getPrepareMessages()) &&
            validateHeightAndRound(msg.getSignedPayload().getPayload())
    ) {
      sendPrecommit(roundState.getProposedBlock());
    }else {
      LOG.debug("Invalid a prepare message.");
    }
  }
  private boolean checkThreshold(Set<?> msg){
    return msg.size()>=roundState.getQuorum();
  }


  private boolean validateHeightAndRound(PactusPayload pactusPayload) {
    return pactusPayload.getHeight()==roundState.getHeight() &&
    pactusPayload.getRound()==roundState.getRound();
  }
  private boolean validateBlockHash(Hash blockHash) {
    PactusBlock block=roundState.getProposedBlock();
    return blockHash.equals(block.getHash());
  }
  private void sendPrecommit(final PactusBlock block) {
    LOG.debug("Sending precommit message. round={}", roundState.getRoundIdentifier());
    try {
      PreCommitPayload unsigned= messageFactory.createPreCommitPayload(block);
      SignedData<PreCommitPayload> signedData= createSignedData(unsigned);
      final PreCommit localPreCommitMessage = messageFactory.createPreCommit(signedData);
      roundState.addPreCommitMessage(localPreCommitMessage);
      transmitter.multicastPreCommit(localPreCommitMessage);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed PreCommit; {}", e.getMessage());
    }
  }

  public void handlePreCommitMessage(final PreCommit msg) {
    LOG.debug("Received a prepare message. round={}. author={}", roundState.getRoundIdentifier(),
            msg.getAuthor());
    roundState.addPreCommitMessage(msg);
    if(validateBlockHash(msg.getSignedPayload().getPayload().getBlockHash()) &&
            checkThreshold(roundState.getPreCommitMessages()) &&
            validateHeightAndRound(msg.getSignedPayload().getPayload())
    ) {
      sendCommit(roundState.getProposedBlock());
    }else {
      LOG.debug("Invalid a PreCommit message.");
    }
  }

  private void sendCommit(final PactusBlock block) {
    LOG.debug("Sending Commit message. round={}", roundState.getRoundIdentifier());
    try {
      CommitPayload unsigned= messageFactory.createCommitPayload(block);
      SignedData<CommitPayload> signedData= createSignedData(unsigned);
      final Commit localCommitMessage = messageFactory.createCommit(signedData);
      roundState.addCommitMessage(localCommitMessage);
      transmitter.multicastCommit(localCommitMessage);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed Commit; {}", e.getMessage());
    }
  }

  public void handleCommitMessage(final Commit msg) {
    LOG.debug("Received a commit message. round={}. author={}", roundState.getRoundIdentifier(),
            msg.getAuthor());
    roundState.addCommitMessage(msg);
    importBlockToChain();
  }

  private void importBlockToChain() {
    final PactusBlock blockToImport =
            blockCreator.createSealedBlock(
                    roundState.getProposedBlock(),
                    roundState.getRoundIdentifier().getRoundNumber(),
                    roundState.getCommitSeals());

    final long blockNumber = blockToImport.getHeader().getNumber();
    if (getRoundIdentifier().getRoundNumber() > 0) {
      LOG.info(
              "Importing proposed block to chain. round={}, hash={}",
              getRoundIdentifier(),
              blockToImport.getHash());
    } else {
      LOG.debug(
              "Importing proposed block to chain. round={}, hash={}",
              getRoundIdentifier(),
              blockToImport.getHash());
    }

    final PactusBlockImporter blockImporter =
            protocolSchedule.getBlockImporter(blockToImport.getHeader());
    final boolean result = blockImporter.importBlock(blockToImport);

    if (!result) {
      LOG.error(
              "Failed to import proposed block to chain. block={} blockHeader={}",
              blockNumber,
              blockToImport.getHeader());
    } else {
      notifyNewBlockListeners(blockToImport);
    }
  }

  private SECPSignature createCommitSeal(final PactusBlock block) {
    final PactusBlock commitBlock = createCommitBlock(block);
    final Hash commitHash = commitBlock.getHash();
    return nodeKey.sign(commitHash);
  }

  private PactusBlock createCommitBlock(final PactusBlock block) {
    return blockInterface.replaceRoundInBlock(block, getRoundIdentifier().getRoundNumber());
  }

  private void notifyNewBlockListeners(final PactusBlock block) {
    observers.forEach(obs -> obs.blockMined(block));
  }
}

//package org.hyperledger.besu.consensus.pactus.statemachine;
//
//import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
//import org.hyperledger.besu.consensus.pactus.factory.PactusRoundFactory.MessageFactory;
//import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;
//import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;
//import org.hyperledger.besu.consensus.pactus.payload.CommitPayload;
//import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Represents a single round of consensus at a given block height in Pactus protocol.
// * Manages proposal creation, message broadcast, and vote tracking.
// */
//public class PactusRound {
//
//  private final long height;
//  private final int round;
//  private final String localValidatorId;
//
//  private final MessageFactory messageFactory;
//  private final PactusMessageTransmitter messageTransmitter;
//
//  private boolean proposalSent = false;
//  private PactusBlock proposedPactusBlock;
//
//  private final Map<String, PreCommitPayload> preCommits = new HashMap<>();
//  private final Map<String, CommitPayload> commits = new HashMap<>();
//
//  public PactusRound(
//      final long height,
//      final int round,
//      final String localValidatorId,
//      final MessageFactory messageFactory,
//      final PactusMessageTransmitter messageTransmitter) {
//
//    this.height = height;
//    this.round = round;
//    this.localValidatorId = localValidatorId;
//    this.messageFactory = messageFactory;
//    this.messageTransmitter = messageTransmitter;
//  }
//
//  /**
//   * Called when the local node is proposer for the round.
//   */
//  public void proposeBlock(PactusBlock pactusBlock, String signature) {
//    if (proposalSent) return;
//
//    this.proposedPactusBlock = pactusBlock;
//    ProposePayload payload = messageFactory.createProposePayload(pactusBlock, round, signature);
//    messageTransmitter.multicastProposal(payload.toString()); // You may serialize to JSON
//    this.proposalSent = true;
//  }
//
//  /**
//   * Records a received pre-commit vote.
//   */
//  public void addPreCommitVote(PreCommitPayload payload) {
//    preCommits.put(payload.getValidatorId(), payload);
//  }
//
//  /**
//   * Records a received commit vote.
//   */
//  public void addCommitVote(CommitPayload payload) {
//    commits.put(payload.getValidatorId(), payload);
//  }
//
//  public int countPreCommits() {
//    return preCommits.size();
//  }
//
//  public int countCommits() {
//    return commits.size();
//  }
//
//  public boolean hasQuorum(int totalValidators) {
//    return countCommits() >= (2 * totalValidators / 3 + 1);
//  }
//
//  public long getHeight() {
//    return height;
//  }
//
//  public int getRound() {
//    return round;
//  }
//
//  public PactusBlock getProposedBlock() {
//    return proposedPactusBlock;
//  }
//
//  public boolean isProposalSent() {
//    return proposalSent;
//  }
//
//  public List<CommitPayload> getCommitPayloads() {
//    return List.copyOf(commits.values());
//  }
//}
