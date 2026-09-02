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

import com.fidecode.biteflowifood.application.dto.BiteflowCatalogChangedEvent;
import com.fidecode.biteflowifood.application.usecase.SyncProductToIfoodUseCase;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class BiteflowCatalogChangedSubscriber {

    private static final Logger LOG = Logger.getLogger(BiteflowCatalogChangedSubscriber.class);
    private static final String PROJECT = "biteflow-dev";
    private static final String SUBSCRIPTION_ID = "biteflow-catalog-changed-sub";

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    SyncProductToIfoodUseCase syncProductUseCase;

    @Inject
    ObjectMapper objectMapper;

    private Subscriber subscriber;
    private ManagedChannel channel;

    void onStart(@Observes StartupEvent event) {
        try {
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT, SUBSCRIPTION_ID);

            MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
                String payload = message.getData().toStringUtf8();
                try {
                    BiteflowCatalogChangedEvent evt = objectMapper.readValue(
                            payload, BiteflowCatalogChangedEvent.class);
                    syncProductUseCase.execute(evt);
                    consumer.ack();
                    LOG.debugf("[SUB-CATALOG] Processado evento de catálogo: storeId=%s productId=%s", evt.storeId(), evt.productId());
                } catch (com.fidecode.biteflowifood.domain.shared.exception.DomainException e) {
                    LOG.warnf("[SUB-CATALOG] Erro de negócio ao processar evento: %s", e.getMessage());
                    consumer.ack();
                } catch (Exception e) {
                    LOG.errorf(e, "[SUB-CATALOG] Erro inesperado — NACK: %s", e.getMessage());
                    consumer.nack();
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
            LOG.infof("[SUB-CATALOG] Subscriber iniciado: %s", SUBSCRIPTION_ID);

        } catch (Exception e) {
            LOG.errorf(e, "[SUB-CATALOG] Falha ao iniciar subscriber: %s", e.getMessage());
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
