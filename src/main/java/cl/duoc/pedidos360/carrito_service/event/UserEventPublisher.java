package cl.duoc.pedidos360.carrito_service.event;

import cl.duoc.pedidos360.carrito_service.config.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica eventos de usuario en Kafka para ms-analytics-recommendations.
 * Si Kafka falla, solo se loguea: la petición del usuario NO debe fallar por esto.
 */
@Slf4j
@Component
public class UserEventPublisher {

    private final KafkaTemplate<String, UserEventDto> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, UserEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Arma el evento y lo publica. Sin userId (usuario anónimo) no se publica nada. */
    public void publicar(UserEventType tipo, String userId, Long productId,
                         String categoria, String marca, Integer cantidad) {
        if (userId == null || userId.isBlank()) {
            log.debug("Evento {} sin userId, no se publica", tipo);
            return;
        }
        UserEventDto evento = new UserEventDto(UUID.randomUUID().toString(), userId, tipo, productId,
                categoria, marca, cantidad, Instant.now());
        try {
            // KEY = userId: todos los eventos de un usuario caen en la misma partición,
            // y Kafka garantiza el orden dentro de una partición (miró -> agregó -> compró).
            kafkaTemplate.send(KafkaTopics.USER_EVENTS, userId, evento)
                    .whenComplete((resultado, error) -> {
                        // Error asíncrono: el broker no confirmó el mensaje
                        if (error != null) {
                            log.error("No se pudo publicar {} del producto {}: {}", tipo, productId, error.getMessage());
                        }
                    });
        } catch (Exception e) {
            // Error síncrono: por ejemplo, no hay metadata del broker dentro de max.block.ms
            log.error("Kafka no disponible, {} del producto {} no se publicó: {}", tipo, productId, e.getMessage());
        }
    }
}
