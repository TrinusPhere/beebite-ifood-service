package com.trinusphere.beebiteifood.infrastructure.messaging;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Inicializa os Tópicos e Subscriptions do Pub/Sub no startup da aplicação.
 * No ambiente local (emulador), cria os recursos automaticamente.
 * Em produção (GCP real), o Terraform/infra já cria os recursos.
 */
@ApplicationScoped
public class PubSubInitializer {

    private static final Logger LOG = Logger.getLogger(PubSubInitializer.class);

    @ConfigProperty(name = "quarkus.google.cloud.project-id", defaultValue = "beebite-dev")
    String projectId;

    // Tópicos publicados pelo serviço de integração (para o BiteFlow consumir)
    private static final String TOPIC_IFOOD_ORDER_PLACED          = "ifood-order-placed";
    private static final String TOPIC_IFOOD_ORDER_STATUS_CHANGED   = "ifood-order-status-changed";
    private static final String TOPIC_IFOOD_CHAT_MESSAGE           = "ifood-chat-message";
    private static final String TOPIC_IFOOD_DISPUTE                = "ifood-order-dispute";
    private static final String TOPIC_IFOOD_CANCELLATION_REQUESTED = "ifood-order-cancellation-requested";

    // Tópicos publicados pelo BiteFlow (para o serviço de integração consumir)
    private static final String TOPIC_BITEFLOW_STATUS_CHANGED      = "beebite-order-status-changed";
    private static final String TOPIC_BITEFLOW_ORDER_CANCELLED      = "beebite-order-cancelled";
    private static final String TOPIC_BITEFLOW_CATALOG_CHANGED     = "beebite-catalog-changed";
    private static final String TOPIC_BITEFLOW_CHAT_MESSAGE        = "beebite-chat-message-outbound";

    // Subscriptions que este serviço consome
    private static final String SUB_BITEFLOW_STATUS_CHANGED = "beebite-order-status-changed-sub";
    private static final String SUB_BITEFLOW_ORDER_CANCELLED = "beebite-order-cancelled-sub";
    private static final String SUB_BITEFLOW_CATALOG_CHANGED = "beebite-catalog-changed-sub";
    private static final String SUB_BITEFLOW_CHAT_MESSAGE = "beebite-chat-message-outbound-sub";

    // Subscriptions que o BiteFlow consome (criamos aqui para facilitar ambiente local)
    private static final String SUB_IFOOD_ORDER_PLACED = "ifood-order-placed-sub";
    private static final String SUB_IFOOD_ORDER_STATUS_CHANGED = "ifood-order-status-changed-sub";
    private static final String SUB_IFOOD_CHAT_MESSAGE = "ifood-chat-message-sub";

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.endpoint")
    Optional<String> emulatorEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.use-plaintext")
    Optional<Boolean> usePlaintext;

    void onStart(@Observes StartupEvent event) {
        if (emulatorEndpoint.isEmpty() || !Boolean.TRUE.equals(usePlaintext.orElse(false))) {
            LOG.info("[PUBSUB-INIT] Emulador não configurado — skipping criação de tópicos/subscriptions.");
            return;
        }

        LOG.infof("[PUBSUB-INIT] Inicializando tópicos no emulador: %s", emulatorEndpoint.get());

        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forTarget(emulatorEndpoint.get())
                    .usePlaintext()
                    .build();

            var channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            var credentialsProvider = NoCredentialsProvider.create();

            createTopics(channelProvider, credentialsProvider);
            createSubscriptions(channelProvider, credentialsProvider);

            channel.shutdown();
            LOG.info("[PUBSUB-INIT] Tópicos e subscriptions criados com sucesso.");

        } catch (Exception e) {
            LOG.errorf(e, "[PUBSUB-INIT] Erro ao inicializar Pub/Sub: %s", e.getMessage());
        }
    }

    private void createTopics(
            FixedTransportChannelProvider channelProvider,
            NoCredentialsProvider credentialsProvider) throws Exception {

        TopicAdminSettings settings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();

        try (TopicAdminClient client = TopicAdminClient.create(settings)) {
            createTopicIfNotExists(client, TOPIC_IFOOD_ORDER_PLACED);
            createTopicIfNotExists(client, TOPIC_IFOOD_ORDER_STATUS_CHANGED);
            createTopicIfNotExists(client, TOPIC_IFOOD_CHAT_MESSAGE);
            createTopicIfNotExists(client, TOPIC_IFOOD_DISPUTE);
            createTopicIfNotExists(client, TOPIC_IFOOD_CANCELLATION_REQUESTED);
            createTopicIfNotExists(client, TOPIC_BITEFLOW_STATUS_CHANGED);
            createTopicIfNotExists(client, TOPIC_BITEFLOW_ORDER_CANCELLED);
            createTopicIfNotExists(client, TOPIC_BITEFLOW_CATALOG_CHANGED);
            createTopicIfNotExists(client, TOPIC_BITEFLOW_CHAT_MESSAGE);
        }
    }

    private void createSubscriptions(
            FixedTransportChannelProvider channelProvider,
            NoCredentialsProvider credentialsProvider) throws Exception {

        SubscriptionAdminSettings settings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();

        try (SubscriptionAdminClient client = SubscriptionAdminClient.create(settings)) {
            createSubscriptionIfNotExists(client, SUB_BITEFLOW_STATUS_CHANGED, TOPIC_BITEFLOW_STATUS_CHANGED);
            createSubscriptionIfNotExists(client, SUB_BITEFLOW_ORDER_CANCELLED, TOPIC_BITEFLOW_ORDER_CANCELLED);
            createSubscriptionIfNotExists(client, SUB_BITEFLOW_CATALOG_CHANGED, TOPIC_BITEFLOW_CATALOG_CHANGED);
            createSubscriptionIfNotExists(client, SUB_BITEFLOW_CHAT_MESSAGE, TOPIC_BITEFLOW_CHAT_MESSAGE);
            createSubscriptionIfNotExists(client, SUB_IFOOD_ORDER_PLACED, TOPIC_IFOOD_ORDER_PLACED);
            createSubscriptionIfNotExists(client, SUB_IFOOD_ORDER_STATUS_CHANGED, TOPIC_IFOOD_ORDER_STATUS_CHANGED);
            createSubscriptionIfNotExists(client, SUB_IFOOD_CHAT_MESSAGE, TOPIC_IFOOD_CHAT_MESSAGE);
        }
    }

    private void createTopicIfNotExists(TopicAdminClient client, String topicId) {
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        try {
            client.getTopic(topicName);
            LOG.debugf("[PUBSUB-INIT] Tópico já existe: %s", topicId);
        } catch (Exception e) {
            client.createTopic(Topic.newBuilder().setName(topicName.toString()).build());
            LOG.infof("[PUBSUB-INIT] Tópico criado: %s", topicId);
        }
    }

    private void createSubscriptionIfNotExists(SubscriptionAdminClient client, String subId, String topicId) {
        ProjectSubscriptionName subName = ProjectSubscriptionName.of(projectId, subId);
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        try {
            client.getSubscription(subName);
            LOG.debugf("[PUBSUB-INIT] Subscription já existe: %s", subId);
        } catch (Exception e) {
            client.createSubscription(Subscription.newBuilder()
                    .setName(subName.toString())
                    .setTopic(topicName.toString())
                    .setPushConfig(PushConfig.getDefaultInstance())
                    .setAckDeadlineSeconds(60)
                    .build());
            LOG.infof("[PUBSUB-INIT] Subscription criada: %s → %s", subId, topicId);
        }
    }
}
