import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

/** Operación explícita por tienda; no activa modos, ejecuta DDL ni modifica precios existentes. */
public class ExtenderCatalogoTiendas {
    static final ObjectMapper JSON = new ObjectMapper();
    static List<Map<String,Object>> rows(Connection c, String sql, String id) throws Exception {
        try(var st=c.prepareStatement(sql)) {
            st.setString(1,id);
            try(var rs=st.executeQuery()) {
                var result=new ArrayList<Map<String,Object>>();var meta=rs.getMetaData();
                while(rs.next()) {
                    var row=new LinkedHashMap<String,Object>();
                    for(int i=1;i<=meta.getColumnCount();i++) {
                        Object value=rs.getObject(i);
                        row.put(meta.getColumnLabel(i),value==null?null:value instanceof byte[] b?HexFormat.of().formatHex(b):value.toString());
                    }
                    result.add(row);
                }
                return result;
            }
        }
    }
    static String text(Map<String,Object> row,String key){return Objects.toString(row.get(key),null);}
    static boolean yes(Map<String,Object> row,String key){return Set.of("1","true","01").contains(Objects.toString(row.get(key),""));}
    static int exec(Connection c,String sql,Object... values)throws Exception{
        try(var st=c.prepareStatement(sql)){for(int i=0;i<values.length;i++)st.setObject(i+1,values[i]);return st.executeUpdate();}
    }
    static String digest(Object value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(value)));}
    static String phone(String value) {
        if(value==null||value.isBlank()||!value.trim().matches("[+0-9() .-]+"))return null;
        String raw=value.trim(),digits=raw.replaceAll("\\D","");
        String candidate=raw.startsWith("+")?"+"+digits:digits.startsWith("00")?"+"+digits.substring(2):digits.startsWith("54")?"+"+digits:digits.matches("[123][0-9]{9}")?"+549"+digits:null;
        if(candidate==null)return null;
        try{
            var util=PhoneNumberUtil.getInstance();var number=util.parse(candidate,"ZZ");
            return util.isValidNumber(number)?util.format(number,PhoneNumberUtil.PhoneNumberFormat.E164):null;
        }catch(Exception ex){return null;}
    }
    static Map<String,Object> snapshot(Connection c,String store,String domain,boolean lock)throws Exception{
        String suffix=lock?" FOR UPDATE":"";
        var sf=rows(c,"SELECT id,revendedor_id,nombre_comercial,dominio_publico,tipo_storefront,telefono_comercial,publicada,updated_at FROM revendedor_storefront WHERE revendedor_id=?"+suffix,store);
        var rev=rows(c,"SELECT id,dominio,activo,updated_at FROM revendedores WHERE id=?"+suffix,store);
        if(sf.size()!=1||rev.size()!=1||!"REVENDEDOR".equals(sf.get(0).get("tipo_storefront")))throw new IllegalStateException("No es una tienda de revendedor");
        String actual=text(sf.get(0),"dominio_publico");if(actual==null)actual=text(rev.get(0),"dominio");
        if(!domain.equals(actual))throw new IllegalStateException("Dominio distinto al revisado");
        var data=new LinkedHashMap<String,Object>();data.put("tienda",store);data.put("dominio",domain);data.put("storefront",sf);data.put("revendedor",rev);
        data.put("productos",rows(c,"SELECT * FROM revendedor_productos WHERE revendedor_id=? ORDER BY id"+suffix,store));
        data.put("configuraciones",rows(c,"SELECT c.* FROM catalogo_productos_configuracion c JOIN revendedor_productos p ON p.id=c.producto_id WHERE p.revendedor_id=? ORDER BY c.producto_id"+suffix,store));
        data.put("configTienda",rows(c,"SELECT * FROM catalogo_configuraciones WHERE revendedor_id=?"+suffix,store));
        data.put("envios",rows(c,"SELECT * FROM catalogo_categorias_envio WHERE revendedor_id=? ORDER BY id"+suffix,store));
        data.put("asesores",rows(c,"SELECT id,revendedor_id,nombre,activo,whatsapp_comercial,updated_at FROM asesores_venta WHERE revendedor_id=? ORDER BY id"+suffix,store));
        return data;
    }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> list(Map<String,Object> data,String key){return (List<Map<String,Object>>)data.get(key);}
    static Map<String,Object> review(Map<String,Object> data)throws Exception{
        var config=new HashMap<String,Map<String,Object>>();for(var row:list(data,"configuraciones"))config.put(text(row,"producto_id"),row);
        var pending=new ArrayList<Map<String,Object>>();var issues=new ArrayList<String>();int preserve=0;
        for(var p:list(data,"productos")){
            if(config.containsKey(p.get("id"))) {preserve++;continue;}
            String value=text(p,"precio_venta");
            if(!"ARS".equals(p.get("moneda"))||value==null||new BigDecimal(value).signum()<=0) {issues.add("Producto sin precio ARS válido: "+p.get("id"));continue;}
            new BigDecimal(value).setScale(2,java.math.RoundingMode.UNNECESSARY);
            pending.add(p);
        }
        var sf=list(data,"storefront").get(0);String old=text(sf,"telefono_comercial"),normalized=phone(old);
        boolean contact=normalized!=null;
        for(var a:list(data,"asesores"))if(yes(a,"activo")&&phone(text(a,"whatsapp_comercial"))!=null)contact=true;
        if(!contact)issues.add("Sin contacto comercial válido");
        var result=new LinkedHashMap<String,Object>();result.put("snapshot",data);result.put("token",digest(data));result.put("pendientes",pending);result.put("preservados",preserve);result.put("problemas",issues);
        result.put("telefonoAnterior",old);result.put("telefonoNormalizado",normalized);
        result.put("evidenciaTelefono","Diez dígitos locales -> +549, mismo destino de normalizeWhatsAppPhone histórico; validado con libphonenumber. Sin inferir números ausentes.");
        return result;
    }
    static Map<String,Object> apply(Connection c,Map<String,Object> review)throws Exception{
        if(!((List<?>)review.get("problemas")).isEmpty())throw new IllegalStateException("Tienda pendiente: "+review.get("problemas"));
        @SuppressWarnings("unchecked") var data=(Map<String,Object>)review.get("snapshot");
        String store=text(data,"tienda");var created=new ArrayList<Map<String,Object>>();
        for(var p:list(review,"pendientes")){
            String id=UUID.randomUUID().toString();
            exec(c,"INSERT INTO catalogo_productos_configuracion(id,producto_id,moneda,precio_original,incluye_envio,categoria_envio_id,version,created_at,updated_at) VALUES(?,?,'ARS',?,b'1',NULL,0,NOW(6),NOW(6))",id,p.get("id"),new BigDecimal(text(p,"precio_venta")));
            created.add(rows(c,"SELECT * FROM catalogo_productos_configuracion WHERE id=?",id).get(0));
        }
        Map<String,Object> config=null;
        if(list(data,"configTienda").isEmpty()){
            String id=UUID.randomUUID().toString();exec(c,"INSERT INTO catalogo_configuraciones(id,revendedor_id,cotizacion_usd,condiciones_entrega,version,created_at,updated_at) VALUES(?,?,NULL,NULL,0,NOW(6),NOW(6))",id,store);
            config=rows(c,"SELECT * FROM catalogo_configuraciones WHERE id=?",id).get(0);
        }
        var before=list(data,"storefront").get(0);Map<String,Object> after=null;
        String normalized=text(review,"telefonoNormalizado");
        if(normalized!=null&&!normalized.equals(review.get("telefonoAnterior"))){
            if(exec(c,"UPDATE revendedor_storefront SET telefono_comercial=?,updated_at=NOW(6) WHERE revendedor_id=?",normalized,store)!=1)throw new IllegalStateException("Contacto no actualizado");
            after=rows(c,"SELECT id,revendedor_id,nombre_comercial,dominio_publico,tipo_storefront,telefono_comercial,publicada,updated_at FROM revendedor_storefront WHERE revendedor_id=?",store).get(0);
        }
        var receipt=new LinkedHashMap<String,Object>();receipt.put("tienda",store);receipt.put("dominio",data.get("dominio"));receipt.put("revisionToken",review.get("token"));receipt.put("creados",created);receipt.put("configCreada",config);receipt.put("storefrontAnterior",before);receipt.put("storefrontActualizado",after);receipt.put("operacionId",UUID.randomUUID().toString());
        if(!created.isEmpty()||config!=null||after!=null)audit(c,receipt,"CATALOGO_EXTENSION_TIENDAS");
        return receipt;
    }
    static void audit(Connection c,Map<String,Object> receipt,String type)throws Exception{
        exec(c,"INSERT INTO auditoria_eventos(id,tipo_evento,referencia_id,payload_json,fecha_evento,created_at,updated_at) VALUES(?,?,?,?,NOW(6),NOW(6),NOW(6))",receipt.get("operacionId"),type,receipt.get("tienda"),JSON.writeValueAsString(receipt));
    }
    static void revert(Connection c,Map<String,Object> receipt)throws Exception{
        String store=text(receipt,"tienda");var current=snapshot(c,store,text(receipt,"dominio"),true);
        for(var row:list(receipt,"creados")){
            var found=rows(c,"SELECT * FROM catalogo_productos_configuracion WHERE id=? FOR UPDATE",text(row,"id"));
            if(found.size()!=1||!found.get(0).equals(row))throw new IllegalStateException("Configuración editada después de migrar; no revertir: "+row.get("id"));
        }
        if(receipt.get("configCreada") instanceof Map<?,?> config){
            if(!list(current,"configTienda").equals(List.of(config)))throw new IllegalStateException("Configuración de tienda editada");
            if(list(current,"configuraciones").size()!=list(receipt,"creados").size()||!list(current,"envios").isEmpty())throw new IllegalStateException("Hay configuraciones nuevas posteriores");
        }
        if(receipt.get("storefrontActualizado") instanceof Map<?,?> sf&&!sf.equals(list(current,"storefront").get(0)))throw new IllegalStateException("Storefront editado después de migrar");
        for(var row:list(receipt,"creados"))exec(c,"DELETE FROM catalogo_productos_configuracion WHERE id=?",row.get("id"));
        if(receipt.get("configCreada") instanceof Map<?,?> config)exec(c,"DELETE FROM catalogo_configuraciones WHERE id=?",config.get("id"));
        if(receipt.get("storefrontActualizado")!=null){@SuppressWarnings("unchecked") var before=(Map<String,Object>)receipt.get("storefrontAnterior");exec(c,"UPDATE revendedor_storefront SET telefono_comercial=?,updated_at=? WHERE revendedor_id=?",before.get("telefono_comercial"),before.get("updated_at"),store);}
        var event=new LinkedHashMap<String,Object>();event.put("tienda",store);event.put("operacionId",UUID.randomUUID().toString());event.put("revertida",receipt.get("operacionId"));audit(c,event,"CATALOGO_EXTENSION_REVERSION");
    }
    public static void main(String[] args)throws Exception{
        if(args.length<4)throw new IllegalArgumentException("Uso: conexión.properties tienda dominio preview | apply token | revert recibo.json");
        String store=UUID.fromString(args[1]).toString(),mode=args[3];boolean preview=mode.equals("preview");
        if(!(preview&&args.length==4)&&!((mode.equals("apply")||mode.equals("revert"))&&args.length==5))throw new IllegalArgumentException("Argumentos inválidos");
        var properties=new Properties();try(var reader=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){properties.load(reader);}
        try(var c=DriverManager.getConnection(properties.getProperty("url"),properties.getProperty("user"),properties.getProperty("password"))){
            c.setTransactionIsolation(preview?Connection.TRANSACTION_REPEATABLE_READ:Connection.TRANSACTION_READ_COMMITTED);c.setReadOnly(preview);c.setAutoCommit(false);
            try{
                Map<String,Object> result;
                if(mode.equals("revert")){
                    @SuppressWarnings("unchecked") var receipt=(Map<String,Object>)JSON.readValue(Files.readString(Path.of(args[4])),Map.class);
                    if(!store.equals(receipt.get("tienda"))||!args[2].equals(receipt.get("dominio")))throw new IllegalArgumentException("Recibo de otra tienda");
                    revert(c,receipt);result=Map.of("tienda",store,"revertido",true);
                }else{
                    var revision=review(snapshot(c,store,args[2],!preview));
                    if(preview){c.rollback();System.out.println(JSON.writeValueAsString(revision));return;}
                    if(!MessageDigest.isEqual(text(revision,"token").getBytes(StandardCharsets.UTF_8),args[4].getBytes(StandardCharsets.UTF_8)))throw new IllegalStateException("Datos cambiaron; generar nueva simulación");
                    result=apply(c,revision);
                }
                c.commit();System.out.println(JSON.writeValueAsString(result));
            }catch(Exception ex){c.rollback();throw ex;}
        }
    }
}
