package cl.duoc.pedidos360.carrito_service.service;

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

    public CarritoService(CarritoRepository carritoRepository) {
        this.carritoRepository = carritoRepository;
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

        return carritoRepository.save(carrito);
    }

    public Carrito quitarItem(Long carritoId, Long itemId) {
        Carrito carrito = obtenerPorId(carritoId);
        validarAbierto(carrito);

        boolean existia = carrito.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!existia) {
            throw new ResourceNotFoundException(
                    "El carrito " + carritoId + " no tiene un item con id " + itemId);
        }

        return carritoRepository.save(carrito);
    }

    public Carrito confirmar(Long carritoId) {
        Carrito carrito = obtenerPorId(carritoId);
        validarAbierto(carrito);

        if (carrito.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede confirmar un carrito vacio");
        }

        carrito.setEstado(EstadoCarrito.CONFIRMADO);
        carrito.setFechaConfirmacion(LocalDateTime.now());
        return carritoRepository.save(carrito);
    }

    private void validarAbierto(Carrito carrito) {
        if (carrito.getEstado() == EstadoCarrito.CONFIRMADO) {
            throw new CarritoConfirmadoException(
                    "El carrito " + carrito.getId() + " ya fue confirmado y no se puede modificar");
        }
    }

}
