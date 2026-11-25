# Acta: PricingService

## Objetivo cumplido
- Se implemento `PricingService` para calcular:
  - `precio_item_final`
  - `monto_seguro`
  - `costo_psp`
- Se aplico formula:
  - `PrecioFinal = round((CostoProveedor + MargenRevendedor + ComisionMD + CostoEnvio + MontoSeguro) / (1 - %PSP))`
  - `ComisionMD = 0` en el calculo interno actual.
- Se usa `Math.round()` para redondeo final.

## Configuracion agregada
En `application.yml`:

```yaml
mercado-digital:
  psp-fee: 0.08
  seguro-transporte: 0.02
```

## Cambios de codigo
- Nuevo archivo:
  - `service/PricingService.java`
- Nueva configuracion:
  - `config/MercadoDigitalProperties.java`
  - `BackendApplication` con `@ConfigurationPropertiesScan`.
- Repositorios extendidos para pricing:
  - `ProveedorStockRepository`
  - `RevendedorMarkupRepository`
  - `TarifaTransportistaRepository`
- DTO publico de productos actualizado:
  - `ProductoResponse` ahora expone:
    - `id`
    - `nombre`
    - `descripcion`
    - `precioFinalEstimado`
    - `categoriaPaquete`
    - `stockDisponible`
    - `demoraEstimadaDias`
  - No expone `precioCosto` ni datos de proveedor.
- `ProductoService`:
  - Calcula `precioFinalEstimado` con `PricingService`.
  - Toma costo desde `proveedor_stock.precio_costo_actual` cuando existe.
  - Usa markup disponible.
  - Usa tarifa por categoria para costo/demora estimada.
- `PedidoService`:
  - Congela en `pedido_items`:
    - `precio_item_final`
    - `monto_seguro`
  - Calcula tambien `markup_aplicado` y `precio_costo` congelados.
  - Mantiene `costo_envio = 0` por ahora (sin motor logistico completo).

## Tests unitarios agregados
- `src/test/java/ar/com/mercadodigital/backend/service/PricingServiceTest.java`
- Casos:
  - sin seguro, con psp
  - con seguro, con psp
  - psp=0
  - redondeos con `Math.round()` (`1000.2`, `1000.5`, `1000.9`)

## Restricciones respetadas
- No se implemento PSP real.
- No se implemento split real.
- Flyway sigue como fuente de verdad.
- `ddl-auto` no se cambio a `create/update`.
