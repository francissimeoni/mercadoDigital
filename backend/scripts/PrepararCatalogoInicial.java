import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Utilidad operativa: una tienda por ejecución. No activa el catálogo ni ejecuta DDL. */
public class PrepararCatalogoInicial {
    static final ObjectMapper JSON = new ObjectMapper();
    record Producto(String id, String sku, String nombre, String moneda, String precioArs,
                    boolean activo, Integer stock, String configuracionId, Long versionConfiguracion,
                    String estado) {}
    record Revision(String tiendaId, String dominio, String nombre, List<Producto> productos, String token) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException("Uso: PrepararCatalogoInicial.java conexion.properties UUID dominio preview|apply [token]");
        }
        boolean apply = args[3].equals("apply");
        if ((!apply && !args[3].equals("preview")) || apply != (args.length == 5)) {
            throw new IllegalArgumentException("Vista previa sin token; aplicación con token de la revisión vigente.");
        }
        String tienda = UUID.fromString(args[1]).toString();
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)) { properties.load(reader); }
        String url = Objects.requireNonNull(properties.getProperty("url"), "Falta URL JDBC");
        try (Connection connection = DriverManager.getConnection(url, properties.getProperty("user"), properties.getProperty("password"))) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setReadOnly(!apply);
            connection.setAutoCommit(false);
            try {
                Revision revision = revisar(connection, tienda, args[2], apply);
                if (!apply) {
                    connection.rollback();
                    System.out.println(JSON.writeValueAsString(revision));
                    return;
                }
                if (!MessageDigest.isEqual(revision.token().getBytes(StandardCharsets.UTF_8), args[4].getBytes(StandardCharsets.UTF_8))) {
                    throw new IllegalStateException("La tienda o sus productos cambiaron. Generá y revisá una nueva vista previa.");
                }
                List<Map<String, String>> insertados = new ArrayList<>();
                for (Producto producto : revision.productos()) {
                    if (!producto.estado().equals("PREPARAR")) continue;
                    String id = UUID.randomUUID().toString();
                    try (var statement = connection.prepareStatement("INSERT INTO catalogo_productos_configuracion "
                            + "(id,producto_id,moneda,precio_original,incluye_envio,categoria_envio_id,version,created_at,updated_at) "
                            + "VALUES(?,?,'ARS',?,b'1',NULL,0,NOW(6),NOW(6))")) {
                        statement.setString(1, id); statement.setString(2, producto.id());
                        statement.setBigDecimal(3, new BigDecimal(producto.precioArs()));
                        statement.executeUpdate();
                    }
                    insertados.add(Map.of("productoId", producto.id(), "configuracionId", id, "precioArs", producto.precioArs()));
                }
                String operacion = UUID.randomUUID().toString();
                var resultado = new LinkedHashMap<String, Object>();
                resultado.put("tiendaId", tienda); resultado.put("dominio", revision.dominio());
                resultado.put("revisionToken", revision.token()); resultado.put("operacionId", operacion);
                resultado.put("configuracionesCreadas", insertados);
                resultado.put("criterio", "Precio anterior ARS con envío incluido, confirmado por el propietario. Sin recargos.");
                resultado.put("actorSql", connection.getMetaData().getUserName());
                if (!insertados.isEmpty()) {
                    try (var statement = connection.prepareStatement("INSERT INTO auditoria_eventos "
                            + "(id,tipo_evento,referencia_id,payload_json,fecha_evento,created_at,updated_at) "
                            + "VALUES(?,'CATALOGO_PREPARACION_INICIAL',?,?,NOW(6),NOW(6),NOW(6))")) {
                        statement.setString(1, operacion); statement.setString(2, tienda);
                        statement.setString(3, JSON.writeValueAsString(resultado)); statement.executeUpdate();
                    }
                }
                connection.commit();
                System.out.println(JSON.writeValueAsString(resultado));
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }

    static Revision revisar(Connection connection, String tienda, String dominio, boolean lock) throws Exception {
        // El mismo bloqueo de tienda que usan las escrituras de catálogo; filas propias
        // bloqueadas además para impedir cambios simultáneos desde el editor anterior.
        String nombre;
        try (var statement = connection.prepareStatement("SELECT nombre_comercial,dominio_publico,tipo_storefront FROM revendedor_storefront WHERE revendedor_id=?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setString(1, tienda);
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || !dominio.equals(rows.getString("dominio_publico")) || !"REVENDEDOR".equals(rows.getString("tipo_storefront"))) {
                    throw new IllegalArgumentException("El UUID y el dominio no identifican la tienda de revendedor esperada.");
                }
                nombre = rows.getString("nombre_comercial");
            }
        }
        List<Producto> productos = new ArrayList<>();
        String query = "SELECT p.id,p.sku,p.nombre,p.moneda,p.precio_venta,p.activo,p.stock,c.id configuracion_id,c.version configuracion_version "
                + "FROM revendedor_productos p LEFT JOIN catalogo_productos_configuracion c ON c.producto_id=p.id "
                + "WHERE p.revendedor_id=? ORDER BY p.id" + (lock ? " FOR UPDATE" : "");
        try (var statement = connection.prepareStatement(query)) {
            statement.setString(1, tienda);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    BigDecimal precio = rows.getBigDecimal("precio_venta");
                    String configuracion = rows.getString("configuracion_id"), moneda = rows.getString("moneda");
                    String estado = configuracion != null ? "CONSERVAR_CONFIGURADO" : !"ARS".equals(moneda)
                            ? "REVISAR_MONEDA" : precio == null || precio.signum() <= 0 ? "REVISAR_PRECIO" : "PREPARAR";
                    productos.add(new Producto(rows.getString("id"), rows.getString("sku"), rows.getString("nombre"),
                            moneda, precio == null ? null : precio.toPlainString(), rows.getBoolean("activo"),
                            (Integer) rows.getObject("stock"), configuracion,
                            configuracion == null ? null : rows.getLong("configuracion_version"), estado));
                }
            }
        }
        String token = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(JSON.writeValueAsBytes(Arrays.asList("ARS_ENVIO_INCLUIDO_V1", tienda, dominio, nombre, productos))));
        return new Revision(tienda, dominio, nombre, List.copyOf(productos), token);
    }
}
