package com.fidecode.biteflowifood.domain.credential;

import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IfoodCredentialTest {

    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void shouldCreateCredentialWithDefaultsDisabled() {
        IfoodCredential cred = new IfoodCredential(STORE_ID, "client-id", "encrypted-secret", "merchant-id");

        assertEquals(STORE_ID, cred.getStoreId());
        assertEquals("client-id", cred.getClientId());
        assertEquals("encrypted-secret", cred.getEncryptedClientSecret());
        assertEquals("merchant-id", cred.getMerchantId());
        assertTrue(cred.isEnabled());
        assertNull(cred.getAccessToken());
        assertNull(cred.getTokenExpiresAt());
        assertNotNull(cred.getCreatedAt());
        assertNotNull(cred.getUpdatedAt());
    }

    @Test
    void shouldReturnTokenInvalidWhenNoToken() {
        IfoodCredential cred = new IfoodCredential(STORE_ID, "client-id", "secret", "merchant-id");
        assertFalse(cred.isTokenValid());
    }

    @Test
    void shouldReturnTokenInvalidWhenExpired() {
        IfoodCredential cred = new IfoodCredential(STORE_ID, "client-id", "secret", "merchant-id");
        cred.updateToken("expired-token", "refresh", 60); // 60 segundos
        // Simula token expirado manualmente via reconstitute
        IfoodCredential expired = IfoodCredential.reconstitute(
                UUID.randomUUID(), STORE_ID, "client-id", "secret", "merchant-id",
                "expired-token", "refresh-token", "auth-code", LocalDateTime.now().minusMinutes(1), true,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1)
        );
        assertFalse(expired.isTokenValid());
    }

    @Test
    void shouldReturnTokenValidWhenWithinExpiry() {
        IfoodCredential cred = new IfoodCredential(STORE_ID, "client-id", "secret", "merchant-id");
        cred.updateToken("valid-token", "refresh", 3600); // 1 hora
        assertTrue(cred.isTokenValid());
        assertEquals("valid-token", cred.getAccessToken());
    }

    @Test
    void shouldReturnTokenInvalidWhenLessThan5MinRemaining() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), STORE_ID, "client-id", "secret", "merchant-id",
                "soon-expiring-token", "refresh-token", "auth-code", LocalDateTime.now().plusMinutes(3), true,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1)
        );
        // Com menos de 5 min, deve ser considerado inválido (margem de segurança)
        assertFalse(cred.isTokenValid());
    }

    @Test
    void shouldUpdateTokenCorrectly() {
        IfoodCredential cred = new IfoodCredential(STORE_ID, "client-id", "secret", "merchant-id");
        cred.updateToken("new-token", "refresh", 7200); // 2 horas

        assertEquals("new-token", cred.getAccessToken());
        assertNotNull(cred.getTokenExpiresAt());
        assertTrue(cred.getTokenExpiresAt().isAfter(LocalDateTime.now().plusHours(1)));
    }

    @Test
    void shouldReconstituteFromPersistence() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        IfoodCredential cred = IfoodCredential.reconstitute(
                id, STORE_ID, "cid", "enc-secret", "mid",
                "token", "refresh-token", "auth-code", now.plusHours(1), true, now, now
        );

        assertEquals(id, cred.getId());
        assertEquals(STORE_ID, cred.getStoreId());
        assertEquals("cid", cred.getClientId());
        assertEquals("enc-secret", cred.getEncryptedClientSecret());
        assertEquals("mid", cred.getMerchantId());
        assertEquals("token", cred.getAccessToken());
        assertTrue(cred.isEnabled());
    }
}
