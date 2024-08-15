package demo.kafka.component;

import demo.kafka.rest.api.TriggerEventsRequest;
import dev.lydtech.component.framework.client.conduktor.BrokenBrokerErrorType;
import dev.lydtech.component.framework.client.conduktor.ConduktorGatewayClient;
import dev.lydtech.component.framework.client.kafka.KafkaClient;
import dev.lydtech.component.framework.client.service.ServiceClient;
import dev.lydtech.component.framework.extension.ComponentTestExtension;
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
 * The test listener consumes the events emitted by the application and asserts the expected number have been received,
 * or if a non-retryable exception is expected to be thrown due to the simulated error scenario, the test asserts for
 * this.
 */
@Slf4j
@ExtendWith(ComponentTestExtension.class)
public class ResilienceCT {

    private static final String GROUP_ID = "ResilienceCT";

    private Consumer consumer;
    private ConduktorGatewayClient conduktorGatewayClient;

    @BeforeEach
    public void setup() {
        // The test consumer for the service's outbound topic.
        consumer = KafkaClient.getInstance().initConsumer(GROUP_ID, "demo-outbound-topic", 3L);

        conduktorGatewayClient = ConduktorGatewayClient.getInstance();
        conduktorGatewayClient.reset();
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        conduktorGatewayClient.reset();
    }

    /**
     * The Conduktor Gateway proxy routes the messages straight through to Kafka.
     */
    @Test
    public void testResilience_Healthy() throws Exception {
        Integer NUMBER_OF_EVENTS = 250;

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
     * This results in a not retryable exception on message produce.  The call to trigger event sending is made with the
     * async flag set to false, so that the 500 error returned can be asserted.
     */
    @Test
    public void testResilience_InvalidRequiredAcks() {
        Integer NUMBER_OF_EVENTS = 250;
        Integer FAILURE_RATE_PERCENTAGE = 20;

        conduktorGatewayClient.simulateBrokenBroker(FAILURE_RATE_PERCENTAGE, BrokenBrokerErrorType.INVALID_REQUIRED_ACKS);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=false")
                .then()
                .statusCode(500)
                .and()
                .body(containsString("org.apache.kafka.common.errors.InvalidRequiredAcksException: Produce request specified an invalid value for required acks."));
    }

    /**
     * The Conduktor Gateway proxy simulates not enough replicas error.
     *
     * This results in a retryable exception on message produce.  The call to trigger event sending is made with the
     * async flag set to true - the sending happens asynchronously so the REST request returns immediately.  The service
     * logs then show the message producing retrying until success.
     */
    @Test
    public void testResilience_NotEnoughReplicas() throws Exception {
        Integer NUMBER_OF_EVENTS = 250;
        Integer FAILURE_RATE_PERCENTAGE = 20;

        conduktorGatewayClient.simulateBrokenBroker(FAILURE_RATE_PERCENTAGE, BrokenBrokerErrorType.NOT_ENOUGH_REPLICAS);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=true")
                .then()
                .statusCode(202);

        KafkaClient.getInstance().consumeAndAssert("NotEnoughReplicas", consumer, NUMBER_OF_EVENTS, 3);
    }

    /**
     * The Conduktor Gateway proxy simulates a corrupt message error.
     *
     * This results in a retryable exception on message produce.  The call to trigger event sending is made with the
     * async flag set to true - the sending happens asynchronously so the REST request returns immediately.  The service
     * logs then show the message producing retrying until success.
     */
    @Test
    public void testResilience_CorruptMessage() throws Exception {
        Integer NUMBER_OF_EVENTS = 250;
        Integer FAILURE_RATE_PERCENTAGE = 20;

        conduktorGatewayClient.simulateBrokenBroker(FAILURE_RATE_PERCENTAGE, BrokenBrokerErrorType.CORRUPT_MESSAGE);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=true")
                .then()
                .statusCode(202);

        KafkaClient.getInstance().consumeAndAssert("CorruptMessage", consumer, NUMBER_OF_EVENTS, 3);
    }

    /**
     * The Conduktor Gateway proxy simulates unknown server error.
     *
     * This results in a not retryable exception on message produce.  The call to trigger event sending is made with the
     * async flag set to false, so that the 500 error returned can be asserted.
     */
    @Test
    public void testResilience_UnknownServerError() {
        Integer NUMBER_OF_EVENTS = 250;
        Integer FAILURE_RATE_PERCENTAGE = 20;

        conduktorGatewayClient.simulateBrokenBroker(FAILURE_RATE_PERCENTAGE, BrokenBrokerErrorType.UNKNOWN_SERVER_ERROR);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=false")
                .then()
                .statusCode(500)
                .and()
                .body(containsString("org.apache.kafka.common.errors.UnknownServerException: The server experienced an unexpected error when processing the request."));
    }

    /**
     * The Conduktor Gateway proxy simulates a leader election error.
     *
     * This results in a retryable exception on message produce.  The call to trigger event sending is made with the
     * async flag set to true - the sending happens asynchronously so the REST request returns immediately.  The service
     * logs then show the message producing retrying until success.
     */
    @Test
    public void testResilience_LeaderElection() throws Exception {
        Integer NUMBER_OF_EVENTS = 250;
        Integer FAILURE_RATE_PERCENTAGE = 20;

        conduktorGatewayClient.simulateLeaderElection(FAILURE_RATE_PERCENTAGE);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=true")
                .then()
                .statusCode(202);

        KafkaClient.getInstance().consumeAndAssert("LeaderElection", consumer, NUMBER_OF_EVENTS, 3);
    }

    /**
     * The Conduktor Gateway proxy simulates a slow broker.
     *
     * A latency of between 20 and 100 milliseconds is added by the gateway to 100% of the produce requests.
     */
    @Test
    public void testResilience_SlowBroker() throws Exception {
        Integer NUMBER_OF_EVENTS = 250;
        Integer RATE_IN_PERCENTAGE = 100;
        Integer MIN_LATENCY = 20;
        Integer MAX_LATENCY = 100;

        conduktorGatewayClient.simulateSlowBroker(RATE_IN_PERCENTAGE, MIN_LATENCY, MAX_LATENCY);

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger?async=true")
                .then()
                .statusCode(202);

        KafkaClient.getInstance().consumeAndAssert("SlowBroker", consumer, NUMBER_OF_EVENTS, 3);
    }
}
