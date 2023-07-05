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
     * Sends the requested number of events.
     */
    public void process(TriggerEventsRequest request) throws Exception {
        int counter = 0;
        log.info("Sending {} events", request.getNumberOfEvents());
        for ( ; counter < request.getNumberOfEvents(); counter++) {
            sendEvent();
            log.info("Events sent so far: {}", counter);
            if (counter % 10000 == 0) {
                log.info("Total events sent: {}", counter);
            }
        }
        log.info("Total events sent: {}", counter);
    }

    /**
     * Send an event choosing one of three keys randomly.
     */
    private void sendEvent() throws Exception {
        String key = String.valueOf(RandomUtils.nextInt(1, 4));
        DemoEvent demoEvent = DemoEvent.builder()
                .firstName(RandomStringUtils.randomAlphabetic(10))
                .middleName(RandomStringUtils.randomAlphabetic(10))
                .lastName(RandomStringUtils.randomAlphabetic(10))
                .build();
        kafkaClient.sendMessage(key, demoEvent);
        log.debug("Sent message with key {}.", key);
    }
}
