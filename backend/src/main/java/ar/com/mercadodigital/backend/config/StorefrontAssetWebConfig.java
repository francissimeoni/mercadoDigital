package ar.com.mercadodigital.backend.config;

import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;
import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnBean(StorefrontAssetStorageService.class)
@RequiredArgsConstructor
public class StorefrontAssetWebConfig implements WebMvcConfigurer {

    private final StorefrontAssetStorageService storefrontAssetStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path baseDir = storefrontAssetStorageService.getBaseDir();
        if (baseDir == null) {
            return;
        }
        registry.addResourceHandler("/public/assets/storefront/**")
                .addResourceLocations(baseDir.toUri().toString());
    }
}
