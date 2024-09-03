package org.example.walletservice.jms.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.walletservice.dto.UpdateEvent;
import org.example.walletservice.exception.EventPublishException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
@Slf4j
public class UpdateEventPublisher implements EventPublisher<UpdateEvent> {


    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Retryable(
            value = {Exception.class}, // Replace with the actual exception type for Solace
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000) // Retry with a 1-second delay
    )
    public void publish(UpdateEvent event,String topic) {
        log.info(" Event Sent for userUpdatedEvent .  Event :{}", event);
        String message = "";
        try {
            // Publish customer update event
            message =  objectMapper.writeValueAsString(event);
            jmsTemplate.convertAndSend(topic,message);
        } catch (JsonProcessingException ex) {
            log.error("Error Parsing Json for Message:{}",message);
            log.error("Exception Message:{}",ex.getMessage());
            throw new EventPublishException("Error Parsing Json for message");
        } catch (Exception ex) {
            log.error("Exception while publishing message to solace");
            log.error("Exception Message:{}",ex.getMessage());
            throw new EventPublishException("Error publishing event :{}" + event  );
        }
        log.info(" Successfully published. Event :{}", event);
    }



    @Recover
    public void recover(Exception e, String message) {
        log.error("Failed to publish message: {} after retries, sending to dead-letter queue", message, e);
        sendToDeadLetterQueue(message);
    }

    private void sendToDeadLetterQueue(String message) {
        try {
            jmsTemplate.convertAndSend("dead-letter-queue", message);
            log.info("Successfully published to dead-letter-queue: {}", message);
        } catch (Exception e) {
            log.error("Failed to send message to dead-letter-queue: {}", message, e);
        }
    }


}
