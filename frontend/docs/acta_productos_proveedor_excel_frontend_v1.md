# Acta - productos proveedor Excel frontend v1

## Objetivo

Implementar la pantalla frontend v1 de carga masiva de productos proveedor desde archivo, conectada al endpoint backend ya disponible de importacion.

Ruta operativa consolidada:
- `/proveedor/carga-excel`

## Alcance realizado

Se reemplazo el placeholder previo por una superficie operativa real que permite:
- seleccionar archivo
- validar extension minima en frontend
- previsualizar encabezados y filas de muestra para `CSV`
- mapear columnas del `CSV` al contrato esperado por backend
- descargar plantilla base de importacion
- separar visualmente datos operativos de oferta vs metadata externa del archivo
- subir archivo al backend real usando `multipart/form-data`
- mostrar estado de carga
- evitar doble submit mientras la importacion esta en curso
- mostrar error de red o backend
- mostrar resumen de importacion
- listar errores por fila
- navegar rapido a `/proveedor/productos`
- permitir una nueva carga luego de finalizar

## Integracion tecnica

Contrato consumido:
- `POST /proveedores/{proveedorId}/productos/importacion`

Campo `multipart` enviado:
- `archivo`

Tipos soportados por frontend:
- `.csv`
- `.xlsx`

Comportamiento extra v1 para `CSV`:
- lectura local del archivo
- deteccion de encabezados
- sugerencia automatica de mapeo por nombre
- edicion manual del mapeo por campo requerido
- generacion de un `CSV` normalizado antes del submit
- bloqueo de importacion si faltan campos por mapear o si hay columnas duplicadas en el mapeo

Respuesta integrada:
- `filasLeidas`
- `creados`
- `actualizados`
- `rechazados`
- `errores`
  - `fila`
  - `sku`
  - `mensaje`

## Decisiones tomadas

- Se mantuvo la ruta real ya definida por el modulo proveedor: `/proveedor/carga-excel`.
- Se mantuvo al backend como fuente de verdad para la importacion final.
- Se agrego previsualizacion y mapeo local solo para `CSV` porque era el formato con el bloqueo operativo actual.
- `XLSX` sigue con subida directa al backend en esta v1 ampliada.
- El mapeo frontend normaliza el archivo antes de subirlo, pero no altera reglas de negocio ni interpreta datos fuera del contrato esperado.
- Se explicita que la pantalla importa oferta proveedor y no canoniza catalogo.
- Los datos auxiliares del archivo no deben presentarse como taxonomia interna ni como vinculacion automatica a `ProductoCanonico`.
- La UI de mapeo se reorganiza alrededor de campos destino del API (`sku`, `nombre/descripcion`, `precioCosto`, `stock`) y no alrededor de columnas crudas del archivo.
- Se invalida el cache de `/proveedor/productos` luego de una importacion exitosa para que el listado refleje lo procesado.
- Se reutilizo el lenguaje visual y los componentes del panel proveedor para mantener consistencia.

## Estados cubiertos

- inicial sin archivo
- archivo invalido
- preview CSV en preparacion
- preview CSV disponible
- mapeo incompleto
- mapeo con columnas duplicadas
- proveedor no resuelto
- subida en progreso
- error de red o backend
- importacion exitosa sin errores
- importacion exitosa con errores por fila

## Validacion

- `npm run build`

## Pendientes

- validar en entorno real con archivos ejemplo `CSV/XLSX`
- definir si en una siguiente iteracion tambien se quiere preview y mapeo para `XLSX`
- evaluar si conviene actualizar otros textos del dashboard proveedor para reflejar el nuevo estado operativo en mas superficies
- alinear aun mas la UI para separar claramente datos operativos de oferta vs metadata externa no canonica
