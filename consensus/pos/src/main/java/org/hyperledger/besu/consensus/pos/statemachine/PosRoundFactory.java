package org.hyperledger.besu.consensus.pos.statemachine;

import org.hyperledger.besu.consensus.common.bft.BftProtocolSchedule;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.payload.MessageFactory;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.MinedBlockObserver;
import org.hyperledger.besu.util.Subscribers;

public class PosRoundFactory {
    public PosRoundFactory(BftFinalState finalState, ProtocolContext protocolContext, BftProtocolSchedule bftProtocolSchedule, Subscribers<MinedBlockObserver> minedBlockObservers, MessageValidatorFactory messageValidatorFactory, MessageFactory messageFactory, PosExtraDataCodec bftExtraDataCodec) {
    }
}
