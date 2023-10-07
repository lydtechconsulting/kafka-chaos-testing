package demo.kafka.integration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import demo.kafka.KafkaDemoConfiguration;
import demo.kafka.event.DemoInboundEvent;
import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.rest.api.TriggerEventsRequest;
import demo.kafka.util.TestData;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { KafkaDemoConfiguration.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true, topics = { "demo-inbound-topic", "demo-outbound-topic" })
public class EndToEndIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testReceiver;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testReceiver() {
            return new KafkaTestListener();
        }
    }

    /**
     * Use this receiver to consume messages from the outbound topic.
     */
    public static class KafkaTestListener {
        AtomicInteger counter = new AtomicInteger(0);

        @KafkaListener(
                groupId = "EndToEndIntegrationTest",
                topics = "demo-outbound-topic",
                properties = "spring.json.value.default.type:demo.kafka.event.DemoOutboundEvent",
                autoStartup = "true")
        void receive(@Payload final DemoOutboundEvent payload) {
            log.debug("KafkaTestListener - Received message: " + payload);
            counter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        // Wait until the partitions are assigned.
        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
        testReceiver.counter.set(0);
    }

    /**
     * Send in a REST request to trigger emitting multiple outbound events.
     *
     * The sending of the outbound events completes before the call returns.
     *
     * This integration test does not utilise Conduktor Gateway.  The application connects directly to Kafka.
     */
    @Test
    public void testTrigger_Success_Sync() throws Exception {
        int totalMessages = 10;

        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(totalMessages);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/demo/trigger?async=false", request, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        Awaitility.await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(totalMessages));
    }

    /**
     * Send in a REST request to trigger emitting multiple outbound events.
     *
     * The sending of the outbound events happens asynchronously with the call returning immediately.
     *
     * This integration test does not utilise Conduktor Gateway.  The application connects directly to Kafka.
     */
    @Test
    public void testTrigger_Success_Async() throws Exception {
        int totalMessages = 10;

        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(totalMessages);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/demo/trigger?async=true", request, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        Awaitility.await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(totalMessages));
    }

    /**
     * Send in a multiple events to the service's inbound topic and ensure an outbound event is emitted to the service's
     * outbound topic for each.
     */
    @Test
    public void testConsumeAndProduceEvents() throws Exception {
        int totalMessages = 10;
        for (long counter=1; counter<=totalMessages; counter++) {
            DemoInboundEvent inboundEvent = TestData.buildDemoInboundEvent(counter);
            kafkaTemplate.send("demo-inbound-topic", inboundEvent).get();
        }

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(totalMessages));
    }
}
