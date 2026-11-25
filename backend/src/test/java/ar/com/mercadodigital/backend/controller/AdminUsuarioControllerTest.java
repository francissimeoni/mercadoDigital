package ar.com.mercadodigital.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.mercadodigital.backend.dto.response.AdminUsuarioResponse;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.service.AdminUsuarioService;

@WebMvcTest(AdminUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUsuarioService adminUsuarioService;

    @Test
    void listadoUsuariosDevuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(adminUsuarioService.listar()).thenReturn(List.of(
                AdminUsuarioResponse.builder()
                        .id(usuarioId)
                        .nombre("Proveedor Uno")
                        .email("proveedor@demo.com")
                        .tipoUsuario(TipoUsuario.PROVEEDOR)
                        .activo(true)
                        .build()));

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(usuarioId.toString()))
                .andExpect(jsonPath("$[0].email").value("proveedor@demo.com"))
                .andExpect(jsonPath("$[0].tipoUsuario").value("PROVEEDOR"));
    }

    @Test
    void crearUsuarioDevuelve201() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(adminUsuarioService.crear(org.mockito.ArgumentMatchers.any())).thenReturn(
                AdminUsuarioResponse.builder()
                        .id(usuarioId)
                        .nombre("Proveedor Uno")
                        .email("proveedor@demo.com")
                        .tipoUsuario(TipoUsuario.PROVEEDOR)
                        .activo(true)
                        .build());

        String body = """
                {
                  "nombre": "Proveedor Uno",
                  "email": "proveedor@demo.com",
                  "password": "secreto123",
                  "tipoUsuario": "PROVEEDOR",
                  "activo": true
                }
                """;

        mockMvc.perform(post("/admin/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(usuarioId.toString()))
                .andExpect(jsonPath("$.tipoUsuario").value("PROVEEDOR"));
    }

    @Test
    void actualizarUsuarioDevuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(adminUsuarioService.actualizar(org.mockito.ArgumentMatchers.eq(usuarioId), org.mockito.ArgumentMatchers.any())).thenReturn(
                AdminUsuarioResponse.builder()
                        .id(usuarioId)
                        .nombre("Proveedor Uno Editado")
                        .email("proveedor.editado@demo.com")
                        .tipoUsuario(TipoUsuario.PROVEEDOR)
                        .activo(false)
                        .build());

        String body = """
                {
                  "nombre": "Proveedor Uno Editado",
                  "email": "proveedor.editado@demo.com",
                  "tipoUsuario": "PROVEEDOR",
                  "activo": false,
                  "nombreEntidad": "Proveedor Uno SA",
                  "proveedorDespachante": true
                }
                """;

        mockMvc.perform(put("/admin/usuarios/{id}", usuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId.toString()))
                .andExpect(jsonPath("$.activo").value(false));
    }
}
