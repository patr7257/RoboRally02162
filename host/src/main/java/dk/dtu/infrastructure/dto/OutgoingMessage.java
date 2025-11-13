package dk.dtu.infrastructure.dto;

/**
 * @author William Pii Jæger
 */
public record OutgoingMessage<T>(
        String type,
        Delivery delivery,
        EventMetaDTO meta,
        T payload) {
}
