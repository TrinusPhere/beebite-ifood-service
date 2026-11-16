package com.trinusphere.beebiteifood.infrastructure.scheduler;

import com.trinusphere.beebiteifood.application.usecase.PollIfoodEventsUseCase;

import io.quarkus.scheduler.Scheduled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

/**
 * Scheduler — IfoodPollingScheduler.
 * Dispara o ciclo de polling a cada 30 segundos.
 * Falhas nunca propagam exception — o scheduler continua rodando.
 */
@ApplicationScoped
public class IfoodPollingScheduler {

    private static final Logger LOG = Logger.getLogger(IfoodPollingScheduler.class);

    private final PollIfoodEventsUseCase pollUseCase;

    @Inject
    public IfoodPollingScheduler(PollIfoodEventsUseCase pollUseCase) {
        this.pollUseCase = pollUseCase;
    }

    @Scheduled(every = "${biteflow.ifood.polling-interval:30s}", identity = "ifood-polling")
    public void poll() {
        try {
            pollUseCase.execute();
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-POLL] Erro não tratado no ciclo de polling: %s", e.getMessage());
            // Nunca propaga exception — o Scheduler do Quarkus continua agendando
        }
    }
}
