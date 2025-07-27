/**
 * Constants Index - Centralized Barrel Export
 * 
 * TypeScript barrel export file providing centralized access to all application constants
 * derived from BMS definitions. Aggregates field, transaction, theme, navigation, validation,
 * keyboard, and message constants into organized namespaces enabling clean imports and 
 * type-safe usage across React components.
 * 
 * This file serves as the single entry point for all constant imports throughout the React
 * application, ensuring consistent access patterns and simplified dependency management.
 * All constants maintain exact compatibility with original BMS definitions while providing
 * modern TypeScript type safety and developer productivity features.
 * 
 * Key Features:
 * - Centralized constant access through organized namespaces
 * - Type-safe re-exports preserving original constant definitions
 * - Clean import patterns for React components
 * - IntelliSense support for all constant categories
 * - Prevents naming conflicts between different constant categories
 * - Enables tree-shaking for optimal bundle size
 * 
 * Usage Examples:
 * ```typescript
 * // Import specific constant categories
 * import { FieldConstants, ThemeConstants } from '@/constants';
 * 
 * // Access specific constants
 * const fieldLength = FieldConstants.FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID;
 * const primaryColor = ThemeConstants.BMS_COLORS.BLUE.main;
 * 
 * // Import everything for comprehensive access
 * import * as Constants from '@/constants';
 * const route = Constants.NavigationConstants.ROUTES.LOGIN;
 * ```
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

// ============================================================================
// INDIVIDUAL CONSTANT MODULE IMPORTS
// ============================================================================

// Field Constants - BMS field definitions and attribute mappings
import {
  BMS_ATTRIBUTES,
  FIELD_LENGTHS,
  FIELD_POSITIONING,
  ATTRIBUTE_MAPPINGS,
  FORMAT_PATTERNS,
  FieldDefinition,
  ScreenDefinition,
  BMSAttribute,
  FieldLength,
  AttributeMapping,
  FormatPattern
} from './FieldConstants';

// Transaction Constants - CICS transaction codes and API endpoints
import {
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  TRANSACTION_CODES,
  SCREEN_TITLES,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TRANSACTION_ROUTES,
  TransactionCode,
  ScreenTitle,
  ComponentMapping,
  TransactionEndpoint,
  ApiBasePath,
  getTransactionTitle,
  getComponentPath,
  isValidTransactionFlow,
  getNextTransactionOptions
} from './TransactionConstants';

// Theme Constants - Material-UI theme configuration from BMS colors
import {
  BMS_COLORS,
  DISPLAY_ATTRIBUTES,
  HIGHLIGHT_STYLES,
  SPACING_CONSTANTS,
  ACCESSIBILITY_CONSTANTS,
  BMSThemeColors,
  DisplayAttributes,
  HighlightStyles,
  SpacingConstants,
  AccessibilityConstants,
  getBMSColor,
  applyDisplayAttribute,
  applyHighlightStyle
} from './ThemeConstants';

// Navigation Constants - React Router paths and breadcrumb configuration
import {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  ROUTE_PARAMETERS,
  RouteKey,
  NavigationFlowKey,
  BreadcrumbPathKey,
  RouteParameterKey,
  ScreenTitleKey,
  HierarchyLevelKey,
  getRouteByTransactionCode,
  getBreadcrumbForRoute,
  getExitPathForRoute,
  isAdminRoute,
  getMenuOptionsForRole,
  buildRouteWithParameters
} from './NavigationConstants';

// Validation Constants - Input validation rules and patterns from BMS definitions
import {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  YUP_SCHEMA_BUILDERS,
  VALIDATION_MESSAGES,
  CROSS_FIELD_VALIDATION,
  FIELD_VALIDATION_CONFIGS
} from './ValidationConstants';

// Keyboard Constants - Function key mappings and accessibility support
import {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  FunctionKeyName,
  AlternativeKeyName,
  TouchGestureName,
  TouchButtonName,
  getFunctionKeyByCode,
  getAlternativeByKeySequence,
  isValidContext,
  getKeyboardShortcutsForContext,
  buildKeyboardEventHandler
} from './KeyboardConstants';

// Message Constants - User-facing text content from BMS definitions
import {
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  SCREEN_TITLES as MESSAGE_SCREEN_TITLES,
  FIELD_LABELS,
  FORMAT_HINTS,
  TABLE_HEADERS,
  INSTRUCTION_MESSAGES,
  SUCCESS_MESSAGES,
  FUNCTION_KEY_COMBINATIONS,
  SPECIAL_DISPLAYS,
  DEFAULT_VALUES,
  FunctionKeyHelpType,
  KeyboardInstructionsType,
  HelpTextType,
  ApiErrorMessagesType,
  ValidationErrorsType,
  FieldErrorMessagesType
} from './MessageConstants';

// ============================================================================
// ORGANIZED CONSTANT NAMESPACES
// ============================================================================

/**
 * Field Constants Namespace
 * 
 * Centralizes all field-related constants including BMS attributes, field lengths,
 * positioning coordinates, attribute mappings, and format patterns. Provides
 * comprehensive field configuration for React components ensuring exact preservation
 * of original BMS field behaviors.
 * 
 * Key Members:
 * - BMS_ATTRIBUTES: BMS field attribute definitions (ASKIP, UNPROT, PROT, etc.)
 * - FIELD_POSITIONING: Screen positioning coordinates from BMS POS attributes  
 * - FIELD_LENGTHS: Field length constraints from BMS LENGTH attributes
 * - ATTRIBUTE_MAPPINGS: BMS attribute to Material-UI component property mappings
 */
export const FieldConstants = {
  BMS_ATTRIBUTES,
  FIELD_POSITIONING,
  FIELD_LENGTHS,
  ATTRIBUTE_MAPPINGS,
  FORMAT_PATTERNS,
  
  // Type definitions
  types: {
    FieldDefinition,
    ScreenDefinition,
    BMSAttribute,
    FieldLength,
    AttributeMapping,
    FormatPattern
  }
} as const;

/**
 * Transaction Constants Namespace
 * 
 * Centralizes all transaction-related identifiers including CICS transaction codes,
 * API endpoints, screen titles, and navigation flow patterns. Provides transaction
 * configuration ensuring traceability between original CICS transactions and React
 * components.
 * 
 * Key Members:
 * - TRANSACTION_ENDPOINTS: API endpoint mappings for microservice integration
 * - API_BASE_PATHS: Base path constants for microservice endpoint routing
 */
export const TransactionConstants = {
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  TRANSACTION_CODES,
  SCREEN_TITLES,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TRANSACTION_ROUTES,
  
  // Utility functions
  getTransactionTitle,
  getComponentPath,
  isValidTransactionFlow,
  getNextTransactionOptions,
  
  // Type definitions
  types: {
    TransactionCode,
    ScreenTitle,
    ComponentMapping,
    TransactionEndpoint,
    ApiBasePath
  }
} as const;

/**
 * Theme Constants Namespace
 * 
 * Centralizes Material-UI theme configuration extracted from BMS color and display
 * attributes. Maps original mainframe color schemes to modern accessible web colors
 * while preserving visual hierarchy and semantic meaning.
 * 
 * Key Members:
 * - BMS_COLORS: Original BMS color mappings to Material-UI palette colors
 * - DISPLAY_ATTRIBUTES: BMS display intensity mappings (BRT, NORM, DRK)
 * - HIGHLIGHT_STYLES: BMS highlight attribute mappings (UNDERLINE, BLINK, REVERSE)
 */
export const ThemeConstants = {
  BMS_COLORS,
  DISPLAY_ATTRIBUTES,
  HIGHLIGHT_STYLES,
  SPACING_CONSTANTS,
  ACCESSIBILITY_CONSTANTS,
  
  // Utility functions
  getBMSColor,
  applyDisplayAttribute,
  applyHighlightStyle,
  
  // Type definitions
  types: {
    BMSThemeColors,
    DisplayAttributes,
    HighlightStyles,
    SpacingConstants,
    AccessibilityConstants
  }
} as const;

/**
 * Navigation Constants Namespace
 * 
 * Centralizes React Router navigation paths and routing configuration extracted
 * from BMS screen flow patterns. Maps original CICS XCTL navigation to modern
 * declarative routing while preserving user experience and security hierarchies.
 * 
 * Key Members:
 * - ROUTES: Primary route definitions mapping BMS screens to React Router paths
 * - NAVIGATION_FLOW: CICS XCTL program transfer patterns using React Router
 * - BREADCRUMB_PATHS: Hierarchical navigation path tracking for user orientation
 */
export const NavigationConstants = {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  ROUTE_PARAMETERS,
  
  // Utility functions
  getRouteByTransactionCode,
  getBreadcrumbForRoute,
  getExitPathForRoute,
  isAdminRoute,
  getMenuOptionsForRole,
  buildRouteWithParameters,
  
  // Type definitions
  types: {
    RouteKey,
    NavigationFlowKey,
    BreadcrumbPathKey,
    RouteParameterKey,
    ScreenTitleKey,
    HierarchyLevelKey
  }
} as const;

/**
 * Validation Constants Namespace
 * 
 * Centralizes comprehensive input validation rules and patterns extracted from
 * BMS field definitions and copybook structures. Provides Yup schema configurations,
 * regex patterns, and validation messages ensuring exact preservation of original
 * COBOL validation logic.
 * 
 * Key Members:
 * - PICIN_PATTERNS: Validation patterns from BMS PICIN definitions
 * - VALIDATION_RULES: BMS validation attribute mappings to Yup schemas
 * - INPUT_MASKS: Format templates for masked input components
 * - FIELD_CONSTRAINTS: Length limits and business rule validation constraints
 */
export const ValidationConstants = {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  YUP_SCHEMA_BUILDERS,
  VALIDATION_MESSAGES,
  CROSS_FIELD_VALIDATION,
  FIELD_VALIDATION_CONFIGS
} as const;

/**
 * Keyboard Constants Namespace
 * 
 * Centralizes comprehensive keyboard navigation and function key mappings preserving
 * original CICS PF key functionality. Provides event handler configurations,
 * alternative key combinations, and accessibility support ensuring consistent
 * keyboard navigation across the modernized application.
 * 
 * Key Members:
 * - FUNCTION_KEYS: Primary function key definitions with JavaScript KeyboardEvent codes
 * - ALTERNATIVE_KEY_COMBINATIONS: Alternative shortcuts for browser compatibility
 * - KEYBOARD_ACCESSIBILITY_CONFIG: WCAG 2.1 AA compliant keyboard accessibility support
 */
export const KeyboardConstants = {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  
  // Utility functions
  getFunctionKeyByCode,
  getAlternativeByKeySequence,
  isValidContext,
  getKeyboardShortcutsForContext,
  buildKeyboardEventHandler,
  
  // Type definitions
  types: {
    FunctionKeyName,
    AlternativeKeyName,
    TouchGestureName,
    TouchButtonName
  }
} as const;

/**
 * Message Constants Namespace
 * 
 * Centralizes all user-facing text content extracted from BMS definitions preserving
 * exact original messaging while providing centralized text management for React
 * components and future internationalization support. Maintains character-for-character
 * compatibility with original COBOL/BMS definitions.
 * 
 * Key Members:
 * - FUNCTION_KEY_HELP: Function key instruction text from BMS definitions
 * - KEYBOARD_INSTRUCTIONS: Comprehensive keyboard navigation help for accessibility
 * - HELP_TEXT: Tooltips, hints, and accessibility descriptions from BMS fields
 * - API_ERROR_MESSAGES: Server-side error messages for API communication failures
 * - VALIDATION_ERRORS: Client-side field validation error messages
 * - FIELD_ERROR_MESSAGES: Specific field validation errors preserving original COBOL text
 */
export const MessageConstants = {
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  SCREEN_TITLES: MESSAGE_SCREEN_TITLES,
  FIELD_LABELS,
  FORMAT_HINTS,
  TABLE_HEADERS,
  INSTRUCTION_MESSAGES,
  SUCCESS_MESSAGES,
  FUNCTION_KEY_COMBINATIONS,
  SPECIAL_DISPLAYS,
  DEFAULT_VALUES,
  
  // Type definitions
  types: {
    FunctionKeyHelpType,
    KeyboardInstructionsType,
    HelpTextType,
    ApiErrorMessagesType,
    ValidationErrorsType,
    FieldErrorMessagesType
  }
} as const;

// ============================================================================
// CONVENIENCE EXPORTS
// ============================================================================

/**
 * All Constants Consolidated
 * 
 * Complete consolidation of all constant categories for comprehensive access.
 * Useful for scenarios requiring access to multiple constant types or for
 * debugging and development purposes.
 */
export const AllConstants = {
  Field: FieldConstants,
  Transaction: TransactionConstants,
  Theme: ThemeConstants,
  Navigation: NavigationConstants,
  Validation: ValidationConstants,
  Keyboard: KeyboardConstants,
  Message: MessageConstants
} as const;

/**
 * Most Commonly Used Constants
 * 
 * Quick access to the most frequently used constants across React components.
 * Provides convenient shortcuts for common development scenarios.
 */
export const CommonConstants = {
  // Frequently used field properties
  BMS_ATTRIBUTES,
  FIELD_LENGTHS: FIELD_LENGTHS.LENGTH_CONSTRAINTS,
  
  // Essential navigation paths
  ROUTES,
  
  // Common validation patterns
  VALIDATION_PATTERNS: PICIN_PATTERNS.NUMERIC_PATTERNS,
  
  // Primary colors and styles
  COLORS: BMS_COLORS,
  
  // Essential function keys
  FUNCTION_KEYS: {
    F3: FUNCTION_KEYS.F3,
    F7: FUNCTION_KEYS.F7,
    F8: FUNCTION_KEYS.F8,
    ENTER: FUNCTION_KEYS.ENTER,
    ESCAPE: FUNCTION_KEYS.ESCAPE
  },
  
  // Common error messages
  ERROR_MESSAGES: FIELD_ERROR_MESSAGES
} as const;

// ============================================================================
// TYPE EXPORTS
// ============================================================================

// Re-export important types for external use
export type {
  // Field types
  FieldDefinition,
  ScreenDefinition,
  BMSAttribute,
  FieldLength,
  AttributeMapping,
  FormatPattern,
  
  // Transaction types
  TransactionCode,
  ScreenTitle,
  ComponentMapping,
  TransactionEndpoint,
  ApiBasePath,
  
  // Theme types
  BMSThemeColors,
  DisplayAttributes,
  HighlightStyles,
  SpacingConstants,
  AccessibilityConstants,
  
  // Navigation types
  RouteKey,
  NavigationFlowKey,
  BreadcrumbPathKey,
  RouteParameterKey,
  ScreenTitleKey,
  HierarchyLevelKey,
  
  // Keyboard types
  FunctionKeyName,
  AlternativeKeyName,
  TouchGestureName,
  TouchButtonName,
  
  // Message types
  FunctionKeyHelpType,
  KeyboardInstructionsType,
  HelpTextType,
  ApiErrorMessagesType,
  ValidationErrorsType,
  FieldErrorMessagesType
};

// ============================================================================
// DEFAULT EXPORT
// ============================================================================

/**
 * Default export providing the most commonly used constant namespaces
 * for convenient importing in React components.
 */
export default {
  Field: FieldConstants,
  Transaction: TransactionConstants,
  Theme: ThemeConstants,
  Navigation: NavigationConstants,
  Validation: ValidationConstants,
  Keyboard: KeyboardConstants,
  Message: MessageConstants,
  
  // Utility access
  Common: CommonConstants,
  All: AllConstants
} as const;