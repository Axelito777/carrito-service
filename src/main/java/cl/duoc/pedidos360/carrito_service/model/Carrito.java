package cl.duoc.pedidos360.carrito_service.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un carrito nace ABIERTO (se le pueden agregar/quitar items) y pasa a
 * CONFIRMADO al finalizar la compra - ese mismo registro es el "pedido".
 *
 * usuarioId queda nulo por ahora: se completara con el "sub" del JWT cuando
 * el API Gateway este configurado para reenviar ese dato al backend.
 */
@Entity
@Table(name = "carritos")
@Data
@NoArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioId; // PENDIENTE: vendra del JWT via el Gateway

    @Enumerated(EnumType.STRING)
    private EstadoCarrito estado = EstadoCarrito.ABIERTO;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaConfirmacion;

    /** Calculado al vuelo, no se guarda como columna en la base de datos. */
    public BigDecimal getTotal() {
        return items.stream()
                .map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
