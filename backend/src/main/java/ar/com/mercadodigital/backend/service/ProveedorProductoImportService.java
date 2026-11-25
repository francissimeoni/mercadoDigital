package ar.com.mercadodigital.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.response.ProveedorProductoImportErrorResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorProductoImportResponse;
import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorProductoImportService {

    private static final String MONEDA_SOPORTADA = "ARS";
    private static final int NOMBRE_MAX = 180;

    private final ProveedorOwnershipService proveedorOwnershipService;
    private final ProductoRepository productoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final PlatformTransactionManager transactionManager;

    public ProveedorProductoImportResponse importar(UUID proveedorId, MultipartFile archivo, Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        validateArchivo(archivo);

        List<FilaImportacion> filas = parseArchivo(archivo);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int creados = 0;
        int actualizados = 0;
        int rechazados = 0;
        List<ProveedorProductoImportErrorResponse> errores = new ArrayList<>();

        for (FilaImportacion fila : filas) {
            try {
                ResultadoFila resultado = transactionTemplate.execute(status -> upsertFila(proveedor, fila));
                if (resultado == ResultadoFila.CREADO) {
                    creados++;
                } else {
                    actualizados++;
                }
            } catch (RuntimeException ex) {
                rechazados++;
                errores.add(ProveedorProductoImportErrorResponse.builder()
                        .fila(fila.numeroFila())
                        .sku(fila.sku())
                        .mensaje(ex.getMessage())
                        .build());
            }
        }

        return ProveedorProductoImportResponse.builder()
                .filasLeidas(filas.size())
                .creados(creados)
                .actualizados(actualizados)
                .rechazados(rechazados)
                .errores(errores)
                .build();
    }

    private ResultadoFila upsertFila(Proveedor proveedor, FilaImportacion fila) {
        validarFila(fila);

        Optional<Producto> existing = productoRepository.findByProveedor_IdAndSkuIgnoreCase(proveedor.getId(), fila.sku());
        Producto producto = existing.orElseGet(Producto::new);
        producto.setProveedor(proveedor);
        producto.setSku(fila.sku());
        producto.setNombre(truncar(fila.descripcion()));
        producto.setDescripcion(fila.descripcion());
        producto.setPrecioCosto(fila.precio());
        producto.setMoneda(MONEDA_SOPORTADA);
        producto.setActivo(true);
        if (!StringUtils.hasText(producto.getCategoriaPaquete())) {
            producto.setCategoriaPaquete(CategoriaPaquete.CHICO.name());
        }

        Producto saved = productoRepository.save(producto);

        ProveedorStock stock = proveedorStockRepository
                .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(proveedor.getId(), saved.getId())
                .orElseGet(ProveedorStock::new);
        stock.setProveedor(proveedor);
        stock.setProducto(saved);
        stock.setStock(fila.stockTotal());
        stock.setPrecioCostoActual(fila.precio());
        proveedorStockRepository.save(stock);

        return existing.isPresent() ? ResultadoFila.ACTUALIZADO : ResultadoFila.CREADO;
    }

    private void validateArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("archivo es obligatorio");
        }
        String nombre = archivo.getOriginalFilename();
        if (!StringUtils.hasText(nombre)) {
            throw new BadRequestException("nombre de archivo invalido");
        }
        String normalized = nombre.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".csv") && !normalized.endsWith(".xlsx")) {
            throw new BadRequestException("Formato no soportado. Solo se admite CSV o XLSX.");
        }
    }

    private List<FilaImportacion> parseArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            return nombre.endsWith(".xlsx") ? parseXlsx(archivo.getInputStream()) : parseCsv(archivo.getInputStream());
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo importado");
        }
    }

    private List<FilaImportacion> parseCsv(InputStream inputStream) throws IOException {
        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            content = reader.lines().reduce("", (acc, line) -> acc + line + "\n");
        }
        content = stripBom(content);
        String[] lines = content.split("\\R", 2);
        if (lines.length == 0 || !StringUtils.hasText(lines[0])) {
            throw new BadRequestException("El archivo no contiene cabecera");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(lines[0]))
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = CSVParser.parse(content, format)) {
            Map<String, Integer> indices = normalizeHeaders(parser.getHeaderMap().keySet());
            validateHeaders(indices);
            List<FilaImportacion> filas = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    values.put(normalizeHeader(header), record.get(header));
                }
                if (isEmptyRow(values.values())) {
                    continue;
                }
                filas.add(mapFila((int) record.getRecordNumber() + 1, values));
            }
            return filas;
        }
    }

    private List<FilaImportacion> parseXlsx(InputStream inputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("El archivo no contiene hojas");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.rowIterator();
            if (!iterator.hasNext()) {
                throw new BadRequestException("El archivo no contiene cabecera");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = iterator.next();
            Map<String, Integer> indices = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                indices.put(normalizeHeader(formatter.formatCellValue(cell)), cell.getColumnIndex());
            }
            validateHeaders(indices);

            List<FilaImportacion> filas = new ArrayList<>();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> entry : indices.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    values.put(entry.getKey(), cell == null ? "" : formatter.formatCellValue(cell));
                }
                if (isEmptyRow(values.values())) {
                    continue;
                }
                filas.add(mapFila(row.getRowNum() + 1, values));
            }
            return filas;
        }
    }

    private Map<String, Integer> normalizeHeaders(Iterable<String> headers) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int index = 0;
        for (String header : headers) {
            result.put(normalizeHeader(header), index++);
        }
        return result;
    }

    private void validateHeaders(Map<String, Integer> headers) {
        List<String> faltantes = new ArrayList<>();
        if (!headers.containsKey("codigo")) {
            faltantes.add("Codigo");
        }
        if (!headers.containsKey("descripcion")) {
            faltantes.add("Descripcion");
        }
        if (!headers.containsKey("precio")) {
            faltantes.add("precio");
        }
        if (!faltantes.isEmpty()) {
            throw new BadRequestException("Faltan columnas requeridas: " + String.join(", ", faltantes));
        }
    }

    private FilaImportacion mapFila(int numeroFila, Map<String, String> values) {
        return new FilaImportacion(
                numeroFila,
                clean(values.get("codigo")),
                clean(values.get("descripcion")),
                parseBigDecimal(values.get("precio"), "precio"),
                sumarStock(values));
    }

    private int sumarStock(Map<String, String> values) {
        return parseInteger(values.get("ros"), "ROS")
                + parseInteger(values.get("mza"), "MZA")
                + parseInteger(values.get("cba"), "CBA")
                + parseInteger(values.get("lug"), "LUG");
    }

    private void validarFila(FilaImportacion fila) {
        if (!StringUtils.hasText(fila.sku())) {
            throw new BadRequestException("Codigo/sku es obligatorio");
        }
        if (!StringUtils.hasText(fila.descripcion())) {
            throw new BadRequestException("Descripcion es obligatoria");
        }
        if (fila.precio() == null || fila.precio().signum() <= 0) {
            throw new BadRequestException("precio debe ser mayor a 0");
        }
        if (fila.stockTotal() < 0) {
            throw new BadRequestException("stock consolidado debe ser mayor o igual a 0");
        }
    }

    private BigDecimal parseBigDecimal(String rawValue, String fieldName) {
        String value = clean(rawValue);
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(fieldName + " es obligatorio");
        }

        String normalized = value.replace(" ", "").replace("$", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
                normalized = normalized.replace(".", "").replace(",", ".");
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(fieldName + " invalido: " + value);
        }
    }

    private int parseInteger(String rawValue, String fieldName) {
        String value = clean(rawValue);
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return parseBigDecimal(value, fieldName).intValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException(fieldName + " debe ser entero: " + value);
        }
    }

    private boolean isEmptyRow(Iterable<String> values) {
        for (String value : values) {
            if (StringUtils.hasText(clean(value))) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String value) {
        String normalized = clean(value);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return normalized;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF'
                ? value.substring(1)
                : value;
    }

    private char detectDelimiter(String headerLine) {
        long semicolon = headerLine.chars().filter(ch -> ch == ';').count();
        long comma = headerLine.chars().filter(ch -> ch == ',').count();
        long tab = headerLine.chars().filter(ch -> ch == '\t').count();
        if (semicolon >= comma && semicolon >= tab && semicolon > 0) {
            return ';';
        }
        if (tab >= comma && tab > 0) {
            return '\t';
        }
        return ',';
    }

    private String truncar(String descripcion) {
        if (descripcion.length() <= NOMBRE_MAX) {
            return descripcion;
        }
        return descripcion.substring(0, NOMBRE_MAX);
    }

    private enum ResultadoFila {
        CREADO,
        ACTUALIZADO
    }

    private record FilaImportacion(
            int numeroFila,
            String sku,
            String descripcion,
            BigDecimal precio,
            int stockTotal) {
    }
}
