package demo.kafka.util;

import demo.kafka.event.DemoInboundEvent;
import demo.kafka.rest.api.TriggerEventsRequest;

public class TestData {

    public static DemoInboundEvent buildDemoInboundEvent(Long sequenceNumber) {
        return DemoInboundEvent.builder()
                .sequenceNumber(sequenceNumber)
                .build();
    }

    public static TriggerEventsRequest buildTriggerEventsRequest(Integer numberOfEvents) {
        return TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
    }
}
