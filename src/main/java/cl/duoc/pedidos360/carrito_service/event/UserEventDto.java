package cl.duoc.pedidos360.carrito_service.event;

import java.time.Instant;

/**
 * Evento publicado en el tópico user-events.
 * CONTRATO: debe ser idéntico (campos y nombres) al UserEventDto de ms-analytics-recommendations.
 */
public record UserEventDto(
        String eventId,          // UUID único (el consumidor lo usa para no duplicar)
        String userId,           // claim oid del JWT; también es la KEY del mensaje
        UserEventType eventType,
        Long productId,
        String productCategory,  // puede ir null
        String brand,            // puede ir null
        Integer quantity,        // null en PRODUCT_VIEWED
        Instant timestamp) {     // cuándo ocurrió
}
