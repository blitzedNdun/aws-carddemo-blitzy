/*
 * AdminMenuRequest.java
 * 
 * Request DTO for administrative menu operations containing user input for admin menu selection.
 * Maps COADM01 BMS admin menu input field and validates admin menu option selection (1-4) 
 * from COADM02Y options structure.
 * 
 * This class represents a direct translation from the COBOL COADM01 BMS mapset OPTION field
 * to support the four administrative menu options:
 * 1. User List (Security) - COUSR00C
 * 2. User Add (Security) - COUSR01C  
 * 3. User Update (Security) - COUSR02C
 * 4. User Delete (Security) - COUSR03C
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for administrative menu operations.
 * 
 * This DTO maps the COADM01 BMS mapset OPTION input field and provides validation
 * for admin menu option selection based on CDEMO-ADMIN-OPT-COUNT from COADM02Y copybook.
 * 
 * Valid option values:
 * <ul>
 *   <li>1 - User List (Security) - COUSR00C</li>
 *   <li>2 - User Add (Security) - COUSR01C</li>
 *   <li>3 - User Update (Security) - COUSR02C</li>
 *   <li>4 - User Delete (Security) - COUSR03C</li>
 * </ul>
 * 
 * The sessionData field maintains user context across admin operations,
 * replicating CICS COMMAREA functionality for state management.
 * 
 * @see <a href="../../../../../../../app/bms/COADM01.bms">COADM01.bms</a>
 * @see <a href="../../../../../../../app/cpy/COADM02Y.cpy">COADM02Y.cpy</a>
 */
@Data
public class AdminMenuRequest {
    
    /**
     * Selected admin menu option number.
     * 
     * Maps to the OPTION field from COADM01 BMS mapset (position 20,41, length 2, numeric).
     * Validates against CDEMO-ADMIN-OPT-COUNT (value 4) from COADM02Y copybook to ensure
     * the selected option falls within the valid range of administrative functions.
     * 
     * Valid values:
     * <ul>
     *   <li>1 - User List (Security)</li>
     *   <li>2 - User Add (Security)</li>
     *   <li>3 - User Update (Security)</li>
     *   <li>4 - User Delete (Security)</li>
     * </ul>
     * 
     * Validation mirrors the original COBOL edit rules for the OPTION field which
     * was defined with ATTRB=(FSET,IC,NORM,NUM,UNPROT) attributes.
     */
    @NotNull(message = "Admin menu option selection is required")
    @Min(value = 1, message = "Admin menu option must be between 1 and 4")
    @Max(value = 4, message = "Admin menu option must be between 1 and 4")
    private Integer selectedOption;
    
    /**
     * Session data for maintaining user context across admin operations.
     * 
     * This field replicates CICS COMMAREA functionality by preserving user session
     * state, authentication context, and transaction flow information across
     * multiple administrative operations. Essential for maintaining security
     * context when navigating between different admin menu functions.
     * 
     * Typically contains:
     * <ul>
     *   <li>User ID and authentication status</li>
     *   <li>Session timeout information</li>
     *   <li>Previous screen context for navigation</li>
     *   <li>Role-based access control data</li>
     * </ul>
     */
    private String sessionData;
}