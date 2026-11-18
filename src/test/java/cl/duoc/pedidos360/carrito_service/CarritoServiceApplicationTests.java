package cl.duoc.pedidos360.carrito_service;

import cl.duoc.pedidos360.carrito_service.event.UserEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Perfil test + KafkaTemplate mockeado: el contexto levanta sin broker de Kafka */
@SpringBootTest
@ActiveProfiles("test")
class CarritoServiceApplicationTests {

	@MockitoBean
	private KafkaTemplate<String, UserEventDto> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
