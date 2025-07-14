/**
 * KeyboardConstants.ts
 * 
 * TypeScript constants file defining comprehensive keyboard navigation and function key mappings
 * preserving original CICS PF key functionality. Provides event handler configurations, alternative
 * key combinations, and accessibility support for React components ensuring consistent keyboard
 * navigation across the modernized application.
 * 
 * This file centralizes all keyboard interaction patterns from the original BMS screen definitions,
 * transforming CICS function key behaviors into modern web application keyboard shortcuts while
 * maintaining exact functional equivalence with the mainframe terminal interface.
 * 
 * Key Features:
 * - Complete CICS PF key mapping (F3, F4, F7, F8, F12, ENTER, ESCAPE)
 * - Browser-compatible alternative key combinations for reserved function keys
 * - Touch device gesture equivalents for mobile and tablet support
 * - WCAG 2.1 AA accessibility compliance for keyboard-only navigation
 * - Event handler configurations for React useKeyboardShortcuts hook patterns
 * - Screen reader support with proper ARIA attributes and announcements
 * 
 * Technology Transformation: CICS/BMS/3270 → React/TypeScript/Web
 * Original System: IBM 3270 Terminal Function Keys with CICS BMS field navigation
 * Target System: Modern web keyboard shortcuts with React event handling
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 */

import { NAVIGATION_FLOW } from './NavigationConstants';

// ==========================================
// Core Function Key Mappings - CICS PF Key Equivalents
// ==========================================

/**
 * Primary function key definitions preserving exact CICS PF key functionality.
 * Each key mapping includes original CICS behavior, JavaScript key codes,
 * and React event handler configurations for seamless integration.
 * 
 * CICS PF Key Behaviors from BMS Analysis:
 * - F3: Exit/Back to previous screen (found in all BMS files)
 * - F4: Clear screen and reset form fields (COBIL00.bms)
 * - F7: Backward/Previous page navigation (COCRDLI.bms, COTRN00.bms)
 * - F8: Forward/Next page navigation (COCRDLI.bms, COTRN00.bms)
 * - F12: Cancel operation and return to main menu (requirement specification)
 * - ENTER: Continue/Submit form data (COSGN00.bms, COMEN01.bms)
 * - ESCAPE: General escape/abort functionality
 */
export const FUNCTION_KEYS = {
  // F3 - Exit/Back Navigation
  F3: {
    key: 'F3',
    keyCode: 114,
    code: 'F3',
    which: 114,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Exit/Back',
    description: 'Navigate back to previous screen or exit current operation',
    cicsEquivalent: 'PF3',
    bmsSource: 'All BMS files - Universal exit key',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'EXIT_SCREEN',
      navigationAction: 'RETURN_TO_PREVIOUS',
      returnPathResolver: (currentPath: string) => NAVIGATION_FLOW.RETURN_PATHS[currentPath] || '/menu/main',
    },
    accessibility: {
      ariaLabel: 'Exit current screen',
      ariaDescription: 'Press F3 to navigate back to the previous screen',
      tabIndex: 0,
      role: 'button',
    },
  },

  // F4 - Clear Screen/Reset Form
  F4: {
    key: 'F4',
    keyCode: 115,
    code: 'F4',
    which: 115,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Clear',
    description: 'Clear all form fields and reset screen to initial state',
    cicsEquivalent: 'PF4',
    bmsSource: 'COBIL00.bms - Clear button functionality',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'CLEAR_FORM',
      formAction: 'RESET_ALL_FIELDS',
      confirmationRequired: true,
      confirmationMessage: 'Are you sure you want to clear all fields?',
    },
    accessibility: {
      ariaLabel: 'Clear form fields',
      ariaDescription: 'Press F4 to clear all form fields and reset the screen',
      tabIndex: 0,
      role: 'button',
    },
  },

  // F7 - Previous Page/Backward Navigation
  F7: {
    key: 'F7',
    keyCode: 118,
    code: 'F7',
    which: 118,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Page Up/Backward',
    description: 'Navigate to previous page in paginated lists',
    cicsEquivalent: 'PF7',
    bmsSource: 'COCRDLI.bms, COTRN00.bms - Pagination controls',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'PREVIOUS_PAGE',
      paginationAction: 'DECREMENT_PAGE',
      disableIfFirstPage: true,
      ariaLiveUpdate: 'Previous page loaded',
    },
    accessibility: {
      ariaLabel: 'Previous page',
      ariaDescription: 'Press F7 to navigate to the previous page',
      tabIndex: 0,
      role: 'button',
    },
  },

  // F8 - Next Page/Forward Navigation
  F8: {
    key: 'F8',
    keyCode: 119,
    code: 'F8',
    which: 119,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Page Down/Forward',
    description: 'Navigate to next page in paginated lists',
    cicsEquivalent: 'PF8',
    bmsSource: 'COCRDLI.bms, COTRN00.bms - Pagination controls',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'NEXT_PAGE',
      paginationAction: 'INCREMENT_PAGE',
      disableIfLastPage: true,
      ariaLiveUpdate: 'Next page loaded',
    },
    accessibility: {
      ariaLabel: 'Next page',
      ariaDescription: 'Press F8 to navigate to the next page',
      tabIndex: 0,
      role: 'button',
    },
  },

  // F12 - Cancel Operation
  F12: {
    key: 'F12',
    keyCode: 123,
    code: 'F12',
    which: 123,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Cancel',
    description: 'Cancel current operation and return to main menu',
    cicsEquivalent: 'PF12',
    bmsSource: 'Technical specification requirement - Cancel functionality',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'CANCEL_OPERATION',
      navigationAction: 'RETURN_TO_MAIN_MENU',
      confirmationRequired: true,
      confirmationMessage: 'Are you sure you want to cancel the current operation?',
      returnPath: '/menu/main',
    },
    accessibility: {
      ariaLabel: 'Cancel operation',
      ariaDescription: 'Press F12 to cancel the current operation and return to main menu',
      tabIndex: 0,
      role: 'button',
    },
  },

  // ENTER - Submit/Continue
  ENTER: {
    key: 'Enter',
    keyCode: 13,
    code: 'Enter',
    which: 13,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Continue/Submit',
    description: 'Submit form data or continue to next screen',
    cicsEquivalent: 'ENTER',
    bmsSource: 'COSGN00.bms, COMEN01.bms - Form submission',
    eventHandler: {
      type: 'keydown',
      preventDefault: false, // Allow default form submission behavior
      stopPropagation: false,
      action: 'SUBMIT_FORM',
      formAction: 'VALIDATE_AND_SUBMIT',
      validationRequired: true,
      ariaLiveUpdate: 'Form submitted successfully',
    },
    accessibility: {
      ariaLabel: 'Submit form',
      ariaDescription: 'Press Enter to submit the form and continue',
      tabIndex: 0,
      role: 'button',
    },
  },

  // ESCAPE - General Escape/Abort
  ESCAPE: {
    key: 'Escape',
    keyCode: 27,
    code: 'Escape',
    which: 27,
    ctrlKey: false,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Escape/Abort',
    description: 'General escape key for closing dialogs and aborting operations',
    cicsEquivalent: 'CLEAR',
    bmsSource: 'General terminal escape functionality',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'ESCAPE_OPERATION',
      dialogAction: 'CLOSE_MODAL',
      formAction: 'CANCEL_EDIT',
    },
    accessibility: {
      ariaLabel: 'Escape current action',
      ariaDescription: 'Press Escape to cancel the current action or close dialogs',
      tabIndex: 0,
      role: 'button',
    },
  },
} as const;

// ==========================================
// Alternative Key Combinations - Browser Compatibility
// ==========================================

/**
 * Alternative key combinations for function keys that are reserved by browsers
 * or operating systems. Provides equivalent functionality using key combinations
 * that are consistently available across different browsers and platforms.
 * 
 * Browser Reserved Keys:
 * - F5: Browser refresh (use Ctrl+R as alternative)
 * - F11: Browser fullscreen (use Ctrl+F11 as alternative)
 * - F12: Browser developer tools (use Ctrl+Esc as alternative)
 * - Some F-keys may be reserved by operating systems
 */
export const ALTERNATIVE_KEY_COMBINATIONS = {
  // Ctrl+R as alternative for F5 (browser refresh conflict)
  CTRL_R_FOR_F5: {
    key: 'r',
    keyCode: 82,
    code: 'KeyR',
    which: 82,
    ctrlKey: true,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Refresh/Reload',
    description: 'Alternative to F5 for refreshing screen data',
    browserConflict: 'F5 reserved for browser refresh',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'REFRESH_SCREEN',
      dataAction: 'RELOAD_DATA',
      confirmationRequired: true,
      confirmationMessage: 'Refresh screen data?',
    },
    accessibility: {
      ariaLabel: 'Refresh screen',
      ariaDescription: 'Press Ctrl+R to refresh screen data',
      tabIndex: 0,
      role: 'button',
    },
  },

  // Ctrl+Esc as alternative for F12 (developer tools conflict)
  CTRL_ESC_FOR_F12: {
    key: 'Escape',
    keyCode: 27,
    code: 'Escape',
    which: 27,
    ctrlKey: true,
    altKey: false,
    shiftKey: false,
    metaKey: false,
    originalFunction: 'Cancel Operation',
    description: 'Alternative to F12 for canceling operations',
    browserConflict: 'F12 reserved for developer tools',
    eventHandler: {
      type: 'keydown',
      preventDefault: true,
      stopPropagation: true,
      action: 'CANCEL_OPERATION',
      navigationAction: 'RETURN_TO_MAIN_MENU',
      confirmationRequired: true,
      confirmationMessage: 'Cancel current operation?',
    },
    accessibility: {
      ariaLabel: 'Cancel operation',
      ariaDescription: 'Press Ctrl+Escape to cancel current operation',
      tabIndex: 0,
      role: 'button',
    },
  },

  // Alt key combinations for additional functionality
  ALT_COMBINATIONS: {
    // Alt+F3 for advanced exit with save prompt
    ALT_F3_SAVE_EXIT: {
      key: 'F3',
      keyCode: 114,
      code: 'F3',
      which: 114,
      ctrlKey: false,
      altKey: true,
      shiftKey: false,
      metaKey: false,
      originalFunction: 'Save and Exit',
      description: 'Save changes and exit current screen',
      eventHandler: {
        type: 'keydown',
        preventDefault: true,
        stopPropagation: true,
        action: 'SAVE_AND_EXIT',
        formAction: 'SAVE_CHANGES',
        navigationAction: 'RETURN_TO_PREVIOUS',
        confirmationRequired: true,
        confirmationMessage: 'Save changes and exit?',
      },
    },

    // Alt+F4 for application exit (Windows convention)
    ALT_F4_APP_EXIT: {
      key: 'F4',
      keyCode: 115,
      code: 'F4',
      which: 115,
      ctrlKey: false,
      altKey: true,
      shiftKey: false,
      metaKey: false,
      originalFunction: 'Exit Application',
      description: 'Exit the entire application',
      eventHandler: {
        type: 'keydown',
        preventDefault: true,
        stopPropagation: true,
        action: 'EXIT_APPLICATION',
        confirmationRequired: true,
        confirmationMessage: 'Are you sure you want to exit the application?',
      },
    },

    // Alt+Enter for full screen mode
    ALT_ENTER_FULLSCREEN: {
      key: 'Enter',
      keyCode: 13,
      code: 'Enter',
      which: 13,
      ctrlKey: false,
      altKey: true,
      shiftKey: false,
      metaKey: false,
      originalFunction: 'Toggle Fullscreen',
      description: 'Toggle fullscreen mode for immersive experience',
      eventHandler: {
        type: 'keydown',
        preventDefault: true,
        stopPropagation: true,
        action: 'TOGGLE_FULLSCREEN',
        uiAction: 'FULLSCREEN_TOGGLE',
      },
    },
  },
} as const;

// ==========================================
// Keyboard Accessibility Configuration - WCAG 2.1 AA Compliance
// ==========================================

/**
 * Comprehensive keyboard accessibility configuration ensuring WCAG 2.1 AA compliance
 * for keyboard-only navigation. Provides focus management, screen reader support,
 * and navigation assistance for users with disabilities.
 * 
 * WCAG 2.1 AA Requirements:
 * - All interactive elements must be keyboard accessible
 * - Focus indicators must be visible and clear
 * - Navigation must be logical and predictable
 * - Screen reader announcements must be meaningful
 * - Bypass mechanisms must be available for repetitive content
 */
export const KEYBOARD_ACCESSIBILITY_CONFIG = {
  // Focus Management Configuration
  FOCUS_MANAGEMENT: {
    // Focus trap configuration for modal dialogs
    FOCUS_TRAP: {
      enabled: true,
      firstFocusableElement: 'input, select, textarea, button, [tabindex="0"]',
      lastFocusableElement: 'input, select, textarea, button, [tabindex="0"]',
      focusableElementsSelector: 'input, select, textarea, button, [href], [tabindex]:not([tabindex="-1"])',
      skipLinks: true,
      skipLinksSelector: '.skip-link',
      skipLinksTarget: '#main-content',
    },

    // Tab navigation configuration
    TAB_NAVIGATION: {
      // Logical tab order matching original BMS field sequence
      tabOrder: [
        'header-navigation',
        'main-content',
        'form-fields',
        'action-buttons',
        'footer-navigation',
      ],
      
      // Tab stops for each screen type
      SCREEN_TAB_STOPS: {
        LOGIN_SCREEN: ['userid-input', 'password-input', 'login-button'],
        MENU_SCREEN: ['menu-options', 'option-input', 'continue-button', 'exit-button'],
        ACCOUNT_VIEW: ['account-search', 'account-fields', 'navigation-buttons'],
        CARD_LIST: ['search-filters', 'card-list', 'pagination-controls'],
        TRANSACTION_LIST: ['search-criteria', 'transaction-list', 'pagination-controls'],
        FORM_SCREEN: ['form-fields', 'submit-button', 'clear-button', 'exit-button'],
      },

      // Focus indicators
      FOCUS_INDICATORS: {
        outlineStyle: '2px solid #0066cc',
        outlineOffset: '2px',
        backgroundColor: 'rgba(0, 102, 204, 0.1)',
        borderRadius: '4px',
        boxShadow: '0 0 0 2px rgba(0, 102, 204, 0.3)',
        transitionDuration: '0.2s',
      },
    },

    // Skip link configuration
    SKIP_LINKS: {
      enabled: true,
      skipToMainContent: {
        text: 'Skip to main content',
        target: '#main-content',
        keyboardShortcut: 'Alt+M',
      },
      skipToNavigation: {
        text: 'Skip to navigation',
        target: '#main-navigation',
        keyboardShortcut: 'Alt+N',
      },
      skipToFooter: {
        text: 'Skip to footer',
        target: '#footer',
        keyboardShortcut: 'Alt+F',
      },
    },
  },

  // Screen Reader Support Configuration
  SCREEN_READER_SUPPORT: {
    // ARIA live regions for dynamic content announcements
    ARIA_LIVE_REGIONS: {
      STATUS_REGION: {
        id: 'status-region',
        ariaLive: 'polite',
        ariaAtomic: 'true',
        ariaRelevant: 'additions text',
        role: 'status',
        className: 'sr-only',
      },
      ALERT_REGION: {
        id: 'alert-region',
        ariaLive: 'assertive',
        ariaAtomic: 'true',
        ariaRelevant: 'additions text',
        role: 'alert',
        className: 'sr-only',
      },
      PROGRESS_REGION: {
        id: 'progress-region',
        ariaLive: 'polite',
        ariaAtomic: 'false',
        ariaRelevant: 'additions text',
        role: 'progressbar',
        className: 'sr-only',
      },
    },

    // Screen reader announcements for function key actions
    FUNCTION_KEY_ANNOUNCEMENTS: {
      F3_PRESSED: 'Navigating back to previous screen',
      F4_PRESSED: 'Clearing all form fields',
      F7_PRESSED: 'Loading previous page',
      F8_PRESSED: 'Loading next page',
      F12_PRESSED: 'Canceling operation and returning to main menu',
      ENTER_PRESSED: 'Submitting form',
      ESCAPE_PRESSED: 'Canceling current action',
    },

    // Form field descriptions and labels
    FORM_ACCESSIBILITY: {
      REQUIRED_FIELD_INDICATOR: ' (required)',
      OPTIONAL_FIELD_INDICATOR: ' (optional)',
      ERROR_MESSAGE_PREFIX: 'Error: ',
      SUCCESS_MESSAGE_PREFIX: 'Success: ',
      FIELD_FORMAT_DESCRIPTIONS: {
        DATE_FIELD: 'Enter date in MM/DD/YYYY format',
        PHONE_FIELD: 'Enter phone number in (XXX) XXX-XXXX format',
        SSN_FIELD: 'Enter social security number in XXX-XX-XXXX format',
        ACCOUNT_NUMBER: 'Enter 11-digit account number',
        CARD_NUMBER: 'Enter 16-digit card number',
      },
    },

    // Page landmark and structure
    PAGE_STRUCTURE: {
      MAIN_LANDMARK: 'main',
      NAVIGATION_LANDMARK: 'navigation',
      BANNER_LANDMARK: 'banner',
      CONTENTINFO_LANDMARK: 'contentinfo',
      COMPLEMENTARY_LANDMARK: 'complementary',
      SEARCH_LANDMARK: 'search',
      FORM_LANDMARK: 'form',
    },
  },

  // Keyboard-Only Navigation Configuration
  KEYBOARD_ONLY_NAVIGATION: {
    // Navigation shortcuts for keyboard users
    NAVIGATION_SHORTCUTS: {
      // Global navigation shortcuts
      GLOBAL_SHORTCUTS: {
        'Alt+1': { action: 'NAVIGATE_TO_MAIN_MENU', description: 'Go to main menu' },
        'Alt+2': { action: 'NAVIGATE_TO_ACCOUNT_VIEW', description: 'Go to account view' },
        'Alt+3': { action: 'NAVIGATE_TO_CARD_LIST', description: 'Go to card list' },
        'Alt+4': { action: 'NAVIGATE_TO_TRANSACTIONS', description: 'Go to transactions' },
        'Alt+5': { action: 'NAVIGATE_TO_BILL_PAYMENT', description: 'Go to bill payment' },
        'Alt+6': { action: 'NAVIGATE_TO_REPORTS', description: 'Go to reports' },
        'Alt+L': { action: 'FOCUS_SEARCH_FIELD', description: 'Focus search field' },
        'Alt+S': { action: 'SUBMIT_FORM', description: 'Submit current form' },
        'Alt+R': { action: 'RESET_FORM', description: 'Reset current form' },
      },

      // Form navigation shortcuts
      FORM_SHORTCUTS: {
        'Ctrl+Home': { action: 'FOCUS_FIRST_FIELD', description: 'Focus first form field' },
        'Ctrl+End': { action: 'FOCUS_LAST_FIELD', description: 'Focus last form field' },
        'Ctrl+PageUp': { action: 'FOCUS_PREVIOUS_SECTION', description: 'Focus previous section' },
        'Ctrl+PageDown': { action: 'FOCUS_NEXT_SECTION', description: 'Focus next section' },
      },

      // List navigation shortcuts
      LIST_SHORTCUTS: {
        'Home': { action: 'FOCUS_FIRST_ITEM', description: 'Focus first list item' },
        'End': { action: 'FOCUS_LAST_ITEM', description: 'Focus last list item' },
        'PageUp': { action: 'FOCUS_PREVIOUS_PAGE', description: 'Focus previous page' },
        'PageDown': { action: 'FOCUS_NEXT_PAGE', description: 'Focus next page' },
        'ArrowUp': { action: 'FOCUS_PREVIOUS_ITEM', description: 'Focus previous item' },
        'ArrowDown': { action: 'FOCUS_NEXT_ITEM', description: 'Focus next item' },
        'Space': { action: 'SELECT_ITEM', description: 'Select current item' },
      },
    },

    // Keyboard help dialog configuration
    KEYBOARD_HELP: {
      enabled: true,
      helpKey: 'F1',
      helpKeyDescription: 'Press F1 for keyboard help',
      helpDialogTitle: 'Keyboard Navigation Help',
      helpContent: {
        GENERAL_NAVIGATION: 'General Navigation',
        FUNCTION_KEYS: 'Function Keys',
        FORM_NAVIGATION: 'Form Navigation',
        LIST_NAVIGATION: 'List Navigation',
        ACCESSIBILITY_FEATURES: 'Accessibility Features',
      },
    },

    // Error handling and feedback
    ERROR_FEEDBACK: {
      INVALID_KEY_COMBINATION: 'Invalid key combination. Press F1 for help.',
      FOCUS_LOST: 'Focus lost. Press Tab to continue navigation.',
      SCREEN_READER_REQUIRED: 'Screen reader required for optimal experience.',
      KEYBOARD_TRAP_ACTIVE: 'Keyboard trap active. Press Escape to exit.',
    },
  },
} as const;

// ==========================================
// Touch Device Equivalents - Mobile and Tablet Support
// ==========================================

/**
 * Touch device equivalents for function key operations enabling mobile and tablet
 * compatibility. Provides gesture-based alternatives and touch-friendly interface
 * elements that replicate original function key behaviors.
 * 
 * Touch Interaction Patterns:
 * - Swipe gestures for navigation (left/right for pagination, up/down for scrolling)
 * - Touch buttons for function key equivalents
 * - Long press for advanced options
 * - Pinch to zoom for accessibility
 * - Voice commands for hands-free operation
 */
export const TOUCH_DEVICE_EQUIVALENTS = {
  // Swipe Gesture Configuration
  SWIPE_GESTURES: {
    // Horizontal swipe gestures for pagination
    HORIZONTAL_SWIPES: {
      SWIPE_LEFT: {
        gesture: 'swipe-left',
        direction: 'left',
        minimumDistance: 50,
        maximumTime: 500,
        functionKeyEquivalent: 'F8',
        action: 'NEXT_PAGE',
        description: 'Swipe left to go to next page',
        feedback: {
          haptic: 'light',
          visual: 'slide-left-animation',
          audio: 'page-turn-sound',
        },
      },

      SWIPE_RIGHT: {
        gesture: 'swipe-right',
        direction: 'right',
        minimumDistance: 50,
        maximumTime: 500,
        functionKeyEquivalent: 'F7',
        action: 'PREVIOUS_PAGE',
        description: 'Swipe right to go to previous page',
        feedback: {
          haptic: 'light',
          visual: 'slide-right-animation',
          audio: 'page-turn-sound',
        },
      },
    },

    // Vertical swipe gestures for navigation
    VERTICAL_SWIPES: {
      SWIPE_UP: {
        gesture: 'swipe-up',
        direction: 'up',
        minimumDistance: 50,
        maximumTime: 500,
        functionKeyEquivalent: 'F3',
        action: 'EXIT_SCREEN',
        description: 'Swipe up to exit current screen',
        feedback: {
          haptic: 'medium',
          visual: 'slide-up-animation',
          audio: 'exit-sound',
        },
      },

      SWIPE_DOWN: {
        gesture: 'swipe-down',
        direction: 'down',
        minimumDistance: 50,
        maximumTime: 500,
        functionKeyEquivalent: 'F4',
        action: 'CLEAR_FORM',
        description: 'Swipe down to clear form',
        feedback: {
          haptic: 'medium',
          visual: 'clear-animation',
          audio: 'clear-sound',
        },
      },
    },

    // Multi-touch gestures
    MULTI_TOUCH_GESTURES: {
      TWO_FINGER_SWIPE_UP: {
        gesture: 'two-finger-swipe-up',
        fingers: 2,
        direction: 'up',
        minimumDistance: 30,
        maximumTime: 300,
        functionKeyEquivalent: 'F12',
        action: 'CANCEL_OPERATION',
        description: 'Two-finger swipe up to cancel operation',
        feedback: {
          haptic: 'heavy',
          visual: 'cancel-animation',
          audio: 'cancel-sound',
        },
      },

      PINCH_TO_ZOOM: {
        gesture: 'pinch',
        minimumScale: 0.5,
        maximumScale: 3.0,
        action: 'ZOOM_CONTENT',
        description: 'Pinch to zoom in/out for better visibility',
        accessibility: true,
        feedback: {
          haptic: 'light',
          visual: 'zoom-indicator',
        },
      },
    },
  },

  // Touch Button Configuration
  TOUCH_BUTTONS: {
    // Function key touch buttons for tablets
    FUNCTION_KEY_BUTTONS: {
      F3_BUTTON: {
        id: 'touch-f3-button',
        position: 'bottom-left',
        size: 'medium',
        icon: 'arrow-back',
        label: 'Back',
        functionKeyEquivalent: 'F3',
        action: 'EXIT_SCREEN',
        styling: {
          backgroundColor: '#007acc',
          color: '#ffffff',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '16px',
          fontWeight: 'bold',
        },
        accessibility: {
          ariaLabel: 'Back button',
          ariaDescription: 'Tap to go back to previous screen',
          minimumTouchTarget: '44px',
        },
      },

      F4_BUTTON: {
        id: 'touch-f4-button',
        position: 'bottom-center',
        size: 'medium',
        icon: 'clear',
        label: 'Clear',
        functionKeyEquivalent: 'F4',
        action: 'CLEAR_FORM',
        styling: {
          backgroundColor: '#ff6b35',
          color: '#ffffff',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '16px',
          fontWeight: 'bold',
        },
        accessibility: {
          ariaLabel: 'Clear button',
          ariaDescription: 'Tap to clear all form fields',
          minimumTouchTarget: '44px',
        },
      },

      F7_BUTTON: {
        id: 'touch-f7-button',
        position: 'bottom-left-center',
        size: 'small',
        icon: 'arrow-left',
        label: 'Prev',
        functionKeyEquivalent: 'F7',
        action: 'PREVIOUS_PAGE',
        styling: {
          backgroundColor: '#28a745',
          color: '#ffffff',
          borderRadius: '6px',
          padding: '8px 12px',
          fontSize: '14px',
          fontWeight: 'normal',
        },
        accessibility: {
          ariaLabel: 'Previous page button',
          ariaDescription: 'Tap to go to previous page',
          minimumTouchTarget: '44px',
        },
      },

      F8_BUTTON: {
        id: 'touch-f8-button',
        position: 'bottom-right-center',
        size: 'small',
        icon: 'arrow-right',
        label: 'Next',
        functionKeyEquivalent: 'F8',
        action: 'NEXT_PAGE',
        styling: {
          backgroundColor: '#28a745',
          color: '#ffffff',
          borderRadius: '6px',
          padding: '8px 12px',
          fontSize: '14px',
          fontWeight: 'normal',
        },
        accessibility: {
          ariaLabel: 'Next page button',
          ariaDescription: 'Tap to go to next page',
          minimumTouchTarget: '44px',
        },
      },

      F12_BUTTON: {
        id: 'touch-f12-button',
        position: 'bottom-right',
        size: 'medium',
        icon: 'cancel',
        label: 'Cancel',
        functionKeyEquivalent: 'F12',
        action: 'CANCEL_OPERATION',
        styling: {
          backgroundColor: '#dc3545',
          color: '#ffffff',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '16px',
          fontWeight: 'bold',
        },
        accessibility: {
          ariaLabel: 'Cancel button',
          ariaDescription: 'Tap to cancel operation',
          minimumTouchTarget: '44px',
        },
      },

      ENTER_BUTTON: {
        id: 'touch-enter-button',
        position: 'bottom-center-right',
        size: 'large',
        icon: 'check',
        label: 'Continue',
        functionKeyEquivalent: 'ENTER',
        action: 'SUBMIT_FORM',
        styling: {
          backgroundColor: '#17a2b8',
          color: '#ffffff',
          borderRadius: '8px',
          padding: '16px 24px',
          fontSize: '18px',
          fontWeight: 'bold',
        },
        accessibility: {
          ariaLabel: 'Continue button',
          ariaDescription: 'Tap to submit form and continue',
          minimumTouchTarget: '44px',
        },
      },
    },

    // Button layout configurations for different screen sizes
    BUTTON_LAYOUTS: {
      MOBILE_PORTRAIT: {
        layout: 'bottom-toolbar',
        buttons: ['F3_BUTTON', 'F4_BUTTON', 'ENTER_BUTTON'],
        spacing: '8px',
        marginBottom: '16px',
      },

      MOBILE_LANDSCAPE: {
        layout: 'side-toolbar',
        buttons: ['F3_BUTTON', 'F7_BUTTON', 'F8_BUTTON', 'F12_BUTTON'],
        spacing: '6px',
        marginRight: '12px',
      },

      TABLET_PORTRAIT: {
        layout: 'floating-toolbar',
        buttons: ['F3_BUTTON', 'F4_BUTTON', 'F7_BUTTON', 'F8_BUTTON', 'F12_BUTTON', 'ENTER_BUTTON'],
        spacing: '12px',
        position: 'bottom-right',
      },

      TABLET_LANDSCAPE: {
        layout: 'integrated-toolbar',
        buttons: ['F3_BUTTON', 'F4_BUTTON', 'F7_BUTTON', 'F8_BUTTON', 'F12_BUTTON', 'ENTER_BUTTON'],
        spacing: '16px',
        position: 'bottom-center',
      },
    },
  },

  // Mobile Navigation Configuration
  MOBILE_NAVIGATION: {
    // Navigation drawer for mobile devices
    NAVIGATION_DRAWER: {
      enabled: true,
      position: 'left',
      swipeToOpen: true,
      swipeThreshold: 20,
      backdropTap: true,
      keyboardShortcut: 'Alt+M',
      accessibility: {
        ariaLabel: 'Navigation menu',
        ariaDescription: 'Swipe from left edge or tap menu button to open',
        focusTrap: true,
      },
    },

    // Bottom navigation bar
    BOTTOM_NAVIGATION: {
      enabled: true,
      height: '56px',
      backgroundColor: '#ffffff',
      borderTop: '1px solid #e0e0e0',
      items: [
        {
          id: 'nav-home',
          label: 'Home',
          icon: 'home',
          route: '/menu/main',
          badge: false,
        },
        {
          id: 'nav-accounts',
          label: 'Accounts',
          icon: 'account-balance',
          route: '/accounts/view',
          badge: false,
        },
        {
          id: 'nav-cards',
          label: 'Cards',
          icon: 'credit-card',
          route: '/cards/list',
          badge: false,
        },
        {
          id: 'nav-transactions',
          label: 'Transactions',
          icon: 'receipt',
          route: '/transactions/list',
          badge: true,
        },
        {
          id: 'nav-more',
          label: 'More',
          icon: 'more-horiz',
          route: '/menu/main',
          badge: false,
        },
      ],
      accessibility: {
        ariaLabel: 'Bottom navigation',
        ariaDescription: 'Navigate between main sections',
        tabIndex: 0,
      },
    },

    // Pull-to-refresh functionality
    PULL_TO_REFRESH: {
      enabled: true,
      pullDistance: 80,
      snapBackDistance: 50,
      functionKeyEquivalent: 'Ctrl+R',
      action: 'REFRESH_SCREEN',
      animation: 'spin',
      feedback: {
        haptic: 'medium',
        visual: 'refresh-spinner',
        audio: 'refresh-sound',
      },
      accessibility: {
        ariaLabel: 'Pull to refresh',
        ariaDescription: 'Pull down to refresh screen data',
        screenReaderAnnouncement: 'Refreshing screen data',
      },
    },

    // Voice commands for hands-free operation
    VOICE_COMMANDS: {
      enabled: true,
      language: 'en-US',
      commands: {
        'go back': { action: 'EXIT_SCREEN', functionKeyEquivalent: 'F3' },
        'clear form': { action: 'CLEAR_FORM', functionKeyEquivalent: 'F4' },
        'next page': { action: 'NEXT_PAGE', functionKeyEquivalent: 'F8' },
        'previous page': { action: 'PREVIOUS_PAGE', functionKeyEquivalent: 'F7' },
        'cancel': { action: 'CANCEL_OPERATION', functionKeyEquivalent: 'F12' },
        'submit': { action: 'SUBMIT_FORM', functionKeyEquivalent: 'ENTER' },
        'main menu': { action: 'NAVIGATE_TO_MAIN_MENU' },
        'help': { action: 'SHOW_HELP' },
      },
      accessibility: {
        ariaLabel: 'Voice commands',
        ariaDescription: 'Say voice commands to navigate',
        visualIndicator: true,
        microphoneIcon: true,
      },
    },
  },
} as const;

// ==========================================
// Utility Functions for Keyboard Event Handling
// ==========================================

/**
 * Utility functions for keyboard event handling and React hook integration.
 * Provides helper functions for key detection, event processing, and
 * accessibility support in React components.
 */
export const KeyboardUtils = {
  /**
   * Check if a keyboard event matches a function key definition
   */
  matchesFunctionKey(event: KeyboardEvent, functionKey: typeof FUNCTION_KEYS[keyof typeof FUNCTION_KEYS]): boolean {
    return (
      event.key === functionKey.key &&
      event.keyCode === functionKey.keyCode &&
      event.ctrlKey === functionKey.ctrlKey &&
      event.altKey === functionKey.altKey &&
      event.shiftKey === functionKey.shiftKey &&
      event.metaKey === functionKey.metaKey
    );
  },

  /**
   * Get function key definition from keyboard event
   */
  getFunctionKeyFromEvent(event: KeyboardEvent): typeof FUNCTION_KEYS[keyof typeof FUNCTION_KEYS] | null {
    for (const [, functionKey] of Object.entries(FUNCTION_KEYS)) {
      if (this.matchesFunctionKey(event, functionKey)) {
        return functionKey;
      }
    }
    return null;
  },

  /**
   * Process keyboard event and execute appropriate action
   */
  processKeyboardEvent(event: KeyboardEvent, currentPath: string): void {
    const functionKey = this.getFunctionKeyFromEvent(event);
    
    if (functionKey) {
      if (functionKey.eventHandler.preventDefault) {
        event.preventDefault();
      }
      
      if (functionKey.eventHandler.stopPropagation) {
        event.stopPropagation();
      }
      
      // Execute the appropriate action based on function key
      this.executeFunctionKeyAction(functionKey, currentPath);
    }
  },

  /**
   * Execute function key action
   */
  executeFunctionKeyAction(functionKey: typeof FUNCTION_KEYS[keyof typeof FUNCTION_KEYS], currentPath: string): void {
    const action = functionKey.eventHandler.action;
    
    switch (action) {
      case 'EXIT_SCREEN':
        const returnPath = functionKey.eventHandler.returnPathResolver?.(currentPath) || '/menu/main';
        // Navigate to return path
        window.history.pushState({}, '', returnPath);
        break;
        
      case 'CLEAR_FORM':
        // Clear all form fields
        document.querySelectorAll('input, textarea, select').forEach((element) => {
          if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
            element.value = '';
          } else if (element instanceof HTMLSelectElement) {
            element.selectedIndex = 0;
          }
        });
        break;
        
      case 'PREVIOUS_PAGE':
      case 'NEXT_PAGE':
        // Handle pagination
        const paginationEvent = new CustomEvent('pagination', {
          detail: { action: functionKey.eventHandler.paginationAction }
        });
        document.dispatchEvent(paginationEvent);
        break;
        
      case 'CANCEL_OPERATION':
        // Handle cancellation
        const cancelEvent = new CustomEvent('cancel', {
          detail: { returnPath: functionKey.eventHandler.returnPath }
        });
        document.dispatchEvent(cancelEvent);
        break;
        
      case 'SUBMIT_FORM':
        // Handle form submission
        const form = document.querySelector('form');
        if (form) {
          form.dispatchEvent(new Event('submit'));
        }
        break;
        
      case 'ESCAPE_OPERATION':
        // Handle escape
        const escapeEvent = new CustomEvent('escape');
        document.dispatchEvent(escapeEvent);
        break;
    }
  },

  /**
   * Announce action to screen readers
   */
  announceToScreenReader(message: string): void {
    const statusRegion = document.getElementById('status-region');
    if (statusRegion) {
      statusRegion.textContent = message;
      
      // Clear the message after announcement
      setTimeout(() => {
        statusRegion.textContent = '';
      }, 1000);
    }
  },

  /**
   * Check if device supports touch
   */
  isTouchDevice(): boolean {
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  },

  /**
   * Get appropriate button layout for current device
   */
  getButtonLayout(): string {
    const isMobile = window.innerWidth <= 768;
    const isPortrait = window.innerHeight > window.innerWidth;
    
    if (isMobile) {
      return isPortrait ? 'MOBILE_PORTRAIT' : 'MOBILE_LANDSCAPE';
    } else {
      return isPortrait ? 'TABLET_PORTRAIT' : 'TABLET_LANDSCAPE';
    }
  },
} as const;

// Export all keyboard constants and utilities
export default {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  KeyboardUtils,
};