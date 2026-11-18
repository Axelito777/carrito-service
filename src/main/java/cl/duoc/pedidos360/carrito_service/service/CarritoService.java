package cl.duoc.pedidos360.carrito_service.service;

import cl.duoc.pedidos360.carrito_service.event.UserEventPublisher;
import cl.duoc.pedidos360.carrito_service.event.UserEventType;
import cl.duoc.pedidos360.carrito_service.exception.CarritoConfirmadoException;
import cl.duoc.pedidos360.carrito_service.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.carrito_service.model.Carrito;
import cl.duoc.pedidos360.carrito_service.model.EstadoCarrito;
import cl.duoc.pedidos360.carrito_service.model.ItemCarrito;
import cl.duoc.pedidos360.carrito_service.repository.CarritoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final UserEventPublisher userEventPublisher;

    public CarritoService(CarritoRepository carritoRepository, UserEventPublisher userEventPublisher) {
        this.carritoRepository = carritoRepository;
        this.userEventPublisher = userEventPublisher;
    }

    public Carrito crearCarrito(String usuarioId) {
    Carrito carrito = new Carrito();
    carrito.setUsuarioId(usuarioId);
    return carritoRepository.save(carrito);
}

    public Carrito obtenerPorId(Long id) {
        return carritoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un carrito con id " + id));
    }

    public Carrito agregarItem(Long carritoId, ItemCarrito item) {
        Carrito carrito = obtenerPorId(carritoId);
        validarAbierto(carrito);

        item.setId(null);
        item.setCarrito(carrito);
        carrito.getItems().add(item);

        Carrito guardado = carritoRepository.save(carrito);
        publicar(UserEventType.ADDED_TO_CART, carrito, item);
        return guardado;
    }

    public Carrito quitarItem(Long carritoId, Long itemId) {
        Carrito carrito = obtenerPorId(carritoId);
        validarAbierto(carrito);

        // Se busca primero para conservar sus datos y poder publicar el evento despues de borrarlo
        ItemCarrito quitado = carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El carrito " + carritoId + " no tiene un item con id " + itemId));
        carrito.getItems().removeIf(item -> item.getId().equals(itemId));

        Carrito guardado = carritoRepository.save(carrito);
        publicar(UserEventType.REMOVED_FROM_CART, carrito, quitado);
        return guardado;
    }

    public Carrito confirmar(Long carritoId) {
        Carrito carrito = obtenerPorId(carritoId);
        validarAbierto(carrito);

        if (carrito.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede confirmar un carrito vacio");
        }

        carrito.setEstado(EstadoCarrito.CONFIRMADO);
        carrito.setFechaConfirmacion(LocalDateTime.now());
        Carrito guardado = carritoRepository.save(carrito);

        // Un evento CART_CONFIRMED por cada item (con su cantidad)
        for (ItemCarrito item : guardado.getItems()) {
            publicar(UserEventType.CART_CONFIRMED, guardado, item);
        }
        return guardado;
    }

    /**
     * Publica el evento en Kafka con el usuarioId guardado al crear el carrito.
     * Si el carrito no tiene usuario (se creo sin token), el publisher no envia nada.
     * Si Kafka falla, solo se loguea: la operacion del carrito no se ve afectada.
     */
    private void publicar(UserEventType tipo, Carrito carrito, ItemCarrito item) {
        userEventPublisher.publicar(tipo, carrito.getUsuarioId(), item.getRelojId(),
                item.getCategoria(), item.getMarca(), item.getCantidad());
    }

    private void validarAbierto(Carrito carrito) {
        if (carrito.getEstado() == EstadoCarrito.CONFIRMADO) {
            throw new CarritoConfirmadoException(
                    "El carrito " + carrito.getId() + " ya fue confirmado y no se puede modificar");
        }
    }

}
