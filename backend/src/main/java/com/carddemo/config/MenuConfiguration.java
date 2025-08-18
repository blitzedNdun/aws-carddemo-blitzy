/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

import com.carddemo.dto.MenuOption;

/**
 * Spring Configuration class for main menu system providing static menu option definitions
 * from COMEN02Y copybook. This class converts the COBOL static data table structure
 * CDEMO-MENU-OPTIONS-DATA to Spring-managed beans for menu option management.
 * 
 * The menu structure maintains exact functional parity with the original COBOL 
 * COMEN02Y.cpy copybook, preserving all menu option numbers, descriptions, 
 * program names, and access levels. This ensures seamless migration from
 * CICS transaction routing to Spring Boot REST controller routing.
 * 
 * Original COBOL Structure (COMEN02Y.cpy):
 * - CDEMO-MENU-OPT-COUNT: 10 menu options
 * - CDEMO-MENU-OPTIONS-DATA: Static table with 10 FILLER entries
 * - Each entry: option number (PIC 9(02)), description (PIC X(35)), 
 *               program name (PIC X(08)), user type (PIC X(01))
 * 
 * Spring Boot Implementation:
 * - @Configuration class with @Bean methods for dependency injection
 * - MenuOption DTOs replacing COBOL data structures
 * - Support for role-based menu filtering matching original RACF integration
 */
@Configuration
public class MenuConfiguration {

    /**
     * Static menu option count matching CDEMO-MENU-OPT-COUNT from COMEN02Y.cpy.
     * Represents the total number of menu options available in the system.
     */
    private static final int MENU_OPTION_COUNT = 10;

    /**
     * Creates and returns the list of default menu options matching the exact
     * structure and content from COMEN02Y.cpy CDEMO-MENU-OPTIONS-DATA.
     * 
     * This method converts the COBOL static data table to MenuOption DTOs,
     * preserving all original values:
     * - Option numbers (1-10)
     * - Descriptions (trimmed to remove COBOL padding)
     * - Program names (CICS transaction codes)
     * - Access levels ('U' for User access)
     * 
     * The mapping maintains functional parity with the original COBOL 
     * BUILD-MENU-OPTIONS procedure from COMEN01C.cbl.
     * 
     * @return List of MenuOption objects representing the main menu structure
     */
    public List<MenuOption> createDefaultMenuOptions() {
        List<MenuOption> menuOptions = new ArrayList<>();

        // Option 1: Account View - COACTVWC - User access
        MenuOption option1 = new MenuOption();
        option1.setOptionNumber(1);
        option1.setDescription("Account View");
        option1.setTransactionCode("COACTVWC");
        option1.setAccessLevel("U");
        menuOptions.add(option1);

        // Option 2: Account Update - COACTUPC - User access  
        MenuOption option2 = new MenuOption();
        option2.setOptionNumber(2);
        option2.setDescription("Account Update");
        option2.setTransactionCode("COACTUPC");
        option2.setAccessLevel("U");
        menuOptions.add(option2);

        // Option 3: Credit Card List - COCRDLIC - User access
        MenuOption option3 = new MenuOption();
        option3.setOptionNumber(3);
        option3.setDescription("Credit Card List");
        option3.setTransactionCode("COCRDLIC");
        option3.setAccessLevel("U");
        menuOptions.add(option3);

        // Option 4: Credit Card View - COCRDSLC - User access
        MenuOption option4 = new MenuOption();
        option4.setOptionNumber(4);
        option4.setDescription("Credit Card View");
        option4.setTransactionCode("COCRDSLC");
        option4.setAccessLevel("U");
        menuOptions.add(option4);

        // Option 5: Credit Card Update - COCRDUPC - User access
        MenuOption option5 = new MenuOption();
        option5.setOptionNumber(5);
        option5.setDescription("Credit Card Update");
        option5.setTransactionCode("COCRDUPC");
        option5.setAccessLevel("U");
        menuOptions.add(option5);

        // Option 6: Transaction List - COTRN00C - User access
        MenuOption option6 = new MenuOption();
        option6.setOptionNumber(6);
        option6.setDescription("Transaction List");
        option6.setTransactionCode("COTRN00C");
        option6.setAccessLevel("U");
        menuOptions.add(option6);

        // Option 7: Transaction View - COTRN01C - User access
        MenuOption option7 = new MenuOption();
        option7.setOptionNumber(7);
        option7.setDescription("Transaction View");
        option7.setTransactionCode("COTRN01C");
        option7.setAccessLevel("U");
        menuOptions.add(option7);

        // Option 8: Transaction Add - COTRN02C - User access
        MenuOption option8 = new MenuOption();
        option8.setOptionNumber(8);
        option8.setDescription("Transaction Add");
        option8.setTransactionCode("COTRN02C");
        option8.setAccessLevel("U");
        menuOptions.add(option8);

        // Option 9: Transaction Reports - CORPT00C - User access
        MenuOption option9 = new MenuOption();
        option9.setOptionNumber(9);
        option9.setDescription("Transaction Reports");
        option9.setTransactionCode("CORPT00C");
        option9.setAccessLevel("U");
        menuOptions.add(option9);

        // Option 10: Bill Payment - COBIL00C - User access
        MenuOption option10 = new MenuOption();
        option10.setOptionNumber(10);
        option10.setDescription("Bill Payment");
        option10.setTransactionCode("COBIL00C");
        option10.setAccessLevel("U");
        menuOptions.add(option10);

        return menuOptions;
    }

    /**
     * Spring Bean method that provides the menu options list for dependency injection.
     * This method creates and configures the complete list of menu options that can be
     * injected into controllers, services, or other Spring-managed components.
     * 
     * The Bean is singleton-scoped by default, ensuring the same menu options list
     * is reused across the application, providing performance benefits and memory
     * efficiency similar to the static COBOL data table approach.
     * 
     * This replaces the COBOL COPY COMEN02Y inclusion pattern with Spring's
     * dependency injection mechanism, allowing any component to access the
     * menu structure through @Autowired or constructor injection.
     * 
     * @return List of MenuOption objects configured as a Spring Bean
     */
    @Bean
    public List<MenuOption> getMenuOptions() {
        return createDefaultMenuOptions();
    }

    /**
     * Returns the total count of menu options available in the system.
     * This method provides the equivalent functionality to CDEMO-MENU-OPT-COUNT
     * from the original COMEN02Y.cpy copybook.
     * 
     * The count is used for validation in menu processing logic, ensuring
     * user input falls within valid option range (1-10), matching the
     * validation logic in COMEN01C.cbl PROCESS-ENTER-KEY procedure.
     * 
     * Usage examples:
     * - Menu option validation in REST controllers
     * - Dynamic menu rendering in React components  
     * - Loop bounds for menu processing operations
     * 
     * @return The total number of menu options (10)
     */
    public int getMenuOptionCount() {
        return MENU_OPTION_COUNT;
    }
}
