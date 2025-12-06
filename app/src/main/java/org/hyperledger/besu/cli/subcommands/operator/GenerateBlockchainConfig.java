/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.cli.subcommands.operator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.Resources;
import jakarta.validation.constraints.NotBlank;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.cli.DefaultCommandValues;
import org.hyperledger.besu.cli.util.VersionProvider;
import org.hyperledger.besu.config.GenesisConfig;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.config.JsonGenesisConfigOptions;
import org.hyperledger.besu.config.JsonUtil;
import org.hyperledger.besu.consensus.ibft.IbftExtraDataCodec;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SECPPrivateKey;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.crypto.SignatureAlgorithmType;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@Command(
        name = "generate-blockchain-config",
        description = "Generate node keypairs and genesis file with RLP encoded extra data.",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
class GenerateBlockchainConfig implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateBlockchainConfig.class);

    private final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
            Suppliers.memoize(SignatureAlgorithmFactory::getInstance);

    @NotBlank
    @Option(
            required = true,
            names = "--config-file",
            paramLabel = DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP,
            description = "Configuration file.",
            arity = "1..1")
    private final File configurationFile = null;

    @NotBlank
    @Option(
            required = true,
            names = "--to",
            paramLabel = DefaultCommandValues.MANDATORY_DIRECTORY_FORMAT_HELP,
            description = "Directory to write output files to.",
            arity = "1..1")
    private final File outputDirectory = null;

    @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"})
    @Option(
            names = "--genesis-file-name",
            paramLabel = DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP,
            description = "Name of the genesis file. (default: ${DEFAULT-VALUE})",
            arity = "1..1")
    private String genesisFileName = "genesis.json";

    @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"})
    @Option(
            names = "--private-key-file-name",
            paramLabel = DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP,
            description = "Name of the private key file. (default: ${DEFAULT-VALUE})",
            arity = "1..1")
    private String privateKeyFileName = "key.priv";

    @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"})
    @Option(
            names = "--public-key-file-name",
            paramLabel = DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP,
            description = "Name of the public key file. (default: ${DEFAULT-VALUE})",
            arity = "1..1")
    private String publicKeyFileName = "key.pub";

    @ParentCommand
    private OperatorSubCommand parentCommand;

    private ObjectNode operatorConfig;
    private ObjectNode genesisConfig;
    private ObjectNode blockchainConfig;
    private ObjectNode nodesConfig;
    private boolean generateNodesKeys;
    private final List<Address> addressesForGenesisExtraData = new ArrayList<>();
    private final List<SECPPublicKey> pksForGenesisExtraData = new ArrayList<>();
    private Path keysDirectory;

    @Override
    public void run() {
        checkPreconditions();
        generateBlockchainConfig();
    }

    private void checkPreconditions() {
        checkNotNull(parentCommand);
        checkNotNull(parentCommand.parentCommand);
        if (isAnyDuplicate(genesisFileName, publicKeyFileName, privateKeyFileName)) {
            throw new IllegalArgumentException("Output file paths must be unique.");
        }
    }

    /** Generates output directory with all required configuration files. */
    private void generateBlockchainConfig() {
        try {
            handleOutputDirectory();
            parseConfig();
            processEcCurve();

            // Removed BLS logic from here as ECDSA (DSS) is standard.

            if (generateNodesKeys) {
                generateNodesKeys();
            } else {
                importPublicKeysFromConfig();
            }
            processExtraData();
            populateInitialStakesIntoAlloc();
            writeGenesisFile(outputDirectory, genesisFileName, genesisConfig);
        } catch (final IOException e) {
            LOG.error("An error occurred while trying to generate network configuration.", e);
        }
    }

    private void importPublicKeysFromConfig() {
        LOG.info("Importing public keys from configuration.");
        JsonUtil.getArrayNode(nodesConfig, "keys")
                .ifPresent(keys -> keys.forEach(this::importPublicKey));
    }

    private void importPublicKey(final JsonNode publicKeyJson) {
        if (publicKeyJson.getNodeType() != JsonNodeType.STRING) {
            throw new IllegalArgumentException(
                    "Invalid key json of type: " + publicKeyJson.getNodeType());
        }
        final String publicKeyText = publicKeyJson.asText();

        try {
            final SECPPublicKey publicKey =
                    SIGNATURE_ALGORITHM.get().createPublicKey(Bytes.fromHexString(publicKeyText));

            if (!SIGNATURE_ALGORITHM.get().isValidPublicKey(publicKey)) {
                throw new IllegalArgumentException(
                        publicKeyText
                                + " is not a valid public key for elliptic curve "
                                + SIGNATURE_ALGORITHM.get().getCurveName());
            }

            writeKeypair(publicKey, null);
            LOG.info("Public key imported from configuration.({})", publicKey.toString());
        } catch (final IOException e) {
            LOG.error("An error occurred while trying to import node public key.", e);
        }
    }

    private void generateNodesKeys() {
        final int nodesCount = JsonUtil.getInt(nodesConfig, "count", 0);
        LOG.info("Generating {} nodes keys.", nodesCount);
        IntStream.range(0, nodesCount).forEach(this::generateNodeKeypair);
    }

    private void generateNodeKeypair(final int node) {
        try {
            LOG.info("Generating keypair for node {}.", node);
            final KeyPair keyPair = SIGNATURE_ALGORITHM.get().generateKeyPair();
            writeKeypair(keyPair.getPublicKey(), keyPair.getPrivateKey());

        } catch (final IOException e) {
            LOG.error("An error occurred while trying to generate node keypair.", e);
        }
    }

    private void writeKeypair(final SECPPublicKey publicKey, final SECPPrivateKey privateKey)
            throws IOException {
        final Address nodeAddress = Util.publicKeyToAddress(publicKey);
        addressesForGenesisExtraData.add(nodeAddress);
        pksForGenesisExtraData.add(publicKey);
        final Path nodeDirectoryPath = keysDirectory.resolve(nodeAddress.toString());
        Files.createDirectory(nodeDirectoryPath);
        createFileAndWrite(nodeDirectoryPath, publicKeyFileName, publicKey.toString());
        if (privateKey != null) {
            createFileAndWrite(nodeDirectoryPath, privateKeyFileName, privateKey.toString());
        }
    }

    private void processExtraData() {
        final ObjectNode configNode =
                JsonUtil.getObjectNode(genesisConfig, "config")
                        .orElseThrow(
                                () -> new IllegalArgumentException("Missing config section in config file"));

        final JsonGenesisConfigOptions genesisConfigOptions =
                JsonGenesisConfigOptions.fromJsonObject(configNode);
        if (genesisConfigOptions.isIbft2()) {
            LOG.info("Generating IBFT extra data.");
            final String extraData =
                    IbftExtraDataCodec.encodeFromAddresses(addressesForGenesisExtraData).toString();
            genesisConfig.put("extraData", extraData);
        } else if (genesisConfigOptions.isQbft()) {
            LOG.info("Generating QBFT extra data.");
            final String extraData =
                    QbftExtraDataCodec.encodeFromAddresses(addressesForGenesisExtraData).toString();
            genesisConfig.put("extraData", extraData);
        } else if (genesisConfigOptions.isPos()) {
            LOG.info("Generating PoS extra data.");

            final String extraData =
                    PosExtraDataCodec.encodeFromAddresses(addressesForGenesisExtraData).toString();

            genesisConfig.put("extraData", extraData);
        }
    }

    private void createFileAndWrite(final Path directory, final String fileName, final String content)
            throws IOException {
        final Path filePath = directory.resolve(fileName);
        Files.write(filePath, content.getBytes(UTF_8), StandardOpenOption.CREATE_NEW);
    }

    private void parseConfig() throws IOException {
        final String configString =
                Resources.toString(configurationFile.toPath().toUri().toURL(), UTF_8);
        final ObjectNode root = JsonUtil.objectNodeFromString(configString);
        operatorConfig = root;
        genesisConfig =
                JsonUtil.getObjectNode(operatorConfig, "genesis").orElse(JsonUtil.createEmptyObjectNode());
        blockchainConfig =
                JsonUtil.getObjectNode(operatorConfig, "blockchain")
                        .orElse(JsonUtil.createEmptyObjectNode());
        nodesConfig =
                JsonUtil.getObjectNode(blockchainConfig, "nodes").orElse(JsonUtil.createEmptyObjectNode());
        generateNodesKeys = JsonUtil.getBoolean(nodesConfig, "generate", false);
    }

    private void processEcCurve() {
        GenesisConfigOptions options = GenesisConfig.fromConfig(genesisConfig).getConfigOptions();
        Optional<String> ecCurve = options.getEcCurve();

        if (ecCurve.isEmpty()) {
            SignatureAlgorithmFactory.setInstance(SignatureAlgorithmType.createDefault());
            return;
        }

        try {
            SignatureAlgorithmFactory.setInstance(SignatureAlgorithmType.create(ecCurve.get()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid parameter for ecCurve in genesis config: " + e.getMessage());
        }
    }

    private void handleOutputDirectory() throws IOException {
        checkNotNull(outputDirectory);
        final Path outputDirectoryPath = outputDirectory.toPath();
        if (outputDirectory.exists()
                && outputDirectory.isDirectory()
                && outputDirectory.list() != null
                && outputDirectory.list().length > 0) {
            throw new IllegalArgumentException("Output directory already exists.");
        } else if (!outputDirectory.exists()) {
            Files.createDirectory(outputDirectoryPath);
        }
        keysDirectory = outputDirectoryPath.resolve("keys");
        Files.createDirectory(keysDirectory);
    }

    private void writeGenesisFile(
            final File directory, final String fileName, final ObjectNode genesis) throws IOException {
        LOG.info("Writing genesis file.");
        Files.write(
                directory.toPath().resolve(fileName),
                JsonUtil.getJson(genesis).getBytes(UTF_8),
                StandardOpenOption.CREATE_NEW);
    }

    private void populateInitialStakesIntoAlloc() {
        try {
            final ObjectNode configNode =
                    JsonUtil.getObjectNode(genesisConfig, "config")
                            .orElseThrow(() -> new IllegalArgumentException("Missing config section in config file"));
            final ObjectNode posNode =
                    JsonUtil.getObjectNode(configNode, "pos").orElse(JsonUtil.createEmptyObjectNode());
            final String contractAddressRaw = JsonUtil.getString(posNode, "contractaddress", "");
            if (contractAddressRaw == null || contractAddressRaw.isEmpty()) {
                LOG.info("No pos.contractaddress present; skipping initial stake population.");
                return;
            }
            final String contractAddressKey = normalizeAddressKey(contractAddressRaw);
            final Optional<ArrayNode> initialStakeArrayOpt = JsonUtil.getArrayNode(posNode, "initialstake");
            if (initialStakeArrayOpt.isEmpty()) {
                LOG.info("No pos.initialstake array; nothing to populate.");
                return;
            }
            final JsonNode initialStakeArray = initialStakeArrayOpt.get();
            final ObjectNode allocNode = JsonUtil.getObjectNode(genesisConfig, "alloc")
                    .orElseGet(() -> genesisConfig.putObject("alloc"));
            final ObjectNode contractAllocNode = JsonUtil.getObjectNode(allocNode, contractAddressKey)
                    .orElseGet(() -> allocNode.putObject(contractAddressKey));
            final ObjectNode storageNode = JsonUtil.getObjectNode(contractAllocNode, "storage")
                    .orElseGet(() -> contractAllocNode.putObject("storage"));
            final long mappingSlotIndex = 0L;
            final int stakesCount = initialStakeArray.size();
            final int addressesCount = addressesForGenesisExtraData.size();
            if (stakesCount == 0) {
                LOG.info("pos.initialstake array is empty; nothing to populate.");
                return;
            }
            if (stakesCount != addressesCount) {
                LOG.warn(
                        "initialstake array length ({}) does not match number of addressesForGenesisExtraData ({}). Will process min(lengths).",
                        stakesCount, addressesCount);
            }
            final int toProcess = Math.min(stakesCount, addressesCount);
            for (int i = 0; i < toProcess; i++) {
                try {
                    final JsonNode stakeNode = initialStakeArray.get(i);
                    final BigInteger stakeBI = stakeNode.isTextual()
                            ? new BigInteger(stakeNode.asText())
                            : BigInteger.valueOf(stakeNode.asLong());
                    final BigInteger stakeToWrite = stakeBI.multiply(BigInteger.TEN.pow(18));
                    final Address addr = addressesForGenesisExtraData.get(i);
                    final Bytes32 mappingSlot = computeMappingSlotForAddress(addr, mappingSlotIndex);
                    final Bytes32 stakeValueBytes = uintToBytes32(stakeToWrite);
                    final String stakeSlotHexKey = "0x" + mappingSlot.toHexString().substring(2).toLowerCase();
                    final String stakeSlotHexValue = "0x" + stakeValueBytes.toHexString().substring(2).toLowerCase();
                    storageNode.put(stakeSlotHexKey, stakeSlotHexValue);
                    final String mappingSlotHexNoPrefix = mappingSlot.toHexString().substring(2);
                    final BigInteger mappingSlotBI = new BigInteger(mappingSlotHexNoPrefix, 16);
                    final BigInteger existsSlotBI = mappingSlotBI.add(BigInteger.ONE);
                    final Bytes32 existsSlotKeyBytes = uintToBytes32(existsSlotBI);
                    final String existsSlotHexKey = "0x" + existsSlotKeyBytes.toHexString().substring(2).toLowerCase();
                    final Bytes32 existsValueBytes = uintToBytes32(BigInteger.ONE);
                    final String existsSlotHexValue = "0x" + existsValueBytes.toHexString().substring(2).toLowerCase();
                    storageNode.put(existsSlotHexKey, existsSlotHexValue);
                    LOG.info("Populated initial stake for index {} addr {}: {} -> {}, {} -> {}",
                            i, addr.toString(), stakeSlotHexKey, stakeSlotHexValue, existsSlotHexKey, existsSlotHexValue);
                } catch (final Exception e) {
                    LOG.error("Failed to populate initial stake for index {}: {}", i, e.getMessage());
                }
            }
            if (stakesCount > addressesCount) {
                LOG.warn("Ignored {} stake entries because there are fewer addresses than stakes.", stakesCount - addressesCount);
            } else if (addressesCount > stakesCount) {
                LOG.warn("Only populated {} of {} addresses (fewer stakes than addresses).", stakesCount, addressesCount);
            }
        } catch (final Exception e) {
            LOG.error("Error populating initial stakes into alloc: {}", e.getMessage());
        }
    }

    private static String normalizeAddressKey(final String raw) {
        return raw.startsWith("0x") || raw.startsWith("0X") ? raw.substring(2) : raw;
    }

    private static Bytes32 uintToBytes32(final BigInteger v) {
        final byte[] src = v.toByteArray();
        final byte[] dest = new byte[32];
        final int srcOffset = Math.max(0, src.length - 32);
        final int length = Math.min(src.length, 32);
        System.arraycopy(src, srcOffset, dest, 32 - length, length);
        return Bytes32.wrap(dest);
    }

    private static Bytes32 computeMappingSlotForAddress(final Address addr, final long mappingSlotIndex) {
        final Bytes addrBytes = Bytes.wrap(addr.toArray());
        final Bytes32 paddedKey = Bytes32.leftPad(addrBytes);
        final Bytes32 slotIndexBytes = uintToBytes32(BigInteger.valueOf(mappingSlotIndex));
        final Bytes concatenated = Bytes.concatenate(paddedKey, slotIndexBytes);
        return org.hyperledger.besu.crypto.Hash.keccak256(Bytes.wrap(concatenated.toArray()));
    }

    private static boolean isAnyDuplicate(final String... values) {
        final Set<String> set = new HashSet<>();
        for (final String value : values) {
            if (!set.add(value)) {
                return true;
            }
        }
        return false;
    }
}