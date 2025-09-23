package dk.dtu.infrastructure.dto;

// Author(s) William Pii Jæger

public record OutgoingMessage<T>(
        String type,
        Delivery delivery,
        EventMetaDTO meta,
        T payload) {
}
