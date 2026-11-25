package ar.com.mercadodigital.backend.config;

import java.io.IOException;
import java.util.Locale;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Retira las rutas de esta aplicación; no modifica el proxy ni cuentas externas. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MercadoPagoRetiradoFilter extends OncePerRequestFilter {
    @Value("${mercadopago.enabled:false}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = org.springframework.web.util.UrlPathHelper.defaultInstance
                .getPathWithinApplication(request).toLowerCase(Locale.ROOT);
        boolean retirado = path.contains("/mercadopago") || path.startsWith("/webhooks/psp")
                || path.matches("/public/pedido/[^/]+/(reanudar-pago|pago)/?");
        if (!enabled && retirado) {
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"code\":\"MERCADOPAGO_RETIRADO\",\"message\":\"MercadoDigital ya no opera con Mercado Pago. Contactá a la tienda para coordinar la compra.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
