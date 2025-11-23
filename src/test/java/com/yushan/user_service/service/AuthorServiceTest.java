package com.yushan.user_service.service;

import com.yushan.user_service.repository.UserRepository;
import com.yushan.user_service.dto.UserProfileResponseDTO;
import com.yushan.user_service.entity.User;
import com.yushan.user_service.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AuthorService authorService;

    private User testUser;
    private String testEmail;
    private String testVerificationCode;
    private UserProfileResponseDTO expectedResponse;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testVerificationCode = "123456";
        
        testUser = new User();
        testUser.setUuid(UUID.randomUUID());
        testUser.setEmail(testEmail);
        testUser.setUsername("testuser");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setGender(1);
        testUser.setLastLogin(new Date());
        testUser.setLastActive(new Date());
        testUser.setIsAuthor(false);
        testUser.setIsAdmin(false);

        expectedResponse = new UserProfileResponseDTO();
        expectedResponse.setEmail(testEmail);
        expectedResponse.setUsername("testuser");
        expectedResponse.setIsAuthor(true);
    }


    @Test
    void upgradeToAuthor_Success() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(testUser);
        when(mailService.verifyEmail(testEmail, testVerificationCode)).thenReturn(true);
        when(userService.getUserProfile(testUser.getUuid())).thenReturn(expectedResponse);

        // When
        UserProfileResponseDTO result = authorService.upgradeToAuthor(testEmail, testVerificationCode);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertTrue(result.getIsAuthor());
        verify(userRepository).save(testUser);
    }

    @Test
    void upgradeToAuthor_UserNotFound() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authorService.upgradeToAuthor(testEmail, testVerificationCode));
        
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeToAuthor_UserAlreadyAuthor() {
        // Given
        testUser.setIsAuthor(true);
        when(userRepository.findByEmail(testEmail)).thenReturn(testUser);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authorService.upgradeToAuthor(testEmail, testVerificationCode));
        
        assertEquals("User is already an author", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeToAuthor_InvalidVerificationCode() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(testUser);
        when(mailService.verifyEmail(testEmail, testVerificationCode)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authorService.upgradeToAuthor(testEmail, testVerificationCode));
        
        assertEquals("Invalid verification code or code expired", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeToAuthor_EmptyEmail() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authorService.upgradeToAuthor("", testVerificationCode));
        
        assertEquals("Email is required", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeToAuthor_NullEmail() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authorService.upgradeToAuthor(null, testVerificationCode));
        
        assertEquals("Email is required", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}
