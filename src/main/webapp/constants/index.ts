/**
 * CardDemo Constants Barrel Export
 * 
 * Centralized barrel export file providing organized access to all application constants
 * derived from BMS definitions and COBOL copybook structures. This file aggregates
 * field, transaction, theme, navigation, validation, keyboard, and message constants
 * into organized namespaces enabling clean imports and type-safe usage across React components.
 * 
 * Purpose:
 * - Aggregates all constant modules for simplified imports across React components
 * - Establishes consistent import patterns for components accessing BMS-derived constants
 * - Provides type-safe re-exports of all constant definitions enabling IntelliSense support
 * - Creates organized constant namespaces preventing naming conflicts between categories
 * 
 * Usage Examples:
 * ```typescript
 * // Import specific constant namespace
 * import { FieldConstants, ValidationConstants } from '@/constants';
 * 
 * // Access specific constants
 * const fieldLength = FieldConstants.FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID;
 * const validation = ValidationConstants.VALIDATION_RULES.MUSTFILL.USER_ID;
 * ```
 * 
 * Technology Transformation: COBOL/BMS/Copybooks → TypeScript/React Constants
 * Original System: IBM COBOL copybooks and BMS screen definitions
 * Target System: TypeScript constant modules with organized namespaces
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 */

// =============================================================================
// FIELD CONSTANTS - BMS Field Definitions and Attribute Mappings
// =============================================================================

import {
  BMS_ATTRIBUTES,
  FIELD_POSITIONING,
  FIELD_LENGTHS,
  ATTRIBUTE_MAPPINGS,
  FORMAT_PATTERNS,
  type BMSAttribute,
  type FieldPosition,
  type FieldLength,
  type AttributeMapping,
  type FormatPattern,
  type BMSFieldConfig,
} from './FieldConstants';

/**
 * Field Constants Namespace
 * 
 * Provides centralized access to all BMS field-related constants including
 * field attributes, positioning, lengths, and Material-UI attribute mappings.
 * Maintains exact preservation of original BMS field behaviors while enabling
 * modern TypeScript type checking and React component integration.
 */
export const FieldConstants = {
  /**
   * BMS field attributes as TypeScript enum for compile-time type safety.
   * Maps directly to CICS BMS ATTRB parameter values maintaining exact behavior.
   */
  BMS_ATTRIBUTES,

  /**
   * Field positioning constants extracted from BMS POS attributes.
   * Converts 3270 terminal coordinates to modern CSS Grid layout system.
   */
  FIELD_POSITIONING,

  /**
   * Field length constraints extracted from BMS LENGTH definitions.
   * Maintains exact character counts for Material-UI TextField components.
   */
  FIELD_LENGTHS,

  /**
   * Mapping constants for converting BMS field attributes to Material-UI component properties.
   * Preserves original BMS field behavior in modern React components.
   */
  ATTRIBUTE_MAPPINGS,

  /**
   * Format patterns extracted from BMS PICIN and PICOUT definitions.
   * Provides input validation and output formatting for field data.
   */
  FORMAT_PATTERNS,
} as const;

// =============================================================================
// TRANSACTION CONSTANTS - CICS Transaction Mappings and API Endpoints
// =============================================================================

import {
  TRANSACTION_CODES,
  SCREEN_TITLES,
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  COMPONENT_MAPPINGS,
  TRANSACTION_FLOW,
  TransactionUtils,
  type TransactionCode,
  type ScreenTitle,
  type TransactionEndpoint,
  type ApiBasePath,
  type ComponentMapping,
} from './TransactionConstants';

/**
 * Transaction Constants Namespace
 * 
 * Provides centralized access to all transaction-related constants including
 * CICS transaction codes, REST API endpoints, component mappings, and
 * navigation flow patterns. Maintains traceability between original CICS
 * transactions and React components while enabling type-safe transaction handling.
 */
export const TransactionConstants = {
  /**
   * Original CICS transaction codes from BMS mapsets.
   * Maps each BMS mapset to its corresponding transaction identifier for audit traceability.
   */
  TRANSACTION_CODES,

  /**
   * Screen titles extracted from BMS mapset titles for consistent header display.
   * Preserves original mainframe screen titles for user familiarity.
   */
  SCREEN_TITLES,

  /**
   * REST API endpoints mapping original CICS transaction codes to Spring Boot microservices.
   * Each endpoint corresponds to a specific transaction type enabling consistent API routing.
   */
  TRANSACTION_ENDPOINTS,

  /**
   * Base API paths for microservice organization.
   * Maps business domains to their corresponding Spring Boot service base paths.
   */
  API_BASE_PATHS,

  /**
   * Component mappings linking BMS maps to their corresponding React component paths.
   * Maintains traceability between original CICS screens and React components.
   */
  COMPONENT_MAPPINGS,

  /**
   * Transaction flow constants preserving original CICS XCTL navigation patterns.
   * Defines screen-to-screen navigation sequences, return paths, and menu hierarchy.
   */
  TRANSACTION_FLOW,

  /**
   * Utility functions for transaction constant operations.
   * Provides programmatic access to transaction mappings and navigation logic.
   */
  TransactionUtils,
} as const;

// =============================================================================
// THEME CONSTANTS - BMS Color and Display Attributes
// =============================================================================

import {
  BMS_COLORS,
  DISPLAY_ATTRIBUTES,
  HIGHLIGHT_STYLES,
  SPACING_CONSTANTS,
  ACCESSIBILITY_CONSTANTS,
  getBmsColor,
  getDisplayAttribute,
  getHighlightStyle,
  getSpacing,
  animationKeyframes,
  type BmsColorKey,
  type DisplayAttributeKey,
  type HighlightStyleKey,
  type SpacingConstantKey,
  type AccessibilityConstantKey,
} from './ThemeConstants';

/**
 * Theme Constants Namespace
 * 
 * Provides centralized access to all theme-related constants including
 * BMS color mappings, display attributes, highlight styles, and accessibility
 * configurations. Maps BMS color and display attributes to Material-UI theme
 * configuration while maintaining WCAG 2.1 AA accessibility compliance.
 */
export const ThemeConstants = {
  /**
   * BMS color mappings to Material-UI theme colors.
   * Maps original BMS color attributes to accessible Material-UI palette colors.
   */
  BMS_COLORS,

  /**
   * BMS display attribute mappings to Material-UI typography and styling.
   * Converts BMS display attributes (BRT, NORM, DRK) to Material-UI typography.
   */
  DISPLAY_ATTRIBUTES,

  /**
   * BMS highlight style mappings to Material-UI emphasis and elevation.
   * Maps BMS HILIGHT attributes to Material-UI elevation and emphasis styles.
   */
  HIGHLIGHT_STYLES,

  /**
   * Spacing constants derived from BMS screen positioning.
   * Defines spacing constants based on BMS screen positioning for visual consistency.
   */
  SPACING_CONSTANTS,

  /**
   * Accessibility constants for WCAG 2.1 AA compliance.
   * Provides accessibility constants ensuring WCAG 2.1 AA compliance.
   */
  ACCESSIBILITY_CONSTANTS,

  /**
   * Utility function to get BMS color with fallback.
   * Provides safe access to BMS colors with TypeScript support.
   */
  getBmsColor,

  /**
   * Utility function to get display attribute styles.
   * Provides safe access to display attributes with TypeScript support.
   */
  getDisplayAttribute,

  /**
   * Utility function to get highlight styles.
   * Provides safe access to highlight styles with TypeScript support.
   */
  getHighlightStyle,

  /**
   * Utility function to get spacing value.
   * Provides safe access to spacing constants with TypeScript support.
   */
  getSpacing,

  /**
   * CSS-in-JS keyframes for animations.
   * Defines animations used in highlight styles.
   */
  animationKeyframes,
} as const;

// =============================================================================
// NAVIGATION CONSTANTS - React Router Paths and Navigation Flow
// =============================================================================

import {
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  NavigationUtils,
} from './NavigationConstants';

/**
 * Navigation Constants Namespace
 * 
 * Provides centralized access to all navigation-related constants including
 * React Router paths, navigation flow patterns, breadcrumb configurations,
 * and XCTL pattern mappings. Maps original CICS XCTL navigation to modern
 * declarative routing while preserving user experience and security hierarchies.
 */
export const NavigationConstants = {
  /**
   * Primary route definitions mapping each BMS screen to its corresponding React Router path.
   * Maintains original CICS transaction code references in path structure for audit traceability.
   */
  ROUTES,

  /**
   * Navigation flow patterns extracted from original CICS XCTL program control.
   * Defines screen-to-screen navigation sequences, return paths, and menu hierarchy.
   */
  NAVIGATION_FLOW,

  /**
   * Breadcrumb navigation configuration providing hierarchical path tracking.
   * Maps each route to its breadcrumb representation with parent-child relationships.
   */
  BREADCRUMB_PATHS,

  /**
   * Navigation utility functions providing programmatic access to navigation
   * constants and route resolution for React Router integration.
   */
  NavigationUtils,
} as const;

// =============================================================================
// VALIDATION CONSTANTS - Input Validation Rules and Patterns
// =============================================================================

import {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  VALIDATION_MESSAGES,
} from './ValidationConstants';

/**
 * Validation Constants Namespace
 * 
 * Provides centralized access to all validation-related constants including
 * PICIN patterns, validation rules, input masks, field constraints, and
 * validation messages. Transforms all BMS VALIDN, PICIN, and MUSTFILL
 * attributes into TypeScript validation constants for React Hook Form.
 */
export const ValidationConstants = {
  /**
   * PICIN validation patterns extracted from BMS field definitions.
   * Each pattern maintains exact COBOL validation logic for client-side validation.
   */
  PICIN_PATTERNS,

  /**
   * Validation rules extracted from BMS VALIDN attributes and field requirements.
   * Maps BMS validation attributes to React Hook Form validation rules.
   */
  VALIDATION_RULES,

  /**
   * Input masks for structured data fields.
   * Provides consistent formatting for user input while maintaining validation.
   */
  INPUT_MASKS,

  /**
   * Field constraints extracted from BMS LENGTH definitions and business rules.
   * Provides comprehensive validation rules for all field types.
   */
  FIELD_CONSTRAINTS,

  /**
   * Centralized validation error messages maintaining consistency with original BMS error messages.
   * Preserves user experience from original COBOL application.
   */
  VALIDATION_MESSAGES,
} as const;

// =============================================================================
// KEYBOARD CONSTANTS - Function Key Mappings and Accessibility
// =============================================================================

import {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  KeyboardUtils,
} from './KeyboardConstants';

/**
 * Keyboard Constants Namespace
 * 
 * Provides centralized access to all keyboard-related constants including
 * function key mappings, alternative key combinations, accessibility
 * configurations, and touch device equivalents. Preserves original CICS
 * PF key functionality while ensuring WCAG 2.1 AA accessibility compliance.
 */
export const KeyboardConstants = {
  /**
   * Primary function key definitions preserving exact CICS PF key functionality.
   * Each key mapping includes original CICS behavior, JavaScript key codes,
   * and React event handler configurations for seamless integration.
   */
  FUNCTION_KEYS,

  /**
   * Alternative key combinations for function keys that are reserved by browsers
   * or operating systems. Provides equivalent functionality using key combinations
   * that are consistently available across different browsers and platforms.
   */
  ALTERNATIVE_KEY_COMBINATIONS,

  /**
   * Comprehensive keyboard accessibility configuration ensuring WCAG 2.1 AA compliance
   * for keyboard-only navigation. Provides focus management, screen reader support,
   * and navigation assistance for users with disabilities.
   */
  KEYBOARD_ACCESSIBILITY_CONFIG,

  /**
   * Touch device equivalents for function key operations enabling mobile and tablet
   * compatibility. Provides gesture-based alternatives and touch-friendly interface
   * elements that replicate original function key behaviors.
   */
  TOUCH_DEVICE_EQUIVALENTS,

  /**
   * Utility functions for keyboard event handling and React hook integration.
   * Provides helper functions for key detection, event processing, and
   * accessibility support in React components.
   */
  KeyboardUtils,
} as const;

// =============================================================================
// MESSAGE CONSTANTS - User-Facing Text and System Messages
// =============================================================================

import {
  SCREEN_HEADERS,
  SCREEN_TITLES as MESSAGE_SCREEN_TITLES,
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES,
  FORM_LABELS,
  TABLE_FORMATTING,
  SUCCESS_MESSAGES,
  SYSTEM_MESSAGES,
  DEFAULT_VALUES,
  RESPONSIVE_MESSAGES,
  I18N_KEYS,
  ACCESSIBILITY,
} from './MessageConstants';

/**
 * Message Constants Namespace
 * 
 * Provides centralized access to all user-facing text content extracted from
 * BMS definitions including field labels, error messages, help text, and
 * system messages. Preserves exact original messaging while providing
 * centralized text management for React components and future
 * internationalization support.
 */
export const MessageConstants = {
  /**
   * Common screen header labels used across all BMS screens.
   */
  SCREEN_HEADERS,

  /**
   * Main screen titles and application branding.
   */
  SCREEN_TITLES: MESSAGE_SCREEN_TITLES,

  /**
   * Function key help text and descriptions.
   * Maintains exact original BMS function key messaging.
   */
  FUNCTION_KEY_HELP,

  /**
   * Keyboard navigation instructions and accessibility information.
   */
  KEYBOARD_INSTRUCTIONS,

  /**
   * Help text, tooltips, and accessibility descriptions.
   */
  HELP_TEXT,

  /**
   * API and system error messages.
   */
  API_ERROR_MESSAGES,

  /**
   * Form validation error messages.
   */
  VALIDATION_ERRORS,

  /**
   * Field-specific error messages preserving original COBOL error text.
   */
  FIELD_ERROR_MESSAGES,

  /**
   * Form field labels extracted from BMS definitions.
   */
  FORM_LABELS,

  /**
   * Table separators and formatting characters from BMS screens.
   */
  TABLE_FORMATTING,

  /**
   * Success messages and confirmation text.
   */
  SUCCESS_MESSAGES,

  /**
   * System status and informational messages.
   */
  SYSTEM_MESSAGES,

  /**
   * Default values and placeholders from BMS definitions.
   */
  DEFAULT_VALUES,

  /**
   * Messages for responsive design and mobile adaptations.
   */
  RESPONSIVE_MESSAGES,

  /**
   * Internationalization-ready message structure for future localization.
   */
  I18N_KEYS,

  /**
   * Accessibility-related constants for screen readers and assistive technologies.
   */
  ACCESSIBILITY,
} as const;

// =============================================================================
// TYPE EXPORTS - Export all types for external usage
// =============================================================================

// Re-export all types for external consumption
export type {
  // Field types
  BMSAttribute,
  FieldPosition,
  FieldLength,
  AttributeMapping,
  FormatPattern,
  BMSFieldConfig,
  
  // Transaction types
  TransactionCode,
  ScreenTitle,
  TransactionEndpoint,
  ApiBasePath,
  ComponentMapping,
  
  // Theme types
  BmsColorKey,
  DisplayAttributeKey,
  HighlightStyleKey,
  SpacingConstantKey,
  AccessibilityConstantKey,
};

// =============================================================================
// DEFAULT EXPORT - Complete constants object for legacy compatibility
// =============================================================================

/**
 * Complete constants object providing legacy import compatibility.
 * Supports both named imports and default import patterns for flexibility.
 * 
 * @example
 * ```typescript
 * // Named imports (preferred)
 * import { FieldConstants, ValidationConstants } from '@/constants';
 * 
 * // Default import (legacy compatibility)
 * import Constants from '@/constants';
 * const fieldLength = Constants.FieldConstants.FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID;
 * ```
 */
export default {
  FieldConstants,
  TransactionConstants,
  ThemeConstants,
  NavigationConstants,
  ValidationConstants,
  KeyboardConstants,
  MessageConstants,
} as const;