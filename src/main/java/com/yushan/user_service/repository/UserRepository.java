package com.yushan.user_service.repository;

import com.yushan.user_service.dto.AdminUserFilterDTO;
import com.yushan.user_service.entity.Library;
import com.yushan.user_service.entity.NovelLibrary;
import com.yushan.user_service.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for User aggregate.
 * Abstracts data access operations for User and its related entities (Library, NovelLibrary).
 */
public interface UserRepository {
    
    // Basic CRUD operations
    User findById(UUID uuid);
    
    User findByEmail(String email);
    
    User save(User user);
    
    void delete(UUID uuid);
    
    // Aggregate-level queries
    /**
     * Find user with library information
     */
    User findUserWithLibrary(UUID userId);
    
    /**
     * Find user with all novel libraries
     */
    User findUserWithNovelLibraries(UUID userId);
    
    // Admin queries
    List<User> findUsersForAdmin(AdminUserFilterDTO filter, int offset);
    
    long countUsersForAdmin(AdminUserFilterDTO filter);
    
    // Ranking queries
    List<User> findAllUsersForRanking();
    
    // Batch operations
    List<User> findByUuids(List<UUID> uuids);
    
    // Library operations (part of User aggregate)
    Library findLibraryByUserId(UUID userId);
    
    Library saveLibrary(Library library);
    
    // NovelLibrary operations (part of User aggregate)
    NovelLibrary findNovelLibraryByUserIdAndNovelId(UUID userId, Integer novelId);
    
    List<NovelLibrary> findNovelLibrariesByUserId(UUID userId);
    
    List<Integer> findNovelIdsByUserId(UUID userId);
    
    List<NovelLibrary> findNovelLibrariesByUserIdWithPagination(UUID userId, List<Integer> novelIds, 
                                                                int offset, int size, String sort, String order);
    
    long countNovelLibrariesByUserId(UUID userId, List<Integer> novelIds);
    
    NovelLibrary saveNovelLibrary(NovelLibrary novelLibrary);
    
    void deleteNovelLibrary(Integer id);
    
    void deleteNovelLibrariesByUserIdAndNovelIds(UUID userId, List<Integer> novelIds);
    
    List<NovelLibrary> findNovelLibrariesByUserIdAndNovelIds(UUID userId, List<Integer> novelIds);
}

