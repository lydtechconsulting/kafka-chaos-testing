package demo.kafka.service;

import demo.kafka.event.DemoEvent;
import demo.kafka.producer.KafkaProducer;
import demo.kafka.properties.KafkaDemoProperties;
import demo.kafka.rest.api.TriggerEventsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemoService {

    @Autowired
    private final KafkaProducer kafkaClient;

    @Autowired
    private final KafkaDemoProperties properties;

    /**
     * Processing happens asynchronously so the caller can return.
     *
     * Either sends a total number of events, or sends events for a set period of time.
     */
    @Async
    public void process(TriggerEventsRequest request) {
        int counter = 0;
        log.info("Sending {} events", request.getNumberOfEvents());
        for ( ; counter < request.getNumberOfEvents(); counter++) {
            sendEvent();
            if (counter % 10000 == 0) {
                log.info("Total events sent: {}", counter);
            }
        }
        log.info("Total events sent: {}", counter);
    }

    /**
     * Send an event choosing one of three keys randomly.  Configuration determines whether to send synchronously or asynchronously.
     */
    private void sendEvent() {
        String key = String.valueOf(RandomUtils.nextInt(1, 4));
        DemoEvent demoEvent = DemoEvent.builder()
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
        try {
            kafkaClient.sendMessageAsync(key, demoEvent).get();
        } catch (Exception e) {
            String message = "Unable to send message";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        log.debug("Sent message with key {}.", key);
    }
}
