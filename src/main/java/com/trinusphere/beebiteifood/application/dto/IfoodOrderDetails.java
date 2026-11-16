package com.trinusphere.beebiteifood.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO — Detalhes completos de um pedido retornados pela API iFood.
 * Espelha a resposta de GET /order/v1.0/orders/{orderId}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
        @JsonProperty("delivery") IfoodDelivery delivery,
        @JsonProperty("schedule") IfoodSchedule schedule
) {

    public IfoodOrderDetails(String id, String displayId, String reference, String createdAt,
                             String orderType, String orderTiming, IfoodMerchantRef merchant,
                             IfoodCustomer customer, List<IfoodItem> items, BigDecimal subTotal,
                             BigDecimal totalFee, List<IfoodBenefit> benefits, IfoodTotal total,
                             IfoodPayments payments, IfoodDelivery delivery) {
        this(id, displayId, reference, createdAt, orderType, orderTiming, merchant,
                customer, items, subTotal, totalFee, benefits, total, payments, delivery, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodSchedule(
            @JsonProperty("deliveryDateTimeStart") String deliveryDateTimeStart,
            @JsonProperty("deliveryDateTimeEnd") String deliveryDateTimeEnd,
            @JsonProperty("preparationStartDateTime") String preparationStartDateTime
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodMerchantRef(@JsonProperty("id") String id, @JsonProperty("name") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodCustomer(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("phone") IfoodPhone phone,
            @JsonProperty("document") String document,
            @JsonProperty("documentType") String documentType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodPhone(
            @JsonProperty("number") String number,
            @JsonProperty("localizer") String localizer,
            @JsonProperty("localizerExpiration") String localizerExpiration
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("totalPrice") BigDecimal totalPrice,
            @JsonProperty("observations") String observations,
            @JsonProperty("options") List<IfoodItemOption> options
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodItemOption(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("totalPrice") BigDecimal totalPrice
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodTotal(
            @JsonProperty("orderAmount") BigDecimal orderAmount,
            @JsonProperty("deliveryFee") BigDecimal deliveryFee,
            @JsonProperty("benefits") BigDecimal benefits,
            @JsonProperty("additionalFees") BigDecimal additionalFees
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodPayments(
            @JsonProperty("prepaid") BigDecimal prepaid,
            @JsonProperty("pending") BigDecimal pending,
            @JsonProperty("methods") List<IfoodPaymentMethod> methods
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodPaymentMethod(
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("currency") String currency,
            @JsonProperty("method") String method,
            @JsonProperty("type") String type,
            @JsonProperty("prepaid") boolean prepaid
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodDelivery(
            @JsonProperty("deliveredBy") String deliveredBy,
            @JsonProperty("deliveryAddress") IfoodAddress deliveryAddress
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodBenefit(
            @JsonProperty("targetId") String targetId,
            @JsonProperty("targetType") String targetType,
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("sponsorshipValues") List<IfoodSponsorshipValue> sponsorshipValues
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IfoodSponsorshipValue(
            @JsonProperty("name") String name,
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("description") String description
    ) {}
}
