package com.yushan.user_service.repository.impl;

import com.yushan.user_service.dao.LibraryMapper;
import com.yushan.user_service.dao.NovelLibraryMapper;
import com.yushan.user_service.dao.UserMapper;
import com.yushan.user_service.dto.AdminUserFilterDTO;
import com.yushan.user_service.entity.Library;
import com.yushan.user_service.entity.NovelLibrary;
import com.yushan.user_service.entity.User;
import com.yushan.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * MyBatis implementation of UserRepository.
 * Handles aggregate-level operations for User, Library, and NovelLibrary.
 */
@Repository
public class MyBatisUserRepository implements UserRepository {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private LibraryMapper libraryMapper;
    
    @Autowired
    private NovelLibraryMapper novelLibraryMapper;
    
    @Override
    public User findById(UUID uuid) {
        return userMapper.selectByPrimaryKey(uuid);
    }
    
    @Override
    public User findByEmail(String email) {
        return userMapper.selectByEmail(email);
    }
    
    @Override
    public User save(User user) {
        if (user.getUuid() == null) {
            // Insert new user
            userMapper.insert(user);
        } else {
            // Check if user exists - if not, insert instead of update
            User existingUser = userMapper.selectByPrimaryKey(user.getUuid());
            if (existingUser == null) {
                // User doesn't exist - insert it
                userMapper.insertSelective(user);
            } else {
                // Update existing user
                userMapper.updateByPrimaryKeySelective(user);
            }
        }
        return user;
    }
    
    @Override
    public void delete(UUID uuid) {
        userMapper.deleteByPrimaryKey(uuid);
    }
    
    @Override
    public User findUserWithLibrary(UUID userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            // Load library for the user (aggregate-level operation)
            libraryMapper.selectByUserId(userId);
        }
        return user;
    }
    
    @Override
    public User findUserWithNovelLibraries(UUID userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            // Load novel libraries for the user (aggregate-level operation)
            novelLibraryMapper.selectNovelIdsByUserId(userId);
        }
        return user;
    }
    
    @Override
    public List<User> findUsersForAdmin(AdminUserFilterDTO filter, int offset) {
        return userMapper.selectUsersForAdmin(filter, offset);
    }
    
    @Override
    public long countUsersForAdmin(AdminUserFilterDTO filter) {
        return userMapper.countUsersForAdmin(filter);
    }
    
    @Override
    public List<User> findAllUsersForRanking() {
        return userMapper.selectAllUsersForRanking();
    }
    
    @Override
    public List<User> findByUuids(List<UUID> uuids) {
        return userMapper.selectByUuids(uuids);
    }
    
    @Override
    public Library findLibraryByUserId(UUID userId) {
        return libraryMapper.selectByUserId(userId);
    }
    
    @Override
    public Library saveLibrary(Library library) {
        if (library.getId() == null) {
            libraryMapper.insertSelective(library);
        } else {
            libraryMapper.updateByPrimaryKeySelective(library);
        }
        return library;
    }
    
    @Override
    public NovelLibrary findNovelLibraryByUserIdAndNovelId(UUID userId, Integer novelId) {
        return novelLibraryMapper.selectByUserIdAndNovelId(userId, novelId);
    }
    
    @Override
    public List<NovelLibrary> findNovelLibrariesByUserId(UUID userId) {
        // This is a simplified version - actual implementation might need pagination
        List<Integer> novelIds = novelLibraryMapper.selectNovelIdsByUserId(userId);
        return novelLibraryMapper.selectByUserIdAndNovelIds(userId, novelIds);
    }
    
    @Override
    public List<Integer> findNovelIdsByUserId(UUID userId) {
        return novelLibraryMapper.selectNovelIdsByUserId(userId);
    }
    
    @Override
    public List<NovelLibrary> findNovelLibrariesByUserIdWithPagination(UUID userId, List<Integer> novelIds, 
                                                                       int offset, int size, String sort, String order) {
        return novelLibraryMapper.selectByUserIdWithPagination(userId, novelIds, offset, size, sort, order);
    }
    
    @Override
    public long countNovelLibrariesByUserId(UUID userId, List<Integer> novelIds) {
        return novelLibraryMapper.countByUserId(userId, novelIds);
    }
    
    @Override
    public NovelLibrary saveNovelLibrary(NovelLibrary novelLibrary) {
        if (novelLibrary.getId() == null) {
            novelLibraryMapper.insertSelective(novelLibrary);
        } else {
            novelLibraryMapper.updateByPrimaryKeySelective(novelLibrary);
        }
        return novelLibrary;
    }
    
    @Override
    public void deleteNovelLibrary(Integer id) {
        novelLibraryMapper.deleteByPrimaryKey(id);
    }
    
    @Override
    public void deleteNovelLibrariesByUserIdAndNovelIds(UUID userId, List<Integer> novelIds) {
        novelLibraryMapper.deleteByUserIdAndNovelIds(userId, novelIds);
    }
    
    @Override
    public List<NovelLibrary> findNovelLibrariesByUserIdAndNovelIds(UUID userId, List<Integer> novelIds) {
        return novelLibraryMapper.selectByUserIdAndNovelIds(userId, novelIds);
    }
}

