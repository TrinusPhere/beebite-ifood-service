package com.fidecode.beebiteifood.infrastructure.crypto;

import com.fidecode.beebiteifood.application.port.SecretEncryptor;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementação AES-256-GCM do SecretEncryptor.
 * Criptografa/descriptografa o clientSecret do iFood antes de persistir no banco.
 *
 * Formato do cipherText: Base64(IV_12_bytes + cipherData + GCM_tag)
 */
@ApplicationScoped
public class AesSecretEncryptor implements SecretEncryptor {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String DEFAULT_DEV_KEY = "BtFl0w-Enc-K3y-32-chars-secret!";

    @ConfigProperty(name = "biteflow.ifood.encryption-key")
    String encryptionKey;

    @ConfigProperty(name = "biteflow.ifood.enforce-prod-key", defaultValue = "true")
    boolean enforceProdKey;

    @PostConstruct
    public void validateKeyConfiguration() {
        if (!enforceProdKey) {
            return;
        }
        if (encryptionKey == null
                || encryptionKey.isBlank()
                || DEFAULT_DEV_KEY.equals(encryptionKey)
                || encryptionKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IfoodIntegrationException(
                    "Configuração insegura em produção: defina IFOOD_ENCRYPTION_KEY exclusiva com pelo menos 32 bytes.");
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] keyBytes = getKeyBytes();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new IfoodIntegrationException("Falha ao criptografar secret", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherData = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(getKeyBytes(), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainText = cipher.doFinal(cipherData);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IfoodIntegrationException("Falha ao descriptografar secret", e);
        }
    }

    /**
     * Garante que a chave tem exatamente 32 bytes (AES-256).
     * Padding com zeros ou truncamento se necessário.
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = new byte[32];
        byte[] configBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(configBytes, 0, keyBytes, 0, Math.min(configBytes.length, 32));
        return keyBytes;
    }
}
