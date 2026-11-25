package ar.com.mercadodigital.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import ar.com.mercadodigital.backend.config.MercadoPagoProperties;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorMercadoPagoOAuthLinkResponse;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.IntegracionPspException;
import ar.com.mercadodigital.backend.service.MercadoPagoOAuthStateService.OAuthSubjectType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MercadoPagoOAuthService {

    private final MercadoPagoProperties mercadoPagoProperties;
    private final MercadoPagoOAuthStateService mercadoPagoOAuthStateService;
    private final MercadoPagoClient mercadoPagoClient;
    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final ProveedorMercadoPagoService proveedorMercadoPagoService;
    private final MercadoPagoMdConfigService mercadoPagoMdConfigService;

    public AdminRevendedorMercadoPagoOAuthLinkResponse generarAuthorizationLink(UUID revendedorId) {
        return generarAuthorizationLinkFor(OAuthSubjectType.REVENDEDOR, revendedorId);
    }

    public AdminRevendedorMercadoPagoOAuthLinkResponse generarAuthorizationLinkProveedor(UUID proveedorId) {
        return generarAuthorizationLinkFor(OAuthSubjectType.PROVEEDOR, proveedorId);
    }

    public AdminRevendedorMercadoPagoOAuthLinkResponse generarAuthorizationLinkMd() {
        return generarAuthorizationLinkFor(OAuthSubjectType.MERCADO_DIGITAL, new UUID(0L, 0L));
    }

    private AdminRevendedorMercadoPagoOAuthLinkResponse generarAuthorizationLinkFor(
            OAuthSubjectType subjectType,
            UUID subjectId) {
        validarConfiguracionOauth();
        MercadoPagoOAuthStateService.OAuthStateToken stateToken = mercadoPagoOAuthStateService.generateFor(subjectType, subjectId);

        String authorizationUrl = UriComponentsBuilder
                .fromUriString(normalizeAuthBaseUrl(mercadoPagoProperties.getAuthBaseUrl()) + "/authorization")
                .queryParam("client_id", mercadoPagoProperties.getClientId().trim())
                .queryParam("response_type", "code")
                .queryParam("platform_id", "mp")
                .queryParam("redirect_uri", mercadoPagoProperties.getRedirectUri().trim())
                .queryParam("state", stateToken.value())
                .build(true)
                .toUriString();

        return AdminRevendedorMercadoPagoOAuthLinkResponse.builder()
                .authorizationUrl(authorizationUrl)
                .redirectUri(mercadoPagoProperties.getRedirectUri().trim())
                .expiresAt(stateToken.expiresAt())
                .build();
    }

    public OAuthCallbackResult procesarCallback(String code, String state) {
        mercadoPagoClient.exigirHabilitado();
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Mercado Pago no devolvio authorization code.");
        }
        MercadoPagoOAuthStateService.OAuthStateSubject subject = mercadoPagoOAuthStateService.parseSubject(state);
        MercadoPagoClient.MercadoPagoOAuthTokens tokens = mercadoPagoClient.intercambiarAuthorizationCode(code.trim(), state);
        if (subject.subjectType() == OAuthSubjectType.REVENDEDOR) {
            revendedorMercadoPagoService.linkFromOAuth(subject.subjectId(), tokens);
            return new OAuthCallbackResult(
                    OAuthSubjectType.REVENDEDOR,
                    subject.subjectId(),
                    true,
                    "La cuenta Mercado Pago del revendedor quedo vinculada correctamente.");
        } else if (subject.subjectType() == OAuthSubjectType.PROVEEDOR) {
            proveedorMercadoPagoService.linkFromOAuth(subject.subjectId(), tokens);
            return new OAuthCallbackResult(
                    OAuthSubjectType.PROVEEDOR,
                    subject.subjectId(),
                    true,
                    "La cuenta Mercado Pago del proveedor quedo vinculada correctamente.");
        }
        mercadoPagoMdConfigService.linkFromOAuth(tokens);
        return new OAuthCallbackResult(
                OAuthSubjectType.MERCADO_DIGITAL,
                subject.subjectId(),
                true,
                "La cuenta Mercado Pago de Mercado Digital quedo vinculada correctamente.");
    }

    public String buildAdminRedirectUrl(OAuthCallbackResult result) {
        String message = StringUtils.hasText(result.message()) ? result.message() : "Proceso Mercado Pago finalizado.";
        return UriComponentsBuilder.fromUriString(resolveAdminRedirectUrl(result.subjectType()))
                .queryParam("mp_oauth_status", result.success() ? "success" : "error")
                .queryParam("mp_subject_type", result.subjectType().name())
                .queryParam("mp_subject_id", result.subjectId() != null ? result.subjectId().toString() : null)
                .queryParam("mp_message", message)
                .build()
                .toUriString();
    }

    public String buildAdminErrorRedirectUrl(OAuthSubjectType subjectType, UUID subjectId, String message) {
        return UriComponentsBuilder.fromUriString(resolveAdminRedirectUrl(subjectType))
                .queryParam("mp_oauth_status", "error")
                .queryParam("mp_subject_type", subjectType.name())
                .queryParam("mp_subject_id", subjectId != null ? subjectId.toString() : null)
                .queryParam("mp_message", StringUtils.hasText(message) ? message : "No se pudo completar la vinculacion con Mercado Pago.")
                .build()
                .toUriString();
    }

    public String buildAdminErrorRedirectUrlFromState(String state, String message) {
        OAuthSubjectType subjectType = OAuthSubjectType.REVENDEDOR;
        UUID subjectId = null;
        try {
            if (StringUtils.hasText(state)) {
                MercadoPagoOAuthStateService.OAuthStateSubject subject = mercadoPagoOAuthStateService.parseSubject(state);
                subjectType = subject.subjectType();
                subjectId = subject.subjectId();
            }
        } catch (Exception ignored) {
        }
        return buildAdminErrorRedirectUrl(subjectType, subjectId, message);
    }

    private void validarConfiguracionOauth() {
        mercadoPagoClient.exigirHabilitado();
        if (!StringUtils.hasText(mercadoPagoProperties.getClientId())
                || !StringUtils.hasText(mercadoPagoProperties.getClientSecret())
                || !StringUtils.hasText(mercadoPagoProperties.getRedirectUri())) {
            throw new IntegracionPspException(
                    "Configuracion OAuth de Mercado Pago incompleta. Revisar MERCADOPAGO_CLIENT_ID, MERCADOPAGO_CLIENT_SECRET y MERCADOPAGO_REDIRECT_URI.");
        }
    }

    private String resolveAdminRedirectUrl(OAuthSubjectType subjectType) {
        if (!StringUtils.hasText(mercadoPagoProperties.getAdminRedirectUrl())) {
            throw new IntegracionPspException("Falta MERCADOPAGO_ADMIN_REDIRECT_URL para volver al admin despues de OAuth.");
        }
        String base = mercadoPagoProperties.getAdminRedirectUrl().trim();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        String path = switch (subjectType) {
            case PROVEEDOR -> "/admin/usuarios";
            case MERCADO_DIGITAL -> "/admin/configuracion";
            default -> "/admin/usuarios";
        };
        return builder.replacePath(path)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String normalizeAuthBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://auth.mercadopago.com.ar";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record OAuthCallbackResult(OAuthSubjectType subjectType, UUID subjectId, boolean success, String message) {
    }
}
