package com.fidecode.biteflowifood.infrastructure.messaging.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import com.fidecode.biteflowifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.usecase.SyncStatusToIfoodUseCase;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Subscriber do tópico: biteflow-order-status-changed.
 * Consome eventos publicados pelo BiteFlow quando o operador muda o status de um pedido iFood.
 * Delega para SyncStatusToIfoodUseCase.
 *
 * Error handling:
 * - Sucesso → ACK
 * - Exceção de negócio esperada → ACK (não vai reprocessar, loga o erro)
 * - Exceção inesperada → NACK (Pub/Sub reentregará a mensagem)
 */
@ApplicationScoped
public class BiteflowStatusChangeSubscriber {

    private static final Logger LOG = Logger.getLogger(BiteflowStatusChangeSubscriber.class);
    private static final String PROJECT = "biteflow-dev";
    private static final String SUBSCRIPTION_ID = "biteflow-order-status-changed-sub";

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    SyncStatusToIfoodUseCase syncStatusUseCase;

    @Inject
    ObjectMapper objectMapper;

    private Subscriber subscriber;
    private ManagedChannel channel;

    void onStart(@Observes StartupEvent event) {
        try {
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT, SUBSCRIPTION_ID);

            MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
                ManagedContext requestContext = Arc.container().requestContext();
                requestContext.activate();
                try {
                    String payload = message.getData().toStringUtf8();
                    BiteflowOrderStatusChangedEvent evt = objectMapper.readValue(
                            payload, BiteflowOrderStatusChangedEvent.class);
                    syncStatusUseCase.execute(evt);
                    consumer.ack();
                    LOG.debugf("[SUB-STATUS] Processado: storeId=%s status=%s", evt.storeId(), evt.newStatus());
                } catch (com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException e) {
                    LOG.errorf(e, "[SUB-STATUS] Erro de integração iFood transitório — NACK para retentativa do Pub/Sub: %s", e.getMessage());
                    consumer.nack(); // Reprocessar: falha transitória com a API do iFood
                } catch (com.fidecode.biteflowifood.domain.shared.exception.DomainException e) {
                    LOG.warnf("[SUB-STATUS] Erro de negócio ao processar evento: %s", e.getMessage());
                    consumer.ack(); // Não reprocessar: erro de negócio não vai mudar
                } catch (Exception e) {
                    LOG.errorf(e, "[SUB-STATUS] Erro inesperado — NACK: %s", e.getMessage());
                    consumer.nack(); // Reprocessar: pode ser erro transitório
                } finally {
                    requestContext.terminate();
                }
            };

            if (Boolean.TRUE.equals(usePlaintext.orElse(false)) && emulatorEndpoint.isPresent()) {
                channel = ManagedChannelBuilder.forTarget(emulatorEndpoint.get()).usePlaintext().build();
                var channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .build();
            } else {
                subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
            }

            subscriber.startAsync().awaitRunning();
            LOG.infof("[SUB-STATUS] Subscriber iniciado: %s", SUBSCRIPTION_ID);

        } catch (Exception e) {
            LOG.errorf(e, "[SUB-STATUS] Falha ao iniciar subscriber: %s", e.getMessage());
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        if (subscriber != null) {
            subscriber.stopAsync();
            try { subscriber.awaitTerminated(5, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        if (channel != null) channel.shutdownNow();
    }
}
