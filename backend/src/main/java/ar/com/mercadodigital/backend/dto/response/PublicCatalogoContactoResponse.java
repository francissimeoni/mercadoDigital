package ar.com.mercadodigital.backend.dto.response;

import java.util.List;
import java.util.UUID;

/** Contrato comercial público: sin configuración de costos ni datos de cuentas internas. */
public record PublicCatalogoContactoResponse(
        UUID productoId, String nombre, String sku, String precioPublicoArs,
        String condicionesEntrega, String storefrontHost, List<Contacto> contactos) {
    public record Contacto(String id, String nombre, boolean responsable, String whatsapp,
                           String fotoUrl, String provincia, String localidad, String zonaAtencion) {
        public Contacto(String id, String nombre, boolean responsable, String whatsapp) {
            this(id, nombre, responsable, whatsapp, null, null, null, null);
        }
    }
}
