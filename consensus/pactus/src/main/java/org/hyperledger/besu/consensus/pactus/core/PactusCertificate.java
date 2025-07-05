package org.hyperledger.besu.consensus.pactus.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.util.SerializeUtil;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;
import org.hyperledger.besu.plugin.data.Signature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PactusCertificate {
    private int height;
    private int round;
    private List<Validator> committers;
    private List<Validator> absentees;
    private Signature signature; //TODO


    public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {
        rlpOutput.writeInt(height);
        rlpOutput.writeInt(round);
        rlpOutput.startList();
        BiConsumer<Validator, RLPOutput> consumer = (validator, rlpOut) -> {
            try {
                rlpOut.writeBytes(SerializeUtil.toBytes(validator));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
        rlpOutput.writeList(committers, consumer);
        rlpOutput.writeList(absentees, consumer);
        rlpOutput.endList();
        rlpOutput.writeBytes(SerializeUtil.toBytes(signature));
    }
    public static PactusCertificate readFrom(RLPInput rlpInput) throws IOException {
        int height= rlpInput.readInt();
        int round=rlpInput.readInt();
        rlpInput.enterList();
        Function<RLPInput, Validator> valueReader = rlpInput1 ->
        {
            try {
                return (Validator) SerializeUtil.toObject(rlpInput1.readBytes(), Validator.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        List<Validator> validators=rlpInput.readList(valueReader);
        List<Validator> absentees=rlpInput.readList(valueReader);
        rlpInput.leaveList();
        Signature sign= (Signature) SerializeUtil.toObject(rlpInput.readBytes(), Signature.class);
        return new PactusCertificate(height,round,validators,absentees,sign);
    }

}
