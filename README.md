# carrito-service (puerto 8082)

Carrito de compras de Pedidos360. Un carrito nace `ABIERTO` y pasa a `CONFIRMADO` al finalizar la compra.
El `usuarioId` se toma del claim `oid` del JWT al crear el carrito.
**Kafka (nuevo):** publica eventos de usuario en el tópico `user-events` para ms-analytics-recommendations.

## Endpoints
| Método | Ruta | Descripción | Evento Kafka |
|---|---|---|---|
| POST | `/carrito` | Crea un carrito (usuario = `oid` del token) | — |
| GET | `/carrito/{id}` | Detalle con total | — |
| POST | `/carrito/{id}/items` | Agrega un ítem | `ADDED_TO_CART` |
| DELETE | `/carrito/{id}/items/{itemId}` | Quita un ítem | `REMOVED_FROM_CART` |
| POST | `/carrito/{id}/confirmar` | Confirma el carrito | `CART_CONFIRMED` (uno por ítem) |

Body de `POST /carrito/{id}/items`:
```json
{ "relojId": 1, "marca": "Casio", "modelo": "G-Shock GA-2100", "categoria": "Deportivo",
  "precioUnitario": 99990, "cantidad": 2 }
```
`categoria` es **opcional** (nuevo): se guarda como "foto" del reloj, igual que marca y modelo, y se usa en
los eventos. Si el frontend no la envía, el evento sale con `productCategory: null` y analytics toma la
categoría de los `PRODUCT_VIEWED` de productos-service.

## Eventos
```json
{ "eventId": "uuid", "userId": "oid", "eventType": "ADDED_TO_CART", "productId": 1,
  "productCategory": "Deportivo", "brand": "Casio", "quantity": 2, "timestamp": "2026-11-16T15:00:00Z" }
```
- `userId` = `usuarioId` guardado al crear el carrito; si el carrito se creó sin token, **no se publica**.
- Key del mensaje = `userId` (los eventos de un usuario mantienen su orden en una misma partición).
- **Si Kafka falla**, se loguea y la operación del carrito funciona igual.
- Productor: `acks=all`, idempotente, JSON sin headers de tipo. No crea tópicos.

## Variables de entorno
| Variable | Por defecto | Descripción |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Brokers Kafka |
| `KAFKA_MAX_BLOCK_MS` | `3000` | Espera máxima al publicar |

## Cómo probarlo
```bash
mvn clean package      # tests sin broker de Kafka (perfil test + KafkaTemplate mockeado)
docker build -t carrito-service .
```
