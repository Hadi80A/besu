/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.pos.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;

import java.io.IOException;

/**
 * Utility class for object serialization using JSON (Jackson).
 * Useful for non-consensus critical data, configuration, or debug structures.
 */
public class SerializeUtil {

    // ObjectMapper is thread-safe and expensive to create; reuse a single instance.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SerializeUtil() {
        // Prevent instantiation
    }

    /**
     * Serializes an object to a JSON byte array wrapped in Tuweni Bytes.
     *
     * @param o the object to serialize
     * @return the serialized bytes
     * @throws JsonProcessingException if serialization fails
     */
    public static Bytes toBytes(final Object o) throws JsonProcessingException {
        return Bytes.wrap(OBJECT_MAPPER.writeValueAsBytes(o));
    }

    /**
     * Deserializes bytes containing JSON back into an object.
     *
     * @param b the bytes to deserialize
     * @param clazz the target class
     * @param <T> the type of the object
     * @return the deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T toObject(final Bytes b, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(b.toArray(), clazz);
    }
}