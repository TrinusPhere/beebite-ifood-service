package com.trinusphere.beebiteifood.infrastructure.config;

import com.trinusphere.beebiteifood.application.dto.BiteflowCatalogChangedEvent;
import com.trinusphere.beebiteifood.application.dto.BiteflowOrderCancelledEvent;
import com.trinusphere.beebiteifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodCancellationRequestedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodDisputeReceivedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodDisputeSettledEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodInterruptionResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodMerchantStatusResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderDetails;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderPlacedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderStatusChangedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodUserCodeResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse;
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

