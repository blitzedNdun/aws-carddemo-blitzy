/**
 * Keyboard Shortcuts Utility for CardDemo Application
 * 
 * This module provides comprehensive keyboard shortcut utilities that preserve
 * original 3270 function key behavior in React components. Enables consistent
 * navigation and operation controls across all BMS-converted screens with
 * modern browser compatibility and touch device support.
 * 
 * Key Features:
 * - Exact 3270 terminal function key behavior preservation
 * - React Router integration for navigation
 * - Browser compatibility with fallback key combinations
 * - Touch device support with virtual function keys
 * - Consistent help text display across all screens
 * - Accessibility compliance with WCAG 2.1 AA standards
 * 
 * Usage:
 * - Import required functions in React components
 * - Use createKeyboardEventHandler for component-specific key handling
 * - Call generateHelpToolbarText for consistent function key instruction display
 * - Implement touch device handlers for mobile compatibility
 */

import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

// Import constants from dependency files
import { 
  FUNCTION_KEYS, 
  ALTERNATIVE_KEY_COMBINATIONS, 
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS,
  KeyboardUtils 
} from '../constants/KeyboardConstants';

import { 
  ROUTES, 
  NAVIGATION_FLOW, 
  BREADCRUMB_PATHS, 
  NavigationUtils 
} from '../constants/NavigationConstants';

import { 
  FUNCTION_KEY_HELP, 
  KEYBOARD_INSTRUCTIONS, 
  HELP_TEXT 
} from '../constants/MessageConstants';

/**
 * Creates a keyboard event handler that preserves 3270 function key behavior
 * while providing modern browser compatibility and React Router integration.
 * 
 * This function implements the core keyboard handling logic that maps original
 * CICS function key patterns to modern web application navigation and actions.
 * 
 * @param {Object} config - Configuration object for keyboard event handling
 * @param {Function} config.navigate - React Router useNavigate hook function
 * @param {string} config.currentPath - Current route path for navigation context
 * @param {Function} config.onSubmit - Callback function for form submission (ENTER key)
 * @param {Function} config.onCancel - Callback function for cancel operations (F12/ESC key)
 * @param {Function} config.onPageUp - Callback function for page up navigation (F7 key)
 * @param {Function} config.onPageDown - Callback function for page down navigation (F8 key)
 * @param {Function} config.onSave - Callback function for save operations (F5 key)
 * @param {Object} config.customActions - Custom action handlers for specific keys
 * @param {boolean} config.preventDefaults - Whether to prevent default browser behavior
 * @param {boolean} config.enableAccessibility - Whether to enable accessibility features
 * @returns {Function} Event handler function for keyboard events
 */
export function createKeyboardEventHandler(config) {
  const {
    navigate,
    currentPath,
    onSubmit,
    onCancel,
    onPageUp,
    onPageDown,
    onSave,
    customActions = {},
    preventDefaults = true,
    enableAccessibility = true
  } = config;

  return function handleKeyboardEvent(event) {
    // Skip handling if user is typing in input fields
    if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
      // Only handle specific keys when in input fields
      if (event.key === 'Enter' && onSubmit) {
        if (preventDefaults) event.preventDefault();
        onSubmit();
        return;
      }
      if (event.key === 'Escape' && onCancel) {
        if (preventDefaults) event.preventDefault();
        onCancel();
        return;
      }
      return;
    }

    // Get action for the current key event
    const action = KeyboardUtils.getActionForEvent(event);
    
    if (!action) {
      return;
    }

    // Prevent default browser behavior for recognized keys
    if (preventDefaults) {
      event.preventDefault();
    }

    // Handle actions based on original 3270 function key behavior
    switch (action) {
      case 'EXIT':
        // F3 Key - Exit/Back Navigation
        // Preserves original CICS F3=Exit behavior
        if (navigate && currentPath) {
          const returnPath = NavigationUtils.getReturnPath(currentPath);
          navigate(returnPath);
        }
        break;

      case 'SUBMIT':
        // ENTER Key - Continue/Process
        // Preserves original CICS ENTER=Continue behavior
        if (onSubmit) {
          onSubmit();
        }
        break;

      case 'CANCEL':
        // F12 Key - Cancel Operation
        // Preserves original CICS F12=Cancel behavior
        if (onCancel) {
          onCancel();
        } else if (navigate && currentPath) {
          // Default cancel behavior - return to previous screen
          const returnPath = NavigationUtils.getReturnPath(currentPath);
          navigate(returnPath);
        }
        break;

      case 'PAGE_UP':
        // F7 Key - Page Up/Backward
        // Preserves original CICS F7=Backward behavior
        if (onPageUp) {
          onPageUp();
        }
        break;

      case 'PAGE_DOWN':
        // F8 Key - Page Down/Forward
        // Preserves original CICS F8=Forward behavior
        if (onPageDown) {
          onPageDown();
        }
        break;

      case 'SAVE':
        // F5 Key - Save/Refresh
        // Preserves original CICS F5=Save behavior
        if (onSave) {
          onSave();
        }
        break;

      case 'QUICK_EXIT':
        // ESCAPE Key - Quick Exit
        // Provides quick exit functionality
        if (onCancel) {
          onCancel();
        } else if (navigate) {
          navigate(ROUTES.LOGIN);
        }
        break;

      default:
        // Handle custom actions
        if (customActions[action]) {
          customActions[action](event);
        }
        break;
    }

    // Announce action to screen readers if accessibility is enabled
    if (enableAccessibility && KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.SHORTCUT_ANNOUNCEMENTS.enabled) {
      announceKeyboardAction(action);
    }
  };
}

/**
 * Gets function key actions configuration for a specific screen context.
 * Maps original BMS function key definitions to modern React component actions.
 * 
 * @param {string} screenType - Type of screen (login, menu, list, detail, etc.)
 * @param {Object} customActions - Custom action overrides for specific screens
 * @returns {Object} Function key actions configuration
 */
export function getFunctionKeyActions(screenType, customActions = {}) {
  const baseActions = {
    F3: {
      key: 'F3',
      action: 'EXIT',
      label: FUNCTION_KEY_HELP.F3_EXIT_TEXT,
      description: 'Exit current screen and return to previous screen',
      enabled: true
    },
    ENTER: {
      key: 'ENTER',
      action: 'SUBMIT',
      label: FUNCTION_KEY_HELP.ENTER_SUBMIT_TEXT,
      description: 'Continue to next screen or submit form',
      enabled: true
    }
  };

  // Add screen-specific function key configurations
  switch (screenType) {
    case 'login':
      return {
        ...baseActions,
        ENTER: {
          ...baseActions.ENTER,
          label: 'ENTER=Sign-on',
          description: 'Sign on to CardDemo application'
        }
      };

    case 'menu':
      return {
        ...baseActions,
        ENTER: {
          ...baseActions.ENTER,
          label: 'ENTER=Continue',
          description: 'Navigate to selected menu option'
        }
      };

    case 'list':
      return {
        ...baseActions,
        F7: {
          key: 'F7',
          action: 'PAGE_UP',
          label: FUNCTION_KEY_HELP.F7_PAGEUP_TEXT,
          description: 'Navigate to previous page',
          enabled: true
        },
        F8: {
          key: 'F8',
          action: 'PAGE_DOWN',
          label: FUNCTION_KEY_HELP.F8_PAGEDOWN_TEXT,
          description: 'Navigate to next page',
          enabled: true
        }
      };

    case 'detail':
      return {
        ...baseActions,
        F5: {
          key: 'F5',
          action: 'SAVE',
          label: 'F5=Save',
          description: 'Save current changes',
          enabled: true
        },
        F12: {
          key: 'F12',
          action: 'CANCEL',
          label: FUNCTION_KEY_HELP.F12_CANCEL_TEXT,
          description: 'Cancel current operation',
          enabled: true
        }
      };

    case 'update':
      return {
        ...baseActions,
        F5: {
          key: 'F5',
          action: 'SAVE',
          label: 'F5=Save',
          description: 'Save current changes',
          enabled: true
        },
        F12: {
          key: 'F12',
          action: 'CANCEL',
          label: FUNCTION_KEY_HELP.F12_CANCEL_TEXT,
          description: 'Cancel current operation',
          enabled: true
        }
      };

    default:
      return baseActions;
  }
}

/**
 * Creates touch device handlers for mobile and tablet compatibility.
 * Implements virtual function key buttons and gesture support to provide
 * equivalent functionality to keyboard shortcuts on touch devices.
 * 
 * @param {Object} config - Configuration object for touch device handling
 * @param {Object} config.actions - Function key actions configuration
 * @param {Function} config.onAction - Callback function for action execution
 * @param {boolean} config.enableGestures - Whether to enable gesture support
 * @param {boolean} config.enableVirtualButtons - Whether to show virtual function key buttons
 * @returns {Object} Touch device handler configuration
 */
export function createTouchDeviceHandlers(config) {
  const {
    actions,
    onAction,
    enableGestures = true,
    enableVirtualButtons = true
  } = config;

  const handlers = {
    virtualButtons: [],
    gestureHandlers: {},
    swipeHandlers: {}
  };

  // Create virtual function key buttons
  if (enableVirtualButtons) {
    Object.values(actions).forEach(action => {
      if (action.enabled) {
        const touchButton = TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS[`${action.action}_BUTTON`];
        if (touchButton) {
          handlers.virtualButtons.push({
            key: action.key,
            action: action.action,
            label: action.label,
            description: action.description,
            position: touchButton.position,
            icon: touchButton.icon,
            ariaLabel: touchButton.ariaLabel,
            onTap: () => onAction(action.action)
          });
        }
      }
    });
  }

  // Create gesture handlers
  if (enableGestures) {
    // Swipe right for F3 Exit/Back
    if (actions.F3 && actions.F3.enabled) {
      handlers.gestureHandlers.swipeRight = {
        action: 'EXIT',
        threshold: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_RIGHT_EXIT.threshold,
        velocity: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_RIGHT_EXIT.velocity,
        handler: () => onAction('EXIT'),
        description: 'Swipe right to go back'
      };
    }

    // Swipe left for forward navigation (F8)
    if (actions.F8 && actions.F8.enabled) {
      handlers.gestureHandlers.swipeLeft = {
        action: 'PAGE_DOWN',
        threshold: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_LEFT_FORWARD.threshold,
        velocity: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_LEFT_FORWARD.velocity,
        handler: () => onAction('PAGE_DOWN'),
        description: 'Swipe left to go forward'
      };
    }

    // Swipe up for previous page (F7)
    if (actions.F7 && actions.F7.enabled) {
      handlers.gestureHandlers.swipeUp = {
        action: 'PAGE_UP',
        threshold: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_UP_PREVIOUS.threshold,
        velocity: TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_UP_PREVIOUS.velocity,
        handler: () => onAction('PAGE_UP'),
        description: 'Swipe up for previous page'
      };
    }

    // Pull-to-refresh for F5 Save
    if (actions.F5 && actions.F5.enabled) {
      handlers.gestureHandlers.pullToRefresh = {
        action: 'SAVE',
        threshold: TOUCH_DEVICE_EQUIVALENTS.MOBILE_NAVIGATION.PULL_TO_REFRESH.threshold,
        handler: () => onAction('SAVE'),
        description: 'Pull down to refresh'
      };
    }
  }

  return handlers;
}

/**
 * Handles alternative key combinations for browser compatibility.
 * Provides fallback keyboard shortcuts when function keys are reserved
 * by the browser or operating system.
 * 
 * @param {KeyboardEvent} event - The keyboard event to process
 * @param {Object} config - Configuration object
 * @param {Function} config.onAction - Callback function for action execution
 * @param {boolean} config.enableAlternatives - Whether to enable alternative key combinations
 * @returns {boolean} Whether the event was handled
 */
export function handleAlternativeKeyCombinations(event, config) {
  const { onAction, enableAlternatives = true } = config;

  if (!enableAlternatives) {
    return false;
  }

  // Check for alternative key combinations
  const alternatives = ALTERNATIVE_KEY_COMBINATIONS;

  // Ctrl+S for F5 Save
  if (KeyboardUtils.isKeyMatch(event, alternatives.CTRL_S_FOR_F5)) {
    event.preventDefault();
    onAction('SAVE');
    return true;
  }

  // Ctrl+R for F5 Refresh (alternative)
  if (KeyboardUtils.isKeyMatch(event, alternatives.CTRL_R_FOR_F5)) {
    event.preventDefault();
    onAction('SAVE');
    return true;
  }

  // Ctrl+Escape for F12 Cancel
  if (KeyboardUtils.isKeyMatch(event, alternatives.CTRL_ESC_FOR_F12)) {
    event.preventDefault();
    onAction('CANCEL');
    return true;
  }

  // Alt+Left for F3 Back
  if (KeyboardUtils.isKeyMatch(event, alternatives.ALT_LEFT_FOR_F3)) {
    event.preventDefault();
    onAction('EXIT');
    return true;
  }

  // Check Alt combinations
  const altCombinations = alternatives.ALT_COMBINATIONS;

  // Alt+E for Exit
  if (KeyboardUtils.isKeyMatch(event, altCombinations.ALT_E_EXIT)) {
    event.preventDefault();
    onAction('EXIT');
    return true;
  }

  // Alt+S for Save
  if (KeyboardUtils.isKeyMatch(event, altCombinations.ALT_S_SAVE)) {
    event.preventDefault();
    onAction('SAVE');
    return true;
  }

  // Alt+C for Cancel
  if (KeyboardUtils.isKeyMatch(event, altCombinations.ALT_C_CANCEL)) {
    event.preventDefault();
    onAction('CANCEL');
    return true;
  }

  return false;
}

/**
 * Generates help toolbar text for consistent function key instruction display.
 * Preserves original BMS help text formatting while providing modern presentation.
 * 
 * @param {string} screenType - Type of screen for context-specific help text
 * @param {Object} actions - Function key actions configuration
 * @param {Object} options - Display options
 * @param {boolean} options.showAlternatives - Whether to show alternative key combinations
 * @param {boolean} options.showMobileInstructions - Whether to show mobile-specific instructions
 * @param {string} options.format - Format style ('horizontal', 'vertical', 'compact')
 * @returns {Object} Help toolbar text configuration
 */
export function generateHelpToolbarText(screenType, actions, options = {}) {
  const {
    showAlternatives = false,
    showMobileInstructions = false,
    format = 'horizontal'
  } = options;

  const helpText = {
    primary: [],
    alternatives: [],
    mobile: [],
    formatted: ''
  };

  // Generate primary function key help text
  Object.values(actions).forEach(action => {
    if (action.enabled) {
      let text = action.label;
      
      // Add alternative key combinations if requested
      if (showAlternatives) {
        const keyConfig = FUNCTION_KEYS[action.key];
        if (keyConfig && keyConfig.browserReserved && keyConfig.alternative) {
          text += ` (${keyConfig.alternative})`;
        }
      }
      
      helpText.primary.push(text);
    }
  });

  // Generate alternative key combinations help text
  if (showAlternatives) {
    const altInstructions = KEYBOARD_INSTRUCTIONS.ALTERNATIVE_COMBINATIONS;
    helpText.alternatives = Object.values(altInstructions);
  }

  // Generate mobile-specific instructions
  if (showMobileInstructions) {
    const mobileInstructions = HELP_TEXT.MOBILE_INSTRUCTIONS;
    helpText.mobile = Object.values(mobileInstructions);
  }

  // Format the help text based on requested format
  switch (format) {
    case 'horizontal':
      helpText.formatted = helpText.primary.join('  ');
      break;
    case 'vertical':
      helpText.formatted = helpText.primary.join('\n');
      break;
    case 'compact':
      helpText.formatted = helpText.primary.join(' ');
      break;
    default:
      helpText.formatted = helpText.primary.join('  ');
  }

  return helpText;
}

/**
 * Creates a navigation helper that integrates with React Router.
 * Provides centralized navigation logic that preserves original CICS
 * navigation patterns while supporting modern routing capabilities.
 * 
 * @param {Function} navigate - React Router useNavigate hook function
 * @param {string} currentPath - Current route path
 * @param {Object} options - Navigation options
 * @param {boolean} options.preserveHistory - Whether to preserve browser history
 * @param {boolean} options.enableBreadcrumbs - Whether to enable breadcrumb tracking
 * @returns {Object} Navigation helper functions
 */
export function createNavigationHelper(navigate, currentPath, options = {}) {
  const {
    preserveHistory = true,
    enableBreadcrumbs = true
  } = options;

  return {
    /**
     * Navigates to the exit/return path for the current screen
     * Preserves original F3=Exit behavior from 3270 terminals
     */
    exitCurrentScreen: () => {
      const returnPath = NavigationUtils.getReturnPath(currentPath);
      if (preserveHistory) {
        navigate(returnPath);
      } else {
        navigate(returnPath, { replace: true });
      }
    },

    /**
     * Navigates to a specific route with validation
     * Ensures navigation follows original CICS XCTL patterns
     */
    navigateToScreen: (targetPath, options = {}) => {
      if (NavigationUtils.isValidRoute(targetPath)) {
        const validDestinations = NavigationUtils.getValidDestinations(currentPath);
        
        if (validDestinations.length === 0 || validDestinations.includes(targetPath)) {
          navigate(targetPath, options);
        } else {
          console.warn(`Navigation to ${targetPath} not allowed from ${currentPath}`);
        }
      } else {
        console.error(`Invalid route: ${targetPath}`);
      }
    },

    /**
     * Gets the current breadcrumb trail
     * Provides navigation context for user orientation
     */
    getCurrentBreadcrumbs: () => {
      if (enableBreadcrumbs) {
        return NavigationUtils.buildBreadcrumbTrail(currentPath);
      }
      return [];
    },

    /**
     * Checks if navigation to a specific route is allowed
     * Enforces original CICS navigation security patterns
     */
    canNavigateTo: (targetPath) => {
      const validDestinations = NavigationUtils.getValidDestinations(currentPath);
      return validDestinations.length === 0 || validDestinations.includes(targetPath);
    },

    /**
     * Gets the screen title for the current path
     * Provides consistent screen identification
     */
    getCurrentScreenTitle: () => {
      const breadcrumb = BREADCRUMB_PATHS.PATH_MAPPING[currentPath];
      if (breadcrumb) {
        return BREADCRUMB_PATHS.SCREEN_TITLES[breadcrumb.transactionCode] || breadcrumb.title;
      }
      return 'CardDemo';
    }
  };
}

/**
 * Gets browser compatibility configuration for keyboard shortcuts.
 * Provides information about browser-specific keyboard handling requirements
 * and compatibility issues with function keys.
 * 
 * @returns {Object} Browser compatibility configuration
 */
export function getBrowserCompatibilityConfig() {
  return {
    /**
     * Function keys that are commonly reserved by browsers
     */
    reservedKeys: [
      { key: 'F5', reason: 'Browser refresh', alternative: 'Ctrl+S' },
      { key: 'F12', reason: 'Developer tools', alternative: 'Ctrl+Escape' },
      { key: 'F11', reason: 'Fullscreen toggle', alternative: 'Alt+Enter' },
      { key: 'F1', reason: 'Help system', alternative: 'Alt+H' }
    ],

    /**
     * Browser-specific compatibility notes
     */
    browserNotes: {
      chrome: {
        reservedKeys: ['F5', 'F12', 'F11', 'Ctrl+Shift+I'],
        recommendations: ['Use Ctrl+S for save', 'Use Ctrl+Escape for cancel']
      },
      firefox: {
        reservedKeys: ['F5', 'F12', 'F11', 'F3'],
        recommendations: ['Use Ctrl+S for save', 'Use Alt+Left for back']
      },
      safari: {
        reservedKeys: ['F5', 'F12', 'F11', 'Cmd+R'],
        recommendations: ['Use Cmd+S for save', 'Use Escape for cancel']
      },
      edge: {
        reservedKeys: ['F5', 'F12', 'F11', 'Ctrl+Shift+I'],
        recommendations: ['Use Ctrl+S for save', 'Use Ctrl+Escape for cancel']
      }
    },

    /**
     * Accessibility considerations
     */
    accessibility: {
      screenReader: {
        enabled: KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.SHORTCUT_ANNOUNCEMENTS.enabled,
        announcements: KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.ARIA_LABELS
      },
      focusManagement: {
        enabled: KEYBOARD_ACCESSIBILITY_CONFIG.FOCUS_MANAGEMENT.FOCUS_TRAP.enabled,
        returnFocus: KEYBOARD_ACCESSIBILITY_CONFIG.FOCUS_MANAGEMENT.FOCUS_TRAP.returnFocusOnClose
      },
      keyboardOnly: {
        enabled: KEYBOARD_ACCESSIBILITY_CONFIG.KEYBOARD_ONLY_NAVIGATION.NAVIGATION_PATTERNS.functionKeys,
        skipLinks: KEYBOARD_ACCESSIBILITY_CONFIG.KEYBOARD_ONLY_NAVIGATION.SKIP_LINKS.enabled
      }
    }
  };
}

/**
 * Announces keyboard action to screen readers
 * Provides accessibility support for keyboard navigation
 * 
 * @param {string} action - The action being performed
 * @private
 */
function announceKeyboardAction(action) {
  const announcement = KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.ARIA_LABELS[action];
  if (announcement) {
    const liveRegion = document.querySelector('[aria-live]');
    if (liveRegion) {
      liveRegion.textContent = `${KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.LIVE_REGIONS.action}${announcement}`;
    }
  }
}

/**
 * Detects if the current device supports touch input
 * Used to determine whether to enable touch-specific features
 * 
 * @returns {boolean} Whether touch input is supported
 * @private
 */
function isTouchDevice() {
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0 || navigator.msMaxTouchPoints > 0;
}

/**
 * Gets the current device type for context-specific behavior
 * 
 * @returns {string} Device type ('desktop', 'tablet', 'mobile')
 * @private
 */
function getDeviceType() {
  if (isTouchDevice()) {
    return window.innerWidth > 768 ? 'tablet' : 'mobile';
  }
  return 'desktop';
}

// Export utility functions for external use
export const KeyboardShortcutUtils = {
  isTouchDevice,
  getDeviceType,
  announceKeyboardAction
};