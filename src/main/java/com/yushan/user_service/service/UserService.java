package com.yushan.user_service.service;

import com.yushan.user_service.repository.UserRepository;
import com.yushan.user_service.dto.UserProfileResponseDTO;
import com.yushan.user_service.dto.UserProfileUpdateRequestDTO;
import com.yushan.user_service.dto.UserProfileUpdateResponseDTO;
import com.yushan.user_service.entity.User;
import com.yushan.user_service.enums.Gender;
import com.yushan.user_service.enums.UserStatus;
import com.yushan.user_service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    /**
     * Load a user's profile by UUID and map to response DTO
     */
    public UserProfileResponseDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return mapToProfileResponse(user);
    }

    /**
     * Update user profile selectively with allowed fields only
     */
    public UserProfileUpdateResponseDTO updateUserProfileSelective(UUID userId, UserProfileUpdateRequestDTO req) {
        User existing = userRepository.findById(userId);
        if (existing == null) {
            return null;
        }

        // Handle email change with verification
        boolean emailChanged = false;
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty() && !req.getEmail().equals(existing.getEmail())) {
            // Check if verification code is provided
            if (req.getVerificationCode() == null || req.getVerificationCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Verification code is required for email change");
            }
            
            // Verify the email code
            boolean isValid = mailService.verifyEmail(req.getEmail(), req.getVerificationCode());
            if (!isValid) {
                throw new IllegalArgumentException("Invalid verification code or code expired");
            }
            
            // Check if new email already exists
            User userWithNewEmail = userRepository.findByEmail(req.getEmail());
            if (userWithNewEmail != null) {
                throw new IllegalArgumentException("Email already exists");
            }
            
            // Only set emailChanged = true AFTER all validations pass
            emailChanged = true;
        }

        User toUpdate = new User();
        toUpdate.setUuid(userId);

        // Optional fields: update only if provided (non-null and non-empty)
        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()) {
            toUpdate.setUsername(req.getUsername().trim());
        }
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty() && !req.getEmail().equals(existing.getEmail())) {
            toUpdate.setEmail(req.getEmail().trim());
        }
        if (req.getGender() != null) {
            toUpdate.setGender(req.getGender().getCode());
            if(Gender.isDefaultAvatar(existing.getAvatarUrl())){
                toUpdate.setAvatarUrl(req.getGender().getAvatarUrl());
            }
        }
        if (req.getAvatarBase64() != null && !req.getAvatarBase64().trim().isEmpty()) {
            toUpdate.setAvatarUrl(convertBase64ToUrl(req.getAvatarBase64()));
        }
        if (req.getProfileDetail() != null && !req.getProfileDetail().trim().isEmpty()) {
            toUpdate.setProfileDetail(req.getProfileDetail().trim());
        }

        // update timestamp
        toUpdate.setUpdateTime(new Date());

        userRepository.save(toUpdate);

        // reload to get latest values
        User updated = userRepository.findById(userId);
        UserProfileResponseDTO profileResponse = mapToProfileResponse(updated);
        
        // Return response with email change flag
        return new UserProfileUpdateResponseDTO(profileResponse, emailChanged);
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

    /**
     * Send verification email for email change
     */
    public void sendEmailChangeVerification(String newEmail) {
        // Check if new email already exists
        User existingUser = userRepository.findByEmail(newEmail);
        if (existingUser != null) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Send verification code to new email
        mailService.sendVerificationCode(newEmail);
    }

    public List<UserProfileResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAllUsersForRanking();
        return users.stream().map(this::mapToProfileResponse).toList();
    }

    public List<UserProfileResponseDTO> getUsersByIds(List<UUID> userIds) {
        List<User> users = userRepository.findByUuids(userIds);
        return users.stream().map(this::mapToProfileResponse).toList();
    }

    @Async
    public void updateLastActiveTime(UUID userId, LocalDateTime lastActive) {
        if (userId == null) {
            log.warn("updateLastActiveTime called with null userId");
            return;
        }

        if (lastActive == null) {
            log.warn("updateLastActiveTime called with null lastActive for userId: {}", userId);
            return;
        }

        // Idempotency check is already done at Listener level
        // This method just updates the database
        User user = userRepository.findById(userId);
        if (user == null) {
            log.warn("User not found for updateLastActiveTime: userId={}", userId);
            return;
        }

        // Only update if the new timestamp is newer than the current one
        // This prevents old events from overwriting newer timestamps
        Date currentLastActive = user.getLastActive();
        Date newLastActive = Date.from(lastActive.atZone(ZoneId.systemDefault()).toInstant());
        
        if (currentLastActive != null && !newLastActive.after(currentLastActive)) {
            log.debug("Skipping updateLastActiveTime: new timestamp {} is not newer than current {} for user: {}", 
                    newLastActive, currentLastActive, userId);
            return;
        }

        user.updateLastActive(newLastActive);
        userRepository.save(user);
        log.info("Successfully updated last active time for user: {}, new timestamp: {}", userId, newLastActive);
    }

    /**
     * Convert Base64 data URL to a regular URL
     * For now, this is a placeholder implementation that returns the Base64 data as-is
     * In a real application, you would save the image to a file storage service
     * and return the public URL
     */
    private String convertBase64ToUrl(String base64DataUrl) {
        // For now, we'll store the Base64 data directly as the URL
        // In production, you should:
        // 1. Extract the image data from the Base64 string
        // 2. Save it to a file storage service (AWS S3, Google Cloud Storage, etc.)
        // 3. Return the public URL
        return base64DataUrl;
    }
}

