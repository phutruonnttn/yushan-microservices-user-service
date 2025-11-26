package com.yushan.user_service.controller;

import com.yushan.user_service.dto.ApiResponse;
import com.yushan.user_service.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal Controller for Gateway and other services
 * 
 * These endpoints are accessible only from internal network (no authentication required)
 * Used for service-to-service communication
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
public class InternalController {

    @Autowired
    private AdminService adminService;

    /**
     * Get list of blocked user IDs (SUSPENDED or BANNED)
     * 
     * This endpoint is used by API Gateway to bootstrap user blocklist on startup
     * No authentication required - accessible only from internal network
     * 
     * @return List of blocked user UUIDs
     */
    @GetMapping("/blocked-users")
    public ApiResponse<List<UUID>> getBlockedUsers() {
        log.debug("Internal request to get blocked users list");
        List<UUID> blockedUserIds = adminService.getBlockedUserIds();
        log.info("Returning {} blocked users to internal caller", blockedUserIds.size());
        return ApiResponse.success("Blocked users retrieved successfully", blockedUserIds);
    }
}

