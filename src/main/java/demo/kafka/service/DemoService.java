package demo.kafka.service;

import demo.kafka.event.DemoInboundEvent;
import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.producer.KafkaProducer;
import demo.kafka.rest.api.TriggerEventsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    public void processInboundEvent(DemoInboundEvent event) throws Exception {
        log.info("Processing inbound event:"+event.getSequenceNumber());
        sendEvent();
    }

    /**
     * Emits an outbound event with a payload of a randomly generated name.
     */
    private void sendEvent() throws Exception {
        sendEvent(RandomStringUtils.randomAlphabetic(10).toLowerCase(), RandomStringUtils.randomAlphabetic(10).toLowerCase(), RandomStringUtils.randomAlphabetic(10).toLowerCase());
    }

    private void sendEvent(String firstName, String middleName, String lastName) throws Exception {
        DemoOutboundEvent demoEvent = DemoOutboundEvent.builder()
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .build();
        kafkaProducer.sendMessage(demoEvent);
    }
}
