package ar.com.mercadodigital.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

class MultitenantModeloMappingTest {

    @Test
    void revendedorStorefrontMantieneMappingEsperado() throws Exception {
        Table table = RevendedorStorefront.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("revendedor_storefront", table.name());

        RevendedorStorefront storefront = new RevendedorStorefront();
        assertFalse(storefront.isPublicada());

        Field revendedorField = RevendedorStorefront.class.getDeclaredField("revendedor");
        assertNotNull(revendedorField.getAnnotation(OneToOne.class));
        JoinColumn joinColumn = revendedorField.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn);
        assertEquals("revendedor_id", joinColumn.name());
        assertTrue(joinColumn.nullable());
        assertTrue(joinColumn.unique());

        Field nombreComercialField = RevendedorStorefront.class.getDeclaredField("nombreComercial");
        Column nombreComercialColumn = nombreComercialField.getAnnotation(Column.class);
        assertNotNull(nombreComercialColumn);
        assertEquals("nombre_comercial", nombreComercialColumn.name());
        assertFalse(nombreComercialColumn.nullable());
    }

    @Test
    void clienteRevendedorMantieneMappingEsperado() throws Exception {
        Table table = ClienteRevendedor.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("cliente_revendedor", table.name());

        ClienteRevendedor clienteRevendedor = new ClienteRevendedor();
        assertTrue(clienteRevendedor.isActivo());
        assertNotNull(clienteRevendedor.getFechaAlta());

        Field usuarioField = ClienteRevendedor.class.getDeclaredField("usuario");
        assertNotNull(usuarioField.getAnnotation(ManyToOne.class));
        JoinColumn usuarioJoin = usuarioField.getAnnotation(JoinColumn.class);
        assertNotNull(usuarioJoin);
        assertEquals("usuario_id", usuarioJoin.name());
        assertFalse(usuarioJoin.nullable());

        Field revendedorField = ClienteRevendedor.class.getDeclaredField("revendedor");
        assertNotNull(revendedorField.getAnnotation(ManyToOne.class));
        JoinColumn revendedorJoin = revendedorField.getAnnotation(JoinColumn.class);
        assertNotNull(revendedorJoin);
        assertEquals("revendedor_id", revendedorJoin.name());
        assertFalse(revendedorJoin.nullable());

        Field origenAltaField = ClienteRevendedor.class.getDeclaredField("origenAlta");
        Column origenAltaColumn = origenAltaField.getAnnotation(Column.class);
        assertNotNull(origenAltaColumn);
        assertEquals("origen_alta", origenAltaColumn.name());
        assertFalse(origenAltaColumn.nullable());
    }

    @Test
    void revendedorYUsuarioExponenRelacionesNuevas() throws Exception {
        Field storefrontField = Revendedor.class.getDeclaredField("storefront");
        OneToOne storefrontRelation = storefrontField.getAnnotation(OneToOne.class);
        assertNotNull(storefrontRelation);
        assertEquals("revendedor", storefrontRelation.mappedBy());

        Field clientesRevendedorField = Revendedor.class.getDeclaredField("clientes");
        OneToMany clientesRevendedorRelation = clientesRevendedorField.getAnnotation(OneToMany.class);
        assertNotNull(clientesRevendedorRelation);
        assertEquals("revendedor", clientesRevendedorRelation.mappedBy());

        Field usuarioClientesField = Usuario.class.getDeclaredField("clientesRevendedor");
        OneToMany usuarioClientesRelation = usuarioClientesField.getAnnotation(OneToMany.class);
        assertNotNull(usuarioClientesRelation);
        assertEquals("usuario", usuarioClientesRelation.mappedBy());
    }
}
