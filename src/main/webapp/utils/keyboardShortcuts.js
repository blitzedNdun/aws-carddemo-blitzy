/**
 * keyboardShortcuts.js
 * 
 * Keyboard shortcut utilities that preserve original 3270 function key behavior in React components,
 * providing consistent navigation and operation controls across all BMS-converted screens with modern
 * browser compatibility.
 * 
 * This utility module implements the core keyboard event handling functionality that maintains exact
 * functional equivalence with the original IBM 3270 terminal interface while providing modern web
 * application features including touch device support, accessibility compliance, and cross-browser
 * compatibility.
 * 
 * Key Features:
 * - Exact preservation of 3270 function key behaviors (F3=Exit, F7=Page Up, F8=Page Down, F12=Cancel, ENTER=Submit)
 * - React Router integration for seamless navigation equivalent to CICS XCTL program flow
 * - Touch device support with on-screen function key buttons for mobile and tablet users
 * - Alternative key combinations for browsers that reserve function keys (Ctrl+R for F5, Ctrl+Esc for F12)
 * - WCAG 2.1 AA accessibility compliance with screen reader support and keyboard-only navigation
 * - Comprehensive error handling and browser compatibility detection
 * - Consistent function key instruction display in help toolbar components
 * 
 * Technology Transformation: IBM 3270 Terminal → Modern Web Application
 * Original System: IBM 3270 Terminal with Physical Function Keys (PF1-PF24)
 * Target System: React Web Application with JavaScript KeyboardEvent Handling
 * 
 * Function Key Mappings from BMS Analysis:
 * - F3: Exit/Back to previous screen (universal across all BMS screens)
 * - F7: Backward/Previous page navigation (COTRN00.bms, COCRDLI.bms)
 * - F8: Forward/Next page navigation (COTRN00.bms, COCRDLI.bms)
 * - F12: Cancel operation and return to main menu (technical specification)
 * - ENTER: Continue/Submit form data (COSGN00.bms, COMEN01.bms)
 * - F4: Clear screen and reset form fields (COBIL00.bms)
 * 
 * Usage Pattern:
 * ```javascript
 * import { createKeyboardEventHandler, getFunctionKeyActions } from './keyboardShortcuts';
 * 
 * // In React component
 * const handleKeyboard = createKeyboardEventHandler(navigate, currentPath);
 * useEffect(() => {
 *   document.addEventListener('keydown', handleKeyboard);
 *   return () => document.removeEventListener('keydown', handleKeyboard);
 * }, [handleKeyboard]);
 * ```
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 * Licensed under the Apache License, Version 2.0
 */

import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FUNCTION_KEYS, ALTERNATIVE_KEY_COMBINATIONS, KEYBOARD_ACCESSIBILITY_CONFIG, TOUCH_DEVICE_EQUIVALENTS } from '../constants/KeyboardConstants';
import { ROUTES, NAVIGATION_FLOW, BREADCRUMB_PATHS } from '../constants/NavigationConstants';
import { FUNCTION_KEY_HELP, KEYBOARD_INSTRUCTIONS, HELP_TEXT } from '../constants/MessageConstants';

// ==========================================
// Core Keyboard Event Handler Creation
// ==========================================

/**
 * Creates a comprehensive keyboard event handler that processes function key presses
 * and executes appropriate navigation actions while preserving original 3270 behavior.
 * 
 * This function generates a keyboard event handler that can be attached to document
 * or component-level keyboard events to provide consistent function key processing
 * across all React components in the application.
 * 
 * @param {function} navigate - React Router useNavigate hook function for programmatic navigation
 * @param {string} currentPath - Current route path for context-aware navigation decisions
 * @param {Object} options - Configuration options for keyboard handling behavior
 * @param {boolean} options.preventDefault - Whether to prevent default browser behavior
 * @param {boolean} options.enableAlternatives - Whether to enable alternative key combinations
 * @param {boolean} options.enableAccessibility - Whether to enable accessibility features
 * @param {function} options.onFunctionKeyPress - Callback function for function key press events
 * @returns {function} Keyboard event handler function ready for addEventListener
 * 
 * @example
 * // Basic usage in React component
 * const navigate = useNavigate();
 * const location = useLocation();
 * const handleKeyboard = createKeyboardEventHandler(navigate, location.pathname);
 * 
 * useEffect(() => {
 *   document.addEventListener('keydown', handleKeyboard);
 *   return () => document.removeEventListener('keydown', handleKeyboard);
 * }, [handleKeyboard]);
 * 
 * @example
 * // Advanced usage with custom options
 * const handleKeyboard = createKeyboardEventHandler(navigate, currentPath, {
 *   preventDefault: true,
 *   enableAlternatives: true,
 *   enableAccessibility: true,
 *   onFunctionKeyPress: (key, action) => console.log(`${key} pressed: ${action}`)
 * });
 */
export function createKeyboardEventHandler(navigate, currentPath, options = {}) {
  const {
    preventDefault = true,
    enableAlternatives = true,
    enableAccessibility = true,
    onFunctionKeyPress = null,
  } = options;

  return function handleKeyboardEvent(event) {
    try {
      // Skip processing if event is from input elements to avoid interference
      if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA' || event.target.isContentEditable) {
        return;
      }

      // Get function key definition from event
      const functionKey = getFunctionKeyFromEvent(event);
      if (!functionKey) {
        // Try alternative key combinations if enabled
        if (enableAlternatives) {
          const alternativeKey = getAlternativeKeyFromEvent(event);
          if (alternativeKey) {
            processFunctionKeyAction(alternativeKey, navigate, currentPath, event, enableAccessibility);
            if (onFunctionKeyPress) {
              onFunctionKeyPress(alternativeKey.key, alternativeKey.eventHandler.action);
            }
            return;
          }
        }
        return;
      }

      // Process the function key action
      processFunctionKeyAction(functionKey, navigate, currentPath, event, enableAccessibility);
      
      // Call custom callback if provided
      if (onFunctionKeyPress) {
        onFunctionKeyPress(functionKey.key, functionKey.eventHandler.action);
      }

    } catch (error) {
      console.error('Error processing keyboard event:', error);
      // Announce error to screen readers if accessibility is enabled
      if (enableAccessibility) {
        announceToScreenReader('Keyboard navigation error occurred');
      }
    }
  };
}

/**
 * Processes a function key action by executing the appropriate navigation or form operation
 * based on the key pressed and current application context.
 * 
 * @param {Object} functionKey - Function key definition object from KeyboardConstants
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @param {KeyboardEvent} event - Original keyboard event
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function processFunctionKeyAction(functionKey, navigate, currentPath, event, enableAccessibility) {
  // Handle event prevention and propagation
  if (functionKey.eventHandler.preventDefault) {
    event.preventDefault();
  }
  if (functionKey.eventHandler.stopPropagation) {
    event.stopPropagation();
  }

  // Announce action to screen readers
  if (enableAccessibility) {
    const announcement = KEYBOARD_ACCESSIBILITY_CONFIG.SCREEN_READER_SUPPORT.FUNCTION_KEY_ANNOUNCEMENTS[`${functionKey.key.toUpperCase()}_PRESSED`];
    if (announcement) {
      announceToScreenReader(announcement);
    }
  }

  // Execute the appropriate action based on function key type
  switch (functionKey.eventHandler.action) {
    case 'EXIT_SCREEN':
      handleExitNavigation(navigate, currentPath, functionKey);
      break;
    
    case 'CLEAR_FORM':
      handleFormClear(functionKey, enableAccessibility);
      break;
    
    case 'PREVIOUS_PAGE':
      handlePaginationAction('PREVIOUS', enableAccessibility);
      break;
    
    case 'NEXT_PAGE':
      handlePaginationAction('NEXT', enableAccessibility);
      break;
    
    case 'CANCEL_OPERATION':
      handleCancelOperation(navigate, functionKey, enableAccessibility);
      break;
    
    case 'SUBMIT_FORM':
      handleFormSubmission(functionKey, enableAccessibility);
      break;
    
    case 'ESCAPE_OPERATION':
      handleEscapeOperation(enableAccessibility);
      break;
    
    case 'REFRESH_SCREEN':
      handleScreenRefresh(enableAccessibility);
      break;
    
    default:
      console.warn(`Unknown function key action: ${functionKey.eventHandler.action}`);
  }
}

/**
 * Handles F3 exit navigation by determining the appropriate return path
 * based on current route and navigation flow patterns.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @param {Object} functionKey - Function key definition object
 */
function handleExitNavigation(navigate, currentPath, functionKey) {
  let returnPath = ROUTES.MENU; // Default fallback
  
  // Use navigation flow return paths if available
  if (NAVIGATION_FLOW.RETURN_PATHS[currentPath]) {
    returnPath = NAVIGATION_FLOW.RETURN_PATHS[currentPath];
  } else if (functionKey.eventHandler.returnPathResolver) {
    returnPath = functionKey.eventHandler.returnPathResolver(currentPath);
  }
  
  // Navigate to the determined return path
  navigate(returnPath);
}

/**
 * Handles F4 form clearing by resetting all form fields to their initial state
 * with optional confirmation dialog.
 * 
 * @param {Object} functionKey - Function key definition object
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handleFormClear(functionKey, enableAccessibility) {
  const shouldConfirm = functionKey.eventHandler.confirmationRequired;
  
  if (shouldConfirm) {
    const confirmMessage = functionKey.eventHandler.confirmationMessage || 'Are you sure you want to clear all fields?';
    if (!window.confirm(confirmMessage)) {
      return;
    }
  }
  
  // Clear all form fields
  document.querySelectorAll('input, textarea, select').forEach((element) => {
    if (element.type === 'checkbox' || element.type === 'radio') {
      element.checked = false;
    } else if (element.tagName === 'SELECT') {
      element.selectedIndex = 0;
    } else if (element.type !== 'button' && element.type !== 'submit' && element.type !== 'reset') {
      element.value = '';
    }
  });
  
  // Announce completion to screen readers
  if (enableAccessibility) {
    announceToScreenReader('Form fields cleared');
  }
  
  // Focus the first form field
  const firstField = document.querySelector('input, textarea, select');
  if (firstField) {
    firstField.focus();
  }
}

/**
 * Handles F7/F8 pagination actions by dispatching custom pagination events
 * that can be handled by list components.
 * 
 * @param {string} direction - 'PREVIOUS' or 'NEXT'
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handlePaginationAction(direction, enableAccessibility) {
  const paginationEvent = new CustomEvent('pagination', {
    detail: { 
      direction: direction,
      action: direction === 'PREVIOUS' ? 'DECREMENT_PAGE' : 'INCREMENT_PAGE'
    }
  });
  
  document.dispatchEvent(paginationEvent);
  
  // Announce page change to screen readers
  if (enableAccessibility) {
    const announcement = direction === 'PREVIOUS' ? 'Loading previous page' : 'Loading next page';
    announceToScreenReader(announcement);
  }
}

/**
 * Handles F12 cancel operation by confirming cancellation and navigating to main menu.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {Object} functionKey - Function key definition object
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handleCancelOperation(navigate, functionKey, enableAccessibility) {
  const shouldConfirm = functionKey.eventHandler.confirmationRequired;
  
  if (shouldConfirm) {
    const confirmMessage = functionKey.eventHandler.confirmationMessage || 'Are you sure you want to cancel the current operation?';
    if (!window.confirm(confirmMessage)) {
      return;
    }
  }
  
  // Navigate to return path (usually main menu)
  const returnPath = functionKey.eventHandler.returnPath || ROUTES.MENU;
  navigate(returnPath);
  
  // Announce cancellation to screen readers
  if (enableAccessibility) {
    announceToScreenReader('Operation cancelled');
  }
}

/**
 * Handles ENTER form submission by triggering form submit event
 * with validation if required.
 * 
 * @param {Object} functionKey - Function key definition object
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handleFormSubmission(functionKey, enableAccessibility) {
  const form = document.querySelector('form');
  if (!form) {
    return;
  }
  
  // Perform validation if required
  if (functionKey.eventHandler.validationRequired) {
    const isValid = form.checkValidity();
    if (!isValid) {
      form.reportValidity();
      if (enableAccessibility) {
        announceToScreenReader('Form contains errors. Please correct them before submitting.');
      }
      return;
    }
  }
  
  // Submit the form
  form.dispatchEvent(new Event('submit', { bubbles: true }));
  
  // Announce submission to screen readers
  if (enableAccessibility) {
    announceToScreenReader('Form submitted');
  }
}

/**
 * Handles ESC escape operation by closing modals and canceling edits.
 * 
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handleEscapeOperation(enableAccessibility) {
  // Close any open modals
  const modals = document.querySelectorAll('[role="dialog"], .modal');
  modals.forEach(modal => {
    const closeButton = modal.querySelector('[data-dismiss="modal"], .close');
    if (closeButton) {
      closeButton.click();
    }
  });
  
  // Cancel any active edits
  const editableElements = document.querySelectorAll('[contenteditable="true"]');
  editableElements.forEach(element => {
    element.blur();
  });
  
  // Announce escape to screen readers
  if (enableAccessibility) {
    announceToScreenReader('Escape pressed');
  }
}

/**
 * Handles screen refresh by reloading current page data.
 * 
 * @param {boolean} enableAccessibility - Whether to announce actions to screen readers
 */
function handleScreenRefresh(enableAccessibility) {
  // Dispatch refresh event that components can listen to
  const refreshEvent = new CustomEvent('refresh', { detail: { timestamp: Date.now() } });
  document.dispatchEvent(refreshEvent);
  
  // Announce refresh to screen readers
  if (enableAccessibility) {
    announceToScreenReader('Screen refreshed');
  }
}

// ==========================================
// Function Key Detection and Mapping
// ==========================================

/**
 * Retrieves comprehensive function key action mappings for all supported keys
 * with their corresponding behaviors and navigation patterns.
 * 
 * @param {string} currentPath - Current route path for context-aware actions
 * @returns {Object} Complete mapping of function keys to their actions and behaviors
 * 
 * @example
 * const actions = getFunctionKeyActions('/accounts/view');
 * console.log(actions.F3); // { key: 'F3', action: 'EXIT_SCREEN', returnPath: '/menu/main' }
 */
export function getFunctionKeyActions(currentPath = '') {
  const actions = {};
  
  // Build actions from function key definitions
  Object.entries(FUNCTION_KEYS).forEach(([keyName, keyDef]) => {
    actions[keyName] = {
      key: keyDef.key,
      keyCode: keyDef.keyCode,
      action: keyDef.eventHandler.action,
      description: keyDef.description,
      originalFunction: keyDef.originalFunction,
      cicsEquivalent: keyDef.cicsEquivalent,
      returnPath: keyDef.eventHandler.returnPath || NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.MENU,
      accessibility: keyDef.accessibility,
      
      // Add context-specific information
      isEnabled: true,
      isVisible: true,
      requiresConfirmation: keyDef.eventHandler.confirmationRequired || false,
      confirmationMessage: keyDef.eventHandler.confirmationMessage || null,
    };
  });
  
  // Add alternative key combinations
  Object.entries(ALTERNATIVE_KEY_COMBINATIONS).forEach(([keyName, keyDef]) => {
    const altKeyName = `ALT_${keyName}`;
    actions[altKeyName] = {
      key: keyDef.key,
      keyCode: keyDef.keyCode,
      action: keyDef.eventHandler.action,
      description: keyDef.description,
      originalFunction: keyDef.originalFunction,
      isAlternative: true,
      browserConflict: keyDef.browserConflict,
      accessibility: keyDef.accessibility,
    };
  });
  
  return actions;
}

/**
 * Detects function key from keyboard event by matching event properties
 * against function key definitions.
 * 
 * @param {KeyboardEvent} event - Keyboard event to analyze
 * @returns {Object|null} Function key definition object or null if no match
 */
function getFunctionKeyFromEvent(event) {
  for (const [, functionKey] of Object.entries(FUNCTION_KEYS)) {
    if (matchesKeyEvent(event, functionKey)) {
      return functionKey;
    }
  }
  return null;
}

/**
 * Detects alternative key combination from keyboard event.
 * 
 * @param {KeyboardEvent} event - Keyboard event to analyze
 * @returns {Object|null} Alternative key definition object or null if no match
 */
function getAlternativeKeyFromEvent(event) {
  for (const [, altKey] of Object.entries(ALTERNATIVE_KEY_COMBINATIONS)) {
    if (matchesKeyEvent(event, altKey)) {
      return altKey;
    }
  }
  return null;
}

/**
 * Checks if a keyboard event matches a key definition.
 * 
 * @param {KeyboardEvent} event - Keyboard event to check
 * @param {Object} keyDef - Key definition object to match against
 * @returns {boolean} True if event matches key definition
 */
function matchesKeyEvent(event, keyDef) {
  return (
    event.key === keyDef.key &&
    event.keyCode === keyDef.keyCode &&
    event.ctrlKey === keyDef.ctrlKey &&
    event.altKey === keyDef.altKey &&
    event.shiftKey === keyDef.shiftKey &&
    event.metaKey === keyDef.metaKey
  );
}

// ==========================================
// Touch Device Support
// ==========================================

/**
 * Creates touch device handlers that provide function key equivalents for mobile
 * and tablet users through gesture recognition and touch button interfaces.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @param {Object} options - Touch handler configuration options
 * @returns {Object} Touch handlers and gesture recognizers
 * 
 * @example
 * const touchHandlers = createTouchDeviceHandlers(navigate, currentPath, {
 *   enableGestures: true,
 *   enableButtons: true,
 *   buttonLayout: 'MOBILE_PORTRAIT'
 * });
 */
export function createTouchDeviceHandlers(navigate, currentPath, options = {}) {
  const {
    enableGestures = true,
    enableButtons = true,
    buttonLayout = 'MOBILE_PORTRAIT',
    enableHapticFeedback = true,
    enableSoundFeedback = false,
  } = options;

  const handlers = {};

  if (enableGestures) {
    // Swipe gesture handlers
    handlers.swipeHandlers = createSwipeGestureHandlers(navigate, currentPath, enableHapticFeedback);
    
    // Pinch gesture handlers
    handlers.pinchHandlers = createPinchGestureHandlers(enableHapticFeedback);
  }

  if (enableButtons) {
    // Touch button handlers
    handlers.buttonHandlers = createTouchButtonHandlers(navigate, currentPath, buttonLayout);
    
    // Button layout configuration
    handlers.buttonLayout = getTouchButtonLayout(buttonLayout);
  }

  return handlers;
}

/**
 * Creates swipe gesture handlers for touch navigation.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @param {boolean} enableHapticFeedback - Whether to enable haptic feedback
 * @returns {Object} Swipe gesture handlers
 */
function createSwipeGestureHandlers(navigate, currentPath, enableHapticFeedback) {
  const swipeHandlers = {};
  
  // Get swipe gestures from touch device equivalents
  const swipeGestures = TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES;
  
  // Create handlers for horizontal swipes (pagination)
  Object.entries(swipeGestures.HORIZONTAL_SWIPES).forEach(([swipeName, swipeConfig]) => {
    swipeHandlers[swipeName] = (event) => {
      // Provide haptic feedback
      if (enableHapticFeedback && navigator.vibrate) {
        navigator.vibrate(swipeConfig.feedback.haptic === 'light' ? 50 : 100);
      }
      
      // Execute the equivalent function key action
      const functionKey = FUNCTION_KEYS[swipeConfig.functionKeyEquivalent];
      if (functionKey) {
        processFunctionKeyAction(functionKey, navigate, currentPath, event, true);
      }
    };
  });
  
  // Create handlers for vertical swipes (navigation)
  Object.entries(swipeGestures.VERTICAL_SWIPES).forEach(([swipeName, swipeConfig]) => {
    swipeHandlers[swipeName] = (event) => {
      // Provide haptic feedback
      if (enableHapticFeedback && navigator.vibrate) {
        navigator.vibrate(swipeConfig.feedback.haptic === 'medium' ? 100 : 150);
      }
      
      // Execute the equivalent function key action
      const functionKey = FUNCTION_KEYS[swipeConfig.functionKeyEquivalent];
      if (functionKey) {
        processFunctionKeyAction(functionKey, navigate, currentPath, event, true);
      }
    };
  });
  
  return swipeHandlers;
}

/**
 * Creates pinch gesture handlers for zoom functionality.
 * 
 * @param {boolean} enableHapticFeedback - Whether to enable haptic feedback
 * @returns {Object} Pinch gesture handlers
 */
function createPinchGestureHandlers(enableHapticFeedback) {
  const pinchConfig = TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.MULTI_TOUCH_GESTURES.PINCH_TO_ZOOM;
  
  return {
    onPinchStart: (event) => {
      // Initialize pinch tracking
      event.preventDefault();
    },
    
    onPinchMove: (event) => {
      // Handle zoom scaling
      event.preventDefault();
    },
    
    onPinchEnd: (event) => {
      // Finalize zoom operation
      if (enableHapticFeedback && navigator.vibrate) {
        navigator.vibrate(50);
      }
      
      // Announce zoom change to screen readers
      announceToScreenReader('Zoom level changed');
    },
  };
}

/**
 * Creates touch button handlers for function key buttons.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @param {string} buttonLayout - Button layout configuration name
 * @returns {Object} Touch button handlers
 */
function createTouchButtonHandlers(navigate, currentPath, buttonLayout) {
  const buttonHandlers = {};
  const buttonConfigs = TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.FUNCTION_KEY_BUTTONS;
  
  Object.entries(buttonConfigs).forEach(([buttonName, buttonConfig]) => {
    buttonHandlers[buttonName] = (event) => {
      event.preventDefault();
      
      // Get the equivalent function key
      const functionKey = FUNCTION_KEYS[buttonConfig.functionKeyEquivalent];
      if (functionKey) {
        processFunctionKeyAction(functionKey, navigate, currentPath, event, true);
      }
    };
  });
  
  return buttonHandlers;
}

/**
 * Gets touch button layout configuration for current device.
 * 
 * @param {string} buttonLayout - Button layout name
 * @returns {Object} Button layout configuration
 */
function getTouchButtonLayout(buttonLayout) {
  const layouts = TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.BUTTON_LAYOUTS;
  return layouts[buttonLayout] || layouts.MOBILE_PORTRAIT;
}

// ==========================================
// Alternative Key Combinations
// ==========================================

/**
 * Handles alternative key combinations for browsers that reserve function keys,
 * providing equivalent functionality through key combinations that are consistently
 * available across different browsers and platforms.
 * 
 * @param {KeyboardEvent} event - Keyboard event to process
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @returns {boolean} True if alternative key combination was processed
 * 
 * @example
 * const wasHandled = handleAlternativeKeyCombinations(event, navigate, '/accounts/view');
 * if (wasHandled) {
 *   // Alternative key combination was processed
 * }
 */
export function handleAlternativeKeyCombinations(event, navigate, currentPath) {
  // Check for Ctrl+R (alternative to F5 refresh)
  if (event.ctrlKey && event.key === 'r') {
    event.preventDefault();
    handleScreenRefresh(true);
    return true;
  }
  
  // Check for Ctrl+Esc (alternative to F12 cancel)
  if (event.ctrlKey && event.key === 'Escape') {
    event.preventDefault();
    handleCancelOperation(navigate, FUNCTION_KEYS.F12, true);
    return true;
  }
  
  // Check for Alt+F3 (save and exit)
  if (event.altKey && event.key === 'F3') {
    event.preventDefault();
    const altConfig = ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_F3_SAVE_EXIT;
    if (altConfig.eventHandler.confirmationRequired) {
      const confirmed = window.confirm(altConfig.eventHandler.confirmationMessage);
      if (confirmed) {
        // Save changes before exiting
        const form = document.querySelector('form');
        if (form) {
          form.dispatchEvent(new Event('submit', { bubbles: true }));
        }
        
        // Navigate to return path
        const returnPath = NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.MENU;
        navigate(returnPath);
      }
    }
    return true;
  }
  
  // Check for Alt+F4 (exit application)
  if (event.altKey && event.key === 'F4') {
    event.preventDefault();
    const altConfig = ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_F4_APP_EXIT;
    if (altConfig.eventHandler.confirmationRequired) {
      const confirmed = window.confirm(altConfig.eventHandler.confirmationMessage);
      if (confirmed) {
        window.close();
      }
    }
    return true;
  }
  
  // Check for Alt+Enter (fullscreen toggle)
  if (event.altKey && event.key === 'Enter') {
    event.preventDefault();
    toggleFullscreen();
    return true;
  }
  
  return false;
}

/**
 * Toggles fullscreen mode for immersive experience.
 */
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(err => {
      console.warn('Error attempting to enable fullscreen:', err);
    });
  } else {
    document.exitFullscreen().catch(err => {
      console.warn('Error attempting to exit fullscreen:', err);
    });
  }
}

// ==========================================
// Help Toolbar Text Generation
// ==========================================

/**
 * Generates consistent function key instruction text for help toolbar components
 * matching original BMS help text patterns extracted from screen definitions.
 * 
 * @param {string} currentPath - Current route path to determine applicable function keys
 * @param {Object} options - Configuration options for help text generation
 * @returns {Object} Help text configuration with formatted instruction strings
 * 
 * @example
 * const helpText = generateHelpToolbarText('/accounts/view', {
 *   includeAlternatives: true,
 *   includeMobileInstructions: true
 * });
 * console.log(helpText.primaryInstructions); // "ENTER=Continue  F3=Exit"
 */
export function generateHelpToolbarText(currentPath = '', options = {}) {
  const {
    includeAlternatives = false,
    includeMobileInstructions = false,
    includeAccessibilityInfo = false,
    format = 'original', // 'original', 'modern', 'abbreviated'
  } = options;

  const helpText = {
    primaryInstructions: '',
    secondaryInstructions: '',
    mobileInstructions: '',
    accessibilityInfo: '',
    functionKeyList: [],
  };

  // Determine which function keys are applicable for current screen
  const applicableKeys = getApplicableFunctionKeys(currentPath);
  
  // Generate primary instruction text based on current path
  helpText.primaryInstructions = generatePrimaryInstructions(currentPath, applicableKeys, format);
  
  // Generate secondary instructions for less common keys
  if (includeAlternatives) {
    helpText.secondaryInstructions = generateSecondaryInstructions(applicableKeys, format);
  }
  
  // Generate mobile-specific instructions
  if (includeMobileInstructions) {
    helpText.mobileInstructions = generateMobileInstructions();
  }
  
  // Generate accessibility information
  if (includeAccessibilityInfo) {
    helpText.accessibilityInfo = generateAccessibilityInfo();
  }
  
  // Build function key list for programmatic access
  helpText.functionKeyList = applicableKeys.map(key => ({
    key: key.key,
    description: key.description,
    shortcut: `${key.key}=${key.originalFunction}`,
  }));

  return helpText;
}

/**
 * Determines which function keys are applicable for the current screen.
 * 
 * @param {string} currentPath - Current route path
 * @returns {Array} Array of applicable function key definitions
 */
function getApplicableFunctionKeys(currentPath) {
  const applicable = [];
  
  // F3 (Exit) is universal across all screens
  applicable.push(FUNCTION_KEYS.F3);
  
  // ENTER (Continue/Submit) is available on most screens
  applicable.push(FUNCTION_KEYS.ENTER);
  
  // Add pagination keys for list screens
  if (currentPath.includes('/list') || currentPath.includes('/transactions') || currentPath.includes('/cards')) {
    applicable.push(FUNCTION_KEYS.F7); // Previous page
    applicable.push(FUNCTION_KEYS.F8); // Next page
  }
  
  // Add form-specific keys for form screens
  if (currentPath.includes('/update') || currentPath.includes('/create') || currentPath.includes('/payments')) {
    applicable.push(FUNCTION_KEYS.F4); // Clear form
    applicable.push(FUNCTION_KEYS.F12); // Cancel
  }
  
  return applicable;
}

/**
 * Generates primary instruction text based on screen type and applicable keys.
 * 
 * @param {string} currentPath - Current route path
 * @param {Array} applicableKeys - Array of applicable function key definitions
 * @param {string} format - Format style ('original', 'modern', 'abbreviated')
 * @returns {string} Primary instruction text
 */
function generatePrimaryInstructions(currentPath, applicableKeys, format) {
  // Use predefined patterns from BMS analysis for specific screens
  if (currentPath === ROUTES.LOGIN) {
    return FUNCTION_KEY_HELP.SIGNIN_NAVIGATION;
  }
  
  if (currentPath === ROUTES.MENU || currentPath === ROUTES.ADMIN_MENU) {
    return FUNCTION_KEY_HELP.BASIC_NAVIGATION;
  }
  
  if (currentPath.includes('/list') || currentPath.includes('/transactions')) {
    return FUNCTION_KEY_HELP.EXTENDED_NAVIGATION;
  }
  
  if (currentPath.includes('/payments/bill')) {
    return FUNCTION_KEY_HELP.BILL_PAYMENT_NAVIGATION;
  }
  
  if (currentPath.includes('/update') || currentPath.includes('/create')) {
    return FUNCTION_KEY_HELP.ACCOUNT_UPDATE_NAVIGATION;
  }
  
  if (currentPath.includes('/cards')) {
    return FUNCTION_KEY_HELP.CARD_LIST_NAVIGATION;
  }
  
  // Generate dynamic instructions based on applicable keys
  const instructions = [];
  
  // Add ENTER instruction if applicable
  const enterKey = applicableKeys.find(key => key.key === 'Enter');
  if (enterKey) {
    instructions.push(FUNCTION_KEY_HELP.ENTER_SUBMIT_TEXT);
  }
  
  // Add F3 instruction (always present)
  instructions.push(FUNCTION_KEY_HELP.F3_EXIT_TEXT);
  
  // Add pagination instructions if applicable
  const f7Key = applicableKeys.find(key => key.key === 'F7');
  const f8Key = applicableKeys.find(key => key.key === 'F8');
  if (f7Key && f8Key) {
    instructions.push(FUNCTION_KEY_HELP.F7_PAGEUP_TEXT);
    instructions.push(FUNCTION_KEY_HELP.F8_PAGEDOWN_TEXT);
  }
  
  return instructions.join('  ');
}

/**
 * Generates secondary instruction text for less common function keys.
 * 
 * @param {Array} applicableKeys - Array of applicable function key definitions
 * @param {string} format - Format style
 * @returns {string} Secondary instruction text
 */
function generateSecondaryInstructions(applicableKeys, format) {
  const instructions = [];
  
  // Add F4 (Clear) if applicable
  const f4Key = applicableKeys.find(key => key.key === 'F4');
  if (f4Key) {
    instructions.push(FUNCTION_KEY_HELP.F4_CLEAR_TEXT);
  }
  
  // Add F12 (Cancel) if applicable
  const f12Key = applicableKeys.find(key => key.key === 'F12');
  if (f12Key) {
    instructions.push(FUNCTION_KEY_HELP.F12_CANCEL_TEXT);
  }
  
  return instructions.join('  ');
}

/**
 * Generates mobile-specific instructions for touch device users.
 * 
 * @returns {string} Mobile instruction text
 */
function generateMobileInstructions() {
  return HELP_TEXT.MOBILE_INSTRUCTIONS.TOUCH_NAVIGATION + ' ' + 
         HELP_TEXT.MOBILE_INSTRUCTIONS.SWIPE_GESTURE + ' ' + 
         HELP_TEXT.MOBILE_INSTRUCTIONS.BUTTON_ALTERNATIVES;
}

/**
 * Generates accessibility information for screen reader users.
 * 
 * @returns {string} Accessibility information text
 */
function generateAccessibilityInfo() {
  return KEYBOARD_INSTRUCTIONS.BASIC_NAVIGATION + ' ' + 
         KEYBOARD_INSTRUCTIONS.FUNCTION_KEY_LIST + ' ' + 
         KEYBOARD_INSTRUCTIONS.ALTERNATIVE_COMBINATIONS;
}

// ==========================================
// Navigation Helper Functions
// ==========================================

/**
 * Creates a comprehensive navigation helper that provides programmatic access
 * to navigation functions and route information for React components.
 * 
 * @param {function} navigate - React Router navigate function
 * @param {string} currentPath - Current route path
 * @returns {Object} Navigation helper object with utility functions
 * 
 * @example
 * const navHelper = createNavigationHelper(navigate, '/accounts/view');
 * navHelper.goBack(); // Navigate to previous screen
 * navHelper.goToMenu(); // Navigate to main menu
 * navHelper.getCurrentScreenInfo(); // Get current screen information
 */
export function createNavigationHelper(navigate, currentPath) {
  return {
    /**
     * Navigate back to the previous screen (F3 equivalent)
     */
    goBack() {
      const returnPath = NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.MENU;
      navigate(returnPath);
    },
    
    /**
     * Navigate to main menu (F12 equivalent)
     */
    goToMenu() {
      navigate(ROUTES.MENU);
    },
    
    /**
     * Navigate to admin menu if user has permissions
     */
    goToAdminMenu() {
      navigate(ROUTES.ADMIN_MENU);
    },
    
    /**
     * Get current screen information
     */
    getCurrentScreenInfo() {
      const breadcrumbData = BREADCRUMB_PATHS.PATH_MAPPING[currentPath];
      return {
        path: currentPath,
        title: breadcrumbData?.title || 'Unknown Screen',
        transactionCode: breadcrumbData?.transactionCode || 'UNKNOWN',
        level: breadcrumbData?.level || 0,
        returnPath: NAVIGATION_FLOW.RETURN_PATHS[currentPath] || ROUTES.MENU,
      };
    },
    
    /**
     * Get breadcrumb trail for current path
     */
    getBreadcrumbTrail() {
      const trail = [];
      const currentBreadcrumb = BREADCRUMB_PATHS.PATH_MAPPING[currentPath];
      
      if (currentBreadcrumb) {
        // Add home breadcrumb
        trail.push({
          path: ROUTES.MENU,
          title: 'Home',
          transactionCode: 'COMEN01',
          level: 0,
          isActive: false,
        });
        
        // Add parent breadcrumbs if level > 1
        if (currentBreadcrumb.level > 1) {
          const returnPath = NAVIGATION_FLOW.RETURN_PATHS[currentPath];
          const parentBreadcrumb = BREADCRUMB_PATHS.PATH_MAPPING[returnPath];
          if (parentBreadcrumb) {
            trail.push({
              ...parentBreadcrumb,
              isActive: false,
            });
          }
        }
        
        // Add current breadcrumb
        trail.push({
          ...currentBreadcrumb,
          isActive: true,
        });
      }
      
      return trail;
    },
    
    /**
     * Check if current path requires admin privileges
     */
    requiresAdminRole() {
      return currentPath.includes('/admin') || 
             currentPath.includes('/users') || 
             currentPath === ROUTES.ADMIN_MENU;
    },
    
    /**
     * Get applicable function keys for current screen
     */
    getApplicableFunctionKeys() {
      return getApplicableFunctionKeys(currentPath);
    },
    
    /**
     * Get help text for current screen
     */
    getHelpText() {
      return generateHelpToolbarText(currentPath, {
        includeAlternatives: true,
        includeMobileInstructions: true,
        includeAccessibilityInfo: true,
      });
    },
  };
}

// ==========================================
// Browser Compatibility Configuration
// ==========================================

/**
 * Provides browser compatibility configuration and feature detection for
 * keyboard shortcuts, ensuring consistent behavior across different browsers
 * and platforms while handling browser-specific limitations.
 * 
 * @returns {Object} Browser compatibility configuration and feature detection results
 * 
 * @example
 * const browserConfig = getBrowserCompatibilityConfig();
 * if (browserConfig.features.supportsKeyboardAPI) {
 *   // Use advanced keyboard features
 * } else {
 *   // Use fallback keyboard handling
 * }
 */
export function getBrowserCompatibilityConfig() {
  const config = {
    // Browser detection
    browser: {
      name: getBrowserName(),
      version: getBrowserVersion(),
      isMobile: isMobileDevice(),
      isTouch: isTouchDevice(),
      supportsFullscreen: supportsFullscreen(),
    },
    
    // Feature detection
    features: {
      supportsKeyboardAPI: typeof KeyboardEvent !== 'undefined',
      supportsCustomEvents: typeof CustomEvent !== 'undefined',
      supportsVibration: 'vibrate' in navigator,
      supportsNotifications: 'Notification' in window,
      supportsServiceWorker: 'serviceWorker' in navigator,
      supportsWebAudio: 'AudioContext' in window || 'webkitAudioContext' in window,
      supportsGamepad: 'getGamepads' in navigator,
    },
    
    // Reserved keys by browser
    reservedKeys: {
      chrome: ['F1', 'F5', 'F11', 'F12', 'Ctrl+T', 'Ctrl+W', 'Ctrl+N'],
      firefox: ['F1', 'F5', 'F11', 'F12', 'Ctrl+T', 'Ctrl+W', 'Ctrl+N'],
      safari: ['F1', 'F5', 'F11', 'F12', 'Cmd+T', 'Cmd+W', 'Cmd+N'],
      edge: ['F1', 'F5', 'F11', 'F12', 'Ctrl+T', 'Ctrl+W', 'Ctrl+N'],
    },
    
    // Alternative key mappings
    alternativeKeys: {
      F1: 'Alt+H', // Help
      F5: 'Ctrl+R', // Refresh
      F11: 'Alt+F11', // Fullscreen
      F12: 'Ctrl+Esc', // Cancel
    },
    
    // Touch device configurations
    touchConfig: {
      minimumTouchTarget: 44, // pixels
      swipeThreshold: 50, // pixels
      longPressThreshold: 500, // milliseconds
      doubleTapThreshold: 300, // milliseconds
    },
    
    // Accessibility configurations
    accessibilityConfig: {
      screenReaderSupport: hasScreenReader(),
      highContrastMode: hasHighContrast(),
      reducedMotion: hasReducedMotion(),
      keyboardOnlyNavigation: preferKeyboardNavigation(),
    },
    
    // Performance configurations
    performanceConfig: {
      debounceDelay: 100, // milliseconds
      throttleDelay: 50, // milliseconds
      animationDuration: 200, // milliseconds
      transitionDuration: 150, // milliseconds
    },
  };
  
  return config;
}

/**
 * Detects current browser name.
 * 
 * @returns {string} Browser name
 */
function getBrowserName() {
  const userAgent = navigator.userAgent;
  
  if (userAgent.includes('Chrome') && !userAgent.includes('Edg')) {
    return 'chrome';
  } else if (userAgent.includes('Firefox')) {
    return 'firefox';
  } else if (userAgent.includes('Safari') && !userAgent.includes('Chrome')) {
    return 'safari';
  } else if (userAgent.includes('Edg')) {
    return 'edge';
  } else {
    return 'unknown';
  }
}

/**
 * Detects current browser version.
 * 
 * @returns {string} Browser version
 */
function getBrowserVersion() {
  const userAgent = navigator.userAgent;
  const match = userAgent.match(/(?:Chrome|Firefox|Safari|Edge)\/(\d+)/);
  return match ? match[1] : 'unknown';
}

/**
 * Detects if device is mobile.
 * 
 * @returns {boolean} True if mobile device
 */
function isMobileDevice() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

/**
 * Detects if device supports touch.
 * 
 * @returns {boolean} True if touch device
 */
function isTouchDevice() {
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
}

/**
 * Detects if browser supports fullscreen API.
 * 
 * @returns {boolean} True if fullscreen is supported
 */
function supportsFullscreen() {
  return document.fullscreenEnabled || document.webkitFullscreenEnabled || document.mozFullScreenEnabled;
}

/**
 * Detects if screen reader is likely active.
 * 
 * @returns {boolean} True if screen reader is detected
 */
function hasScreenReader() {
  return window.speechSynthesis && window.speechSynthesis.getVoices().length > 0;
}

/**
 * Detects if high contrast mode is active.
 * 
 * @returns {boolean} True if high contrast mode is detected
 */
function hasHighContrast() {
  return window.matchMedia && window.matchMedia('(prefers-contrast: high)').matches;
}

/**
 * Detects if user prefers reduced motion.
 * 
 * @returns {boolean} True if reduced motion is preferred
 */
function hasReducedMotion() {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Detects if user prefers keyboard navigation.
 * 
 * @returns {boolean} True if keyboard navigation is preferred
 */
function preferKeyboardNavigation() {
  return !isTouchDevice() || hasScreenReader();
}

// ==========================================
// Utility Functions
// ==========================================

/**
 * Announces message to screen readers using ARIA live regions.
 * 
 * @param {string} message - Message to announce
 * @param {string} priority - Priority level ('polite' or 'assertive')
 */
function announceToScreenReader(message, priority = 'polite') {
  const regionId = priority === 'assertive' ? 'alert-region' : 'status-region';
  let region = document.getElementById(regionId);
  
  if (!region) {
    region = document.createElement('div');
    region.id = regionId;
    region.setAttribute('aria-live', priority);
    region.setAttribute('aria-atomic', 'true');
    region.setAttribute('class', 'sr-only');
    region.style.position = 'absolute';
    region.style.width = '1px';
    region.style.height = '1px';
    region.style.overflow = 'hidden';
    region.style.clip = 'rect(1px, 1px, 1px, 1px)';
    document.body.appendChild(region);
  }
  
  region.textContent = message;
  
  // Clear the message after announcement
  setTimeout(() => {
    region.textContent = '';
  }, 1000);
}

/**
 * Debounces function calls to prevent excessive execution.
 * 
 * @param {function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {function} Debounced function
 */
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * Throttles function calls to limit execution frequency.
 * 
 * @param {function} func - Function to throttle
 * @param {number} limit - Time limit in milliseconds
 * @returns {function} Throttled function
 */
function throttle(func, limit) {
  let inThrottle;
  return function executedFunction(...args) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

// Export all functions as named exports
export {
  // Core exports are already exported above
  debounce,
  throttle,
  announceToScreenReader,
};