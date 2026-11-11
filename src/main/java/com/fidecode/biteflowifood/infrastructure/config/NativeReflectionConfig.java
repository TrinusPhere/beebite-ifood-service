package com.fidecode.biteflowifood.infrastructure.config;

import com.fidecode.biteflowifood.application.dto.BiteflowCatalogChangedEvent;
import com.fidecode.biteflowifood.application.dto.BiteflowOrderCancelledEvent;
import com.fidecode.biteflowifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.dto.IfoodCancellationRequestedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodChatMessageReceivedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodDisputeReceivedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodDisputeSettledEvent;
import com.fidecode.biteflowifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.dto.IfoodOrderPlacedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.dto.IfoodUserCodeResponse;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse;
import com.fidecode.biteflowifood.application.dto.catalog.CatalogDiffResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse;
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

