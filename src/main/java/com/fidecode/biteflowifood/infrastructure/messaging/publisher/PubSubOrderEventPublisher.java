package com.fidecode.biteflowifood.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import com.fidecode.biteflowifood.application.dto.IfoodOrderPlacedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodChatMessageReceivedEvent;
import com.fidecode.biteflowifood.application.port.OrderEventPublisher;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementa OrderEventPublisher usando o Cliente Nativo do GCP Pub/Sub.
 * Mantém dois Publishers pré-inicializados no startup para performance.
 */
@ApplicationScoped
public class PubSubOrderEventPublisher implements OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(PubSubOrderEventPublisher.class);
    private static final String PROJECT = "biteflow-dev";
    private static final String TOPIC_PLACED = "ifood-order-placed";
    private static final String TOPIC_STATUS_CHANGED = "ifood-order-status-changed";
    private static final String TOPIC_CHAT_MESSAGE = "ifood-chat-message";

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    @Inject
    ObjectMapper objectMapper;

    private Publisher placedPublisher;
    private Publisher statusChangedPublisher;
    private Publisher chatMessagePublisher;
    private ManagedChannel channel;

    @PostConstruct
    void init() {
        try {
            placedPublisher = buildPublisher(TOPIC_PLACED);
            statusChangedPublisher = buildPublisher(TOPIC_STATUS_CHANGED);
            chatMessagePublisher = buildPublisher(TOPIC_CHAT_MESSAGE);
            LOG.info("[PUBSUB-PUB] Publishers inicializados.");
        } catch (Exception e) {
            LOG.errorf(e, "[PUBSUB-PUB] Erro ao inicializar publishers: %s", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        shutdownPublisher(placedPublisher);
        shutdownPublisher(statusChangedPublisher);
        shutdownPublisher(chatMessagePublisher);
        if (channel != null) channel.shutdownNow();
    }

    @Override
    public void publishOrderPlaced(IfoodOrderPlacedEvent event) {
        publish(placedPublisher, event, TOPIC_PLACED);
    }

    @Override
    public void publishOrderStatusChanged(IfoodOrderStatusChangedEvent event) {
        publish(statusChangedPublisher, event, TOPIC_STATUS_CHANGED);
    }

    @Override
    public void publishChatMessage(IfoodChatMessageReceivedEvent event) {
        publish(chatMessagePublisher, event, TOPIC_CHAT_MESSAGE);
    }

    private void publish(Publisher publisher, Object payload, String topic) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(json))
                    .build();
            publisher.publish(message).get(10, TimeUnit.SECONDS);
            LOG.debugf("[PUBSUB-PUB] Mensagem publicada no tópico: %s", topic);
        } catch (Exception e) {
            throw new IfoodIntegrationException("Falha ao publicar mensagem no tópico " + topic, e);
        }
    }

    private Publisher buildPublisher(String topicId) throws Exception {
        ProjectTopicName topicName = ProjectTopicName.of(PROJECT, topicId);

        if (Boolean.TRUE.equals(usePlaintext.orElse(false)) && emulatorEndpoint.isPresent()) {
            if (channel == null) {
                channel = ManagedChannelBuilder.forTarget(emulatorEndpoint.get()).usePlaintext().build();
            }
            var channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            return Publisher.newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
        }

        return Publisher.newBuilder(topicName).build();
    }

    private void shutdownPublisher(Publisher publisher) {
        if (publisher != null) {
            try {
                publisher.shutdown();
                publisher.awaitTermination(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.warnf("[PUBSUB-PUB] Erro ao desligar publisher: %s", e.getMessage());
            }
        }
    }
}
