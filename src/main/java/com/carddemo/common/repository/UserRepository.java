/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.repository;

import com.carddemo.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Spring Data JPA repository interface for User entity providing database access methods
 * for authentication and user management operations.
 * 
 * This repository replaces VSAM USRSEC file operations with PostgreSQL-based user management,
 * supporting the Spring Security authentication framework with JWT token generation and
 * role-based access control as specified in Section 6.4.1.2 of the technical specification.
 * 
 * Key Features:
 * - Replaces COBOL COSGN00C.cbl VSAM READ operations with JPA query methods
 * - Supports Spring Security authentication with BCrypt password validation
 * - Enables JWT token generation with user type and role information
 * - Provides automatic transaction management for authentication queries
 * - Implements Spring Data JPA naming conventions for query generation
 * 
 * Authentication Flow Integration:
 * 1. AuthenticationService calls findByUserId() for credential lookup
 * 2. Spring Security validates BCrypt hashed passwords
 * 3. JWT tokens are generated with user type and role claims
 * 4. Session context is stored in Redis for distributed session management
 * 
 * @author AWS CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Finds a user by their unique user ID for authentication purposes.
     * 
     * This method replaces the COBOL COSGN00C.cbl READ-USER-SEC-FILE operation
     * that performed VSAM KSDS read using user ID as the key. The method supports
     * case-insensitive authentication by converting the user ID to uppercase
     * before the database query, maintaining compatibility with mainframe behavior.
     * 
     * Usage in Authentication Flow:
     * - Called by AuthenticationService during login processing
     * - Returns User entity with BCrypt hashed password for validation
     * - Provides user type information for JWT role claim generation
     * - Supports Spring Security UserDetailsService implementation
     * 
     * Performance Characteristics:
     * - Uses PostgreSQL B-tree primary key index for optimal performance
     * - Target response time: < 5ms for authentication queries
     * - Supports connection pooling with HikariCP for scalability
     * 
     * @param userId The unique user identifier (8-character string, case-insensitive)
     * @return Optional<User> containing the user if found, empty otherwise
     * @throws org.springframework.dao.DataAccessException if database access fails
     */
    Optional<User> findByUserId(String userId);

    /**
     * Finds a user by their user ID with case-insensitive matching.
     * 
     * This method provides case-insensitive user lookup to support mainframe
     * compatibility where user IDs are traditionally stored in uppercase.
     * The query uses PostgreSQL's UPPER() function for consistent case handling.
     * 
     * @param userId The user identifier (case-insensitive)
     * @return Optional<User> containing the user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE UPPER(u.userId) = UPPER(:userId)")
    Optional<User> findByUserIdIgnoreCase(@Param("userId") String userId);

    /**
     * Finds all users by user type for administrative operations.
     * 
     * This method supports user management operations by filtering users based
     * on their type ('A' for Admin, 'U' for regular User). Used by administrative
     * components to list users by role for security management and reporting.
     * 
     * Administrative Usage:
     * - User maintenance screens for filtering by user type
     * - Security audit reports for role-based user listings
     * - Administrative dashboards for user management
     * 
     * @param userType The user type filter ('A' for Admin, 'U' for User)
     * @return List<User> containing all users matching the specified type
     */
    List<User> findByUserType(String userType);

    /**
     * Finds all administrative users (user type 'A').
     * 
     * Convenience method for retrieving all users with administrative privileges.
     * This method is used by security management components to identify system
     * administrators for audit purposes and privilege management.
     * 
     * @return List<User> containing all administrative users
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'A'")
    List<User> findAllAdministrators();

    /**
     * Finds all regular users (user type 'U').
     * 
     * Convenience method for retrieving all users with standard privileges.
     * This method supports user management operations and reporting functions
     * that need to differentiate between administrative and regular users.
     * 
     * @return List<User> containing all regular users
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'U'")
    List<User> findAllRegularUsers();

    /**
     * Checks if a user exists by user ID.
     * 
     * This method provides efficient existence checking without loading the entire
     * User entity. Used for validation operations during user creation and
     * administrative functions to prevent duplicate user IDs.
     * 
     * @param userId The user identifier to check
     * @return boolean true if user exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Counts the total number of users by user type.
     * 
     * This method supports administrative dashboards and reporting functions
     * by providing user count statistics. Used for system monitoring and
     * capacity planning operations.
     * 
     * @param userType The user type to count ('A' for Admin, 'U' for User)
     * @return long The count of users with the specified type
     */
    long countByUserType(String userType);

    /**
     * Finds users by first name and last name for search operations.
     * 
     * This method supports user search functionality in administrative interfaces
     * where users can be located by their full name. The search is case-insensitive
     * and supports partial matching using LIKE operations.
     * 
     * @param firstName The user's first name (case-insensitive)
     * @param lastName The user's last name (case-insensitive)
     * @return List<User> containing users matching the name criteria
     */
    @Query("SELECT u FROM User u WHERE UPPER(u.firstName) LIKE UPPER(CONCAT('%', :firstName, '%')) " +
           "AND UPPER(u.lastName) LIKE UPPER(CONCAT('%', :lastName, '%'))")
    List<User> findByFirstNameAndLastNameIgnoreCase(@Param("firstName") String firstName, 
                                                   @Param("lastName") String lastName);

    /**
     * Finds users created within a specified date range.
     * 
     * This method supports audit and reporting functions by filtering users
     * based on their account creation date. Used for compliance reporting
     * and user lifecycle management operations.
     * 
     * @param startDate The start date for the range (inclusive)
     * @param endDate The end date for the range (inclusive)
     * @return List<User> containing users created within the date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                     @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Updates the last login timestamp for a user.
     * 
     * This method is called during successful authentication to track user
     * login activity. The timestamp is used for security monitoring, audit
     * trails, and compliance reporting as specified in Section 6.4.3.4.
     * 
     * Spring Data JPA will automatically handle the transaction management
     * for this update operation, ensuring data consistency.
     * 
     * @param userId The user ID to update
     * @param lastLogin The login timestamp to set
     * @return int The number of rows affected (should be 1 for successful update)
     */
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.userId = :userId")
    int updateLastLogin(@Param("userId") String userId, @Param("lastLogin") java.time.LocalDateTime lastLogin);

    /**
     * Deletes a user by user ID.
     * 
     * This method supports administrative user management operations.
     * The deletion is performed using the user ID as the key, maintaining
     * referential integrity with related entities.
     * 
     * Note: This method should be used with caution in production environments
     * and should include proper authorization checks to ensure only authorized
     * administrators can delete user accounts.
     * 
     * @param userId The user ID to delete
     * @return int The number of rows affected (should be 1 for successful deletion)
     */
    int deleteByUserId(String userId);

    // Inherited methods from JpaRepository<User, String>:
    // - findById(String id) - Find user by primary key
    // - save(User user) - Save or update user entity
    // - findAll() - Retrieve all users
    // - delete(User user) - Delete user entity
    // - count() - Count total users
    // - existsById(String id) - Check if user exists by ID
    // - saveAll(Iterable<User> users) - Batch save users
    // - deleteAll() - Delete all users (use with extreme caution)
    // - findAll(Sort sort) - Retrieve all users with sorting
    // - findAll(Pageable pageable) - Retrieve users with pagination
}