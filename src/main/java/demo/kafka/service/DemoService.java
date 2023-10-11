package demo.kafka.service;

import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.producer.KafkaProducer;
import demo.kafka.rest.api.TriggerEventsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemoService {

    @Autowired
    private final KafkaProducer kafkaProducer;

    /**
     * Sends the requested number of events.
     *
     * Processing happens asynchronously (the hand off happens due to the @Async annotation), so the caller can return immediately.
     */
    @Async
    public void triggerAsync(TriggerEventsRequest request) throws Exception {
        int counter = 0;
        log.info("Sending {} events", request.getNumberOfEvents());
        for ( ; counter < request.getNumberOfEvents(); counter++) {
            sendEvent();
            log.info("Events sent so far: {}", counter);
        }
        log.info("Total events sent: {}", counter);
    }

    /**
     * Sends the requested number of events.
     *
     * Processing happens synchronously, only returning when complete.
     */
    public void triggerSync(TriggerEventsRequest request) throws Exception {
        int counter = 0;
        log.info("Sending {} events", request.getNumberOfEvents());
        for ( ; counter < request.getNumberOfEvents(); counter++) {
            sendEvent();
            log.info("Events sent so far: {}", counter);
        }
        log.info("Total events sent: {}", counter);
    }

    /**
     * Emits an outbound event with a payload of a randomly generated name.
     */
    private void sendEvent() throws Exception {
        DemoOutboundEvent demoEvent = DemoOutboundEvent.builder()
                .name(randomAlphabetic(1).toUpperCase() + randomAlphabetic(7).toLowerCase())
                .build();
        kafkaProducer.sendMessage(demoEvent);
    }
}
