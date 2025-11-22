package com.yushan.user_service.entity;

import com.yushan.user_service.enums.UserStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID uuid;

    private String email;

    private String username;

    private String hashPassword;

    private String avatarUrl;

    private String profileDetail;

    private Date birthday;

    private Integer gender;

    private Integer status;

    private Boolean isAuthor;

    private Boolean isAdmin;

    private Date createTime;

    private Date updateTime;

    private Date lastLogin;

    private Date lastActive;

    public User(UUID uuid, String email, String username, String hashPassword, String avatarUrl, String profileDetail, Date birthday, Integer gender, Integer status, Boolean isAuthor, Boolean isAdmin, Date createTime, Date updateTime, Date lastLogin, Date lastActive) {
        this.uuid = uuid;
        this.email = email;
        this.username = username;
        this.hashPassword = hashPassword;
        this.avatarUrl = avatarUrl;
        this.profileDetail = profileDetail;
        this.birthday = birthday != null ? new Date(birthday.getTime()) : null;
        this.gender = gender;
        this.status = status;
        this.isAuthor = isAuthor;
        this.isAdmin = isAdmin;
        this.createTime = createTime != null ? new Date(createTime.getTime()) : null;
        this.updateTime = updateTime != null ? new Date(updateTime.getTime()) : null;
        this.lastLogin = lastLogin != null ? new Date(lastLogin.getTime()) : null;
        this.lastActive = lastActive != null ? new Date(lastActive.getTime()) : null;
    }

    public User() {
        super();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword == null ? null : hashPassword.trim();
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl == null ? null : avatarUrl.trim();
    }

    public String getProfileDetail() {
        return profileDetail;
    }

    public void setProfileDetail(String profileDetail) {
        this.profileDetail = profileDetail == null ? null : profileDetail.trim();
    }

    public Date getBirthday() {
        return birthday != null ? new Date(birthday.getTime()) : null;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday != null ? new Date(birthday.getTime()) : null;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsAuthor() {
        return isAuthor;
    }

    public void setIsAuthor(Boolean isAuthor) {
        this.isAuthor = isAuthor;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Date getCreateTime() {
        return createTime != null ? new Date(createTime.getTime()) : null;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime != null ? new Date(createTime.getTime()) : null;
    }

    public Date getUpdateTime() {
        return updateTime != null ? new Date(updateTime.getTime()) : null;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime != null ? new Date(updateTime.getTime()) : null;
    }

    public Date getLastLogin() {
        return lastLogin != null ? new Date(lastLogin.getTime()) : null;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin != null ? new Date(lastLogin.getTime()) : null;
    }

    public Date getLastActive() {
        return lastActive != null ? new Date(lastActive.getTime()) : null;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive != null ? new Date(lastActive.getTime()) : null;
    }

    // ==================== Business Logic Methods ====================

    /**
     * Change user status and update timestamp
     */
    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus.getCode();
        this.updateTime = new Date();
    }

    /**
     * Change user status with validation
     */
    public void changeStatusTo(UserStatus newStatus, UserStatus... allowedFromStatuses) {
        if (allowedFromStatuses != null && allowedFromStatuses.length > 0) {
            UserStatus currentStatus = UserStatus.fromCode(this.status);
            boolean isAllowed = false;
            for (UserStatus allowedStatus : allowedFromStatuses) {
                if (currentStatus == allowedStatus) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                throw new IllegalStateException("Cannot change status from " + currentStatus + " to " + newStatus);
            }
        }
        changeStatus(newStatus);
    }

    /**
     * Suspend user account
     */
    public void suspend() {
        changeStatusTo(UserStatus.SUSPENDED, UserStatus.NORMAL);
    }

    /**
     * Ban user account
     */
    public void ban() {
        changeStatusTo(UserStatus.BANNED, UserStatus.NORMAL, UserStatus.SUSPENDED);
    }

    /**
     * Unsuspend user account (restore to normal)
     */
    public void unsuspend() {
        changeStatusTo(UserStatus.NORMAL, UserStatus.SUSPENDED);
    }

    /**
     * Unban user account (restore to normal)
     */
    public void unban() {
        changeStatusTo(UserStatus.NORMAL, UserStatus.BANNED);
    }

    /**
     * Upgrade user to author
     */
    public void upgradeToAuthor() {
        this.isAuthor = true;
        this.updateTime = new Date();
    }

    /**
     * Promote user to admin
     */
    public void promoteToAdmin() {
        this.isAdmin = true;
        this.updateTime = new Date();
    }

    /**
     * Demote user from admin
     */
    public void demoteFromAdmin() {
        this.isAdmin = false;
        this.updateTime = new Date();
    }

    /**
     * Update last login time
     */
    public void updateLastLogin() {
        this.lastLogin = new Date();
        this.updateTime = new Date();
    }

    /**
     * Update last login time with specific date
     */
    public void updateLastLogin(Date loginTime) {
        this.lastLogin = loginTime != null ? new Date(loginTime.getTime()) : new Date();
        this.updateTime = new Date();
    }

    /**
     * Update last active time
     */
    public void updateLastActive() {
        this.lastActive = new Date();
    }

    /**
     * Update last active time with specific date
     */
    public void updateLastActive(Date activeTime) {
        this.lastActive = activeTime != null ? new Date(activeTime.getTime()) : new Date();
    }

    /**
     * Update timestamp (updateTime)
     */
    public void updateTimestamp() {
        this.updateTime = new Date();
    }

    /**
     * Initialize as new user with default values
     */
    public void initializeAsNew() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        Date now = new Date();
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
        if (this.lastLogin == null) {
            this.lastLogin = now;
        }
        if (this.lastActive == null) {
            this.lastActive = now;
        }
        if (this.status == null) {
            this.status = UserStatus.NORMAL.getCode();
        }
        if (this.isAuthor == null) {
            this.isAuthor = false;
        }
        if (this.isAdmin == null) {
            this.isAdmin = false;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if user has normal status
     */
    public boolean isNormal() {
        return UserStatus.fromCode(this.status) == UserStatus.NORMAL;
    }

    /**
     * Check if user is suspended
     */
    public boolean isSuspended() {
        return UserStatus.fromCode(this.status) == UserStatus.SUSPENDED;
    }

    /**
     * Check if user is banned
     */
    public boolean isBanned() {
        return UserStatus.fromCode(this.status) == UserStatus.BANNED;
    }

    /**
     * Check if user is active (not suspended or banned)
     */
    public boolean isActive() {
        return isNormal();
    }

    /**
     * Check if user is an author
     */
    public boolean isAuthorUser() {
        return Boolean.TRUE.equals(this.isAuthor);
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdminUser() {
        return Boolean.TRUE.equals(this.isAdmin);
    }

    /**
     * Check if user can be suspended
     */
    public boolean canBeSuspended() {
        return isNormal();
    }

    /**
     * Check if user can be banned
     */
    public boolean canBeBanned() {
        UserStatus currentStatus = UserStatus.fromCode(this.status);
        return currentStatus == UserStatus.NORMAL || currentStatus == UserStatus.SUSPENDED;
    }
}