/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.entity.UserSecurity;
import com.carddemo.dto.UserListResponse;
import com.carddemo.dto.UserListDto;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Service class implementing user listing functionality translated from COUSR00C.cbl.
 * 
 * This service provides comprehensive user management operations including:
 * - Paginated user listings with F7/F8 navigation equivalent
 * - Role-based filtering for admin and regular users  
 * - User search by ID and name functionality
 * - Active/inactive status filtering
 * - Page navigation controls and boundary validation
 * - Complete COBOL-to-Java functional parity
 * 
 * Key Functions:
 * - Converts COBOL MAIN-PARA logic to Java service methods
 * - Replaces VSAM USRSEC browse operations with JPA pagination
 * - Transforms COBOL user type filtering to Spring Data queries
 * - Maps BMS screen constraints (10 users per page) to validation rules
 * - Preserves COBOL F7/F8 pagination behavior through navigation methods
 * 
 * COBOL Program Structure Translation:
 * - MAIN-PARA -> listUsers() main entry point
 * - PROCESS-PAGE-FORWARD -> processPageForward() with F8 functionality
 * - PROCESS-PAGE-BACKWARD -> processPageBackward() with F7 functionality 
 * - VALIDATE-INPUT -> validatePagination() input validation
 * - POPULATE-USER-LIST -> getPagedUsers() data retrieval
 * - SEND-USRLST-SCREEN -> UserListResponse construction
 * 
 * This implementation maintains exact behavior of COBOL COUSR00C program
 * while leveraging modern Spring Boot architecture for improved performance.
 */
@Service
@RequiredArgsConstructor
public class UserListService {

    private static final Logger logger = LoggerFactory.getLogger(UserListService.class);
    
    /**
     * Repository for accessing user security data - replaces VSAM USRSEC file operations.
     * Injected via constructor to ensure immutable dependency reference.
     */
    private final UserSecurityRepository userSecurityRepository;

    /**
     * Main user listing method that replaces COBOL MAIN-PARA functionality.
     * Handles user list display with pagination equivalent to BMS COUSR00 screen.
     * 
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page (max 10)
     * @return UserListResponse with user list and pagination metadata
     */
    public UserListResponse listUsers(int pageNumber, int pageSize) {
        logger.debug("Processing user list request: page={}, size={}", pageNumber, pageSize);
        
        try {
            // Validate pagination parameters (replaces COBOL validation logic)
            validatePagination(pageNumber, pageSize);
            
            // Retrieve paginated users (replaces VSAM STARTBR/READNEXT operations)
            UserListResponse response = getPagedUsers(pageNumber, pageSize);
            
            logger.debug("Successfully processed user list request. Found {} users on page {}", 
                        response.getUsers().size(), pageNumber);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing user list request: {}", e.getMessage(), e);
            // Return empty response with error indication (replaces COBOL error handling)
            UserListResponse errorResponse = new UserListResponse();
            errorResponse.setUsers(new ArrayList<>());
            errorResponse.setPageNumber(pageNumber);
            errorResponse.setTotalCount(0L);
            errorResponse.setHasNextPage(false);
            errorResponse.setHasPreviousPage(false);
            return errorResponse;
        }
    }

    /**
     * Processes forward page navigation equivalent to COBOL F8 key functionality.
     * Advances to the next page in the user list pagination.
     * 
     * @param currentPage Current page number (1-based)
     * @param pageSize Number of users per page
     * @return UserListResponse for the next page
     */
    public UserListResponse processPageForward(int currentPage, int pageSize) {
        logger.debug("Processing page forward: current page={}, size={}", currentPage, pageSize);
        
        // Calculate next page number
        int nextPage = currentPage + 1;
        
        // Get users for the next page
        return getPagedUsers(nextPage, pageSize);
    }

    /**
     * Processes backward page navigation equivalent to COBOL F7 key functionality.
     * Returns to the previous page in the user list pagination.
     * 
     * @param currentPage Current page number (1-based)  
     * @param pageSize Number of users per page
     * @return UserListResponse for the previous page
     */
    public UserListResponse processPageBackward(int currentPage, int pageSize) {
        logger.debug("Processing page backward: current page={}, size={}", currentPage, pageSize);
        
        // Calculate previous page number (minimum 1)
        int previousPage = Math.max(1, currentPage - 1);
        
        // Get users for the previous page
        return getPagedUsers(previousPage, pageSize);
    }

    /**
     * Validates pagination parameters matching COBOL input validation logic.
     * Enforces BMS screen constraints and business rules.
     * 
     * @param pageNumber Page number to validate (must be positive)
     * @param pageSize Page size to validate (must be 1-10)
     * @throws IllegalArgumentException if validation fails
     */
    public void validatePagination(int pageNumber, int pageSize) {
        logger.debug("Validating pagination: page={}, size={}", pageNumber, pageSize);
        
        // Validate page number (replaces COBOL page number validation)
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }
        
        // Validate page size (replaces COBOL screen capacity validation)
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        if (pageSize > 10) {
            throw new IllegalArgumentException("Page size cannot exceed 10");
        }
        
        logger.debug("Pagination validation successful: page={}, size={}", pageNumber, pageSize);
    }

    /**
     * Filters users by role type matching COBOL user type logic.
     * Supports admin ("01") and regular user ("02") filtering.
     * 
     * @param userType User type to filter by ("01"=Admin, "02"=Regular)
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page
     * @return UserListResponse with filtered users
     */
    public UserListResponse filterByRole(String userType, int pageNumber, int pageSize) {
        logger.debug("Filtering users by role: type={}, page={}, size={}", userType, pageNumber, pageSize);
        
        try {
            validatePagination(pageNumber, pageSize);
            
            // Convert to 0-based page index for JPA
            Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("secUsrId"));
            
            // Get users by type with pagination
            Page<UserSecurity> userPage = userSecurityRepository.findByUserTypeWithPagination(userType, pageable);
            
            // Convert to DTOs
            List<UserListDto> userDtos = userPage.getContent().stream()
                    .map(this::convertToUserListDto)
                    .collect(Collectors.toList());
            
            // Build response
            return buildUserListResponse(userDtos, userPage, pageNumber);
            
        } catch (Exception e) {
            logger.error("Error filtering users by role {}: {}", userType, e.getMessage(), e);
            return createEmptyResponse(pageNumber);
        }
    }

    /**
     * Searches for users by ID or name matching COBOL search functionality.
     * Provides comprehensive user lookup capabilities.
     * 
     * @param searchTerm Search term (user ID or name)
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page
     * @return UserListResponse with matching users
     */
    public UserListResponse findUsersBySearch(String searchTerm, int pageNumber, int pageSize) {
        logger.debug("Searching users: term={}, page={}, size={}", searchTerm, pageNumber, pageSize);
        
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return createEmptyResponse(pageNumber);
            }
            
            validatePagination(pageNumber, pageSize);
            
            List<UserSecurity> foundUsers = new ArrayList<>();
            
            // First try exact ID match (replaces COBOL READ with EQUAL)
            Optional<UserSecurity> userById = userSecurityRepository.findBySecUsrId(searchTerm.trim());
            if (userById.isPresent()) {
                foundUsers.add(userById.get());
            } else {
                // Try name search (replaces COBOL substring search)
                foundUsers.addAll(userSecurityRepository.findByUsernameContainingIgnoreCase(searchTerm.trim()));
            }
            
            // Apply pagination to results
            int startIndex = (pageNumber - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, foundUsers.size());
            
            List<UserSecurity> pagedUsers = foundUsers.subList(
                Math.min(startIndex, foundUsers.size()), 
                Math.min(endIndex, foundUsers.size())
            );
            
            // Convert to DTOs
            List<UserListDto> userDtos = pagedUsers.stream()
                    .map(this::convertToUserListDto)
                    .collect(Collectors.toList());
            
            // Create manual page information
            UserListResponse response = new UserListResponse();
            response.setUsers(userDtos);
            response.setPageNumber(pageNumber);
            response.setTotalCount((long) foundUsers.size());
            response.setHasNextPage(endIndex < foundUsers.size());
            response.setHasPreviousPage(pageNumber > 1);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error searching users with term {}: {}", searchTerm, e.getMessage(), e);
            return createEmptyResponse(pageNumber);
        }
    }

    /**
     * Filters users by active/inactive status.
     * Supports administrative user management operations.
     * 
     * @param isActive Filter for active (true) or inactive (false) users
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page
     * @return UserListResponse with status-filtered users
     */
    public UserListResponse filterByStatus(boolean isActive, int pageNumber, int pageSize) {
        logger.debug("Filtering users by status: active={}, page={}, size={}", isActive, pageNumber, pageSize);
        
        try {
            validatePagination(pageNumber, pageSize);
            
            // Convert to 0-based page index for JPA
            Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("secUsrId"));
            
            // Get users by enabled status with pagination
            Page<UserSecurity> userPage = userSecurityRepository.findByEnabledWithPagination(isActive, pageable);
            
            // Convert to DTOs
            List<UserListDto> userDtos = userPage.getContent().stream()
                    .map(this::convertToUserListDto)
                    .collect(Collectors.toList());
            
            // Build response
            return buildUserListResponse(userDtos, userPage, pageNumber);
            
        } catch (Exception e) {
            logger.error("Error filtering users by status {}: {}", isActive, e.getMessage(), e);
            return createEmptyResponse(pageNumber);
        }
    }

    /**
     * Processes user selection matching COBOL selection logic.
     * Validates and returns selected user for further operations.
     * 
     * @param userId User ID to select
     * @return Selected UserSecurity entity
     * @throws IllegalArgumentException if user not found
     */
    public UserSecurity processUserSelection(String userId) {
        logger.debug("Processing user selection: userId={}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        
        Optional<UserSecurity> user = userSecurityRepository.findBySecUsrId(userId.trim());
        if (user.isPresent()) {
            logger.debug("User selected successfully: {}", userId);
            return user.get();
        } else {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }

    /**
     * Retrieves paginated users with correct navigation state calculation.
     * Core pagination method used by other service methods.
     * 
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page
     * @return UserListResponse with paginated user data
     */
    public UserListResponse getPagedUsers(int pageNumber, int pageSize) {
        logger.debug("Getting paged users: page={}, size={}", pageNumber, pageSize);
        
        try {
            validatePagination(pageNumber, pageSize);
            
            // Convert to 0-based page index for JPA
            Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("secUsrId"));
            
            // Get users with pagination
            Page<UserSecurity> userPage = userSecurityRepository.findAll(pageable);
            
            // Convert to DTOs
            List<UserListDto> userDtos = userPage.getContent().stream()
                    .map(this::convertToUserListDto)
                    .collect(Collectors.toList());
            
            // Build response
            return buildUserListResponse(userDtos, userPage, pageNumber);
            
        } catch (Exception e) {
            logger.error("Error getting paged users: {}", e.getMessage(), e);
            return createEmptyResponse(pageNumber);
        }
    }

    /**
     * Main processing method that executes complete user list workflow.
     * Coordinates filtering, pagination, and response building.
     * 
     * @param pageNumber Current page number (1-based)
     * @param pageSize Number of users per page
     * @param userType Optional user type filter
     * @param searchTerm Optional search term
     * @param statusFilter Optional status filter
     * @return UserListResponse with processed results
     */
    public UserListResponse mainProcess(int pageNumber, int pageSize, String userType, 
                                      String searchTerm, Boolean statusFilter) {
        logger.debug("Main process: page={}, size={}, type={}, search={}, status={}", 
                    pageNumber, pageSize, userType, searchTerm, statusFilter);
        
        try {
            // Apply filters based on provided parameters
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                return findUsersBySearch(searchTerm, pageNumber, pageSize);
            } else if (userType != null && !userType.trim().isEmpty()) {
                return filterByRole(userType, pageNumber, pageSize);
            } else if (statusFilter != null) {
                return filterByStatus(statusFilter, pageNumber, pageSize);
            } else {
                // Default: return all users with pagination
                return getPagedUsers(pageNumber, pageSize);
            }
            
        } catch (Exception e) {
            logger.error("Error in main process: {}", e.getMessage(), e);
            return createEmptyResponse(pageNumber);
        }
    }

    /**
     * Converts UserSecurity entity to UserListDto for display.
     * Replaces COBOL POPULATE-USER-DATA paragraph logic.
     * 
     * @param user UserSecurity entity from database
     * @return UserListDto formatted for display
     */
    private UserListDto convertToUserListDto(UserSecurity user) {
        if (user == null) {
            logger.warn("Cannot convert null user to UserListDto");
            return null;
        }
        
        UserListDto dto = new UserListDto();
        
        try {
            // Map user fields (replaces COBOL MOVE statements)
            dto.setUserId(user.getSecUsrId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setUserType(user.getUserType());
            
            // Set default selection flag (empty for initial display)
            dto.setSelectionFlag("");
            
            logger.debug("Converted user {} to UserListDto", user.getSecUsrId());
            
        } catch (Exception e) {
            logger.error("Error converting user {} to UserListDto: {}", user.getSecUsrId(), e.getMessage(), e);
            throw new RuntimeException("Failed to convert user to display format", e);
        }
        
        return dto;
    }

    /**
     * Builds UserListResponse with pagination metadata.
     * Replaces COBOL response construction logic.
     * 
     * @param userDtos List of user DTOs for display
     * @param userPage Spring Data Page with pagination info
     * @param currentPageNumber Current page number (1-based)
     * @return Complete UserListResponse
     */
    private UserListResponse buildUserListResponse(List<UserListDto> userDtos, Page<UserSecurity> userPage, 
                                                  int currentPageNumber) {
        logger.debug("Building response for {} users on page {}", userDtos.size(), currentPageNumber);
        
        UserListResponse response = new UserListResponse();
        
        try {
            // Set user list (limited to 10 per page per BMS screen constraint)
            response.setUsers(userDtos);
            
            // Set pagination metadata
            response.setPageNumber(currentPageNumber);
            response.setTotalCount(userPage.getTotalElements());
            response.setHasNextPage(userPage.hasNext());
            response.setHasPreviousPage(userPage.hasPrevious());
            
            logger.debug("Built response: page {} of {}, {} total users, next={}, prev={}", 
                        response.getPageNumber(), 
                        userPage.getTotalPages(),
                        response.getTotalCount(),
                        response.getHasNextPage(),
                        response.getHasPreviousPage());
            
        } catch (Exception e) {
            logger.error("Error building user list response: {}", e.getMessage(), e);
            return createEmptyResponse(currentPageNumber);
        }
        
        return response;
    }

    /**
     * Creates empty response for error conditions.
     * Ensures consistent error handling across all methods.
     * 
     * @param pageNumber Current page number
     * @return Empty UserListResponse
     */
    private UserListResponse createEmptyResponse(int pageNumber) {
        UserListResponse response = new UserListResponse();
        response.setUsers(new ArrayList<>());
        response.setPageNumber(pageNumber);
        response.setTotalCount(0L);
        response.setHasNextPage(false);
        response.setHasPreviousPage(false);
        return response;
    }
}