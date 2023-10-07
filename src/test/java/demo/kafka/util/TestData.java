package demo.kafka.util;

import demo.kafka.rest.api.TriggerEventsRequest;

public class TestData {

    public static TriggerEventsRequest buildTriggerEventsRequest(Integer numberOfEvents) {
        return TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
    }
}
