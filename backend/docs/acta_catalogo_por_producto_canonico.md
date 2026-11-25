# Acta: Catalogo por Producto Canonico

## Endpoint nuevo
- `GET /catalogo?revendedorId=<UUID>`

## DTO nuevo
- `CatalogoItemResponse`:
  - `productoCanonicoId`
  - `nombrePublico`
  - `mejorPrecioFinalEstimado`
  - `demoraEstimadaDias`
  - `stockDisponible`
  - `cantidadOfertas`

## Reglas implementadas
- Una sola fila por producto canónico.
- Solo se consideran ofertas con `stock > 0`.
- Si un canónico no tiene ofertas con stock positivo, no aparece en catálogo.
- La mejor oferta se determina por:
  1. `precioFinalEstimado` asc
  2. `demoraEstimadaDias` asc
  3. `stockDisponible` desc
- La mejor oferta define:
  - `mejorPrecioFinalEstimado`
  - `demoraEstimadaDias`
  - `stockDisponible`
- `cantidadOfertas` cuenta solo ofertas con stock positivo.
- Orden final del catálogo para estabilidad:
  - `nombrePublico` asc

## Reutilizacion de logica
- Se reutiliza `OfertaProductoCanonicoService`:
  - nuevo metodo `getOfertasConStock(...)`
  - mantiene endpoint existente `/productos-canonicos/{id}/ofertas`

## Ejemplo JSON
```json
[
  {
    "productoCanonicoId": "9e85d902-8f4b-4c0a-901f-42f6302a52c0",
    "nombrePublico": "Xiaomi Redmi 14C",
    "mejorPrecioFinalEstimado": 139900,
    "demoraEstimadaDias": 5,
    "stockDisponible": 12,
    "cantidadOfertas": 3
  }
]
```

## Como poblar datos de prueba
1. Insertar productos canónicos activos en `productos_canonicos`.
2. Insertar productos activos ligados por `producto_canonico_id`.
3. Insertar `proveedor_stock` por producto/proveedor.
4. Insertar `revendedor_markups` por `revendedorId + productoId`.
5. (Opcional) Insertar `tarifas_transportista` para mejorar demora/costo.
6. Consultar:
   - `/productos-canonicos/{id}/ofertas?revendedorId=...`
   - `/catalogo?revendedorId=...`

## Archivos principales
- `controller/CatalogoController.java`
- `service/CatalogoService.java`
- `dto/response/CatalogoItemResponse.java`
- `service/OfertaProductoCanonicoService.java` (reutilizacion y filtro por stock)
