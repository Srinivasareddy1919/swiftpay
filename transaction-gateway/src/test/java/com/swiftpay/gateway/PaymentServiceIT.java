package com.swiftpay.gateway;

import com.swiftpay.gateway.api.PaymentRequest;
import com.swiftpay.gateway.api.PaymentResponse;
import com.swiftpay.gateway.service.DuplicateTransactionException;
import com.swiftpay.gateway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.redis.testcontainers.RedisContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class PaymentServiceIT {

    @Container static PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    @Container static KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    @Container static RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired PaymentService paymentService;

    @Test
    void submits_then_rejects_duplicate() {
        String id = UUID.randomUUID().toString();
        PaymentRequest req = new PaymentRequest(id, "user-1", "user-2", new BigDecimal("10.00"), "USD");

        PaymentResponse first = paymentService.submit(req);
        assertThat(first.status()).isEqualTo("PENDING");

        assertThatThrownBy(() -> paymentService.submit(req))
                .isInstanceOf(DuplicateTransactionException.class);
    }
}
