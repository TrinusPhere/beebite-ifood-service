package com.trinusphere.beebiteifood.infrastructure.messaging.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinusphere.beebiteifood.application.usecase.SendIfoodChatMessageUseCase;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

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

/**
 * @deprecated O iFood não suporta chat via API REST nem eventos de backend.
 * O chat oficial com clientes é provido exclusivamente pelo Widget JS no frontend.
 */
@Deprecated(forRemoval = true)
@ApplicationScoped
public class BiteflowChatMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(BiteflowChatMessageSubscriber.class);
    private static final String SUBSCRIPTION = "beebite-chat-message-outbound-sub";

    @ConfigProperty(name = "quarkus.google.cloud.project-id", defaultValue = "beebite-dev")
    String projectId;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    ObjectMapper objectMapper;

    private Subscriber subscriber;
    private ManagedChannel channel;

    void onStart(@Observes StartupEvent event) {
        if (emulatorEndpoint.isEmpty() && !Boolean.TRUE.equals(usePlaintext.orElse(false))) {
            LOG.info("[PUBSUB-SUB] Pub/Sub não configurado — subscriber de chat não iniciado.");
            return;
        }

        try {
            ProjectSubscriptionName subName = ProjectSubscriptionName.of(projectId, SUBSCRIPTION);

            if (Boolean.TRUE.equals(usePlaintext.orElse(false)) && emulatorEndpoint.isPresent()) {
                channel = ManagedChannelBuilder.forTarget(emulatorEndpoint.get()).usePlaintext().build();
                var channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                subscriber = Subscriber.newBuilder(subName, this::handleMessage)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .build();
            } else {
                subscriber = Subscriber.newBuilder(subName, this::handleMessage).build();
            }

            subscriber.startAsync().awaitRunning();
            LOG.info("[PUBSUB-SUB] Subscriber de chat iFood iniciado.");
        } catch (Exception e) {
            LOG.errorf(e, "[PUBSUB-SUB] Falha ao iniciar subscriber de chat: %s", e.getMessage());
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        if (subscriber != null) {
            subscriber.stopAsync();
            try {
                subscriber.awaitTerminated(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    private void handleMessage(PubsubMessage message, AckReplyConsumer consumer) {
        String payload = message.getData().toStringUtf8();
        try {
            JsonNode root = objectMapper.readTree(payload);
            String orderId = root.has("orderId") ? root.get("orderId").asText() : "desconhecido";
            LOG.warnf("[PUBSUB-SUB] Mensagem de chat descartada (orderId=%s): o iFood não suporta envio de chat via API/PubSub. O chat deve ser operado pelo Widget oficial do iFood.", orderId);
            consumer.ack();
        } catch (Exception e) {
            LOG.warnf(e, "[PUBSUB-SUB] Falha ao ler payload de chat legado: %s", e.getMessage());
            consumer.ack();
        }
    }
}
