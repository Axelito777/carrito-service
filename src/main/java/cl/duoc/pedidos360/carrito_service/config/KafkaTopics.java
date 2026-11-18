package cl.duoc.pedidos360.carrito_service.config;

/**
 * Nombres de tópicos de Kafka (nada de strings sueltos en el código).
 * El tópico lo crea kafka-infra/create-topics.sh; este servicio NO crea tópicos.
 */
public final class KafkaTopics {

    public static final String USER_EVENTS = "user-events";

    private KafkaTopics() {
    }
}
