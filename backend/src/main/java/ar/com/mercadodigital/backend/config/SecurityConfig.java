package ar.com.mercadodigital.backend.config;

import jakarta.servlet.Filter;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ar.com.mercadodigital.backend.security.JwtFilter;

@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/webhooks/psp/mercadopago").permitAll()
                        .requestMatchers("/webhooks/psp", "/webhooks/psp/**").denyAll()
                        .requestMatchers("/catalogo", "/catalogo/**", "/pedido/**", "/revendedores/**")
                        .hasAnyAuthority("REVENDEDOR", "ADMIN_MD")
                        .requestMatchers("/cliente/storefront/**").hasAuthority("CLIENTE")
                        .requestMatchers("/proveedores/**").hasAnyAuthority("PROVEEDOR", "ADMIN_MD")
                        .requestMatchers(HttpMethod.POST, "/envios/*/listo-para-retiro")
                        .hasAuthority("PROVEEDOR")
                        .requestMatchers("/logistica/**", "/envios/**").hasAuthority("LOGISTICA_MD")
                        .requestMatchers("/rma/**").hasAuthority("ADMIN_MD")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN_MD")
                        .requestMatchers("/productos/**", "/productos-canonicos/**", "/media/**")
                        .hasAuthority("ADMIN_MD")
                        .requestMatchers("/notificaciones/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    public SecurityFilterChain securityFilterChainSinSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
