/**
 * Keyboard Constants for CardDemo Application
 * 
 * This file provides comprehensive keyboard navigation and function key mappings
 * preserving original CICS PF key functionality. Maps mainframe function keys to
 * modern web application keyboard events while maintaining exact user experience.
 * 
 * Enables consistent keyboard navigation across all React components while
 * preserving the original COBOL/CICS keyboard interaction patterns.
 */

import { NAVIGATION_FLOW } from './NavigationConstants';

/**
 * Function Key Definitions
 * Maps CICS function keys to their modern web equivalents
 * preserving exact mainframe behavior patterns
 */
export const FUNCTION_KEYS = {
  /**
   * F3 Key - Exit/Back Navigation
   * CICS Pattern: F3=Exit, F3=Back
   * Behavior: Navigate to parent screen or exit current function
   */
  F3: {
    code: 'F3',
    keyCode: 114,
    key: 'F3',
    description: 'Exit/Back Navigation',
    action: 'EXIT',
    navigationAction: (currentPath: string) => {
      return NAVIGATION_FLOW.RETURN_PATHS[currentPath] || '/login';
    }
  },

  /**
   * F5 Key - Save/Refresh
   * CICS Pattern: F5=Save
   * Behavior: Save current form data or refresh screen
   * Note: Browser reserved - requires alternative key combination
   */
  F5: {
    code: 'F5',
    keyCode: 116,
    key: 'F5',
    description: 'Save/Refresh',
    action: 'SAVE',
    browserReserved: true,
    alternative: 'Ctrl+S'
  },

  /**
   * F7 Key - Page Up/Backward
   * CICS Pattern: F7=Backward
   * Behavior: Navigate to previous page in paginated lists
   */
  F7: {
    code: 'F7',
    keyCode: 118,
    key: 'F7',
    description: 'Page Up/Backward',
    action: 'PAGE_UP',
    paginationAction: 'PREVIOUS'
  },

  /**
   * F8 Key - Page Down/Forward
   * CICS Pattern: F8=Forward
   * Behavior: Navigate to next page in paginated lists
   */
  F8: {
    code: 'F8',
    keyCode: 119,
    key: 'F8',
    description: 'Page Down/Forward',
    action: 'PAGE_DOWN',
    paginationAction: 'NEXT'
  },

  /**
   * F12 Key - Cancel Operation
   * CICS Pattern: F12=Cancel
   * Behavior: Cancel current operation and return to previous state
   * Note: Browser reserved - requires alternative key combination
   */
  F12: {
    code: 'F12',
    keyCode: 123,
    key: 'F12',
    description: 'Cancel Operation',
    action: 'CANCEL',
    browserReserved: true,
    alternative: 'Ctrl+Escape'
  },

  /**
   * ENTER Key - Continue/Process
   * CICS Pattern: ENTER=Continue, ENTER=Process
   * Behavior: Submit form or continue to next screen
   */
  ENTER: {
    code: 'Enter',
    keyCode: 13,
    key: 'Enter',
    description: 'Continue/Process',
    action: 'SUBMIT',
    formAction: 'SUBMIT'
  },

  /**
   * ESCAPE Key - Quick Exit
   * CICS Pattern: ESC=Quick Exit
   * Behavior: Quick exit from current screen
   */
  ESCAPE: {
    code: 'Escape',
    keyCode: 27,
    key: 'Escape',
    description: 'Quick Exit',
    action: 'QUICK_EXIT'
  }
} as const;

/**
 * Alternative Key Combinations
 * Provides browser-compatible alternatives for reserved function keys
 * ensures consistent behavior across different browsers and platforms
 */
export const ALTERNATIVE_KEY_COMBINATIONS = {
  /**
   * Ctrl+R for F5 Refresh
   * Alternative for F5 when browser refresh is not desired
   */
  CTRL_R_FOR_F5: {
    keyCode: 82,
    ctrlKey: true,
    shiftKey: false,
    altKey: false,
    description: 'Refresh/Save Alternative',
    originalKey: 'F5',
    action: 'SAVE'
  },

  /**
   * Ctrl+S for F5 Save
   * Standard save shortcut as F5 alternative
   */
  CTRL_S_FOR_F5: {
    keyCode: 83,
    ctrlKey: true,
    shiftKey: false,
    altKey: false,
    description: 'Save Data',
    originalKey: 'F5',
    action: 'SAVE'
  },

  /**
   * Ctrl+Escape for F12 Cancel
   * Alternative for F12 when browser dev tools shortcut conflicts
   */
  CTRL_ESC_FOR_F12: {
    keyCode: 27,
    ctrlKey: true,
    shiftKey: false,
    altKey: false,
    description: 'Cancel Operation',
    originalKey: 'F12',
    action: 'CANCEL'
  },

  /**
   * Alt+Left for F3 Back
   * Browser-standard back navigation
   */
  ALT_LEFT_FOR_F3: {
    keyCode: 37,
    ctrlKey: false,
    shiftKey: false,
    altKey: true,
    description: 'Back Navigation',
    originalKey: 'F3',
    action: 'EXIT'
  },

  /**
   * Alt Key Combinations
   * Additional alternatives for improved accessibility
   */
  ALT_COMBINATIONS: {
    /**
     * Alt+E for Exit (F3)
     */
    ALT_E_EXIT: {
      keyCode: 69,
      ctrlKey: false,
      shiftKey: false,
      altKey: true,
      description: 'Exit Current Screen',
      originalKey: 'F3',
      action: 'EXIT'
    },

    /**
     * Alt+S for Save (F5)
     */
    ALT_S_SAVE: {
      keyCode: 83,
      ctrlKey: false,
      shiftKey: false,
      altKey: true,
      description: 'Save Current Data',
      originalKey: 'F5',
      action: 'SAVE'
    },

    /**
     * Alt+C for Cancel (F12)
     */
    ALT_C_CANCEL: {
      keyCode: 67,
      ctrlKey: false,
      shiftKey: false,
      altKey: true,
      description: 'Cancel Current Operation',
      originalKey: 'F12',
      action: 'CANCEL'
    }
  }
} as const;

/**
 * Keyboard Accessibility Configuration
 * Ensures WCAG 2.1 AA compliance for keyboard-only navigation
 * and screen reader compatibility
 */
export const KEYBOARD_ACCESSIBILITY_CONFIG = {
  /**
   * Focus Management Configuration
   * Manages keyboard focus across form elements and screens
   */
  FOCUS_MANAGEMENT: {
    /**
     * Focus trap configuration for modal dialogs
     */
    FOCUS_TRAP: {
      enabled: true,
      returnFocusOnClose: true,
      initialFocus: '[data-autofocus]',
      fallbackFocus: 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    },

    /**
     * Tab order configuration
     */
    TAB_ORDER: {
      skipLinks: true,
      skipToContent: 'main',
      skipToNavigation: 'nav',
      customTabIndex: true
    },

    /**
     * Focus indicators
     */
    FOCUS_INDICATORS: {
      visible: true,
      highContrast: true,
      customStyle: {
        outline: '2px solid #0066cc',
        outlineOffset: '2px',
        borderRadius: '4px'
      }
    }
  },

  /**
   * Screen Reader Support
   * Provides ARIA labels and announcements for function keys
   */
  SCREEN_READER_SUPPORT: {
    /**
     * ARIA labels for function keys
     */
    ARIA_LABELS: {
      F3: 'Exit current screen and return to previous screen',
      F5: 'Save current form data',
      F7: 'Navigate to previous page',
      F8: 'Navigate to next page',
      F12: 'Cancel current operation',
      ENTER: 'Submit form or continue to next screen',
      ESCAPE: 'Quick exit from current screen'
    },

    /**
     * Live region announcements
     */
    LIVE_REGIONS: {
      navigation: 'Navigation change: ',
      action: 'Action performed: ',
      error: 'Error: ',
      success: 'Success: '
    },

    /**
     * Keyboard shortcut announcements
     */
    SHORTCUT_ANNOUNCEMENTS: {
      enabled: true,
      verbosity: 'medium', // 'low', 'medium', 'high'
      includeAlternatives: true
    }
  },

  /**
   * Keyboard-Only Navigation
   * Ensures all functionality is accessible via keyboard
   */
  KEYBOARD_ONLY_NAVIGATION: {
    /**
     * Skip navigation links
     */
    SKIP_LINKS: {
      enabled: true,
      skipToMain: 'Skip to main content',
      skipToNav: 'Skip to navigation',
      skipToSearch: 'Skip to search'
    },

    /**
     * Keyboard navigation patterns
     */
    NAVIGATION_PATTERNS: {
      arrowKeys: true,
      pageUpDown: true,
      homeEnd: true,
      spaceBar: true,
      functionKeys: true
    },

    /**
     * Keyboard event handling
     */
    EVENT_HANDLING: {
      preventDefault: true,
      stopPropagation: false,
      debounceDelay: 100
    }
  }
} as const;

/**
 * Touch Device Equivalents
 * Maps keyboard shortcuts to touch gestures for mobile and tablet compatibility
 */
export const TOUCH_DEVICE_EQUIVALENTS = {
  /**
   * Swipe Gestures
   * Maps function key actions to touch gestures
   */
  SWIPE_GESTURES: {
    /**
     * Swipe right for F3 Exit/Back
     */
    SWIPE_RIGHT_EXIT: {
      originalKey: 'F3',
      action: 'EXIT',
      gesture: 'swipe-right',
      threshold: 50,
      velocity: 0.3,
      description: 'Swipe right to go back'
    },

    /**
     * Swipe left for forward navigation
     */
    SWIPE_LEFT_FORWARD: {
      originalKey: 'F8',
      action: 'PAGE_DOWN',
      gesture: 'swipe-left',
      threshold: 50,
      velocity: 0.3,
      description: 'Swipe left to go forward'
    },

    /**
     * Swipe up for previous page
     */
    SWIPE_UP_PREVIOUS: {
      originalKey: 'F7',
      action: 'PAGE_UP',
      gesture: 'swipe-up',
      threshold: 50,
      velocity: 0.3,
      description: 'Swipe up for previous page'
    },

    /**
     * Swipe down for next page
     */
    SWIPE_DOWN_NEXT: {
      originalKey: 'F8',
      action: 'PAGE_DOWN',
      gesture: 'swipe-down',
      threshold: 50,
      velocity: 0.3,
      description: 'Swipe down for next page'
    }
  },

  /**
   * Touch Buttons
   * Virtual buttons for function key actions
   */
  TOUCH_BUTTONS: {
    /**
     * Exit/Back button
     */
    EXIT_BUTTON: {
      originalKey: 'F3',
      action: 'EXIT',
      position: 'top-left',
      icon: 'arrow-left',
      label: 'Back',
      ariaLabel: 'Go back to previous screen'
    },

    /**
     * Save button
     */
    SAVE_BUTTON: {
      originalKey: 'F5',
      action: 'SAVE',
      position: 'top-right',
      icon: 'save',
      label: 'Save',
      ariaLabel: 'Save current data'
    },

    /**
     * Cancel button
     */
    CANCEL_BUTTON: {
      originalKey: 'F12',
      action: 'CANCEL',
      position: 'bottom-left',
      icon: 'times',
      label: 'Cancel',
      ariaLabel: 'Cancel current operation'
    },

    /**
     * Continue/Submit button
     */
    CONTINUE_BUTTON: {
      originalKey: 'ENTER',
      action: 'SUBMIT',
      position: 'bottom-right',
      icon: 'check',
      label: 'Continue',
      ariaLabel: 'Continue to next screen'
    }
  },

  /**
   * Mobile Navigation
   * Touch-specific navigation patterns
   */
  MOBILE_NAVIGATION: {
    /**
     * Pull-to-refresh for F5 refresh
     */
    PULL_TO_REFRESH: {
      originalKey: 'F5',
      action: 'SAVE',
      enabled: true,
      threshold: 80,
      description: 'Pull down to refresh'
    },

    /**
     * Double-tap for quick actions
     */
    DOUBLE_TAP_ACTIONS: {
      /**
       * Double-tap to exit
       */
      DOUBLE_TAP_EXIT: {
        originalKey: 'F3',
        action: 'EXIT',
        enabled: true,
        delay: 300,
        description: 'Double-tap to exit'
      },

      /**
       * Double-tap to save
       */
      DOUBLE_TAP_SAVE: {
        originalKey: 'F5',
        action: 'SAVE',
        enabled: true,
        delay: 300,
        description: 'Double-tap to save'
      }
    },

    /**
     * Long-press actions
     */
    LONG_PRESS_ACTIONS: {
      /**
       * Long-press for cancel
       */
      LONG_PRESS_CANCEL: {
        originalKey: 'F12',
        action: 'CANCEL',
        enabled: true,
        duration: 800,
        description: 'Long-press to cancel'
      }
    }
  }
} as const;

/**
 * Keyboard Event Handler Types
 * Type definitions for keyboard event handling
 */
export interface KeyboardEventHandler {
  keyCode: number;
  key: string;
  ctrlKey?: boolean;
  shiftKey?: boolean;
  altKey?: boolean;
  handler: (event: KeyboardEvent) => void;
  preventDefault?: boolean;
  stopPropagation?: boolean;
}

/**
 * Keyboard Shortcut Configuration
 * Configuration for implementing keyboard shortcuts in React components
 */
export interface KeyboardShortcutConfig {
  key: string;
  action: string;
  description: string;
  handler: () => void;
  enabled?: boolean;
  global?: boolean;
  preventDefault?: boolean;
}

/**
 * Utility Functions for Keyboard Handling
 * Helper functions for implementing keyboard navigation patterns
 */
export const KeyboardUtils = {
  /**
   * Checks if a key combination matches the expected pattern
   * @param event - Keyboard event
   * @param expectedKey - Expected key configuration
   * @returns Boolean indicating if keys match
   */
  isKeyMatch: (event: KeyboardEvent, expectedKey: any): boolean => {
    return (
      event.keyCode === expectedKey.keyCode &&
      !!event.ctrlKey === !!expectedKey.ctrlKey &&
      !!event.shiftKey === !!expectedKey.shiftKey &&
      !!event.altKey === !!expectedKey.altKey
    );
  },

  /**
   * Gets the action for a given key event
   * @param event - Keyboard event
   * @returns Action string or null if no match
   */
  getActionForEvent: (event: KeyboardEvent): string | null => {
    // Check function keys first
    for (const [keyName, keyConfig] of Object.entries(FUNCTION_KEYS)) {
      if (event.keyCode === keyConfig.keyCode) {
        return keyConfig.action;
      }
    }

    // Check alternative key combinations
    for (const [altName, altConfig] of Object.entries(ALTERNATIVE_KEY_COMBINATIONS)) {
      if (altName !== 'ALT_COMBINATIONS' && KeyboardUtils.isKeyMatch(event, altConfig)) {
        return altConfig.action;
      }
    }

    // Check Alt combinations
    for (const [altName, altConfig] of Object.entries(ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS)) {
      if (KeyboardUtils.isKeyMatch(event, altConfig)) {
        return altConfig.action;
      }
    }

    return null;
  },

  /**
   * Creates a keyboard event handler for React components
   * @param config - Keyboard shortcut configuration
   * @returns Event handler function
   */
  createKeyboardHandler: (config: KeyboardShortcutConfig): KeyboardEventHandler => {
    return {
      keyCode: FUNCTION_KEYS[config.key as keyof typeof FUNCTION_KEYS]?.keyCode || 0,
      key: config.key,
      handler: (event: KeyboardEvent) => {
        if (config.preventDefault !== false) {
          event.preventDefault();
        }
        if (config.enabled !== false) {
          config.handler();
        }
      },
      preventDefault: config.preventDefault !== false,
      stopPropagation: false
    };
  },

  /**
   * Formats keyboard shortcut for display
   * @param key - Function key name
   * @returns Formatted shortcut string
   */
  formatShortcut: (key: string): string => {
    const keyConfig = FUNCTION_KEYS[key as keyof typeof FUNCTION_KEYS];
    if (!keyConfig) return key;

    let shortcut = keyConfig.key;
    if (keyConfig.browserReserved && keyConfig.alternative) {
      shortcut += ` (${keyConfig.alternative})`;
    }

    return shortcut;
  }
};

/**
 * Export default consolidated keyboard constants
 */
export default {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  KeyboardUtils
};