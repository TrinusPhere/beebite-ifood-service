package com.fidecode.biteflowifood.infrastructure.messaging.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.usecase.SendIfoodChatMessageUseCase;
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

@ApplicationScoped
public class BiteflowChatMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(BiteflowChatMessageSubscriber.class);
    private static final String PROJECT = "biteflow-dev";
    private static final String SUBSCRIPTION = "biteflow-chat-message-outbound-sub";

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    SendIfoodChatMessageUseCase sendIfoodChatMessageUseCase;

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
            ProjectSubscriptionName subName = ProjectSubscriptionName.of(PROJECT, SUBSCRIPTION);

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
            String storeId = root.get("storeId").asText();
            String orderId = root.get("orderId").asText();
            String chatMessage = root.get("message").asText();

            sendIfoodChatMessageUseCase.execute(storeId, orderId, chatMessage);
            consumer.ack();
            LOG.infof("[PUBSUB-SUB] Mensagem de chat enviada com sucesso para o iFood: orderId=%s", orderId);
        } catch (Exception e) {
            LOG.errorf(e, "[PUBSUB-SUB] Falha ao processar mensagem de chat do BiteFlow — NACK: %s", e.getMessage());
            consumer.nack();
        }
    }
}
