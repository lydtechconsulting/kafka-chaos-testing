package demo.kafka;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Slf4j
@ComponentScan(basePackages = {"demo.kafka"})
@Configuration
public class KafkaDemoConfiguration {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                                           @Value("${kafka.producer.retries}") Integer retries,
                                                           @Value("${kafka.producer.delivery.timeout.ms}") Integer deliveryTimeoutMs,
                                                           @Value("${kafka.producer.request.timeout.ms}") Integer requestTimeoutMs,
                                                           @Value("${kafka.producer.linger.ms}") Integer lingerMs) {
        log.info("Producer config: retries: {} - delivery.timeout.ms: {} - request.timeout.ms: {} - linger.ms: {}", retries, deliveryTimeoutMs, requestTimeoutMs, lingerMs);
        final Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.RETRIES_CONFIG, retries);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
