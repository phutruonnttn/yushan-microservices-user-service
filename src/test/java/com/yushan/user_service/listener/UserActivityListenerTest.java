package com.yushan.user_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yushan.user_service.service.IdempotencyService;
import com.yushan.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityListenerTest {

    @Mock
    private UserService userService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private UserActivityListener userActivityListener;

    @Test
    void handleUserActivity_shouldUpdateLastActiveTime_whenUserIdIsNotNull() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now().withNano(0);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("userId", userId.toString());
        payloadMap.put("timestamp", timestamp.toString());

        String payload = objectMapper.writeValueAsString(payloadMap);
        
        when(idempotencyService.isProcessed(anyString(), eq("UserActivity"))).thenReturn(false); // Not processed yet
        doNothing().when(idempotencyService).markAsProcessed(anyString(), eq("UserActivity"));

        userActivityListener.handleUserActivity(payload);

        verify(idempotencyService).isProcessed(anyString(), eq("UserActivity"));
        verify(userService, times(1)).updateLastActiveTime(eq(userId), eq(timestamp));
        verify(idempotencyService).markAsProcessed(anyString(), eq("UserActivity"));
    }

    @Test
    void handleUserActivity_shouldNotUpdateLastActiveTime_whenUserIdIsNull() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().withNano(0);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("userId", null);
        payloadMap.put("timestamp", timestamp.toString());

        String payload = objectMapper.writeValueAsString(payloadMap);

        userActivityListener.handleUserActivity(payload);

        verify(userService, never()).updateLastActiveTime(any(), any());
    }

    @Test
    void handleUserActivity_shouldNotUpdateLastActiveTime_whenPayloadIsInvalidJson() {
        String invalidPayload = "not a json";

        // When & Then
        try {
            userActivityListener.handleUserActivity(invalidPayload);
        } catch (RuntimeException e) {
            // Expected: RuntimeException is thrown to trigger Kafka retry
        }

        verify(userService, never()).updateLastActiveTime(any(), any());
    }
}
