# Acta - Productos proveedor manual v1

Fecha: 2026-03-18

## Objetivo

Auditar e implementar la mejor version posible de `/proveedor/productos` respetando el modelo real del sistema y sin inventar endpoints, permisos ni logica de negocio inexistente.

## Modelo real encontrado

- La oferta concreta del marketplace corresponde a un `producto` especifico de un proveedor.
- Los equivalentes se agrupan por `producto_canonico`.
- La media se modela por `producto_media`.
- El backend soporta multiples imagenes y video opcional por producto.
- La media expone:
  - `tipoMedia`
  - `url`
  - `orden`
  - `esPrincipal`

## Soporte real backend auditado

Se encontro:

- `GET /productos`
- `GET /productos/{id}`
- `GET /productos/{id}/media`
- `POST /productos/{id}/media`
- `DELETE /media/{id}`
- `GET /productos-canonicos/{id}/ofertas?revendedorId=...`

Pero el control de seguridad actual deja claro que:

- `/productos/**` es `ADMIN_MD`
- `/productos-canonicos/**` es `ADMIN_MD`
- `/media/**` es `ADMIN_MD`
- `PROVEEDOR` solo tiene contrato real sobre `/proveedores/**` y `POST /envios/*/listo-para-retiro`

## Que se pudo implementar efectivamente

- Se reemplazo el placeholder de `/proveedor/productos` por una vista profesional y util.
- La pantalla ahora documenta:
  - modelo real producto / canonico / media
  - matriz de soporte backend auditado
  - permisos reales detectados
  - limites actuales para proveedor
- Se mantuvo coherencia visual con el dashboard y el modulo proveedor.

## Que no se implemento por falta de contrato real o permisos

- Listado real de productos del proveedor
- Alta manual de producto/oferta
- Edicion manual
- Activacion o desactivacion
- Gestion real de stock o costo para proveedor
- Gestion real de media multiple desde el rol proveedor
- Seleccion de imagen principal desde frontend proveedor

No se implemento porque hoy implicaria inventar permisos o contratos que el backend no expone para `PROVEEDOR`.

## Media multiple y video

El backend si contempla media multiple y video opcional a nivel tecnico por `producto_media`, con orden y marca de principal. Eso quedo explicitado en la pantalla y documentado para hilos futuros.

Sin embargo, esa capacidad no esta hoy disponible para el rol proveedor en frontend porque los endpoints correspondientes quedan restringidos a `ADMIN_MD`.

## Preparacion para carga por Excel

La pagina deja explicitado que el siguiente hilo natural para el modulo proveedor es la carga por Excel o un contrato proveedor especifico de productos/ofertas, segun se habilite desde backend.

## Conclusion

El resultado correcto en esta etapa no era simular un ABM falso, sino cerrar una pantalla honesta y profesional sobre `/proveedor/productos`, dejando claro el modelo real existente, el soporte backend encontrado y las limitaciones actuales del rol proveedor.
