package demo.kafka.util;

import demo.kafka.event.DemoEvent;
import demo.kafka.rest.api.TriggerEventsRequest;
import org.apache.commons.lang3.RandomStringUtils;

public class TestData {

    public static DemoEvent buildDemoEvent(String id) {
        return DemoEvent.builder()
                .firstName(RandomStringUtils.randomAlphabetic(10))
                .middleName(RandomStringUtils.randomAlphabetic(10))
                .lastName(RandomStringUtils.randomAlphabetic(10))
                .build();
    }

    public static TriggerEventsRequest buildTriggerEventsRequest(Integer numberOfEvents) {
        return TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
    }
}
