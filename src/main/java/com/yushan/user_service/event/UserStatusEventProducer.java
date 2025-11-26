package com.yushan.user_service.event;

import com.yushan.user_service.event.dto.UserStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka Event Producer for User Status Changes
 * 
 * Publishes UserStatusChangedEvent to "user-status-events" topic.
 * This event is consumed by API Gateway to update Redis blocklist in real-time.
 * 
 * Format: Direct JSON (not wrapped in EventEnvelope) - Gateway expects direct deserialization
 */
@Slf4j
@Service
public class UserStatusEventProducer {

    private static final String TOPIC = "user-status-events";
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send UserStatusChangedEvent to Kafka
     * 
     * @param event UserStatusChangedEvent to publish
     */
    public void sendUserStatusChangedEvent(UserStatusChangedEvent event) {
        try {
            String key = event.getUserId(); // Use userId as Kafka key for partitioning
            
            log.info("Sending UserStatusChangedEvent to topic {} for user: {} ({} -> {})", 
                TOPIC, event.getUserId(), event.getOldStatus(), event.getNewStatus());
            kafkaTemplate.send(TOPIC, key, event);
        } catch (Exception e) {
            log.error("Error sending UserStatusChangedEvent for user: {}", event.getUserId(), e);
        }
    }
}

