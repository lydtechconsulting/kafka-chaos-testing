package demo.kafka.component;

import java.util.List;

import demo.kafka.rest.api.TriggerEventsRequest;
import dev.lydtech.component.framework.client.conduktor.ConduktorGatewayClient;
import dev.lydtech.component.framework.client.kafka.KafkaClient;
import dev.lydtech.component.framework.client.service.ServiceClient;
import dev.lydtech.component.framework.extension.TestContainersSetupExtension;
import dev.lydtech.component.framework.mapper.JsonMapper;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

    @BeforeEach
    public void setup() {
        consumer = KafkaClient.getInstance().initConsumer(GROUP_ID, "demo-topic", 3L);

        ConduktorGatewayClient conduktorGatewayClient = ConduktorGatewayClient.getInstance();
        conduktorGatewayClient.reset();
//        conduktorGatewayClient.injectChaos(BrokerErrorType.NOT_ENOUGH_REPLICAS, BrokerErrorType.BROKER_NOT_AVAILABLE);
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
    }

    @Test
    public void testHealthyBroker() throws Exception {
        Integer NUMBER_OF_EVENTS = 100;

        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(NUMBER_OF_EVENTS)
                .build();

        RestAssured.given()
                .spec(ServiceClient.getInstance().getRequestSpecification())
                .contentType("application/json")
                .body(JsonMapper.writeToJson(request))
                .post("/v1/demo/trigger")
                .then()
                .statusCode(202);

        List<ConsumerRecord<String, String>> outboundEvents = KafkaClient.getInstance().consumeAndAssert("testHealthyBroker", consumer, NUMBER_OF_EVENTS, 3);
    }
}
