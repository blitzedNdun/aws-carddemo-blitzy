/**
 * CardDemo - Keyboard Navigation Constants
 * 
 * TypeScript constants file defining comprehensive keyboard navigation and function key mappings
 * preserving original CICS PF key functionality. Provides event handler configurations,
 * alternative key combinations, and accessibility support for React components ensuring
 * consistent keyboard navigation across the modernized application.
 * 
 * This file centralizes all keyboard interaction patterns from the original BMS screens,
 * maintaining exact functional equivalence with CICS terminal behavior while enabling
 * modern web accessibility standards and cross-platform compatibility.
 * 
 * Key CICS Function Key Patterns Preserved:
 * - F3=Exit: Universal exit function returning to previous screen or main menu
 * - F7=Backward: Previous page navigation for paginated lists (COCRDLI, COTRN00)
 * - F8=Forward: Next page navigation for paginated lists (COCRDLI, COTRN00)
 * - F12=Cancel: Cancel current operation and return without saving changes
 * - F5=Save: Save changes in update screens (COACTUP, COCRDUP)
 * - ENTER=Continue/Process: Submit forms and continue operations
 * - ESC=Cancel: Alternative cancel operation for modern keyboard navigation
 * 
 * Browser Compatibility Considerations:
 * - F5 refresh conflicts handled with Ctrl+R alternative
 * - F12 developer tools conflicts handled with Ctrl+Esc alternative
 * - F11 fullscreen conflicts handled with Alt+F11 alternative
 * - Function key availability varies across browsers and operating systems
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

import { NAVIGATION_FLOW } from './NavigationConstants';

/**
 * Primary Function Key Definitions
 * 
 * Maps original CICS PF key functionality to JavaScript KeyboardEvent codes,
 * preserving exact behavioral patterns from BMS screen definitions while
 * providing modern event handling capabilities for React components.
 * 
 * Function Key Analysis from BMS Screens:
 * - COSGN00.bms: "ENTER=Sign-on  F3=Exit"
 * - COMEN01.bms: "ENTER=Continue  F3=Exit"
 * - COCRDLI.bms: "F3=Exit F7=Backward  F8=Forward"
 * - COACTUP.bms: "ENTER=Process F3=Exit F5=Save F12=Cancel"
 * - COTRN00.bms: "ENTER=Continue  F3=Back  F7=Backward  F8=Forward"
 * 
 * Each function key configuration includes:
 * - keyCode: JavaScript KeyboardEvent key identifier
 * - description: User-friendly description of function
 * - action: Standardized action identifier for event handlers
 * - contexts: Array of screen contexts where key is active
 * - preventDefault: Whether to prevent default browser behavior
 */
export const FUNCTION_KEYS = {
  // F3 - Universal Exit Function (Primary Navigation)
  F3: {
    keyCode: 'F3',
    altKeyCode: 'Escape',
    description: 'Exit current screen and return to previous level',
    action: 'EXIT',
    shortLabel: 'F3=Exit',
    longLabel: 'F3=Exit to Previous Screen',
    contexts: ['login', 'menu', 'account', 'card', 'transaction', 'user', 'admin', 'report'],
    preventDefault: true,
    requiresConfirmation: false,
    navigationPath: NAVIGATION_FLOW.RETURN_PATHS.DEFAULT_EXIT,
    handler: 'handleExit',
    accessibility: {
      ariaLabel: 'Exit current screen',
      screenReaderText: 'Press F3 or Escape to exit current screen and return to previous level'
    }
  },

  // F7 - Backward Navigation (Pagination)
  F7: {
    keyCode: 'F7',
    altKeyCode: 'PageUp',
    description: 'Navigate to previous page in list',
    action: 'PAGE_BACKWARD',
    shortLabel: 'F7=Backward',
    longLabel: 'F7=Previous Page',
    contexts: ['card_list', 'transaction_list', 'user_list', 'account_list'],
    preventDefault: true,
    requiresConfirmation: false,
    navigationPath: null,
    handler: 'handlePageBackward',
    accessibility: {
      ariaLabel: 'Go to previous page',
      screenReaderText: 'Press F7 or Page Up to navigate to the previous page in the list'
    }
  },

  // F8 - Forward Navigation (Pagination)
  F8: {
    keyCode: 'F8',
    altKeyCode: 'PageDown',
    description: 'Navigate to next page in list',
    action: 'PAGE_FORWARD',
    shortLabel: 'F8=Forward',
    longLabel: 'F8=Next Page',
    contexts: ['card_list', 'transaction_list', 'user_list', 'account_list'],
    preventDefault: true,
    requiresConfirmation: false,
    navigationPath: null,
    handler: 'handlePageForward',
    accessibility: {
      ariaLabel: 'Go to next page',
      screenReaderText: 'Press F8 or Page Down to navigate to the next page in the list'
    }
  },

  // F12 - Cancel Operation
  F12: {
    keyCode: 'F12',
    altKeyCode: 'Escape',
    description: 'Cancel current operation without saving changes',
    action: 'CANCEL',
    shortLabel: 'F12=Cancel',
    longLabel: 'F12=Cancel Operation',
    contexts: ['account_update', 'card_update', 'transaction_add', 'user_create', 'user_update'],
    preventDefault: true,
    requiresConfirmation: true,
    navigationPath: null,
    handler: 'handleCancel',
    accessibility: {
      ariaLabel: 'Cancel current operation',
      screenReaderText: 'Press F12 or Escape to cancel the current operation without saving changes'
    }
  },

  // ENTER - Continue/Process Operations
  ENTER: {
    keyCode: 'Enter',
    altKeyCode: null,
    description: 'Continue with current operation or submit form',
    action: 'CONTINUE',
    shortLabel: 'ENTER=Continue',
    longLabel: 'ENTER=Continue/Submit',
    contexts: ['login', 'menu', 'form_submit', 'search', 'navigation'],
    preventDefault: false,
    requiresConfirmation: false,
    navigationPath: null,
    handler: 'handleContinue',
    accessibility: {
      ariaLabel: 'Continue or submit',
      screenReaderText: 'Press Enter to continue with the current operation or submit the form'
    }
  },

  // ESCAPE - Modern Cancel Alternative
  ESCAPE: {
    keyCode: 'Escape', 
    altKeyCode: null,
    description: 'Cancel current operation or close modal',
    action: 'ESCAPE',
    shortLabel: 'ESC=Cancel',
    longLabel: 'ESC=Cancel/Close',
    contexts: ['modal', 'dropdown', 'popup', 'form_cancel'],
    preventDefault: true,
    requiresConfirmation: false,
    navigationPath: null,
    handler: 'handleEscape',
    accessibility: {
      ariaLabel: 'Cancel or close',
      screenReaderText: 'Press Escape to cancel the current operation or close the current dialog'
    }
  }
} as const;

/**
 * Alternative Key Combinations for Browser Compatibility
 * 
 * Provides alternative keyboard shortcuts for function keys that conflict with
 * browser reserved keys or are unavailable on certain devices. Ensures consistent
 * functionality across different browsers, operating systems, and device types.
 * 
 * Browser Conflict Resolution:
 * - F5 (Refresh): Replaced with Ctrl+R to avoid browser refresh conflicts
 * - F12 (DevTools): Replaced with Ctrl+Esc to avoid developer tools conflicts
 * - F11 (Fullscreen): Replaced with Alt+F11 to avoid fullscreen toggle conflicts
 * - Function keys: May not be available on compact keyboards or mobile devices
 * 
 * Each alternative includes:
 * - keys: Array of key combinations (modifier + key)
 * - description: User-friendly description
 * - equivalentTo: Original function key this replaces
 * - platforms: Specific platforms where this alternative is recommended
 */
export const ALTERNATIVE_KEY_COMBINATIONS = {
  // Ctrl+R Alternative for F5 (Save/Refresh Conflict Resolution)
  CTRL_R_FOR_F5: {
    keys: ['Control', 'KeyR'],
    keySequence: 'Ctrl+R',
    description: 'Alternative to F5 for save operations',
    equivalentTo: 'F5',
    action: 'SAVE',
    shortLabel: 'Ctrl+R=Save',
    longLabel: 'Ctrl+R=Save Changes',
    contexts: ['account_update', 'card_update', 'user_update', 'form_save'],
    preventDefault: true,
    platforms: ['windows', 'linux', 'mac'],
    handler: 'handleSave',
    accessibility: {
      ariaLabel: 'Save changes',
      screenReaderText: 'Press Control+R to save the current changes'
    }
  },

  // Ctrl+Esc Alternative for F12 (DevTools Conflict Resolution)
  CTRL_ESC_FOR_F12: {
    keys: ['Control', 'Escape'],
    keySequence: 'Ctrl+Esc',
    description: 'Alternative to F12 for cancel operations',
    equivalentTo: 'F12',
    action: 'CANCEL',
    shortLabel: 'Ctrl+Esc=Cancel',
    longLabel: 'Ctrl+Esc=Cancel Operation',
    contexts: ['form_cancel', 'operation_cancel', 'modal_close'],
    preventDefault: true,
    platforms: ['windows', 'linux', 'mac'],
    handler: 'handleCancel',
    accessibility: {
      ariaLabel: 'Cancel operation',
      screenReaderText: 'Press Control+Escape to cancel the current operation'
    }
  },

  // Alt+Key Combinations for Enhanced Navigation
  ALT_COMBINATIONS: {
    // Alt+B for Backward (F7 Alternative)
    ALT_B_BACKWARD: {
      keys: ['Alt', 'KeyB'],
      keySequence: 'Alt+B',
      description: 'Navigate to previous page',
      equivalentTo: 'F7',
      action: 'PAGE_BACKWARD',
      shortLabel: 'Alt+B=Backward',
      contexts: ['pagination', 'list_navigation'],
      preventDefault: true,
      handler: 'handlePageBackward'
    },

    // Alt+F for Forward (F8 Alternative)
    ALT_F_FORWARD: {
      keys: ['Alt', 'KeyF'],
      keySequence: 'Alt+F',
      description: 'Navigate to next page',
      equivalentTo: 'F8',
      action: 'PAGE_FORWARD',
      shortLabel: 'Alt+F=Forward',
      contexts: ['pagination', 'list_navigation'],
      preventDefault: true,
      handler: 'handlePageForward'
    },

    // Alt+X for Exit (F3 Alternative)
    ALT_X_EXIT: {
      keys: ['Alt', 'KeyX'],
      keySequence: 'Alt+X',
      description: 'Exit current screen',
      equivalentTo: 'F3',
      action: 'EXIT',
      shortLabel: 'Alt+X=Exit',
      contexts: ['navigation', 'screen_exit'],
      preventDefault: true,
      handler: 'handleExit'
    },

    // Alt+S for Save (F5 Alternative)
    ALT_S_SAVE: {
      keys: ['Alt', 'KeyS'],
      keySequence: 'Alt+S',
      description: 'Save current changes',
      equivalentTo: 'F5',
      action: 'SAVE',
      shortLabel: 'Alt+S=Save',
      contexts: ['form_save', 'data_update'],
      preventDefault: true,
      handler: 'handleSave'
    }
  }
} as const;

/**
 * Keyboard Accessibility Configuration
 * 
 * Provides comprehensive keyboard accessibility support ensuring WCAG 2.1 AA
 * compliance for keyboard-only navigation. Defines focus management patterns,
 * screen reader support, and keyboard navigation sequences that maintain
 * usability for assistive technology users.
 * 
 * WCAG 2.1 AA Requirements Addressed:
 * - 2.1.1 Keyboard: All functionality available from keyboard
 * - 2.1.2 No Keyboard Trap: Focus can move away from any component
 * - 2.4.3 Focus Order: Keyboard focus follows logical sequence
 * - 2.4.7 Focus Visible: Keyboard focus indicator is visible
 * - 4.1.3 Status Messages: Important changes announced to screen readers
 */
export const KEYBOARD_ACCESSIBILITY_CONFIG = {
  // Focus Management for Screen Navigation
  FOCUS_MANAGEMENT: {
    // Tab Order Configuration
    TAB_ORDER: {
      priority: ['error_messages', 'primary_actions', 'form_fields', 'navigation', 'secondary_actions'],
      skipLinks: ['#main-content', '#navigation', '#search'],
      trapFocus: ['modal', 'dropdown', 'popup'],
      restoreFocus: true,
      focusOnRouteChange: true
    },

    // Focus Indicators and Visual Feedback
    FOCUS_INDICATORS: {
      outlineWidth: '2px',
      outlineStyle: 'solid',
      outlineColor: '#005fcc',
      outlineOffset: '2px',
      backgroundColor: 'rgba(0, 95, 204, 0.1)',
      borderRadius: '4px',
      transition: 'all 0.2s ease-in-out'
    },

    // Focus Sequence Patterns
    FOCUS_SEQUENCES: {
      login: ['userid', 'password', 'submit', 'help'],
      menu: ['option1', 'option2', 'option3', 'option4', 'option5', 'option6', 'exit'],
      list: ['search', 'filter', 'sort', 'items', 'pagination', 'actions'],
      form: ['fields', 'validation', 'save', 'cancel', 'help'],
      modal: ['close', 'content', 'actions']
    },

    // Keyboard Shortcuts Help
    HELP_SHORTCUTS: {
      showHelp: ['F1', '?', 'Shift+?'],
      hideHelp: ['Escape', 'F1'],
      toggleShortcuts: ['Control+/', 'Alt+?']
    }
  },

  // Screen Reader Support and Announcements
  SCREEN_READER_SUPPORT: {
    // Live Region Announcements
    LIVE_REGIONS: {
      errors: 'assertive',
      status: 'polite',
      navigation: 'polite',
      progress: 'polite'
    },

    // ARIA Labels and Descriptions
    ARIA_LABELS: {
      functionKeys: {
        f3: 'Exit current screen, press F3 or Escape',
        f7: 'Previous page, press F7 or Page Up',
        f8: 'Next page, press F8 or Page Down',
        f12: 'Cancel operation, press F12 or Control+Escape',
        enter: 'Continue or submit, press Enter',
        escape: 'Cancel or close, press Escape'
      },
      navigation: {
        menu: 'Main navigation menu, use arrow keys to navigate',
        breadcrumb: 'Breadcrumb navigation, use Tab to navigate',
        pagination: 'Page navigation, use F7 and F8 or arrow keys'
      }
    },

    // Screen Reader Instructions
    INSTRUCTIONS: {
      formNavigation: 'Use Tab to move between fields, Enter to submit, Escape to cancel',
      listNavigation: 'Use arrow keys to navigate items, Enter to select, F7 and F8 for pages',
      menuNavigation: 'Use arrow keys or number keys to select options, Enter to confirm',
      modalNavigation: 'Use Tab to navigate within dialog, Escape to close'
    },

    // Status Announcements
    STATUS_MESSAGES: {
      pageChange: 'Page {page} of {total}',
      itemSelected: '{item} selected',
      operationComplete: '{operation} completed successfully',
      errorOccurred: 'Error: {message}',
      loading: 'Loading, please wait',
      saved: 'Changes saved successfully'
    }
  },

  // Keyboard-Only Navigation Patterns
  KEYBOARD_ONLY_NAVIGATION: {
    // Arrow Key Navigation
    ARROW_KEYS: {
      menus: true,
      lists: true,
      grids: true,
      tabs: true,
      breadcrumbs: false
    },

    // Enter Key Behavior
    ENTER_BEHAVIOR: {
      buttons: 'activate',
      links: 'follow',
      menuItems: 'select',
      listItems: 'select',
      forms: 'submit'
    },

    // Space Key Behavior
    SPACE_BEHAVIOR: {
      buttons: 'activate',
      checkboxes: 'toggle',
      radioButtons: 'select',
      scrolling: 'page_down'
    },

    // Escape Key Behavior
    ESCAPE_BEHAVIOR: {
      modals: 'close',
      dropdowns: 'close',
      forms: 'cancel',
      search: 'clear'
    }
  }
} as const;

/**
 * Touch Device Keyboard Equivalents
 * 
 * Provides touch-friendly alternatives to keyboard functions for mobile and
 * tablet devices where physical function keys are not available. Enables
 * consistent user experience across desktop, mobile, and tablet platforms
 * while preserving original CICS navigation patterns.
 * 
 * Touch Interaction Patterns:
 * - Swipe gestures replace F7/F8 pagination
 * - Touch buttons provide function key alternatives
 * - Long press actions for advanced functions
 * - Gesture combinations for power users
 */
export const TOUCH_DEVICE_EQUIVALENTS = {
  // Swipe Gestures for Navigation and Pagination
  SWIPE_GESTURES: {
    // Horizontal Swipes for Pagination (F7/F8 Replacement)
    SWIPE_LEFT: {
      action: 'PAGE_FORWARD',
      equivalentTo: 'F8',
      description: 'Swipe left to go to next page',
      contexts: ['card_list', 'transaction_list', 'user_list'],
      sensitivity: 'medium',
      minimumDistance: 50,
      handler: 'handlePageForward',
      feedback: 'haptic'
    },

    SWIPE_RIGHT: {
      action: 'PAGE_BACKWARD',
      equivalentTo: 'F7',
      description: 'Swipe right to go to previous page',
      contexts: ['card_list', 'transaction_list', 'user_list'],
      sensitivity: 'medium',
      minimumDistance: 50,
      handler: 'handlePageBackward',
      feedback: 'haptic'
    },

    // Vertical Swipes for Screen Navigation
    SWIPE_DOWN: {
      action: 'CLOSE_KEYBOARD',
      equivalentTo: 'Escape',
      description: 'Swipe down to close on-screen keyboard',
      contexts: ['form_input', 'search'],
      sensitivity: 'low',
      minimumDistance: 30,
      handler: 'handleCloseKeyboard'
    },

    // Edge Swipes for Exit Functions
    EDGE_SWIPE_LEFT: {
      action: 'EXIT',
      equivalentTo: 'F3',
      description: 'Swipe from left edge to exit current screen',
      contexts: ['navigation', 'screen_exit'],
      sensitivity: 'high',
      minimumDistance: 20,
      edgeWidth: 20,
      handler: 'handleExit'
    }
  },

  // Touch Buttons for Function Key Actions
  TOUCH_BUTTONS: {
    // Primary Action Buttons
    EXIT_BUTTON: {
      label: 'Exit',
      icon: 'arrow_back',
      equivalentTo: 'F3',
      action: 'EXIT',
      position: 'top_left',
      size: 'medium',
      style: 'secondary',
      contexts: ['all_screens'],
      handler: 'handleExit'
    },

    SAVE_BUTTON: {
      label: 'Save',
      icon: 'save',
      equivalentTo: 'F5',
      action: 'SAVE',
      position: 'top_right',
      size: 'medium',
      style: 'primary',
      contexts: ['form_edit', 'data_update'],
      handler: 'handleSave'
    },

    CANCEL_BUTTON: {
      label: 'Cancel',
      icon: 'close',
      equivalentTo: 'F12',
      action: 'CANCEL',
      position: 'bottom_left',
      size: 'medium',
      style: 'secondary',
      contexts: ['form_edit', 'modal'],
      handler: 'handleCancel'
    },

    // Pagination Control Buttons
    PREV_PAGE_BUTTON: {
      label: 'Previous',
      icon: 'chevron_left',
      equivalentTo: 'F7',
      action: 'PAGE_BACKWARD',
      position: 'bottom_left',
      size: 'small',
      style: 'ghost',
      contexts: ['pagination'],
      handler: 'handlePageBackward'
    },

    NEXT_PAGE_BUTTON: {
      label: 'Next',
      icon: 'chevron_right',
      equivalentTo: 'F8',
      action: 'PAGE_FORWARD',
      position: 'bottom_right',
      size: 'small',
      style: 'ghost',
      contexts: ['pagination'],
      handler: 'handlePageForward'
    }
  },

  // Mobile Navigation Patterns
  MOBILE_NAVIGATION: {
    // Bottom Navigation Bar
    BOTTOM_BAR: {
      height: '56px',
      backgroundColor: '#f5f5f5',
      borderTop: '1px solid #e0e0e0',
      items: [
        { icon: 'home', label: 'Menu', action: 'NAVIGATE_MENU' },
        { icon: 'arrow_back', label: 'Back', action: 'EXIT' },
        { icon: 'search', label: 'Search', action: 'FOCUS_SEARCH' },
        { icon: 'more_horiz', label: 'More', action: 'SHOW_OPTIONS' }
      ]
    },

    // Floating Action Button
    FAB: {
      size: '56px',
      position: 'bottom_right',
      margin: '16px',
      icon: 'add',
      action: 'PRIMARY_ACTION',
      contexts: ['list_screens'],
      elevation: 6
    },

    // Pull-to-Refresh
    PULL_TO_REFRESH: {
      threshold: 100,
      resistance: 2.5,
      refreshText: 'Pull to refresh',
      releaseText: 'Release to refresh',
      loadingText: 'Refreshing...',
      action: 'REFRESH_DATA',
      contexts: ['list_screens']
    },

    // Long Press Actions
    LONG_PRESS: {
      duration: 500,
      actions: {
        LIST_ITEM: 'SHOW_CONTEXT_MENU',
        BUTTON: 'SHOW_TOOLTIP',
        IMAGE: 'SHOW_PREVIEW'
      }
    }
  }
} as const;

// Type definitions for keyboard constants
export type FunctionKeyName = keyof typeof FUNCTION_KEYS;
export type AlternativeKeyName = keyof typeof ALTERNATIVE_KEY_COMBINATIONS;
export type TouchGestureName = keyof typeof TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES;
export type TouchButtonName = keyof typeof TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS;

// Utility functions for keyboard event handling
export const getFunctionKeyByCode = (keyCode: string): typeof FUNCTION_KEYS[FunctionKeyName] | null => {
  return Object.values(FUNCTION_KEYS).find(fk => fk.keyCode === keyCode || fk.altKeyCode === keyCode) || null;
};

export const getAlternativeByKeySequence = (keys: string[]): any => {
  return Object.values(ALTERNATIVE_KEY_COMBINATIONS).find(alt => 
    Array.isArray(alt.keys) && alt.keys.every(key => keys.includes(key))
  ) || null;
};

export const isValidContext = (functionKey: string, context: string): boolean => {
  const fk = FUNCTION_KEYS[functionKey as FunctionKeyName];
  return fk ? fk.contexts.includes(context) : false;
};

export const getKeyboardShortcutsForContext = (context: string) => {
  const shortcuts: any[] = [];
  
  Object.entries(FUNCTION_KEYS).forEach(([key, config]) => {
    if (config.contexts.includes(context)) {
      shortcuts.push({
        key,
        shortLabel: config.shortLabel,
        description: config.description,
        keyCode: config.keyCode,
        altKeyCode: config.altKeyCode
      });
    }
  });
  
  return shortcuts;
};

export const buildKeyboardEventHandler = (action: string, context: string) => {
  return (event: KeyboardEvent) => {
    const functionKey = getFunctionKeyByCode(event.key);
    
    if (functionKey && isValidContext(functionKey.action, context)) {
      if (functionKey.preventDefault) {
        event.preventDefault();
      }
      
      // Return action for component to handle
      return {
        action: functionKey.action,
        handler: functionKey.handler,
        requiresConfirmation: functionKey.requiresConfirmation,
        navigationPath: functionKey.navigationPath
      };
    }
    
    return null;
  };
};

// Export all keyboard constants as default for convenient importing
export default {
  FUNCTION_KEYS,
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  getFunctionKeyByCode,
  getAlternativeByKeySequence,
  isValidContext,
  getKeyboardShortcutsForContext,
  buildKeyboardEventHandler
};