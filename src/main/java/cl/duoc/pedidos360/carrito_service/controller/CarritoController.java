package cl.duoc.pedidos360.carrito_service.controller;

import cl.duoc.pedidos360.carrito_service.model.Carrito;
import cl.duoc.pedidos360.carrito_service.model.ItemCarrito;
import cl.duoc.pedidos360.carrito_service.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;

@RestController
@RequestMapping("/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @PostMapping
    public ResponseEntity<Carrito> crear(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String usuarioId = extraerUsuarioId(authHeader);
    return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.crearCarrito(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Carrito> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(carritoService.obtenerPorId(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Carrito> agregarItem(@PathVariable Long id, @Valid @RequestBody ItemCarrito item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.agregarItem(id, item));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Carrito> quitarItem(@PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.quitarItem(id, itemId));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Carrito> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(carritoService.confirmar(id));
    }
    private String extraerUsuarioId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return null;
    }
    try {
        String token = authHeader.substring(7);
        String[] partes = token.split("\\.");
        String payload = partes[1];

        int resto = payload.length() % 4;
        if (resto == 2) payload += "==";
        else if (resto == 3) payload += "=";

        String payloadJson = new String(Base64.getUrlDecoder().decode(payload));

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"oid\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(payloadJson);
        return m.find() ? m.group(1) : null;
    } catch (Exception e) {
        System.err.println("No se pudo extraer usuarioId del token: " + e.getMessage());
        return null;
    }
}

}
