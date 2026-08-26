package com.fidecode.biteflowifood.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ifood_processed_event")
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String eventId, UUID storeId) {
        this.eventId = eventId;
        this.storeId = storeId;
        this.processedAt = LocalDateTime.now();
    }

    public String getEventId()                 { return eventId; }
    public void setEventId(String eventId)     { this.eventId = eventId; }
    public UUID getStoreId()                   { return storeId; }
    public void setStoreId(UUID storeId)       { this.storeId = storeId; }
    public LocalDateTime getProcessedAt()      { return processedAt; }
    public void setProcessedAt(LocalDateTime t){ this.processedAt = t; }
}
