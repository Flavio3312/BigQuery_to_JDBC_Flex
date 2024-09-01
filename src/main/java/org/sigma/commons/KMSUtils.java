package org.sigma.commons;




import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities to decrypt using KMS key. */
public class KMSUtils {

    /* Logger for class. */
    private static final Logger LOG = LoggerFactory.getLogger(KMSUtils.class);
    private static final Pattern KEYNAME_PATTERN =
            Pattern.compile(
                    "projects/([^/]+)/locations/([a-zA-Z0-9_-]{1,63})/keyRings/"
                            + "[a-zA-Z0-9_-]{1,63}/cryptoKeys/[a-zA-Z0-9_-]{1,63}");

    public static ValueProvider<String> maybeDecrypt(String unencrypted, String kmsKey) {
        if (kmsKey == null || kmsKey.isEmpty()) {
            LOG.info("KMS Key is not specified. Not decrypting.");
            return StaticValueProvider.of(unencrypted);
        }

        if (!isKmsKey(kmsKey)) {
            IllegalArgumentException exception =
                    new IllegalArgumentException("Provided KMS Key %s is invalid");
            throw new RuntimeException(exception);
        }

        return new ValueProvider<>() {
            @Override
            public String get() {
                String decrypted;
                try {
                    decrypted = decryptWithKMS(unencrypted /*value*/, kmsKey /*key*/);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return decrypted;
            }

            @Override
            public boolean isAccessible() {
                return true;
            }
        };
    }

    private static boolean isKmsKey(String kmsKey) {
        return KEYNAME_PATTERN.matcher(kmsKey).matches();
    }

    /**
     * Uses the GCP KMS client to decrypt an encrypted value using a KMS key of the form
     * projects/{gcp_project}/locations/{key_region}/keyRings/{key_ring}/cryptoKeys/{kms_key_name} The
     * encrypted value should be a base64 encrypted string which has been encrypted using the KMS
     * encrypt API call. See <a
     * href="https://cloud.google.com/kms/docs/reference/rest/v1/projects.locations.keyRings.cryptoKeys/encrypt">
     * this KMS API Encrypt Link</a>.
     */
    private static String decryptWithKMS(String encryptedValue, String kmsKey) throws IOException {
        /*
         * kmsKey should be in the following format:
         * projects/{gcp_project}/locations/{key_region}/keyRings/{key_ring}/cryptoKeys/{kms_key_name}
         */
        byte[] cipherText = Base64.getDecoder().decode(encryptedValue.getBytes(StandardCharsets.UTF_8));
        try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
            // Decrypt the ciphertext with Cloud KMS.
            DecryptResponse response = client.decrypt(kmsKey, ByteString.copyFrom(cipherText));
            // Extract the plaintext from the response.
            return new String(response.getPlaintext().toByteArray());
        }
    }
}