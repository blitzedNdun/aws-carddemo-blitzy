package com.carddemo.repository;

import com.carddemo.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for UserSecurity entity providing authentication and authorization data access.
 * Replaces VSAM USRSEC file access with modern JPA operations for user_security table.
 * Supports user lookup by username for Spring Security integration, password verification support, and user role management.
 * 
 * This repository provides core user authentication data access for the modernized credit card management system,
 * converting VSAM USRSEC access patterns to Spring Data JPA repository methods. It enables Spring Security
 * UserDetailsService integration and supports all user management operations required by the application.
 * 
 * Key functionality:
 * - User authentication through findByUsername() for Spring Security integration
 * - User management operations through standard JPA repository methods
 * - User lookup by COBOL-style user ID through findBySecUsrId()
 * - User role management through findByUserType() for authorization
 * - Efficient existence checks through existsBySecUsrId()
 */
@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, Long> {

    /**
     * Finds a user by username for Spring Security authentication.
     * This method supports the Spring Security UserDetailsService implementation
     * by providing user lookup functionality during authentication.
     * 
     * @param username the username to search for
     * @return Optional containing UserSecurity if found, empty otherwise
     */
    Optional<UserSecurity> findByUsername(String username);

    /**
     * Finds a user by the COBOL-style user ID (SEC-USR-ID).
     * This method provides compatibility with legacy COBOL user ID patterns
     * and supports user lookup operations that reference the original VSAM key structure.
     * 
     * @param secUsrId the user ID from COBOL SEC-USR-ID field (8 characters)
     * @return Optional containing UserSecurity if found, empty otherwise
     */
    Optional<UserSecurity> findBySecUsrId(String secUsrId);

    /**
     * Finds a user by user ID (alias for findBySecUsrId for compatibility).
     * This method provides an alias for user lookup operations that use userId
     * instead of secUsrId, ensuring compatibility with UserDetailService.
     * 
     * @param userId the user ID (8 characters) - same as SEC-USR-ID
     * @return Optional containing UserSecurity if found, empty otherwise
     */
    default Optional<UserSecurity> findByUserId(String userId) {
        return findBySecUsrId(userId);
    }

    /**
     * Finds users by user type for role-based operations.
     * Supports user management and authorization operations by filtering users
     * based on their type ('A' for Admin, 'U' for User).
     * 
     * @param userType the user type to filter by ('A' = Admin, 'U' = User)
     * @return List of UserSecurity entities matching the specified user type
     */
    List<UserSecurity> findByUserType(String userType);

    /**
     * Checks if a user exists by the COBOL-style user ID.
     * Provides efficient existence check without loading the full entity,
     * useful for validation and duplicate checking operations.
     * 
     * @param secUsrId the user ID from COBOL SEC-USR-ID field (8 characters)
     * @return true if user exists, false otherwise
     */
    boolean existsBySecUsrId(String secUsrId);

    /**
     * Checks if a user exists by username.
     * Provides efficient existence check for username uniqueness validation
     * during user creation and modification operations.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Finds all enabled users.
     * Supports administrative operations by filtering out disabled user accounts,
     * providing a view of active users for management purposes.
     * 
     * @return List of enabled UserSecurity entities
     */
    List<UserSecurity> findByEnabledTrue();

    /**
     * Finds users with failed login attempts greater than specified threshold.
     * Supports security monitoring by identifying users with multiple failed login attempts,
     * enabling account lockout and security alert functionality.
     * 
     * @param threshold the minimum number of failed attempts to filter by
     * @return List of UserSecurity entities with failed attempts above threshold
     */
    List<UserSecurity> findByFailedLoginAttemptsGreaterThan(int threshold);

    // Note: Standard JpaRepository methods are automatically available:
    // - findById(Long id) - finds user by primary key
    // - findAll() - finds all users
    // - findAll(Pageable pageable) - finds users with pagination
    // - save(UserSecurity user) - saves or updates user
    // - saveAll(Iterable<UserSecurity> users) - saves multiple users
    // - delete(UserSecurity user) - deletes user
    // - deleteById(Long id) - deletes user by primary key
    // - count() - counts total number of users
    // - existsById(Long id) - checks if user exists by primary key
}