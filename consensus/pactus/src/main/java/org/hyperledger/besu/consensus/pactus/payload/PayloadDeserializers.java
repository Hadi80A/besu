// PayloadDeserializers.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Utility class for deserializing Pactus consensus payloads.
 */
public class PayloadDeserializers {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static ProposePayload deserializeProposePayload(byte[] data) throws IOException {
    return objectMapper.readValue(data, ProposePayload.class);
  }

  public static PreCommitPayload deserializePreCommitPayload(byte[] data) throws IOException {
    return objectMapper.readValue(data, PreCommitPayload.class);
  }

  public static CommitPayload deserializeCommitPayload(byte[] data) throws IOException {
    return objectMapper.readValue(data, CommitPayload.class);
  }

  public static CertificatePayload deserializeCertificatePayload(byte[] data) throws IOException {
    return objectMapper.readValue(data, CertificatePayload.class);
  }

  public static <T> T genericDeserialize(byte[] data, Class<T> type) throws IOException {
    return objectMapper.readValue(data, type);
  }
}
