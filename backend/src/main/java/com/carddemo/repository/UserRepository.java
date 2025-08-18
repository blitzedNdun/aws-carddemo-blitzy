package com.carddemo.repository;

import com.carddemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for User entity providing business user profile data access.
 * Complements UserSecurityRepository by handling business-focused user profile operations.
 * Replaces VSAM user profile file access with modern JPA operations for users table.
 * 
 * This repository provides business user profile data access for the modernized credit card management system,
 * converting VSAM user profile access patterns to Spring Data JPA repository methods. It works alongside
 * UserSecurityRepository to provide complete user information management.
 * 
 * Key functionality:
 * - User profile lookup by user ID through findByUserId()
 * - User profile search by name through findByFirstNameContainingOrLastNameContaining()
 * - Department-based user management through findByDepartment()
 * - Email-based lookup through findByEmail()
 * - Efficient existence checks through existsByUserId()
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by business user ID.
     * This method supports user profile lookup for UserDetailService integration.
     * Maps to COBOL user ID field from business user records.
     * 
     * @param userId Business user ID (8 characters) matching the user identifier
     * @return Optional<User> containing user profile if found, empty if not found
     */
    Optional<User> findByUserId(String userId);

    /**
     * Finds a user by email address.
     * Supports email-based user lookup for business operations.
     * 
     * @param email User's email address
     * @return Optional<User> containing user profile if found, empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds users by department.
     * Supports departmental user management and reporting.
     * 
     * @param department Department name or code
     * @return List<User> containing all users in the specified department
     */
    List<User> findByDepartment(String department);

    /**
     * Finds users by partial name match (first name or last name).
     * Supports user search functionality for administrative operations.
     * 
     * @param firstName Partial or complete first name to search for
     * @param lastName Partial or complete last name to search for
     * @return List<User> containing users matching the name criteria
     */
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);

    /**
     * Checks if a user exists by user ID.
     * Provides efficient existence check without loading the full entity.
     * 
     * @param userId Business user ID to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Checks if a user exists by email.
     * Supports unique email validation for user management operations.
     * 
     * @param email Email address to check
     * @return true if user with email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Custom query to find users by full name.
     * Combines first and last name search with exact matching.
     * 
     * @param firstName User's first name
     * @param lastName User's last name
     * @return List<User> containing users with matching full name
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) = LOWER(:firstName) AND LOWER(u.lastName) = LOWER(:lastName)")
    List<User> findByFullName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Custom query to find active users by department with ordering.
     * Supports departmental reporting with consistent ordering.
     * 
     * @param department Department name or code
     * @return List<User> containing users in department ordered by name
     */
    @Query("SELECT u FROM User u WHERE u.department = :department ORDER BY u.lastName, u.firstName")
    List<User> findByDepartmentOrderedByName(@Param("department") String department);

    /**
     * Counts users by department.
     * Supports departmental analytics and reporting.
     * 
     * @param department Department name or code
     * @return Long count of users in the department
     */
    long countByDepartment(String department);
}