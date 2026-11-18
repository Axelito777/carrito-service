package cl.duoc.pedidos360.carrito_service.config;

import cl.duoc.pedidos360.carrito_service.event.UserEventDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor de Kafka (este servicio solo publica, no consume de Kafka).
 * Independiente de la configuración de RabbitMQ.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** Máximo que send() espera al broker. Corto para que la petición HTTP no se cuelgue si Kafka cae. */
    @Value("${kafka.producer.max-block-ms:3000}")
    private int maxBlockMs;

    @Bean
    public ProducerFactory<String, UserEventDto> userEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        // JSON limpio, sin headers con el nombre de la clase Java del productor
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        // acks=all: el broker confirma cuando todas las réplicas en sync (mín. 2) tienen el mensaje
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // Idempotente: los reintentos del productor no generan duplicados en Kafka
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, UserEventDto> userEventKafkaTemplate(
            ProducerFactory<String, UserEventDto> userEventProducerFactory) {
        return new KafkaTemplate<>(userEventProducerFactory);
    }
}
