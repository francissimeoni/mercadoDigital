# Acta: Producto Canonico y Comparador de Ofertas

## Migracion V5
- Archivo: `src/main/resources/db/migration/V5__productos_canonicos.sql`
- Cambios:
  - tabla `productos_canonicos`:
    - `id`, `marca`, `modelo`, `nombre_publico`, `atributos_json`, `activo`, `created_at`, `updated_at`
  - alter en `productos`:
    - columna `producto_canonico_id`
    - FK `fk_producto_canonico` a `productos_canonicos(id)`
    - indice `idx_productos_canonico_id`

## Entidades afectadas
- Nueva entidad: `ProductoCanonico`
- `Producto`:
  - nueva relacion `ManyToOne` con `ProductoCanonico`
- Repositorios:
  - nuevo `ProductoCanonicoRepository`
  - `ProductoRepository` con `findByProductoCanonico_IdAndActivoTrue(...)`

## Endpoint nuevo
- `GET /productos-canonicos/{id}/ofertas?revendedorId=<UUID>`
- Devuelve ofertas del mismo producto canónico ordenadas por:
  1. `precioFinalEstimado` asc
  2. `demoraEstimadaDias` asc
  3. `stockDisponible` desc

## DTOs
- `ProductoResponse` ahora incluye `productoCanonicoId`
- Nuevo DTO `OfertaProductoResponse`:
  - `productoId`
  - `productoCanonicoId`
  - `precioFinalEstimado`
  - `stockDisponible`
  - `demoraEstimadaDias`
  - `categoriaPaquete`

## Regla de demora y moneda
- Si no hay tarifa, demora default backend: `5` (representa rango 3-7 dias).
- Moneda soportada por ahora: `ARS`.
  - Si el producto tiene otra moneda se responde error controlado.

## Ejemplo de respuesta JSON
```json
[
  {
    "productoId": "0f4e8df6-54f1-4fdf-9f12-9fdd8e4fef19",
    "productoCanonicoId": "9e85d902-8f4b-4c0a-901f-42f6302a52c0",
    "precioFinalEstimado": 143478,
    "stockDisponible": 12,
    "demoraEstimadaDias": 5,
    "categoriaPaquete": "CHICO"
  }
]
```

## Como poblar datos de prueba
1. Insertar un `producto_canonico`.
2. Insertar dos o mas `productos` de distintos proveedores con el mismo `producto_canonico_id`.
3. Insertar `proveedor_stock` para cada producto.
4. Insertar `revendedor_markups` para `revendedorId + productoId`.
5. (Opcional) Insertar `tarifas_transportista` por `categoria_paquete`.
6. Invocar endpoint de ofertas con ese `producto_canonico_id` y `revendedorId`.
