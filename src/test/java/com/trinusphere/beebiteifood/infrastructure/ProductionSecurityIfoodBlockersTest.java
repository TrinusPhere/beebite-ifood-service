package com.trinusphere.beebiteifood.infrastructure;

import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import com.trinusphere.beebiteifood.infrastructure.crypto.AesSecretEncryptor;
import com.trinusphere.beebiteifood.infrastructure.messaging.PubSubInitializer;
import com.trinusphere.beebiteifood.infrastructure.messaging.publisher.PubSubOrderEventPublisher;
import com.trinusphere.beebiteifood.infrastructure.messaging.subscriber.BiteflowCancellationSubscriber;
import com.trinusphere.beebiteifood.infrastructure.messaging.subscriber.BiteflowCatalogChangedSubscriber;
import com.trinusphere.beebiteifood.infrastructure.messaging.subscriber.BiteflowStatusChangeSubscriber;
import com.trinusphere.beebiteifood.infrastructure.storage.LocalImageStorage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityIfoodBlockersTest {

    @Test
    @DisplayName("Bloqueador #2 (iFood Service): Publishers e Subscribers devem injetar projectId via @ConfigProperty")
    void pubSubComponentsShouldInjectProjectId() throws Exception {
        Class<?>[] classes = {
                PubSubInitializer.class,
                PubSubOrderEventPublisher.class,
                BiteflowCancellationSubscriber.class,
                BiteflowCatalogChangedSubscriber.class,
                BiteflowStatusChangeSubscriber.class
        };

        for (Class<?> clazz : classes) {
            Field field = clazz.getDeclaredField("projectId");
            ConfigProperty cfg = field.getAnnotation(ConfigProperty.class);
            assertNotNull(cfg, clazz.getSimpleName() + " deve anotar projectId com @ConfigProperty");
            assertEquals("quarkus.google.cloud.project-id", cfg.name());
        }
    }

    @Test
    @DisplayName("Bloqueador #4 (iFood Service): AesSecretEncryptor deve rejeitar chave default de dev quando enforceProdKey=true")
    void aesSecretEncryptorShouldRejectDefaultDevKeyInProd() throws Exception {
        AesSecretEncryptor encryptor = new AesSecretEncryptor();

        Field keyField = AesSecretEncryptor.class.getDeclaredField("encryptionKey");
        keyField.setAccessible(true);
        keyField.set(encryptor, "BtFl0w-Enc-K3y-32-chars-secret!");

        Field enforceField = AesSecretEncryptor.class.getDeclaredField("enforceProdKey");
        enforceField.setAccessible(true);
        enforceField.set(encryptor, true);

        assertThrows(IfoodIntegrationException.class, encryptor::validateKeyConfiguration,
                "Em produção, a chave AES default do repositório deve ser rejeitada");
    }

    @Test
    @DisplayName("Bloqueador #5: LocalImageStorage deve ter diretório configurável e bloquear esquema file:// (SSRF)")
    void localImageStorageShouldUseConfigurableDirAndBlockFileScheme(@TempDir Path tempDir) throws Exception {
        LocalImageStorage storage = new LocalImageStorage();

        Field dirField = LocalImageStorage.class.getDeclaredField("uploadDir");
        dirField.setAccessible(true);
        dirField.set(storage, tempDir.toString());

        Field urlField = LocalImageStorage.class.getDeclaredField("publicBaseUrl");
        urlField.setAccessible(true);
        urlField.set(storage, "https://cdn.chefsburguer.com.br/images");

        String maliciousUrl = "file:///C:/Windows/win.ini";
        String result = storage.storeImageFromUrl(maliciousUrl);

        assertNull(result, "URLs com esquema file:// devem ser bloqueadas por segurança (prevenção de SSRF/LFI)");
        assertEquals(0, tempDir.toFile().listFiles().length, "Nenhum arquivo local deve ser copiado a partir de file://");
    }
}

