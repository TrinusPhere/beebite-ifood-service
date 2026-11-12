package com.fidecode.beebiteifood.application.port;

/**
 * Port — SecretEncryptor.
 * Abstração para criptografia de secrets sensíveis (clientSecret do iFood).
 * Implementado em infrastructure por AesSecretEncryptor (AES-256-GCM).
 */
public interface SecretEncryptor {

    /** Criptografa o texto plano e retorna o texto cifrado em Base64. */
    String encrypt(String plainText);

    /** Descriptografa o texto cifrado (Base64) e retorna o texto plano. */
    String decrypt(String cipherText);
}
