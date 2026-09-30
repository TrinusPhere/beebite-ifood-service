package com.fidecode.biteflowifood.infrastructure.persistence.repository;

import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração (P11) — PanacheIfoodCredentialRepository.
 *
 * Valida o ciclo completo contra o banco real (Dev Services do Quarkus).
 * Cada teste é @Transactional — rollback automático garante isolamento.
 *
 * Cobre o bug real: `persist()` lançava EntityExistsException ao tentar
 * salvar uma credencial já existente (ex: renovação de token no polling).
 */
@QuarkusTest
class PanacheIfoodCredentialRepositoryIT {

    @Inject
    IfoodCredentialRepository repository;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private IfoodCredential novaCredential(UUID storeId) {
        return new IfoodCredential(storeId, "client-id-test", "enc-secret-test", "merchant-id-test");
    }

    // ── Testes de criação (persist) ───────────────────────────────────────────

    @Test
    @Transactional
    void save_deveInserirNovaCredencial() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential nova = novaCredential(storeId);
        assertNull(nova.getId(), "Nova credencial nao deve ter ID antes do persist");

        IfoodCredential salva = repository.save(nova);

        assertNotNull(salva.getId(), "ID deve ser gerado pelo banco");
        assertEquals(storeId, salva.getStoreId());
        assertEquals("client-id-test", salva.getClientId());
        assertEquals("merchant-id-test", salva.getMerchantId());
        assertTrue(salva.isEnabled());
    }

    @Test
    @Transactional
    void save_devePersistirTokenAposInsercao() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential nova = novaCredential(storeId);
        nova.updateToken("access-123", "refresh-456", 3600L);

        IfoodCredential salva = repository.save(nova);

        assertEquals("access-123", salva.getAccessToken());
        assertEquals("refresh-456", salva.getRefreshToken());
        assertNotNull(salva.getTokenExpiresAt());
    }

    // ── Testes de atualização (merge) — O BUG REAL ───────────────────────────

    /**
     * ESTE TESTE TERIA CAPTURADO O BUG EM PRODUCAO.
     *
     * Cenario: token expira -> polling renova -> save() e chamado com ID ja existente.
     * Antes da correcao: persist() lancava EntityExistsException.
     * Apos a correcao: merge() atualiza silenciosamente.
     */
    @Test
    @Transactional
    void save_deveAtualizarTokenDeCredencialExistente_semEntityExistsException() {
        // Fase 1: inserir credencial inicial
        UUID storeId = UUID.randomUUID();
        IfoodCredential inserida = repository.save(novaCredential(storeId));
        assertNotNull(inserida.getId(), "Pre-condicao: deve ter sido inserida com ID");

        // Fase 2: renovar token — exatamente o que PollIfoodEventsUseCase faz
        inserida.updateToken("novo-access-token", "novo-refresh-token", 7200L);

        // Esta linha era o ponto de falha: lancava EntityExistsException com persist()
        IfoodCredential atualizada = assertDoesNotThrow(
                () -> repository.save(inserida),
                "save() com ID existente NAO deve lancar excecao (deve usar merge, nao persist)"
        );

        assertEquals("novo-access-token", atualizada.getAccessToken());
        assertEquals("novo-refresh-token", atualizada.getRefreshToken());
        assertEquals(inserida.getId(), atualizada.getId(), "ID deve permanecer o mesmo");
    }

    @Test
    @Transactional
    void save_deveAtualizarMerchantIdDeCredencialExistente() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential inserida = repository.save(novaCredential(storeId));

        inserida.setMerchantId("novo-merchant-id");
        IfoodCredential atualizada = repository.save(inserida);

        assertEquals("novo-merchant-id", atualizada.getMerchantId());
    }

    // ── Testes de leitura ─────────────────────────────────────────────────────

    @Test
    @Transactional
    void findByStoreId_deveRetornarCredencialCorreta() {
        UUID storeId = UUID.randomUUID();
        repository.save(novaCredential(storeId));

        Optional<IfoodCredential> encontrada = repository.findByStoreId(storeId);

        assertTrue(encontrada.isPresent());
        assertEquals(storeId, encontrada.get().getStoreId());
    }

    @Test
    @Transactional
    void findByStoreId_deveRetornarVazioParaStoreIdInexistente() {
        Optional<IfoodCredential> resultado = repository.findByStoreId(UUID.randomUUID());
        assertTrue(resultado.isEmpty());
    }

    @Test
    @Transactional
    void findAllEnabled_deveRetornarApenasCredenciaisAtivas() {
        UUID storeAtiva = UUID.randomUUID();
        UUID storeDesativada = UUID.randomUUID();

        repository.save(novaCredential(storeAtiva));

        IfoodCredential salvaDesativada = repository.save(novaCredential(storeDesativada));
        salvaDesativada.setEnabled(false);
        repository.save(salvaDesativada);

        List<IfoodCredential> habilitadas = repository.findAllEnabled();

        boolean contemAtiva = habilitadas.stream()
                .anyMatch(c -> storeAtiva.equals(c.getStoreId()) && c.isEnabled());
        boolean contemDesativada = habilitadas.stream()
                .anyMatch(c -> storeDesativada.equals(c.getStoreId()));

        assertTrue(contemAtiva, "Credencial ativa deve estar na lista");
        assertFalse(contemDesativada, "Credencial desativada NAO deve estar na lista");
    }

    // ── Testes de remocao ─────────────────────────────────────────────────────

    @Test
    @Transactional
    void deleteByStoreId_deveRemoverCredencial() {
        UUID storeId = UUID.randomUUID();
        repository.save(novaCredential(storeId));
        assertTrue(repository.findByStoreId(storeId).isPresent(), "Pre-condicao: deve existir");

        repository.deleteByStoreId(storeId);

        assertTrue(repository.findByStoreId(storeId).isEmpty(), "Apos delete, nao deve existir");
    }

    @Test
    @Transactional
    void deleteByStoreId_naoDeveLancarExcecaoParaStoreIdInexistente() {
        assertDoesNotThrow(() -> repository.deleteByStoreId(UUID.randomUUID()));
    }
}
