package com.yushan.user_service.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Status Changed Event
 * Published when a user's status changes (e.g., NORMAL -> SUSPENDED, SUSPENDED -> BANNED)
 * 
 * Used by API Gateway to update Redis blocklist in real-time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusChangedEvent {
    private String userId;
    private String oldStatus;  // Can be null if user is newly created
    private String newStatus;
    private LocalDateTime timestamp;
    
    public UserStatusChangedEvent(String userId, String oldStatus, String newStatus) {
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }
}

