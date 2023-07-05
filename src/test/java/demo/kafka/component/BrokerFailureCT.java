package demo.kafka.component;

import demo.kafka.rest.api.TriggerEventsRequest;
import dev.lydtech.component.framework.client.conduktor.BrokerErrorType;
import dev.lydtech.component.framework.client.conduktor.ConduktorGatewayClient;
import dev.lydtech.component.framework.client.kafka.KafkaClient;
import dev.lydtech.component.framework.client.service.ServiceClient;
import dev.lydtech.component.framework.extension.TestContainersSetupExtension;
import dev.lydtech.component.framework.mapper.JsonMapper;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.Matchers.containsString;

/**
 * Each tests sends in a REST request to trigger sending multiple events.
 *
 * The REST call returns immediately, as it sends the events asychronously.
 *
 * The test listener consumes the events emitted by the application and asserts the expected number have been received.
 */
@Slf4j
@ExtendWith(TestContainersSetupExtension.class)
public class BrokerFailureCT {

    private static final String GROUP_ID = "BrokerFailureCT";

    private Consumer consumer;
    private ConduktorGatewayClient conduktorGatewayClient;

    @BeforeEach
    public void setup() {
        consumer = KafkaClient.getInstance().initConsumer(GROUP_ID, "demo-topic", 3L);

        conduktorGatewayClient = ConduktorGatewayClient.getInstance();
        conduktorGatewayClient.reset();
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
    }

    /**
     * The Conduktor Gateway proxy routes the messages straight through to Kafka.
     */
    @Test
    public void testBroker_Healthy() throws Exception {
        Integer NUMBER_OF_EVENTS = 1000;

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger")
                .then()
                .statusCode(200);

        KafkaClient.getInstance().consumeAndAssert("Healthy", consumer, NUMBER_OF_EVENTS, 3);
    }

    /**
     * The Conduktor Gateway proxy simulates invalid required acks error.
     *
     * Not retryable.
     */
    @Test
    public void testBroker_InvalidRequiredAcks() throws Exception {
        conduktorGatewayClient.injectChaos(BrokerErrorType.INVALID_REQUIRED_ACKS);

        Integer NUMBER_OF_EVENTS = 1000;

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger")
                .then()
                .statusCode(500)
                .and()
                .body(containsString("org.apache.kafka.common.errors.InvalidRequiredAcksException: Produce request specified an invalid value for required acks."));
    }

    /**
     * The Conduktor Gateway proxy simulates request timed out error.
     *
     * Retryable.
     */
    @Test
    public void testBroker_RequestTimedOut() throws Exception {
        conduktorGatewayClient.injectChaos(BrokerErrorType.REQUEST_TIMED_OUT);

        Integer NUMBER_OF_EVENTS = 1000;

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger")
                .then()
                .statusCode(200);

        KafkaClient.getInstance().consumeAndAssert("RequestTimedOut", consumer, NUMBER_OF_EVENTS, 3);
    }

    /**
     * The Conduktor Gateway proxy simulates not enough replicas error.
     *
     * Retryable.
     */
    @Test
    public void testBroker_NotEnoughReplicas() throws Exception {
        conduktorGatewayClient.injectChaos(BrokerErrorType.NOT_ENOUGH_REPLICAS);

        Integer NUMBER_OF_EVENTS = 1000;

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger")
                .then()
                .statusCode(200);

        KafkaClient.getInstance().consumeAndAssert("NotEnoughReplicas", consumer, NUMBER_OF_EVENTS, 3);
    }
}
