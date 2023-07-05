package demo.kafka.service;

import demo.kafka.event.DemoEvent;
import demo.kafka.producer.KafkaProducer;
import demo.kafka.properties.KafkaDemoProperties;
import demo.kafka.rest.api.TriggerEventsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DemoServiceTest {

    private KafkaProducer mockKafkaClient;
    private KafkaDemoProperties mockKafkaDemoProperties;
    private DemoService service;

    @BeforeEach
    public void setUp() {
        mockKafkaClient = mock(KafkaProducer.class);
        mockKafkaDemoProperties = mock(KafkaDemoProperties.class);
        service = new DemoService(mockKafkaClient, mockKafkaDemoProperties);
    }

    /**
     * Ensure the Kafka client is called to emit the expected number of events.
     */
    @Test
    public void testProcess_NumberOfEvents() throws Exception {
        when(mockKafkaClient.sendMessage(anyString(), any(DemoEvent.class))).thenReturn(mock(SendResult.class));

        TriggerEventsRequest testEvent = TriggerEventsRequest.builder()
                .numberOfEvents(10)
                .build();
        service.process(testEvent);
        verify(mockKafkaClient, times(testEvent.getNumberOfEvents().intValue())).sendMessage(anyString(), any(DemoEvent.class));
    }
}
