package com.fidecode.beebiteifood.infrastructure.messaging.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import com.fidecode.beebiteifood.application.dto.BiteflowOrderCancelledEvent;
import com.fidecode.beebiteifood.application.usecase.CancelOnIfoodUseCase;

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
 * Subscriber do tópico: biteflow-order-cancelled.
 * Consome eventos publicados pelo BiteFlow quando o operador cancela um pedido iFood.
 * Delega para CancelOnIfoodUseCase.
 */
@ApplicationScoped
public class BiteflowCancellationSubscriber {

    private static final Logger LOG = Logger.getLogger(BiteflowCancellationSubscriber.class);
    private static final String SUBSCRIPTION_ID = "biteflow-order-cancelled-sub";

    @ConfigProperty(name = "quarkus.google.cloud.project-id", defaultValue = "biteflow-dev")
    String projectId;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    CancelOnIfoodUseCase cancelUseCase;

    @Inject
    ObjectMapper objectMapper;

    private Subscriber subscriber;
    private ManagedChannel channel;

    void onStart(@Observes StartupEvent event) {
        try {
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, SUBSCRIPTION_ID);

            MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
                ManagedContext requestContext = Arc.container().requestContext();
                requestContext.activate();
                try {
                    String payload = message.getData().toStringUtf8();
                    BiteflowOrderCancelledEvent evt = objectMapper.readValue(
                            payload, BiteflowOrderCancelledEvent.class);
                    cancelUseCase.execute(evt);
                    consumer.ack();
                    LOG.debugf("[SUB-CANCEL] Processado: orderId=%s", evt.ifoodOrderId());
                } catch (com.fidecode.beebiteifood.domain.shared.exception.DomainException e) {
                    LOG.warnf("[SUB-CANCEL] Erro de negócio: %s", e.getMessage());
                    consumer.ack();
                } catch (Exception e) {
                    LOG.errorf(e, "[SUB-CANCEL] Erro inesperado — NACK: %s", e.getMessage());
                    consumer.nack();
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
            LOG.infof("[SUB-CANCEL] Subscriber iniciado: %s", SUBSCRIPTION_ID);

        } catch (Exception e) {
            LOG.errorf(e, "[SUB-CANCEL] Falha ao iniciar subscriber: %s", e.getMessage());
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
