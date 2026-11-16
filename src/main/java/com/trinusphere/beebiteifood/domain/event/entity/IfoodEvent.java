package com.trinusphere.beebiteifood.domain.event.entity;

import java.time.Instant;
import java.util.Map;

/**
 * Domain entity — IfoodEvent.
 * POJO puro representando um evento recebido do polling do iFood.
 * NÃO é persistido — eventos são processados e descartados após ACK.
 */
public class IfoodEvent {

    private final String id;
    private final String code;
    private final String fullCode;
    private final String orderId;
    private final String merchantId;
    private final Instant createdAt;
    private final String salesChannel;
    private final Map<String, Object> metadata;

    public IfoodEvent(String id, String code, String fullCode, String orderId,
                      String merchantId, Instant createdAt, String salesChannel,
                      Map<String, Object> metadata) {
        this.id = id;
        this.code = code;
        this.fullCode = fullCode;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.createdAt = createdAt;
        this.salesChannel = salesChannel;
        this.metadata = metadata;
    }

    public String getId()           { return id; }
    public String getCode()         { return code; }
    public String getFullCode()     { return fullCode; }
    public String getOrderId()      { return orderId; }
    public String getMerchantId()   { return merchantId; }
    public Instant getCreatedAt()   { return createdAt; }
    public String getSalesChannel() { return salesChannel; }
    public Map<String, Object> getMetadata() { return metadata; }
}
