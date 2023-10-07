package demo.kafka.service;

import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.producer.KafkaProducer;
import demo.kafka.rest.api.TriggerEventsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DemoServiceTest {

    private KafkaProducer mockKafkaClient;
    private DemoService service;

    @BeforeEach
    public void setUp() {
        mockKafkaClient = mock(KafkaProducer.class);
        service = new DemoService(mockKafkaClient);
    }

    /**
     * Ensure the Kafka client is called to emit the expected number of events.
     */
    @Test
    public void testTriggerSync_NumberOfEvents() throws Exception {
        when(mockKafkaClient.sendMessage(any(DemoOutboundEvent.class))).thenReturn(mock(SendResult.class));

        TriggerEventsRequest testEvent = TriggerEventsRequest.builder()
                .numberOfEvents(10)
                .build();
        service.triggerSync(testEvent);
        verify(mockKafkaClient, times(testEvent.getNumberOfEvents().intValue())).sendMessage(any(DemoOutboundEvent.class));
    }

    /**
     * Ensure the Kafka client is called to emit the expected number of events.
     */
    @Test
    public void testTriggerAsync_NumberOfEvents() throws Exception {
        when(mockKafkaClient.sendMessage(any(DemoOutboundEvent.class))).thenReturn(mock(SendResult.class));

        TriggerEventsRequest testEvent = TriggerEventsRequest.builder()
                .numberOfEvents(10)
                .build();
        service.triggerAsync(testEvent);
        verify(mockKafkaClient, times(testEvent.getNumberOfEvents().intValue())).sendMessage(any(DemoOutboundEvent.class));
    }
}
