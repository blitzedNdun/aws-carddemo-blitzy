/**
 * CardDemo - Keyboard Shortcuts Utility
 * 
 * Comprehensive keyboard shortcut utilities that preserve original 3270 function key behavior 
 * in React components, providing consistent navigation and operation controls across all 
 * BMS-converted screens with modern browser compatibility and touch device support.
 * 
 * This utility centralizes all keyboard interaction patterns from the original BMS screens,
 * maintaining exact functional equivalence with CICS terminal behavior while enabling
 * modern web accessibility standards and cross-platform compatibility.
 * 
 * Key Features:
 * - React custom hook (useKeyboardShortcuts) for standardized function key handling
 * - Function key mappings preserving original CICS PF key functionality
 * - Browser compatibility with fallback combinations for reserved keys
 * - Touch device support with gesture and button alternatives
 * - Consistent function key instruction display for help toolbars
 * - Full accessibility compliance with screen reader support
 * 
 * Original BMS Function Key Patterns Preserved:
 * - F3=Exit: Universal exit function returning to previous screen or main menu
 * - F7=Backward: Previous page navigation for paginated lists
 * - F8=Forward: Next page navigation for paginated lists  
 * - F12=Cancel: Cancel current operation and return without saving changes
 * - ENTER=Continue/Process: Submit forms and continue operations
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

import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  KeyboardConstants, 
  FUNCTION_KEYS, 
  ALTERNATIVE_KEY_COMBINATIONS,
  KEYBOARD_ACCESSIBILITY_CONFIG,
  TOUCH_DEVICE_EQUIVALENTS
} from '../constants/KeyboardConstants';
import { 
  NavigationConstants,
  ROUTES, 
  NAVIGATION_FLOW, 
  BREADCRUMB_PATHS
} from '../constants/NavigationConstants';
import { 
  MessageConstants,
  FUNCTION_KEY_HELP, 
  KEYBOARD_INSTRUCTIONS, 
  HELP_TEXT
} from '../constants/MessageConstants';

/**
 * Creates a comprehensive keyboard event handler with CICS function key behavior
 * 
 * Generates event handlers that capture keyboard events and map them to appropriate
 * actions based on the current screen context, preserving exact CICS PF key functionality
 * while providing modern browser compatibility and accessibility support.
 * 
 * @param {string} context - Screen context for determining valid function keys
 * @param {Object} options - Configuration options for event handling
 * @param {Function} options.onFunctionKey - Callback for function key actions
 * @param {Function} options.onAlternativeKey - Callback for alternative key combinations
 * @param {boolean} options.enablePreventDefault - Whether to prevent default browser behavior
 * @param {boolean} options.enableAccessibility - Whether to include accessibility features
 * @returns {Function} Configured keyboard event handler function
 * 
 * @example
 * // Usage in React component
 * const handleKeyboard = createKeyboardEventHandler('account_view', {
 *   onFunctionKey: (action, keyInfo) => console.log('Function key:', action),
 *   onAlternativeKey: (action, keyInfo) => console.log('Alternative key:', action),
 *   enablePreventDefault: true,
 *   enableAccessibility: true
 * });
 * 
 * // Attach to component
 * useEffect(() => {
 *   document.addEventListener('keydown', handleKeyboard);
 *   return () => document.removeEventListener('keydown', handleKeyboard);
 * }, [handleKeyboard]);
 */
export const createKeyboardEventHandler = (context, options = {}) => {
  const {
    onFunctionKey = () => {},
    onAlternativeKey = () => {},
    enablePreventDefault = true,
    enableAccessibility = true
  } = options;

  return (event) => {
    const { key, ctrlKey, altKey, shiftKey, metaKey } = event;
    
    // Check for primary function keys
    const functionKey = KeyboardConstants.getFunctionKeyByCode(key);
    if (functionKey && KeyboardConstants.isValidContext(functionKey.action, context)) {
      if (enablePreventDefault && functionKey.preventDefault) {
        event.preventDefault();
      }
      
      const keyInfo = {
        ...functionKey,
        originalEvent: event,
        context,
        timestamp: Date.now()
      };
      
      onFunctionKey(functionKey.action, keyInfo);
      
      // Announce to screen readers if accessibility enabled
      if (enableAccessibility && functionKey.accessibility) {
        announceToScreenReader(functionKey.accessibility.screenReaderText);
      }
      
      return true;
    }
    
    // Check for alternative key combinations
    const keySequence = [];
    if (ctrlKey) keySequence.push('Control');
    if (altKey) keySequence.push('Alt');
    if (shiftKey) keySequence.push('Shift');
    if (metaKey) keySequence.push('Meta');
    keySequence.push(event.code || key);
    
    const alternativeKey = KeyboardConstants.getAlternativeByKeySequence(keySequence);
    if (alternativeKey && alternativeKey.contexts?.includes(context)) {
      if (enablePreventDefault && alternativeKey.preventDefault) {
        event.preventDefault();
      }
      
      const keyInfo = {
        ...alternativeKey,
        originalEvent: event,
        context,
        keySequence,
        timestamp: Date.now()
      };
      
      onAlternativeKey(alternativeKey.action, keyInfo);
      
      // Announce to screen readers if accessibility enabled
      if (enableAccessibility && alternativeKey.accessibility) {
        announceToScreenReader(alternativeKey.accessibility.screenReaderText);
      }
      
      return true;
    }
    
    return false;
  };
};

/**
 * Gets available function key actions for a specific screen context
 * 
 * Retrieves all valid function key actions available in the current screen context,
 * providing information about key mappings, descriptions, and alternative combinations
 * for displaying in help text or UI components.
 * 
 * @param {string} context - Screen context to filter function keys
 * @param {Object} options - Optional filtering and formatting options
 * @param {boolean} options.includeAlternatives - Include alternative key combinations
 * @param {boolean} options.includeTouch - Include touch device alternatives
 * @param {string} options.format - Output format ('short', 'long', 'detailed')
 * @returns {Array} Array of available function key actions with metadata
 * 
 * @example
 * // Get function keys for transaction list screen
 * const actions = getFunctionKeyActions('transaction_list', {
 *   includeAlternatives: true,
 *   includeTouch: true,
 *   format: 'detailed'
 * });
 * 
 * // Returns:
 * // [
 * //   {
 * //     key: 'F3',
 * //     action: 'EXIT',
 * //     shortLabel: 'F3=Exit',
 * //     description: 'Exit current screen and return to previous level',
 * //     alternatives: ['Escape', 'Alt+X'],
 * //     touchEquivalent: { gesture: 'edge_swipe_left', button: 'exit_button' }
 * //   },
 * //   ...
 * // ]
 */
export const getFunctionKeyActions = (context, options = {}) => {
  const {
    includeAlternatives = false,
    includeTouch = false,
    format = 'short'
  } = options;
  
  const actions = [];
  
  // Get primary function keys for context
  Object.entries(FUNCTION_KEYS).forEach(([keyName, keyConfig]) => {
    if (keyConfig.contexts.includes(context)) {
      const action = {
        key: keyName,
        keyCode: keyConfig.keyCode,
        action: keyConfig.action,
        shortLabel: keyConfig.shortLabel,
        description: keyConfig.description,
        handler: keyConfig.handler
      };
      
      if (format === 'long' || format === 'detailed') {
        action.longLabel = keyConfig.longLabel;
        action.requiresConfirmation = keyConfig.requiresConfirmation;
        action.navigationPath = keyConfig.navigationPath;
      }
      
      if (format === 'detailed') {
        action.accessibility = keyConfig.accessibility;
        action.contexts = keyConfig.contexts;
        action.preventDefault = keyConfig.preventDefault;
      }
      
      // Add alternative key combinations if requested
      if (includeAlternatives) {
        action.alternatives = [];
        if (keyConfig.altKeyCode) {
          action.alternatives.push(keyConfig.altKeyCode);
        }
        
        // Find alternative combinations
        Object.values(ALTERNATIVE_KEY_COMBINATIONS).forEach(altConfig => {
          if (altConfig.equivalentTo === keyName && altConfig.contexts?.includes(context)) {
            action.alternatives.push(altConfig.keySequence || altConfig.keys?.join('+'));
          }
        });
      }
      
      // Add touch device alternatives if requested
      if (includeTouch) {
        action.touchEquivalent = {
          gesture: null,
          button: null
        };
        
        // Find gesture equivalent
        Object.entries(TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES).forEach(([gestureName, gestureConfig]) => {
          if (gestureConfig.equivalentTo === keyName && gestureConfig.contexts?.includes(context)) {
            action.touchEquivalent.gesture = gestureName.toLowerCase();
          }
        });
        
        // Find button equivalent
        Object.entries(TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS).forEach(([buttonName, buttonConfig]) => {
          if (buttonConfig.equivalentTo === keyName && buttonConfig.contexts?.includes(context)) {
            action.touchEquivalent.button = buttonName.toLowerCase();
          }
        });
      }
      
      actions.push(action);
    }
  });
  
  return actions;
};

/**
 * Creates touch device handlers for function key equivalents
 * 
 * Generates touch-friendly event handlers that provide equivalent functionality
 * to keyboard function keys for mobile and tablet devices, including swipe gestures,
 * touch buttons, and long press actions.
 * 
 * @param {string} context - Screen context for touch handlers
 * @param {Object} options - Configuration options for touch handling
 * @param {Function} options.onGesture - Callback for gesture actions
 * @param {Function} options.onButtonPress - Callback for button press actions
 * @param {boolean} options.enableHapticFeedback - Enable haptic feedback
 * @param {boolean} options.enableSwipeGestures - Enable swipe gesture detection
 * @returns {Object} Touch event handlers and configuration
 * 
 * @example
 * // Create touch handlers for card list screen
 * const touchHandlers = createTouchDeviceHandlers('card_list', {
 *   onGesture: (action, gestureInfo) => handleGestureAction(action),
 *   onButtonPress: (action, buttonInfo) => handleButtonAction(action),
 *   enableHapticFeedback: true,
 *   enableSwipeGestures: true
 * });
 * 
 * // Apply to container element
 * <div {...touchHandlers.gestureHandlers}>
 *   {touchHandlers.renderButtons()}
 * </div>
 */
export const createTouchDeviceHandlers = (context, options = {}) => {
  const {
    onGesture = () => {},
    onButtonPress = () => {},
    enableHapticFeedback = true,
    enableSwipeGestures = true
  } = options;
  
  const touchState = {
    startX: 0,
    startY: 0,
    startTime: 0,
    isSwipeActive: false
  };
  
  // Swipe gesture handlers
  const gestureHandlers = enableSwipeGestures ? {
    onTouchStart: (event) => {
      const touch = event.touches[0];
      touchState.startX = touch.clientX;
      touchState.startY = touch.clientY;
      touchState.startTime = Date.now();
      touchState.isSwipeActive = true;
    },
    
    onTouchMove: (event) => {
      if (!touchState.isSwipeActive) return;
      
      const touch = event.touches[0];
      const deltaX = touch.clientX - touchState.startX;
      const deltaY = touch.clientY - touchState.startY;
      
      // Prevent scrolling during horizontal swipes
      if (Math.abs(deltaX) > Math.abs(deltaY)) {
        event.preventDefault();
      }
    },
    
    onTouchEnd: (event) => {
      if (!touchState.isSwipeActive) return;
      
      const touch = event.changedTouches[0];
      const deltaX = touch.clientX - touchState.startX;
      const deltaY = touch.clientY - touchState.startY;
      const deltaTime = Date.now() - touchState.startTime;
      
      touchState.isSwipeActive = false;
      
      // Determine swipe direction and action
      const absX = Math.abs(deltaX);
      const absY = Math.abs(deltaY);
      
      if (absX > 50 && absX > absY && deltaTime < 500) {
        let gestureAction = null;
        let gestureConfig = null;
        
        if (deltaX > 0) {
          // Swipe right - check for backward action
          gestureConfig = TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_RIGHT;
          if (gestureConfig.contexts.includes(context)) {
            gestureAction = gestureConfig.action;
          }
        } else {
          // Swipe left - check for forward action
          gestureConfig = TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.SWIPE_LEFT;
          if (gestureConfig.contexts.includes(context)) {
            gestureAction = gestureConfig.action;
          }
        }
        
        if (gestureAction && gestureConfig) {
          if (enableHapticFeedback && gestureConfig.feedback === 'haptic' && navigator.vibrate) {
            navigator.vibrate(50); // Short vibration
          }
          
          onGesture(gestureAction, {
            ...gestureConfig,
            deltaX,
            deltaY,
            deltaTime,
            context
          });
        }
      }
      
      // Check for edge swipe (exit gesture)
      if (touchState.startX < 20 && deltaX > 50 && deltaTime < 300) {
        const edgeGesture = TOUCH_DEVICE_EQUIVALENTS.SWIPE_GESTURES.EDGE_SWIPE_LEFT;
        if (edgeGesture.contexts.includes(context)) {
          onGesture(edgeGesture.action, {
            ...edgeGesture,
            deltaX,
            deltaY,
            deltaTime,
            context,
            isEdgeSwipe: true
          });
        }
      }
    }
  } : {};
  
  // Touch button renderer
  const renderButtons = () => {
    const buttons = [];
    
    Object.entries(TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS).forEach(([buttonName, buttonConfig]) => {
      if (buttonConfig.contexts.includes(context) || buttonConfig.contexts.includes('all_screens')) {
        buttons.push({
          key: buttonName,
          ...buttonConfig,
          onClick: () => {
            if (enableHapticFeedback && navigator.vibrate) {
              navigator.vibrate(30); // Short vibration for button press
            }
            onButtonPress(buttonConfig.action, {
              ...buttonConfig,
              context,
              timestamp: Date.now()
            });
          }
        });
      }
    });
    
    return buttons;
  };
  
  return {
    gestureHandlers,
    renderButtons,
    touchState: () => ({ ...touchState })
  };
};

/**
 * Handles alternative key combinations for browser compatibility
 * 
 * Processes alternative keyboard combinations that provide equivalent functionality
 * to function keys that may be reserved by browsers or unavailable on certain
 * devices, ensuring consistent user experience across all platforms.
 * 
 * @param {KeyboardEvent} event - Keyboard event to process
 * @param {string} context - Current screen context
 * @param {Object} options - Processing options
 * @param {Function} options.onMatch - Callback when alternative combination is matched
 * @param {boolean} options.enableFallbacks - Enable fallback combinations
 * @returns {boolean} Whether an alternative combination was handled
 * 
 * @example
 * // Handle keyboard event with alternatives
 * const handled = handleAlternativeKeyCombinations(event, 'account_update', {
 *   onMatch: (action, keyInfo) => {
 *     switch(action) {
 *       case 'SAVE':
 *         handleSave();
 *         break;
 *       case 'CANCEL':
 *         handleCancel();
 *         break;
 *     }
 *   },
 *   enableFallbacks: true
 * });
 */
export const handleAlternativeKeyCombinations = (event, context, options = {}) => {
  const {
    onMatch = () => {},
    enableFallbacks = true
  } = options;
  
  const { key, ctrlKey, altKey, shiftKey, code } = event;
  
  // Build key sequence array
  const keySequence = [];
  if (ctrlKey) keySequence.push('Control');
  if (altKey) keySequence.push('Alt');
  if (shiftKey) keySequence.push('Shift');
  keySequence.push(code || key);
  
  // Check direct alternative combinations
  Object.entries(ALTERNATIVE_KEY_COMBINATIONS).forEach(([altName, altConfig]) => {
    if (Array.isArray(altConfig.keys) && 
        altConfig.keys.every(k => keySequence.includes(k)) &&
        altConfig.contexts?.includes(context)) {
      
      event.preventDefault();
      
      onMatch(altConfig.action, {
        ...altConfig,
        keySequence,
        originalEvent: event,
        context,
        timestamp: Date.now()
      });
      
      return true;
    }
  });
  
  // Check nested alternative combinations
  if (ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS) {
    Object.entries(ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS).forEach(([altName, altConfig]) => {
      if (Array.isArray(altConfig.keys) && 
          altConfig.keys.every(k => keySequence.includes(k)) &&
          altConfig.contexts?.includes(context)) {
        
        event.preventDefault();
        
        onMatch(altConfig.action, {
          ...altConfig,
          keySequence,
          originalEvent: event,
          context,
          timestamp: Date.now()
        });
        
        return true;
      }
    });
  }
  
  // Check browser-specific fallbacks if enabled
  if (enableFallbacks) {
    // Ctrl+R for Save (F5 alternative)
    if (ctrlKey && key === 'r' && ['form_save', 'account_update', 'card_update'].includes(context)) {
      event.preventDefault();
      onMatch('SAVE', {
        action: 'SAVE',
        keySequence: 'Ctrl+R',
        description: 'Save current changes',
        originalEvent: event,
        context,
        isFallback: true
      });
      return true;
    }
    
    // Ctrl+Esc for Cancel (F12 alternative)
    if (ctrlKey && key === 'Escape' && ['form_cancel', 'operation_cancel'].includes(context)) {
      event.preventDefault();
      onMatch('CANCEL', {
        action: 'CANCEL',
        keySequence: 'Ctrl+Esc',
        description: 'Cancel current operation',
        originalEvent: event,
        context,
        isFallback: true
      });
      return true;
    }
  }
  
  return false;
};

/**
 * Generates help toolbar text for function key instructions
 * 
 * Creates formatted help text strings for displaying function key instructions
 * in toolbar components, preserving exact BMS help text format while providing
 * modern accessibility and localization support.
 * 
 * @param {string} context - Screen context for relevant function keys
 * @param {Object} options - Formatting and content options
 * @param {string} options.format - Format style ('short', 'long', 'accessible')
 * @param {boolean} options.includeAlternatives - Include alternative key combinations
 * @param {string} options.separator - Separator between key instructions
 * @param {boolean} options.showKeyboardHints - Show keyboard navigation hints
 * @returns {string|Object} Formatted help text or structured help object
 * 
 * @example
 * // Generate short help text for account view
 * const helpText = generateHelpToolbarText('account_view', {
 *   format: 'short',
 *   separator: '  '
 * });
 * // Returns: "F3=Exit"
 * 
 * // Generate accessible help with alternatives
 * const accessibleHelp = generateHelpToolbarText('transaction_list', {
 *   format: 'accessible',
 *   includeAlternatives: true,
 *   showKeyboardHints: true
 * });
 * // Returns structured object with screen reader text and visual text
 */
export const generateHelpToolbarText = (context, options = {}) => {
  const {
    format = 'short',
    includeAlternatives = false,
    separator = '  ',
    showKeyboardHints = false
  } = options;
  
  // Get available function keys for context
  const availableKeys = getFunctionKeyActions(context, {
    includeAlternatives,
    format: format === 'accessible' ? 'detailed' : format
  });
  
  if (format === 'short') {
    // Generate concise BMS-style help text
    const keyTexts = availableKeys.map(key => key.shortLabel);
    return keyTexts.join(separator);
  }
  
  if (format === 'long') {
    // Generate descriptive help text
    const keyTexts = availableKeys.map(key => {
      let text = key.longLabel || key.shortLabel;
      if (includeAlternatives && key.alternatives?.length > 0) {
        text += ` (${key.alternatives.join(', ')})`;
      }
      return text;
    });
    return keyTexts.join(separator);
  }
  
  if (format === 'accessible') {
    // Generate structured accessible help
    const visualText = availableKeys.map(key => key.shortLabel).join(separator);
    const screenReaderText = availableKeys.map(key => {
      const alternatives = includeAlternatives && key.alternatives?.length > 0 
        ? ` or ${key.alternatives.join(' or ')}` 
        : '';
      return `${key.description}. Press ${key.keyCode}${alternatives}.`;
    }).join(' ');
    
    const keyboardHints = showKeyboardHints ? KEYBOARD_INSTRUCTIONS.BASIC_NAVIGATION : '';
    
    return {
      visualText,
      screenReaderText,
      keyboardHints,
      availableKeys,
      ariaLabel: `Function keys available: ${availableKeys.map(k => k.keyCode).join(', ')}`
    };
  }
  
  // Default to short format
  return generateHelpToolbarText(context, { ...options, format: 'short' });
};

/**
 * Creates navigation helper functions for React Router integration
 * 
 * Generates helper functions that integrate keyboard navigation with React Router,
 * providing seamless navigation that preserves CICS XCTL transfer patterns while
 * enabling modern single-page application behavior.
 * 
 * @param {Function} navigate - React Router navigate function
 * @param {Object} options - Navigation configuration options
 * @param {string} options.currentRoute - Current route path
 * @param {Object} options.routeState - Current route state
 * @param {Function} options.onNavigate - Callback before navigation
 * @param {boolean} options.preserveState - Preserve route state during navigation
 * @returns {Object} Navigation helper functions
 * 
 * @example
 * // Create navigation helpers in React component
 * const navigate = useNavigate();
 * const navHelpers = createNavigationHelper(navigate, {
 *   currentRoute: location.pathname,
 *   routeState: location.state,
 *   onNavigate: (action, path) => console.log('Navigating:', action, path),
 *   preserveState: true
 * });
 * 
 * // Use navigation helpers
 * navHelpers.handleExit(); // F3=Exit navigation
 * navHelpers.handleCancel(); // F12=Cancel navigation
 * navHelpers.navigateToRoute('/menu'); // Direct navigation
 */
export const createNavigationHelper = (navigate, options = {}) => {
  const {
    currentRoute = '',
    routeState = {},
    onNavigate = () => {},
    preserveState = true
  } = options;
  
  // Get exit path for current route
  const getExitPath = () => {
    return NavigationConstants.getExitPathForRoute(currentRoute);
  };
  
  // Get cancel path for current route (usually returns to previous screen)
  const getCancelPath = () => {
    if (routeState?.returnPath) {
      return routeState.returnPath;
    }
    
    // Determine cancel path based on route type
    if (currentRoute.includes('/update')) {
      return currentRoute.replace('/update', '/view');
    } else if (currentRoute.includes('/create') || currentRoute.includes('/add')) {
      return currentRoute.split('/').slice(0, -1).join('/');
    } else {
      return getExitPath();
    }
  };
  
  // Navigate with state preservation
  const navigateWithState = (path, action, additionalState = {}) => {
    onNavigate(action, path);
    
    const navigationState = preserveState ? {
      ...routeState,
      previousRoute: currentRoute,
      navigationAction: action,
      timestamp: Date.now(),
      ...additionalState
    } : additionalState;
    
    navigate(path, { state: navigationState });
  };
  
  return {
    // F3=Exit navigation handler
    handleExit: () => {
      const exitPath = getExitPath();
      navigateWithState(exitPath, 'EXIT', {
        exitReason: 'function_key',
        fromRoute: currentRoute
      });
    },
    
    // F12=Cancel navigation handler
    handleCancel: () => {
      const cancelPath = getCancelPath();
      navigateWithState(cancelPath, 'CANCEL', {
        cancelReason: 'function_key',
        fromRoute: currentRoute
      });
    },
    
    // Page navigation handlers for F7/F8
    handlePageBackward: (currentPage = 1) => {
      if (currentPage > 1) {
        const newPath = updatePageInPath(currentRoute, currentPage - 1);
        navigateWithState(newPath, 'PAGE_BACKWARD', {
          pageDirection: 'backward',
          fromPage: currentPage,
          toPage: currentPage - 1
        });
      }
    },
    
    handlePageForward: (currentPage = 1, maxPage = 1) => {
      if (currentPage < maxPage) {
        const newPath = updatePageInPath(currentRoute, currentPage + 1);
        navigateWithState(newPath, 'PAGE_FORWARD', {
          pageDirection: 'forward',
          fromPage: currentPage,
          toPage: currentPage + 1
        });
      }
    },
    
    // Direct route navigation
    navigateToRoute: (path, additionalState = {}) => {
      navigateWithState(path, 'NAVIGATE', additionalState);
    },
    
    // Menu option navigation
    navigateToMenuOption: (optionNumber) => {
      const menuConfig = NAVIGATION_FLOW.MENU_HIERARCHY.MAIN_MENU;
      const option = menuConfig.children.find(child => child.option === String(optionNumber));
      
      if (option) {
        navigateWithState(option.path, 'MENU_SELECTION', {
          menuOption: optionNumber,
          optionTitle: option.title
        });
      }
    },
    
    // Get available navigation paths
    getAvailablePaths: () => {
      const flowConfig = NAVIGATION_FLOW.XCTL_PATTERNS[`FROM_${currentRoute.replace('/', '').toUpperCase()}`];
      return flowConfig || [];
    },
    
    // Utility functions
    getCurrentBreadcrumb: () => {
      return NavigationConstants.getBreadcrumbForRoute(currentRoute);
    },
    
    isAdminRoute: () => {
      return NavigationConstants.isAdminRoute(currentRoute);
    },
    
    getExitPath,
    getCancelPath
  };
};

/**
 * Gets browser compatibility configuration for keyboard handling
 * 
 * Provides browser-specific configuration for optimal keyboard handling,
 * including function key availability, alternative combinations, and
 * platform-specific workarounds for consistent cross-browser experience.
 * 
 * @param {Object} options - Configuration detection options
 * @param {boolean} options.detectPlatform - Auto-detect user platform
 * @param {boolean} options.detectBrowser - Auto-detect user browser
 * @param {boolean} options.includeWorkarounds - Include browser-specific workarounds
 * @returns {Object} Browser compatibility configuration
 * 
 * @example
 * // Get compatibility config with auto-detection
 * const config = getBrowserCompatibilityConfig({
 *   detectPlatform: true,
 *   detectBrowser: true,
 *   includeWorkarounds: true
 * });
 * 
 * // Use config to adjust keyboard handling
 * if (!config.functionKeys.F5.available) {
 *   // Use alternative Ctrl+R for save
 *   setupAlternativeSaveHandler();
 * }
 */
export const getBrowserCompatibilityConfig = (options = {}) => {
  const {
    detectPlatform = true,
    detectBrowser = true,
    includeWorkarounds = true
  } = options;
  
  // Platform detection
  const platform = detectPlatform ? detectUserPlatform() : 'unknown';
  
  // Browser detection
  const browser = detectBrowser ? detectUserBrowser() : 'unknown';
  
  // Function key availability by browser/platform
  const functionKeyAvailability = {
    F1: { available: true, reserved: false },
    F2: { available: true, reserved: false },
    F3: { available: true, reserved: false },
    F4: { available: true, reserved: browser === 'chrome' }, // Address bar in Chrome
    F5: { available: false, reserved: true }, // Refresh in all browsers
    F6: { available: true, reserved: browser === 'firefox' }, // Location bar in Firefox
    F7: { available: true, reserved: false },
    F8: { available: true, reserved: false },
    F9: { available: true, reserved: browser === 'firefox' }, // Developer tools in Firefox
    F10: { available: true, reserved: platform === 'windows' }, // Menu bar activation
    F11: { available: false, reserved: true }, // Fullscreen in all browsers
    F12: { available: false, reserved: true } // Developer tools in all browsers
  };
  
  // Alternative combinations based on unavailable function keys
  const recommendedAlternatives = {
    F5_SAVE: platform === 'mac' ? ['Cmd+S', 'Ctrl+R'] : ['Ctrl+S', 'Ctrl+R'],
    F11_FULLSCREEN: ['Alt+F11'],
    F12_CANCEL: ['Ctrl+Esc', 'Escape']
  };
  
  // Browser-specific workarounds
  const workarounds = includeWorkarounds ? {
    preventF5Refresh: true,
    preventF11Fullscreen: false, // Usually handled by browser
    preventF12DevTools: false, // Cannot prevent, must use alternatives
    enableContextMenu: false, // Disable right-click if needed
    
    // Platform-specific adjustments
    macCommandKeySupport: platform === 'mac',
    windowsAltKeySupport: platform === 'windows',
    
    // Browser-specific fixes
    chromeF4Workaround: browser === 'chrome',
    firefoxF6Workaround: browser === 'firefox',
    safariKeyboardSupport: browser === 'safari'
  } : {};
  
  return {
    platform,
    browser,
    functionKeys: functionKeyAvailability,
    alternatives: recommendedAlternatives,
    workarounds,
    
    // Utility methods
    isKeyAvailable: (keyCode) => {
      const keyInfo = functionKeyAvailability[keyCode];
      return keyInfo ? keyInfo.available && !keyInfo.reserved : false;
    },
    
    getAlternativeFor: (keyCode) => {
      const altKey = `${keyCode}_SAVE` in recommendedAlternatives ? `${keyCode}_SAVE` :
                     `${keyCode}_CANCEL` in recommendedAlternatives ? `${keyCode}_CANCEL` :
                     `${keyCode}_FULLSCREEN` in recommendedAlternatives ? `${keyCode}_FULLSCREEN` : null;
      return altKey ? recommendedAlternatives[altKey] : [];
    },
    
    needsWorkaround: (keyCode) => {
      const keyInfo = functionKeyAvailability[keyCode];
      return keyInfo ? keyInfo.reserved : false;
    }
  };
};

/**
 * Announces text to screen readers for accessibility
 * 
 * @private
 * @param {string} text - Text to announce
 */
const announceToScreenReader = (text) => {
  const announcement = document.createElement('div');
  announcement.setAttribute('aria-live', 'polite');
  announcement.setAttribute('aria-atomic', 'true');
  announcement.className = 'sr-only';
  announcement.textContent = text;
  
  document.body.appendChild(announcement);
  
  // Remove after announcement
  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
};

/**
 * Detects user platform
 * 
 * @private
 * @returns {string} Platform identifier
 */
const detectUserPlatform = () => {
  const userAgent = navigator.userAgent.toLowerCase();
  
  if (userAgent.includes('mac')) return 'mac';
  if (userAgent.includes('win')) return 'windows';
  if (userAgent.includes('linux')) return 'linux';
  if (userAgent.includes('android')) return 'android';
  if (userAgent.includes('iphone') || userAgent.includes('ipad')) return 'ios';
  
  return 'unknown';
};

/**
 * Detects user browser
 * 
 * @private
 * @returns {string} Browser identifier
 */
const detectUserBrowser = () => {
  const userAgent = navigator.userAgent.toLowerCase();
  
  if (userAgent.includes('chrome') && !userAgent.includes('edge')) return 'chrome';
  if (userAgent.includes('firefox')) return 'firefox';
  if (userAgent.includes('safari') && !userAgent.includes('chrome')) return 'safari';
  if (userAgent.includes('edge')) return 'edge';
  if (userAgent.includes('opera')) return 'opera';
  
  return 'unknown';
};

/**
 * Updates page parameter in URL path
 * 
 * @private
 * @param {string} path - Current path
 * @param {number} page - New page number
 * @returns {string} Updated path with new page number
 */
const updatePageInPath = (path, page) => {
  const url = new URL(path, window.location.origin);
  url.searchParams.set('page', page.toString());
  return url.pathname + url.search;
};

// Export all utility functions for convenient importing
export default {
  createKeyboardEventHandler,
  getFunctionKeyActions,
  createTouchDeviceHandlers,
  handleAlternativeKeyCombinations,
  generateHelpToolbarText,
  createNavigationHelper,
  getBrowserCompatibilityConfig
};