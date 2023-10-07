package demo.kafka.producer;

import demo.kafka.properties.KafkaDemoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {
    @Autowired
    private final KafkaDemoProperties properties;

    @Autowired
    private final KafkaTemplate kafkaTemplate;

    public SendResult sendMessage(Object payload) throws Exception {
        final ProducerRecord<String, Object> record = new ProducerRecord<>(properties.getOutboundTopic(), payload);
        try {
            return (SendResult) kafkaTemplate.send(record).get();
        } catch(Exception e) {
            if(e.getCause()!=null && e.getCause() instanceof KafkaProducerException) {
                // The KafkaProducerException wraps the underlying cause, such as InvalidRequiredAcksException.
                throw new Exception(e.getCause().getCause());
            }
            throw e;
        }
    }
}
