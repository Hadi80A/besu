package org.hyperledger.besu.consensus.pos.validation;

import org.hyperledger.besu.consensus.common.bft.BftProtocolSchedule;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.ethereum.ProtocolContext;

public class MessageValidatorFactory {
    public MessageValidatorFactory(ProposerSelector proposerSelector, BftProtocolSchedule bftProtocolSchedule, ProtocolContext protocolContext, PosExtraDataCodec bftExtraDataCodec) {
    }
}
