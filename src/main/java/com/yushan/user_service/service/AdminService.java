package com.yushan.user_service.service;

import com.yushan.user_service.repository.UserRepository;
import com.yushan.user_service.dto.AdminUserFilterDTO;
import com.yushan.user_service.dto.PageResponseDTO;
import com.yushan.user_service.dto.UserProfileResponseDTO;
import com.yushan.user_service.entity.User;
import com.yushan.user_service.enums.Gender;
import com.yushan.user_service.enums.UserStatus;
import com.yushan.user_service.event.UserStatusEventProducer;
import com.yushan.user_service.event.dto.UserStatusChangedEvent;
import com.yushan.user_service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionAwareKafkaPublisher transactionAwareKafkaPublisher;
    
    @Autowired
    private UserStatusEventProducer userStatusEventProducer;

    /**
     * Promote user to admin by email
     */
    public UserProfileResponseDTO promoteToAdmin(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // Find user by email
        User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }

        // Check if user is already admin
        if (user.getIsAdmin() != null && user.getIsAdmin()) {
            throw new IllegalArgumentException("User is already an admin");
        }

        // Update user to admin
        user.promoteToAdmin();
        userRepository.save(user);

        // Return updated user profile
        return userService.getUserProfile(user.getUuid());
    }

    public PageResponseDTO<UserProfileResponseDTO> listUsers(AdminUserFilterDTO filter) {
        int offset = filter.getPage() * filter.getSize();
        long totalElements = userRepository.countUsersForAdmin(filter);

        List<User> users = userRepository.findUsersForAdmin(filter, offset);

        List<UserProfileResponseDTO> userProfiles = users.stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(userProfiles, totalElements, filter.getPage(), filter.getSize());
    }

    /**
     * Get list of blocked user IDs (SUSPENDED or BANNED)
     * Used by API Gateway to bootstrap user blocklist
     */
    public List<UUID> getBlockedUserIds() {
        List<UUID> blockedUserIds = new ArrayList<>();
        
        // Get SUSPENDED users
        AdminUserFilterDTO suspendedFilter = new AdminUserFilterDTO(0, Integer.MAX_VALUE, UserStatus.SUSPENDED, null, null, "createTime", "asc");
        List<User> suspendedUsers = userRepository.findUsersForAdmin(suspendedFilter, 0);
        blockedUserIds.addAll(suspendedUsers.stream().map(User::getUuid).collect(Collectors.toList()));
        
        // Get BANNED users
        AdminUserFilterDTO bannedFilter = new AdminUserFilterDTO(0, Integer.MAX_VALUE, UserStatus.BANNED, null, null, "createTime", "asc");
        List<User> bannedUsers = userRepository.findUsersForAdmin(bannedFilter, 0);
        blockedUserIds.addAll(bannedUsers.stream().map(User::getUuid).collect(Collectors.toList()));
        
        return blockedUserIds;
    }

    @Transactional
    public void updateUserStatus(UUID userUuid, UserStatus newStatus) {
        User user = userRepository.findById(userUuid);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with UUID: " + userUuid);
        }

        UserStatus oldStatus = UserStatus.fromCode(user.getStatus());
        
        User userToUpdate = new User();
        userToUpdate.setUuid(userUuid);
        userToUpdate.changeStatus(newStatus);
        userToUpdate.setUpdateTime(new Date());

        userRepository.save(userToUpdate);
        
        // Publish UserStatusChangedEvent AFTER transaction commit
        final UUID finalUserUuid = userUuid;
        final UserStatus finalOldStatus = oldStatus;
        final UserStatus finalNewStatus = newStatus;
        transactionAwareKafkaPublisher.publishAfterCommit(() -> {
            UserStatusChangedEvent event = new UserStatusChangedEvent(
                finalUserUuid.toString(),
                finalOldStatus != null ? finalOldStatus.name() : null,
                finalNewStatus.name()
            );
            userStatusEventProducer.sendUserStatusChangedEvent(event);
        });
    }

    private UserProfileResponseDTO mapToProfileResponse(User user) {
        UserProfileResponseDTO dto = new UserProfileResponseDTO();
        dto.setUuid(user.getUuid().toString());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setProfileDetail(user.getProfileDetail());
        dto.setBirthday(user.getBirthday());
        dto.setGender(Gender.fromCode(user.getGender()));
        dto.setStatus(UserStatus.fromCode(user.getStatus()));
        dto.setIsAuthor(user.getIsAuthor());
        dto.setIsAdmin(user.getIsAdmin());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        dto.setLastActive(user.getLastActive());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }
}
