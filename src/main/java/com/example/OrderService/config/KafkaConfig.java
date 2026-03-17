package com.example.OrderService.config;

import com.example.OrderService.exception.OrderAccessDeniedException;
import com.example.OrderService.exception.OrderNotFoundException;
import com.example.OrderService.exception.PaymentAmountMismatchException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff defaultBackOff = new FixedBackOff(5000L, FixedBackOff.UNLIMITED_ATTEMPTS);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(defaultBackOff);

        BackOff limitedBackOff = new FixedBackOff(5000L, 10L);

        errorHandler.setBackOffFunction((record, exception) -> {

            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

            if (cause instanceof OrderNotFoundException ||
                    cause instanceof PaymentAmountMismatchException ||
                    cause instanceof OrderAccessDeniedException ||
                    cause instanceof IllegalArgumentException ||
                    cause instanceof NullPointerException) {

                return limitedBackOff;
            }

            return null;
        });

        return errorHandler;
    }
}
