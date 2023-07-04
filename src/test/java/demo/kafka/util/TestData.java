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
                .addressLine1(RandomStringUtils.randomAlphabetic(30))
                .addressLine2(RandomStringUtils.randomAlphabetic(30))
                .addressLine3(RandomStringUtils.randomAlphabetic(30))
                .addressLine4(RandomStringUtils.randomAlphabetic(30))
                .town(RandomStringUtils.randomAlphabetic(15))
                .city(RandomStringUtils.randomAlphabetic(15))
                .county(RandomStringUtils.randomAlphabetic(15))
                .postcode(RandomStringUtils.randomAlphanumeric(8))
                .telephoneNumber(RandomStringUtils.randomNumeric(16))
                .mobileNumber(RandomStringUtils.randomNumeric(16))
                .emailAddress(RandomStringUtils.randomAlphabetic(10)+"@"+RandomStringUtils.randomAlphabetic(10))
                .build();
    }

    public static TriggerEventsRequest buildTriggerEventsRequest(Integer numberOfEvents) {
        return TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
    }
}
