package com.fidecode.biteflowifood.infrastructure.config;

import com.fidecode.biteflowifood.application.dto.BiteflowCatalogChangedEvent;
import com.fidecode.biteflowifood.application.dto.BiteflowOrderCancelledEvent;
import com.fidecode.biteflowifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodCancellationRequestedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodDisputeReceivedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodDisputeSettledEvent;
import com.fidecode.biteflowifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.dto.IfoodOrderPlacedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.dto.IfoodUserCodeResponse;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeReflectionConfigTest {

    @Test
    void shouldRegisterAllExternalAndPubSubDtosForNativeReflection() {
        RegisterForReflection annotation = NativeReflectionConfig.class.getAnnotation(RegisterForReflection.class);
        assertNotNull(annotation, "NativeReflectionConfig deve ter @RegisterForReflection para compilacao GraalVM Native");

        List<Class<?>> registered = Arrays.asList(annotation.targets());
        assertTrue(registered.contains(BiteflowCatalogChangedEvent.class));
        assertTrue(registered.contains(BiteflowOrderCancelledEvent.class));
        assertTrue(registered.contains(BiteflowOrderStatusChangedEvent.class));
        assertTrue(registered.contains(IfoodOrderPlacedEvent.class));
        assertTrue(registered.contains(IfoodOrderPlacedEvent.DeliveryAddress.class));
        assertTrue(registered.contains(IfoodOrderPlacedEvent.OrderItem.class));
        assertTrue(registered.contains(IfoodOrderPlacedEvent.ItemOption.class));
        assertTrue(registered.contains(IfoodOrderPlacedEvent.Payment.class));
        assertTrue(registered.contains(IfoodOrderStatusChangedEvent.class));
        assertTrue(registered.contains(IfoodDisputeReceivedEvent.class));
        assertTrue(registered.contains(IfoodDisputeSettledEvent.class));
        assertTrue(registered.contains(IfoodCancellationRequestedEvent.class));
        assertTrue(registered.contains(IfoodTokenResponse.class));
        assertTrue(registered.contains(IfoodUserCodeResponse.class));
        assertTrue(registered.contains(IfoodMerchantStatusResponse.class));
        assertTrue(registered.contains(IfoodInterruptionResponse.class));
        assertTrue(registered.contains(IfoodOrderDetails.class));
        assertTrue(registered.contains(BiteflowCatalogRequest.CreateProductRequest.class));
        assertTrue(registered.contains(BiteflowCatalogResponse.ProductResponse.class));
        assertTrue(registered.contains(IfoodCatalogRequest.IfoodCreateProductRequest.class));
        assertTrue(registered.contains(IfoodCatalogResponse.IfoodItemFull.class));
    }
}

