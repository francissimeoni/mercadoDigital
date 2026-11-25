# Retiro de Mercado Pago de MercadoDigital

Decisión del propietario del 24/11/2026: no utilizar Mercado Pago para ventas, membresías ni otras operaciones de MercadoDigital. Autorizó el despliegue compartido y las migraciones V55–V57, con catálogo nuevo únicamente para Mega Reconquista.

## Alcance y separación de Vidra

- Se modifica exclusivamente este proyecto y sus rutas `/opt/mercado-digital` y `/var/www/mercado-digital-*`.
- Solo se permite reiniciar `mercado-digital.service` (8081). Vidra usa `vidra-backend.service` (8082).
- No se revocan claves, aplicaciones, permisos ni webhooks en el proveedor externo: podrían estar compartidos con Vidra.
- No se modifica ni reinicia Nginx, Docker, MySQL ni Vidra. Se conservan las configuraciones y credenciales existentes de Vidra.
- `artifacts/piloto-mega/vidra-antes.json` registra PID, estado, hashes de configuración/entorno/binarios y respuesta HTTPS 200. El despliegue exige una comparación idéntica al terminar.

## Implementación

`mercadopago.enabled` queda en `false` por defecto y en ambos perfiles de aplicación. El despliegue fija además `MERCADOPAGO_ENABLED=false` únicamente en el archivo de entorno de MercadoDigital. No depende de la bandera de catálogo de una tienda.

- Las rutas de vinculación/configuración, callbacks, webhooks y apertura/reanudación de checkout responden 410 sin ejecutar sus controladores, incluidas rutas codificadas.
- Los servicios rechazan la creación de pedidos PSP antes de crear clientes/pedidos y la reapertura antes de consultar o modificar pagos, incluso con una URL de checkout previamente guardada.
- Los cuatro transportes del cliente (preferencias, consulta de pagos, OAuth y payouts) verifican el retiro antes de cualquier llamada externa, aun si se conservan credenciales.
- El cierre logístico histórico omite el payout y audita `SPLIT_PAYOUT_RETIRADO`. No registra fondos enviados ni un payout exitoso ficticio.
- La API pública no ofrece Mercado Pago ni su recargo. No se eliminan importes ni estados históricos.
- La interfaz retira controles de vinculación/cobro y no consulta la configuración MD retirada. El panel explica que los pagos se coordinan fuera de la plataforma. Los pedidos PSP anteriores permiten consultar su historial, sin enlace ni redirección de cobro; los comprobantes de transferencias siguen disponibles.
- El checklist de publicación ya no exige configurar Mercado Pago.

No se encontró un circuito separado de suscripciones/membresías automáticas en los fuentes examinados. No se creó otro mecanismo de cobro de membresías. El bloqueo del cliente central también evita usarlo para futuros cobros sin un cambio explícito de configuración/código.

## Evidencia y validaciones

Los logs y resultados están en `artifacts/piloto-mega/`, con prefijo `retiro-mp` y `retiro-http`. Se compilan backend y frontend y se ejecutan las suites completas pertinentes. Las pruebas del adaptador histórico habilitan explícitamente la integración solo dentro de sus mocks/entornos de prueba; las nuevas pruebas verifican el retiro por defecto.

Los primeros ensayos detectaron expectativas anteriores que permitían reabrir pagos PSP y verificaciones Mockito que no contemplaban el nuevo control previo de habilitación. Se conservaron las pruebas de contratos históricos en un escenario explícito y se agregaron pruebas separadas del comportamiento retirado; no se habilita la integración en producción para satisfacerlas.

## Límites operativos

- Se conserva código del adaptador y datos históricos para trazabilidad. Retirar la operación no equivale a eliminar esas tablas o clases.
- Los enlaces o suscripciones que ya existan directamente en la cuenta externa no se cancelan desde este despliegue. No se hicieron cobros reales, reembolsos ni pruebas financieras sobre Vidra.
- Si llega un pago antiguo después del retiro, MercadoDigital no lo conciliará automáticamente mediante webhooks; deberá revisarse con la tienda/proveedor de pago antes de cobrar nuevamente.
- El proxy mantiene su límite previo de 20 MB por solicitud. El backend admite 5 MB por imagen y 26 MB totales, pero una solicitud de más de 20 MB será rechazada por el proxy. No se cambia Nginx en este piloto.
- Google conserva su configuración. Una respuesta de inicio de sesión/redirect no equivale a probar un ingreso completo con una cuenta real.

El resultado efectivo del despliegue y sus comprobaciones se registra en `PILOTO_MEGA_RECONQUISTA.md` y `artifacts/piloto-mega/despliegue-resultados.json`; este documento por sí solo no afirma que se haya publicado.
