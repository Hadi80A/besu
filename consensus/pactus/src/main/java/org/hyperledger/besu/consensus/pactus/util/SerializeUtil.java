package org.hyperledger.besu.consensus.pactus.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;

import java.io.IOException;

public class SerializeUtil {

    public static Bytes toBytes(Object o) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return Bytes.wrap(objectMapper.writeValueAsBytes(o));
    }

    public static  <T> T toObject(Bytes b,Class<T> clazz) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(b.toArray(),clazz);
    }
}
