/**
 * CardDemo Constants Barrel Export
 * 
 * Centralized barrel export file providing type-safe access to all application constants
 * derived from BMS definitions. Aggregates field, transaction, theme, navigation, 
 * validation, keyboard, and message constants into organized namespaces for clean imports
 * and consistent usage across React components.
 * 
 * This barrel export preserves exact BMS field behaviors while enabling modern TypeScript
 * development patterns and IntelliSense support across all React components.
 * 
 * Generated from BMS maps: COSGN00, COMEN01, COACTVW, COACTUP, COCRDLI, COCRDSL,
 * COCRDUP, COTRN00, COTRN01, COTRN02, COBIL00, CORPT00, COADM01, COUSR00-03
 * 
 * @author Blitzy agent
 * @version 1.0.0
 */

// =============================================================================
// INDIVIDUAL CONSTANT IMPORTS
// =============================================================================

// Field Constants - BMS attribute mappings and field configurations
import { 
  BMS_ATTRIBUTES, 
  FIELD_POSITIONING, 
  FIELD_LENGTHS, 
  ATTRIBUTE_MAPPINGS 
} from './FieldConstants';

// Transaction Constants - API endpoints and routing configuration
import { 
  TRANSACTION_ENDPOINTS, 
  API_BASE_PATHS 
} from './TransactionConstants';

// Theme Constants - Material-UI theme and styling mappings
import { 
  BMS_COLORS, 
  DISPLAY_ATTRIBUTES, 
  HIGHLIGHT_STYLES 
} from './ThemeConstants';

// Navigation Constants - React Router paths and navigation flow
import { 
  ROUTES, 
  NAVIGATION_FLOW, 
  BREADCRUMB_PATHS 
} from './NavigationConstants';

// Validation Constants - Input validation rules and patterns
import { 
  PICIN_PATTERNS, 
  VALIDATION_RULES, 
  INPUT_MASKS, 
  FIELD_CONSTRAINTS 
} from './ValidationConstants';

// Keyboard Constants - Function key mappings and accessibility
import { 
  FUNCTION_KEYS, 
  ALTERNATIVE_KEY_COMBINATIONS, 
  KEYBOARD_ACCESSIBILITY_CONFIG 
} from './KeyboardConstants';

// Message Constants - User-facing text content and error messages
import { 
  FUNCTION_KEY_HELP, 
  KEYBOARD_INSTRUCTIONS, 
  HELP_TEXT, 
  API_ERROR_MESSAGES, 
  VALIDATION_ERRORS, 
  FIELD_ERROR_MESSAGES 
} from './MessageConstants';

// =============================================================================
// ORGANIZED NAMESPACE EXPORTS
// =============================================================================

/**
 * Field Constants Namespace
 * Provides centralized access to all BMS field-related constants including
 * attribute mappings, positioning coordinates, length constraints, and
 * Material-UI property transformations.
 */
export const FieldConstants = {
  /** BMS attribute definitions for field behavior configuration */
  BMS_ATTRIBUTES,
  
  /** Field positioning coordinates converted from BMS POS attributes */
  FIELD_POSITIONING,
  
  /** Field length constraints extracted from BMS LENGTH definitions */
  FIELD_LENGTHS,
  
  /** BMS to Material-UI attribute mapping configurations */
  ATTRIBUTE_MAPPINGS
} as const;

/**
 * Transaction Constants Namespace
 * Provides centralized access to all transaction-related identifiers and
 * API endpoint configurations for microservice communication.
 */
export const TransactionConstants = {
  /** REST API endpoints mapped from CICS transaction codes */
  TRANSACTION_ENDPOINTS,
  
  /** Base path constants for Spring Boot microservices */
  API_BASE_PATHS
} as const;

/**
 * Theme Constants Namespace
 * Provides centralized access to all Material-UI theme configurations
 * mapped from BMS color and display attributes ensuring WCAG compliance.
 */
export const ThemeConstants = {
  /** BMS color mappings to Material-UI palette colors */
  BMS_COLORS,
  
  /** BMS display attribute mappings to Material-UI typography */
  DISPLAY_ATTRIBUTES,
  
  /** BMS highlight style mappings to Material-UI emphasis styling */
  HIGHLIGHT_STYLES
} as const;

/**
 * Navigation Constants Namespace
 * Provides centralized access to all React Router navigation paths and
 * flow configurations preserving original CICS XCTL patterns.
 */
export const NavigationConstants = {
  /** React Router path definitions mapped from BMS screens */
  ROUTES,
  
  /** Navigation flow patterns preserving CICS XCTL behavior */
  NAVIGATION_FLOW,
  
  /** Breadcrumb navigation paths maintaining screen hierarchy */
  BREADCRUMB_PATHS
} as const;

/**
 * Validation Constants Namespace
 * Provides centralized access to all input validation rules, patterns,
 * and constraints extracted from BMS field definitions and COBOL logic.
 */
export const ValidationConstants = {
  /** PICIN validation patterns from BMS definitions */
  PICIN_PATTERNS,
  
  /** Validation rules mapped from BMS attributes */
  VALIDATION_RULES,
  
  /** Input masks for formatted field entry */
  INPUT_MASKS,
  
  /** Field constraint definitions for length and range validation */
  FIELD_CONSTRAINTS
} as const;

/**
 * Keyboard Constants Namespace
 * Provides centralized access to all keyboard navigation and function key
 * mappings preserving original CICS PF key functionality with accessibility.
 */
export const KeyboardConstants = {
  /** Function key definitions mapped from CICS PF keys */
  FUNCTION_KEYS,
  
  /** Alternative key combinations for browser compatibility */
  ALTERNATIVE_KEY_COMBINATIONS,
  
  /** Keyboard accessibility configuration for WCAG 2.1 AA compliance */
  KEYBOARD_ACCESSIBILITY_CONFIG
} as const;

/**
 * Message Constants Namespace
 * Provides centralized access to all user-facing text content, error messages,
 * and help text extracted from BMS definitions for consistent messaging.
 */
export const MessageConstants = {
  /** Function key help text from BMS definitions */
  FUNCTION_KEY_HELP,
  
  /** Keyboard navigation instructions for user guidance */
  KEYBOARD_INSTRUCTIONS,
  
  /** Help text and tooltips for field guidance */
  HELP_TEXT,
  
  /** API error messages for network and server errors */
  API_ERROR_MESSAGES,
  
  /** General validation error messages */
  VALIDATION_ERRORS,
  
  /** Field-specific error messages from COBOL business logic */
  FIELD_ERROR_MESSAGES
} as const;

// =============================================================================
// CONVENIENCE RE-EXPORTS
// =============================================================================

/**
 * Re-export all individual constants for direct import access
 * Enables both namespace imports and direct constant imports
 */
export {
  // Field Constants
  BMS_ATTRIBUTES,
  FIELD_POSITIONING,
  FIELD_LENGTHS,
  ATTRIBUTE_MAPPINGS,
  
  // Transaction Constants
  TRANSACTION_ENDPOINTS,
  API_BASE_PATHS,
  
  // Theme Constants
  BMS_COLORS,
  DISPLAY_ATTRIBUTES,
  HIGHLIGHT_STYLES,
  
  // Navigation Constants
  ROUTES,
  NAVIGATION_FLOW,
  BREADCRUMB_PATHS,
  
  // Validation Constants
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS,
  
  // Keyboard Constants
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  
  // Message Constants
  FUNCTION_KEY_HELP,
  KEYBOARD_INSTRUCTIONS,
  HELP_TEXT,
  API_ERROR_MESSAGES,
  VALIDATION_ERRORS,
  FIELD_ERROR_MESSAGES
};

// =============================================================================
// TYPE EXPORTS
// =============================================================================

/**
 * Re-export types from individual constant files for comprehensive type support
 */
export type { BMSAttribute, FieldName, FieldPosition, AttributeMapping, FormatPattern, FieldConfig } from './FieldConstants';
export type { BreadcrumbData } from '../types/NavigationTypes';

// =============================================================================
// DEFAULT EXPORT
// =============================================================================

/**
 * Default export providing all constants in a single object
 * Enables importing all constants with a single import statement
 */
export default {
  Field: FieldConstants,
  Transaction: TransactionConstants,
  Theme: ThemeConstants,
  Navigation: NavigationConstants,
  Validation: ValidationConstants,
  Keyboard: KeyboardConstants,
  Message: MessageConstants
} as const;