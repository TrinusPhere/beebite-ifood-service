package com.fidecode.biteflowifood.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO — Detalhes completos de um pedido retornados pela API iFood.
 * Espelha a resposta de GET /order/v1.0/orders/{orderId}.
 */
public record IfoodOrderDetails(
        @JsonProperty("id") String id,
        @JsonProperty("displayId") String displayId,
        @JsonProperty("reference") String reference,
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("orderType") String orderType,
        @JsonProperty("orderTiming") String orderTiming,
        @JsonProperty("merchant") IfoodMerchantRef merchant,
        @JsonProperty("customer") IfoodCustomer customer,
        @JsonProperty("items") List<IfoodItem> items,
        @JsonProperty("subTotal") BigDecimal subTotal,
        @JsonProperty("totalFee") BigDecimal totalFee,
        @JsonProperty("benefits") List<IfoodBenefit> benefits,
        @JsonProperty("total") IfoodTotal total,
        @JsonProperty("payments") IfoodPayments payments,
        @JsonProperty("delivery") IfoodDelivery delivery
) {

    public record IfoodMerchantRef(@JsonProperty("id") String id, @JsonProperty("name") String name) {}

    public record IfoodCustomer(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("phone") String phone,
            @JsonProperty("document") String document,
            @JsonProperty("documentType") String documentType
    ) {}

    public record IfoodItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("totalPrice") BigDecimal totalPrice,
            @JsonProperty("observations") String observations,
            @JsonProperty("options") List<IfoodItemOption> options
    ) {}

    public record IfoodItemOption(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("totalPrice") BigDecimal totalPrice
    ) {}

    public record IfoodTotal(
            @JsonProperty("orderAmount") BigDecimal orderAmount,
            @JsonProperty("deliveryFee") BigDecimal deliveryFee,
            @JsonProperty("benefits") BigDecimal benefits,
            @JsonProperty("additionalFees") BigDecimal additionalFees
    ) {}

    public record IfoodPayments(
            @JsonProperty("prepaid") BigDecimal prepaid,
            @JsonProperty("pending") BigDecimal pending,
            @JsonProperty("methods") List<IfoodPaymentMethod> methods
    ) {}

    public record IfoodPaymentMethod(
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("currency") String currency,
            @JsonProperty("method") String method,
            @JsonProperty("type") String type,
            @JsonProperty("prepaid") boolean prepaid
    ) {}

    public record IfoodDelivery(
            @JsonProperty("deliveredBy") String deliveredBy,
            @JsonProperty("deliveryAddress") IfoodAddress deliveryAddress
    ) {}

    public record IfoodAddress(
            @JsonProperty("streetName") String streetName,
            @JsonProperty("streetNumber") String streetNumber,
            @JsonProperty("formattedAddress") String formattedAddress,
            @JsonProperty("neighborhood") String neighborhood,
            @JsonProperty("complement") String complement,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state,
            @JsonProperty("country") String country,
            @JsonProperty("postalCode") String postalCode,
            @JsonProperty("reference") String reference,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude
    ) {}

    public record IfoodBenefit(
            @JsonProperty("targetId") String targetId,
            @JsonProperty("targetType") String targetType,
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("sponsorshipValues") List<IfoodSponsorshipValue> sponsorshipValues
    ) {}

    public record IfoodSponsorshipValue(
            @JsonProperty("name") String name,
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("description") String description
    ) {}
}
