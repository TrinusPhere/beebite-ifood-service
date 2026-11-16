package com.trinusphere.beebiteifood.infrastructure.config;

import com.trinusphere.beebiteifood.application.dto.BiteflowCatalogChangedEvent;
import com.trinusphere.beebiteifood.application.dto.BiteflowOrderCancelledEvent;
import com.trinusphere.beebiteifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodAcceptDisputeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodAlternativeDisputeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodCancellationReason;
import com.trinusphere.beebiteifood.application.dto.IfoodCancellationRequestedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodChatMessageReceivedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodDisputeReceivedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodDisputeSettledEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodInterruptionResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodMerchantStatusResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderDetails;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderPlacedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderStatusChangedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodRejectDisputeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodUserCodeResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.CatalogDiffResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registra todos os DTOs usados via java.net.http.HttpClient + ObjectMapper (API iFood / BiteFlow API)
 * e mensageria Google Cloud Pub/Sub para funcionamento em imagem nativa GraalVM/Mandrel.
 */
@RegisterForReflection(targets = {
        BiteflowCatalogChangedEvent.class,
        BiteflowOrderCancelledEvent.class,
        BiteflowOrderStatusChangedEvent.class,
        IfoodOrderPlacedEvent.class,
        IfoodOrderPlacedEvent.DeliveryAddress.class,
        IfoodOrderPlacedEvent.OrderItem.class,
        IfoodOrderPlacedEvent.ItemOption.class,
        IfoodOrderPlacedEvent.Payment.class,
        IfoodOrderStatusChangedEvent.class,
        IfoodChatMessageReceivedEvent.class,
        IfoodDisputeReceivedEvent.class,
        IfoodDisputeSettledEvent.class,
        IfoodCancellationRequestedEvent.class,
        IfoodTokenResponse.class,
        IfoodUserCodeResponse.class,
        IfoodMerchantStatusResponse.class,
        IfoodInterruptionResponse.class,
        IfoodCancellationReason.class,
        IfoodAcceptDisputeRequest.class,
        IfoodRejectDisputeRequest.class,
        IfoodAlternativeDisputeRequest.class,
        IfoodOrderDetails.class,
        IfoodOrderDetails.IfoodSchedule.class,
        IfoodOrderDetails.IfoodMerchantRef.class,
        IfoodOrderDetails.IfoodCustomer.class,
        IfoodOrderDetails.IfoodPhone.class,
        IfoodOrderDetails.IfoodItem.class,
        IfoodOrderDetails.IfoodItemOption.class,
        IfoodOrderDetails.IfoodTotal.class,
        IfoodOrderDetails.IfoodPayments.class,
        IfoodOrderDetails.IfoodPaymentMethod.class,
        IfoodOrderDetails.IfoodDelivery.class,
        IfoodOrderDetails.IfoodAddress.class,
        IfoodOrderDetails.IfoodBenefit.class,
        IfoodOrderDetails.IfoodSponsorshipValue.class,
        BiteflowCatalogRequest.CreateCategoryRequest.class,
        BiteflowCatalogRequest.CreateProductRequest.class,
        BiteflowCatalogRequest.ChannelPriceDto.class,
        BiteflowCatalogRequest.TechnicalSheetItemDto.class,
        BiteflowCatalogRequest.CreateModifierGroupRequest.class,
        BiteflowCatalogRequest.ModifierOptionDto.class,
        BiteflowCatalogResponse.CategoryResponse.class,
        BiteflowCatalogResponse.ProductResponse.class,
        BiteflowCatalogResponse.ChannelPriceDto.class,
        BiteflowCatalogResponse.TechnicalSheetItemDto.class,
        BiteflowCatalogResponse.ModifierGroupResponse.class,
        BiteflowCatalogResponse.ModifierOptionDto.class,
        IfoodCatalogRequest.IfoodCreateCategoryRequest.class,
        IfoodCatalogRequest.IfoodItemData.class,
        IfoodCatalogRequest.IfoodProductData.class,
        IfoodCatalogRequest.IfoodOptionGroupReference.class,
        IfoodCatalogRequest.IfoodCreateProductRequest.class,
        IfoodCatalogRequest.IfoodUpdatePriceRequest.class,
        IfoodCatalogRequest.IfoodUpdateStatusRequest.class,
        IfoodCatalogRequest.IfoodItemPatchRequest.class,
        IfoodCatalogRequest.IfoodShift.class,
        IfoodCatalogRequest.IfoodContextModifier.class,
        IfoodCatalogRequest.IfoodUpdateProductRequest.class,
        IfoodCatalogRequest.IfoodPrice.class,
        IfoodCatalogRequest.IfoodOptionGroup.class,
        IfoodCatalogRequest.IfoodOption.class,
        IfoodCatalogRequest.IfoodBatchPriceItem.class,
        IfoodCatalogRequest.IfoodBatchStatusItem.class,
        IfoodCatalogRequest.IfoodBatchResponse.class,
        IfoodCatalogRequest.IfoodBatchStatusResponse.class,
        IfoodCatalogRequest.IfoodBatchResultItem.class,
        IfoodCatalogRequest.IfoodUpdateOptionPriceRequest.class,
        IfoodCatalogRequest.IfoodPriceByCatalog.class,
        IfoodCatalogRequest.IfoodUpdateOptionStatusRequest.class,
        IfoodCatalogRequest.IfoodStatusByCatalog.class,
        IfoodCatalogRequest.IfoodUpdateOptionGroupStatusRequest.class,
        IfoodCatalogResponse.IfoodCatalog.class,
        IfoodCatalogResponse.IfoodCategory.class,
        IfoodCatalogResponse.IfoodCategoryItemsResponse.class,
        IfoodCatalogResponse.IfoodItemFull.class,
        IfoodCatalogResponse.IfoodItemData.class,
        IfoodCatalogResponse.IfoodProduct.class,
        IfoodCatalogResponse.IfoodOptionGroup.class,
        IfoodCatalogResponse.IfoodOption.class,
        IfoodCatalogResponse.IfoodPrice.class,
        IfoodCatalogResponse.IfoodContextModifier.class,
        IfoodCatalogResponse.IfoodUnsellableCategory.class,
        IfoodCatalogResponse.IfoodUnsellableItem.class,
        CatalogDiffResponse.class,
        CatalogDiffResponse.CatalogDiffEntry.class,
        CatalogDiffResponse.IfoodItemPresentation.class
})
public class NativeReflectionConfig {
}

