/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.dto.UserListResponse;
import com.carddemo.dto.UserListDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

/**
 * Comprehensive unit test suite for UserListService implementing COBOL COUSR00C program functionality.
 * 
 * Tests validate complete user management operations including:
 * - User pagination logic equivalent to COBOL F7/F8 navigation
 * - Role-based filtering for admin and regular users  
 * - User search functionality by ID and name
 * - Active/inactive status filtering
 * - Page navigation controls and boundary conditions
 * - 100% business logic coverage for mainframe-to-cloud migration
 * 
 * This test suite ensures functional parity with the original COBOL COUSR00C program
 * while validating the Spring Boot service implementation meets all performance and
 * business requirements specified in the modernization project.
 * 
 * Key Test Areas:
 * - COBOL 1000-PROCESS-USER-LIST paragraph equivalent  
 * - VSAM STARTBR/READNEXT operation replication through JPA pagination
 * - BMS COUSR00 screen pagination (10 users per page maximum)
 * - COBOL user type filtering ("01"=Admin, "02"=Regular)
 * - F7/F8 key navigation through page forward/backward methods
 * - Input validation matching COBOL field validation logic
 * - Error handling equivalent to COBOL ABEND handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserListService - COBOL COUSR00C Program Migration Tests")
class UserListServiceTest extends BaseServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    private UserListService userListService;

    // Test data entities
    private UserSecurity adminUser;
    private UserSecurity regularUser;
    private UserSecurity inactiveUser;
    private List<UserSecurity> testUsers;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Initialize service with mocked repository
        userListService = new UserListService(userSecurityRepository);
        
        // Create test users with different roles and statuses
        createTestUsers();
        
        // Reset mocks for each test
        resetMocks();
    }

    /**
     * Creates test UserSecurity entities for comprehensive testing scenarios.
     * Mimics COBOL user data structures from CSUSR01Y copybook.
     */
    private void createTestUsers() {
        // Admin user - equivalent to COBOL SEC-USR-TYPE "01"
        adminUser = new UserSecurity();
        adminUser.setId(1L);
        adminUser.setSecUsrId("ADMIN001");
        adminUser.setUsername("admin001");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setUserType("01"); // Admin type from COBOL
        adminUser.setEnabled(true);
        
        // Regular user - equivalent to COBOL SEC-USR-TYPE "02"
        regularUser = new UserSecurity();
        regularUser.setId(2L);
        regularUser.setSecUsrId("USER0001");
        regularUser.setUsername("user0001");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setUserType("02"); // Regular user type from COBOL
        regularUser.setEnabled(true);
        
        // Inactive user for status filtering tests
        inactiveUser = new UserSecurity();
        inactiveUser.setId(3L);
        inactiveUser.setSecUsrId("USER0002");
        inactiveUser.setUsername("user0002");
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setUserType("02");
        inactiveUser.setEnabled(false); // Inactive status
        
        testUsers = Arrays.asList(adminUser, regularUser, inactiveUser);
    }

    /**
     * Resets all mock objects to ensure test isolation.
     * Prevents mock state from affecting subsequent test executions.
     */
    public void resetMocks() {
        reset(userSecurityRepository);
    }

    @Nested
    @DisplayName("User List Display - COBOL 1000-PROCESS-USER-LIST Equivalent")
    class UserListDisplayTests {

        @Test
        @DisplayName("Should successfully list users with pagination - Main list functionality")
        void shouldListUsersWithPagination() {
            // Arrange - Setup page data equivalent to BMS screen capacity
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> userPage = new PageImpl<>(testUsers, pageable, testUsers.size());

            when(userSecurityRepository.findAll(pageable)).thenReturn(userPage);

            // Act - Execute main list operation
            UserListResponse response = userListService.listUsers(pageNumber, pageSize);

            // Assert - Validate COBOL-equivalent response structure
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(3);
            assertThat(response.getPageNumber()).isEqualTo(1);
            assertThat(response.getTotalCount()).isEqualTo(3L);
            assertThat(response.getHasNextPage()).isFalse();
            assertThat(response.getHasPreviousPage()).isFalse();

            // Verify correct repository method called
            verify(userSecurityRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle empty user list gracefully")
        void shouldHandleEmptyUserList() {
            // Arrange - Empty result set
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(userSecurityRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            UserListResponse response = userListService.listUsers(pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
            assertThat(response.getHasNextPage()).isFalse();
            assertThat(response.getHasPreviousPage()).isFalse();
        }

        @Test
        @DisplayName("Should validate pagination parameters - COBOL input validation equivalent")
        void shouldValidatePaginationParameters() {
            // Test invalid page number
            assertThatThrownBy(() -> userListService.listUsers(0, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Page number must be positive");

            // Test invalid page size - negative
            assertThatThrownBy(() -> userListService.listUsers(1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Page size must be positive");

            // Test invalid page size - exceeds BMS screen limit
            assertThatThrownBy(() -> userListService.listUsers(1, 11))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Page size cannot exceed 10");
        }
    }

    @Nested
    @DisplayName("Page Navigation - COBOL F7/F8 Key Functionality")
    class PageNavigationTests {

        @Test
        @DisplayName("Should process page forward - F8 key equivalent")
        void shouldProcessPageForward() {
            // Arrange
            int currentPage = 1;
            int pageSize = 10;
            int nextPage = 2;
            Pageable pageable = PageRequest.of(1, 10, Sort.by("secUsrId"));
            Page<UserSecurity> nextPageData = new PageImpl<>(Arrays.asList(adminUser), pageable, 11);

            when(userSecurityRepository.findAll(pageable)).thenReturn(nextPageData);

            // Act
            UserListResponse response = userListService.processPageForward(currentPage, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPageNumber()).isEqualTo(nextPage);
            assertThat(response.getUsers()).hasSize(1);
            verify(userSecurityRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should process page backward - F7 key equivalent")
        void shouldProcessPageBackward() {
            // Arrange
            int currentPage = 2;
            int pageSize = 10;
            int previousPage = 1;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> previousPageData = new PageImpl<>(testUsers, pageable, 11);

            when(userSecurityRepository.findAll(pageable)).thenReturn(previousPageData);

            // Act
            UserListResponse response = userListService.processPageBackward(currentPage, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPageNumber()).isEqualTo(previousPage);
            assertThat(response.getUsers()).hasSize(3);
            verify(userSecurityRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should not go below page 1 when processing backward")
        void shouldNotGoBelowPageOne() {
            // Arrange
            int currentPage = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> firstPageData = new PageImpl<>(testUsers, pageable, 3);

            when(userSecurityRepository.findAll(pageable)).thenReturn(firstPageData);

            // Act
            UserListResponse response = userListService.processPageBackward(currentPage, pageSize);

            // Assert - Should stay on page 1
            assertThat(response.getPageNumber()).isEqualTo(1);
            verify(userSecurityRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Role-Based Filtering - COBOL User Type Processing")
    class RoleBasedFilteringTests {

        @Test
        @DisplayName("Should filter users by admin role - COBOL type '01'")
        void shouldFilterUsersByAdminRole() {
            // Arrange
            String adminType = "01";
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> adminUsers = new PageImpl<>(Arrays.asList(adminUser), pageable, 1);

            when(userSecurityRepository.findByUserTypeWithPagination(adminType, pageable))
                    .thenReturn(adminUsers);

            // Act
            UserListResponse response = userListService.filterByRole(adminType, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getUserType()).isEqualTo("01");
            assertThat(response.getUsers().get(0).getUserId()).isEqualTo("ADMIN001");
            verify(userSecurityRepository).findByUserTypeWithPagination(adminType, pageable);
        }

        @Test
        @DisplayName("Should filter users by regular user role - COBOL type '02'")
        void shouldFilterUsersByRegularRole() {
            // Arrange
            String userType = "02";
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> regularUsers = new PageImpl<>(Arrays.asList(regularUser, inactiveUser), pageable, 2);

            when(userSecurityRepository.findByUserTypeWithPagination(userType, pageable))
                    .thenReturn(regularUsers);

            // Act
            UserListResponse response = userListService.filterByRole(userType, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(2);
            assertThat(response.getUsers().get(0).getUserType()).isEqualTo("02");
            assertThat(response.getUsers().get(1).getUserType()).isEqualTo("02");
            verify(userSecurityRepository).findByUserTypeWithPagination(userType, pageable);
        }
    }

    @Nested
    @DisplayName("User Search - COBOL Search Functionality")
    class UserSearchTests {

        @Test
        @DisplayName("Should find user by exact ID match - COBOL READ with EQUAL")
        void shouldFindUserByExactId() {
            // Arrange
            String searchTerm = "ADMIN001";
            int pageNumber = 1;
            int pageSize = 10;

            when(userSecurityRepository.findBySecUsrId(searchTerm))
                    .thenReturn(Optional.of(adminUser));

            // Act
            UserListResponse response = userListService.findUsersBySearch(searchTerm, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getUserId()).isEqualTo("ADMIN001");
            assertThat(response.getTotalCount()).isEqualTo(1L);
            verify(userSecurityRepository).findBySecUsrId(searchTerm);
        }

        @Test
        @DisplayName("Should find users by name search - COBOL substring search")
        void shouldFindUsersByNameSearch() {
            // Arrange
            String searchTerm = "Admin";
            int pageNumber = 1;
            int pageSize = 10;

            when(userSecurityRepository.findBySecUsrId(searchTerm))
                    .thenReturn(Optional.empty());
            when(userSecurityRepository.findByUsernameContainingIgnoreCase(searchTerm))
                    .thenReturn(Arrays.asList(adminUser));

            // Act
            UserListResponse response = userListService.findUsersBySearch(searchTerm, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getFirstName()).isEqualTo("Admin");
            verify(userSecurityRepository).findByUsernameContainingIgnoreCase(searchTerm);
        }

        @Test
        @DisplayName("Should handle empty search term")
        void shouldHandleEmptySearchTerm() {
            // Act
            UserListResponse response = userListService.findUsersBySearch("", 1, 10);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
        }

        @Test
        @DisplayName("Should handle null search term")
        void shouldHandleNullSearchTerm() {
            // Act
            UserListResponse response = userListService.findUsersBySearch(null, 1, 10);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Status Filtering - Active/Inactive User Management")
    class StatusFilteringTests {

        @Test
        @DisplayName("Should filter active users only")
        void shouldFilterActiveUsers() {
            // Arrange
            boolean isActive = true;
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> activeUsers = new PageImpl<>(Arrays.asList(adminUser, regularUser), pageable, 2);

            when(userSecurityRepository.findByEnabledWithPagination(isActive, pageable))
                    .thenReturn(activeUsers);

            // Act
            UserListResponse response = userListService.filterByStatus(isActive, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(2);
            // Note: UserListDto doesn't have isEnabled field, so we check the source data setup
            verify(userSecurityRepository).findByEnabledWithPagination(isActive, pageable);
        }

        @Test
        @DisplayName("Should filter inactive users only")
        void shouldFilterInactiveUsers() {
            // Arrange
            boolean isActive = false;
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> inactiveUsers = new PageImpl<>(Arrays.asList(inactiveUser), pageable, 1);

            when(userSecurityRepository.findByEnabledWithPagination(isActive, pageable))
                    .thenReturn(inactiveUsers);

            // Act
            UserListResponse response = userListService.filterByStatus(isActive, pageNumber, pageSize);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getUserId()).isEqualTo("USER0002");
            verify(userSecurityRepository).findByEnabledWithPagination(isActive, pageable);
        }
    }

    @Nested
    @DisplayName("User Selection - COBOL Selection Processing")
    class UserSelectionTests {

        @Test
        @DisplayName("Should process valid user selection")
        void shouldProcessValidUserSelection() {
            // Arrange
            String userId = "ADMIN001";
            when(userSecurityRepository.findBySecUsrId(userId))
                    .thenReturn(Optional.of(adminUser));

            // Act
            UserSecurity selectedUser = userListService.processUserSelection(userId);

            // Assert
            assertThat(selectedUser).isNotNull();
            assertThat(selectedUser.getSecUsrId()).isEqualTo("ADMIN001");
            verify(userSecurityRepository).findBySecUsrId(userId);
        }

        @Test
        @DisplayName("Should handle invalid user selection")
        void shouldHandleInvalidUserSelection() {
            // Arrange
            String userId = "INVALID";
            when(userSecurityRepository.findBySecUsrId(userId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userListService.processUserSelection(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: INVALID");
        }

        @Test
        @DisplayName("Should handle empty user ID selection")
        void shouldHandleEmptyUserIdSelection() {
            // Act & Assert
            assertThatThrownBy(() -> userListService.processUserSelection(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID cannot be empty");
        }
    }

    @Nested
    @DisplayName("Main Process - Complete Workflow Integration")
    class MainProcessTests {

        @Test
        @DisplayName("Should execute main process with search term")
        void shouldExecuteMainProcessWithSearch() {
            // Arrange
            int pageNumber = 1;
            int pageSize = 10;
            String searchTerm = "ADMIN001";
            
            when(userSecurityRepository.findBySecUsrId(searchTerm))
                    .thenReturn(Optional.of(adminUser));

            // Act
            UserListResponse response = userListService.mainProcess(pageNumber, pageSize, null, searchTerm, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getUserId()).isEqualTo("ADMIN001");
        }

        @Test
        @DisplayName("Should execute main process with role filter")
        void shouldExecuteMainProcessWithRoleFilter() {
            // Arrange
            int pageNumber = 1;
            int pageSize = 10;
            String userType = "01";
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> adminUsers = new PageImpl<>(Arrays.asList(adminUser), pageable, 1);

            when(userSecurityRepository.findByUserTypeWithPagination(userType, pageable))
                    .thenReturn(adminUsers);

            // Act
            UserListResponse response = userListService.mainProcess(pageNumber, pageSize, userType, null, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getUserType()).isEqualTo("01");
        }

        @Test
        @DisplayName("Should execute main process with status filter")
        void shouldExecuteMainProcessWithStatusFilter() {
            // Arrange
            int pageNumber = 1;
            int pageSize = 10;
            Boolean statusFilter = true;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> activeUsers = new PageImpl<>(Arrays.asList(adminUser, regularUser), pageable, 2);

            when(userSecurityRepository.findByEnabledWithPagination(statusFilter, pageable))
                    .thenReturn(activeUsers);

            // Act
            UserListResponse response = userListService.mainProcess(pageNumber, pageSize, null, null, statusFilter);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(2);
        }

        @Test
        @DisplayName("Should execute main process with no filters - default list")
        void shouldExecuteMainProcessWithNoFilters() {
            // Arrange
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> allUsers = new PageImpl<>(testUsers, pageable, 3);

            when(userSecurityRepository.findAll(pageable)).thenReturn(allUsers);

            // Act
            UserListResponse response = userListService.mainProcess(pageNumber, pageSize, null, null, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).hasSize(3);
            verify(userSecurityRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Data Conversion - UserSecurity to UserListDto Mapping")
    class DataConversionTests {

        @Test
        @DisplayName("Should convert UserSecurity entity to UserListDto correctly")
        void shouldConvertUserSecurityToUserListDto() {
            // Arrange - Setup paginated data
            int pageNumber = 1;
            int pageSize = 10;
            Pageable pageable = PageRequest.of(0, 10, Sort.by("secUsrId"));
            Page<UserSecurity> userPage = new PageImpl<>(Arrays.asList(adminUser), pageable, 1);

            when(userSecurityRepository.findAll(pageable)).thenReturn(userPage);

            // Act
            UserListResponse response = userListService.listUsers(pageNumber, pageSize);

            // Assert
            assertThat(response.getUsers()).hasSize(1);
            UserListDto dto = response.getUsers().get(0);
            assertThat(dto.getUserId()).isEqualTo("ADMIN001");
            assertThat(dto.getFirstName()).isEqualTo("Admin");
            assertThat(dto.getLastName()).isEqualTo("User");
            assertThat(dto.getUserType()).isEqualTo("01");
            assertThat(dto.getSelectionFlag()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Error Handling - COBOL ABEND Equivalent Processing")  
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptions() {
            // Arrange
            int pageNumber = 1;
            int pageSize = 10;
            when(userSecurityRepository.findAll(any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database connection error"));

            // Act
            UserListResponse response = userListService.listUsers(pageNumber, pageSize);

            // Assert - Should return empty response instead of throwing
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
            assertThat(response.getPageNumber()).isEqualTo(pageNumber);
        }

        @Test
        @DisplayName("Should handle null repository response")
        void shouldHandleNullRepositoryResponse() {
            // Arrange
            String searchTerm = "NOTFOUND";
            when(userSecurityRepository.findBySecUsrId(searchTerm))
                    .thenReturn(Optional.empty());
            when(userSecurityRepository.findByUsernameContainingIgnoreCase(searchTerm))
                    .thenReturn(Collections.emptyList());

            // Act
            UserListResponse response = userListService.findUsersBySearch(searchTerm, 1, 10);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUsers()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
        }
    }
}