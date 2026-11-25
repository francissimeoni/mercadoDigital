# Acta - Decision importacion proveedor sin canonizacion v1

Fecha: 2026-03-18

## Contexto

Durante la implementacion de la carga masiva de productos proveedor desde `CSV/XLSX` se confirmo que el archivo fuente mezcla:

- datos operativos de oferta proveedor
- metadata auxiliar del proveedor
- referencias externas a otra plataforma
- senales potencialmente utiles para homologacion futura

El backend disponible en v1 no resuelve canonizacion ni taxonomia interna. El importador actual solo actualiza la oferta concreta del proveedor.

## Decision tomada

Se define que la importacion v1 de proveedor tiene alcance operativo y no canonico.

Esto implica:

- la importacion crea o actualiza `Producto` del proveedor
- la importacion crea o actualiza `ProveedorStock`
- la importacion no crea `ProductoCanonico`
- la importacion no asocia automaticamente productos a un `ProductoCanonico`
- la importacion no interpreta `Grupo`, `Rubro`, `Tipo`, `IVA` o `Part Number` como referencias confiables al dominio interno de Mercado Digital

## Datos con impacto real en v1

La importacion queda orientada a datos operativos de oferta:

- `Codigo` -> `sku`
- `Descripcion` -> `nombre` y `descripcion`
- `precio` -> `precioCosto`
- `ROS + MZA + CBA + LUG` -> `stock`

Estos son los datos que hoy impactan el modelo operativo del proveedor.

## Datos auxiliares sin normalizacion

Los siguientes campos se consideran metadata externa del archivo proveedor:

- `Tipo`
- `IVA`
- `Grupo`
- `Rubro`
- `Part Number`

En particular:

- `Grupo` y `Rubro` no deben interpretarse como categorias internas MD
- no existe hoy una tabla de equivalencias contra taxonomia propia
- no debe presentarse en UI como si hubiera vinculacion automatica con `ProductoCanonico` o categorias internas

## Justificacion

Se toma esta decision por consistencia con el modelo real del sistema:

- el cliente final navega catalogo por `ProductoCanonico`
- el proveedor hoy solo gestiona su oferta concreta
- el backend disponible no expone flujo de canonizacion automatica
- forzar una normalizacion durante la importacion generaria falsa precision y acoplamiento a IDs externos no confiables

## Implicancias de producto

La pantalla de importacion proveedor debe comunicar claramente:

- que carga oferta proveedor
- que no canoniza
- que los datos auxiliares del archivo no equivalen al dominio interno MD

La experiencia debe centrarse en:

- `sku`
- `descripcion`
- `precio`
- `stock`

## Implicancias tecnicas

La visibilidad en storefront sigue dependiendo de:

- existencia de `ProductoCanonico`
- asociacion de una oferta proveedor a ese canonico
- stock disponible
- reglas de seleccion de mejor oferta

Por lo tanto:

- un producto importado puede existir operativamente en proveedor
- pero no necesariamente volverse visible en catalogo publico hasta ser canonizado o asociado

## Decision futura habilitada

Se habilita como linea futura un modulo administrativo de canonizacion bajo `ADMIN_MD`, separado del flujo de importacion proveedor.

Ese modulo podra incluir:

- bandeja de productos proveedor pendientes
- asociacion manual a `ProductoCanonico`
- creacion de nuevos canonicos cuando corresponda
- sugerencias asistidas por IA con validacion humana final

La IA se considera asistencia de matching y priorizacion, no fuente de verdad.

## Conclusion

La importacion proveedor v1 queda formalmente definida como una carga basica de oferta proveedor, no como un flujo de homologacion o canonizacion de catalogo.
