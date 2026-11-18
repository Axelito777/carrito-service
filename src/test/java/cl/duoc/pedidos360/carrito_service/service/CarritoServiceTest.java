package cl.duoc.pedidos360.carrito_service.service;

import cl.duoc.pedidos360.carrito_service.config.KafkaTopics;
import cl.duoc.pedidos360.carrito_service.event.UserEventDto;
import cl.duoc.pedidos360.carrito_service.event.UserEventPublisher;
import cl.duoc.pedidos360.carrito_service.event.UserEventType;
import cl.duoc.pedidos360.carrito_service.exception.CarritoConfirmadoException;
import cl.duoc.pedidos360.carrito_service.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.carrito_service.model.Carrito;
import cl.duoc.pedidos360.carrito_service.model.EstadoCarrito;
import cl.duoc.pedidos360.carrito_service.model.ItemCarrito;
import cl.duoc.pedidos360.carrito_service.repository.CarritoRepository;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Eventos de Kafka del carrito, con KafkaTemplate mockeado (sin broker) */
@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private KafkaTemplate<String, UserEventDto> kafkaTemplate;

    private CarritoService service;

    @BeforeEach
    void setUp() {
        service = new CarritoService(carritoRepository, new UserEventPublisher(kafkaTemplate));
        lenient().when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
    }

    private Carrito carrito(String usuarioId, ItemCarrito... items) {
        Carrito c = new Carrito();
        c.setId(1L);
        c.setUsuarioId(usuarioId);
        for (ItemCarrito i : items) {
            i.setCarrito(c);
            c.getItems().add(i);
        }
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(c));
        return c;
    }

    private ItemCarrito item(Long id, Long relojId, String marca, String categoria, int cantidad) {
        return new ItemCarrito(id, relojId, marca, "Modelo", categoria, new BigDecimal("50000"), cantidad, null);
    }

    private List<UserEventDto> eventosPublicados(int cantidad) {
        ArgumentCaptor<UserEventDto> captor = ArgumentCaptor.forClass(UserEventDto.class);
        verify(kafkaTemplate, times(cantidad)).send(eq(KafkaTopics.USER_EVENTS), eq("oid-ana"), captor.capture());
        return captor.getAllValues();
    }

    @Test
    void agregarItem_publicaAddedToCartConCantidad() {
        carrito("oid-ana");

        service.agregarItem(1L, item(null, 7L, "Casio", "Deportivo", 2));

        UserEventDto e = eventosPublicados(1).get(0);
        assertThat(e.eventType()).isEqualTo(UserEventType.ADDED_TO_CART);
        assertThat(e.productId()).isEqualTo(7L);
        assertThat(e.productCategory()).isEqualTo("Deportivo");
        assertThat(e.brand()).isEqualTo("Casio");
        assertThat(e.quantity()).isEqualTo(2);
    }

    @Test
    void quitarItem_publicaRemovedFromCartConLosDatosDelItemQuitado() {
        Carrito c = carrito("oid-ana", item(10L, 7L, "Casio", null, 3), item(11L, 8L, "Seiko", "Elegante", 1));

        service.quitarItem(1L, 10L);

        assertThat(c.getItems()).extracting(ItemCarrito::getId).containsExactly(11L);
        UserEventDto e = eventosPublicados(1).get(0);
        assertThat(e.eventType()).isEqualTo(UserEventType.REMOVED_FROM_CART);
        assertThat(e.productId()).isEqualTo(7L);
        assertThat(e.quantity()).isEqualTo(3);
        assertThat(e.productCategory()).isNull(); // el frontend aún no la envía
    }

    @Test
    void quitarItemInexistente_404_sinPublicar() {
        carrito("oid-ana", item(10L, 7L, "Casio", null, 1));

        assertThatThrownBy(() -> service.quitarItem(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void confirmar_publicaUnCartConfirmedPorItem() {
        Carrito c = carrito("oid-ana", item(10L, 7L, "Casio", "Deportivo", 2), item(11L, 8L, "Seiko", "Elegante", 1));

        service.confirmar(1L);

        assertThat(c.getEstado()).isEqualTo(EstadoCarrito.CONFIRMADO);
        List<UserEventDto> eventos = eventosPublicados(2);
        assertThat(eventos).allMatch(e -> e.eventType() == UserEventType.CART_CONFIRMED);
        assertThat(eventos).extracting(UserEventDto::productId).containsExactly(7L, 8L);
        assertThat(eventos).extracting(UserEventDto::quantity).containsExactly(2, 1);
    }

    @Test
    void carritoYaConfirmado_noPublica() {
        Carrito c = carrito("oid-ana", item(10L, 7L, "Casio", null, 1));
        c.setEstado(EstadoCarrito.CONFIRMADO);

        assertThatThrownBy(() -> service.agregarItem(1L, item(null, 8L, "Seiko", null, 1)))
                .isInstanceOf(CarritoConfirmadoException.class);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void carritoSinUsuario_funcionaPeroNoPublica() {
        carrito(null);

        Carrito r = service.agregarItem(1L, item(null, 7L, "Casio", null, 1));

        assertThat(r.getItems()).hasSize(1);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void kafkaCaido_laOperacionFuncionaIgual() {
        carrito("oid-ana");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new KafkaException("broker caído"));

        Carrito r = service.agregarItem(1L, item(null, 7L, "Casio", null, 1));

        assertThat(r.getItems()).hasSize(1);
    }
}
