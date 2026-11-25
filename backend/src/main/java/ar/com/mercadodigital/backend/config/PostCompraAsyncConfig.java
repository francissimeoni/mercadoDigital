/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class PostCompraAsyncConfig {
    @Bean(name={"postCompraTaskExecutor"})
    public Executor postCompraTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("post-compra-");
        executor.initialize();
        return executor;
    }
}

