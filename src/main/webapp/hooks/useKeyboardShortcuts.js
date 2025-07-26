/**
 * CardDemo - Keyboard Shortcuts React Hook
 * 
 * Custom React hook implementing mainframe function key behavior patterns through
 * JavaScript keydown event handlers, providing F3=Exit, F7=Page Up, F8=Page Down,
 * and F12=Cancel functionality with browser compatibility fallbacks and touch device
 * support for consistent navigation across all React components.
 * 
 * This hook preserves the original CICS terminal function key patterns while adapting
 * them for modern web browsers and mobile devices. It provides comprehensive keyboard
 * navigation support that maintains exact functional equivalence with the original
 * BMS screen navigation patterns found in COSGN00.bms, COMEN01.bms, and COCRDLI.bms.
 * 
 * Key CICS Function Key Patterns Preserved:
 * - F3=Exit: Universal exit function returning to previous screen or main menu
 * - F7=Backward: Previous page navigation for paginated lists (equivalent to Page Up)
 * - F8=Forward: Next page navigation for paginated lists (equivalent to Page Down)
 * - F12=Cancel: Cancel current operation and return without saving changes
 * - Alternative combinations: Ctrl+R for F5, Ctrl+Esc for F12 to avoid browser conflicts
 * 
 * Browser Compatibility Features:
 * - Function key conflict resolution for browsers that reserve F5 (refresh) and F12 (dev tools)
 * - Fallback keyboard combinations using Ctrl and Alt modifiers
 * - Touch device support with on-screen buttons for mobile and tablet users
 * - Event prevention handling to avoid browser default behaviors
 * 
 * React Integration Patterns:
 * - useEffect for document-level keyboard event listeners with proper cleanup
 * - useCallback for stable event handler references to prevent unnecessary re-renders
 * - useNavigate integration for programmatic navigation equivalent to CICS XCTL
 * - Custom event dispatching for component-specific keyboard handling
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

import { useCallback, useEffect, useState, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

// Import navigation utilities for F3=Exit functionality and state management
import NavigationHelpers from '../utils/navigationHelpers.js';

// Import keyboard constants for function key mapping and alternative combinations
import { 
  ALTERNATIVE_KEY_COMBINATIONS,
  FUNCTION_KEYS,
  TOUCH_DEVICE_EQUIVALENTS,
  getFunctionKeyByCode,
  getAlternativeByKeySequence,
  isValidContext,
  buildKeyboardEventHandler
} from '../constants/KeyboardConstants.ts';

// Import navigation constants for F3=Exit routing and breadcrumb management
import { 
  ROUTES,
  NAVIGATION_FLOW,
  EXIT_PATHS
} from '../constants/NavigationConstants.ts';

/**
 * Touch Device Detection Utilities
 * 
 * Provides comprehensive touch device detection for enabling mobile-specific
 * keyboard alternatives and gesture support. Handles various device types
 * including smartphones, tablets, and hybrid devices with touch capabilities.
 */
const detectTouchDevice = () => {
  return (
    'ontouchstart' in window ||
    navigator.maxTouchPoints > 0 ||
    navigator.msMaxTouchPoints > 0 ||
    /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
  );
};

const getDeviceType = () => {
  const userAgent = navigator.userAgent.toLowerCase();
  const isMobile = /android|iphone|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
  const isTablet = /ipad|android(?!.*mobile)/i.test(userAgent);
  
  return {
    isMobile,
    isTablet,
    isTouch: detectTouchDevice(),
    isDesktop: !isMobile && !isTablet
  };
};

/**
 * Keyboard Event Utilities
 * 
 * Internal utility functions for keyboard event processing, modifier key
 * detection, and event handling coordination. These functions support the
 * main hook functionality and provide consistent event processing patterns.
 */
const isModifierPressed = (event, modifiers) => {
  return modifiers.every(modifier => {
    switch (modifier) {
      case 'Control':
        return event.ctrlKey;
      case 'Alt':
        return event.altKey;
      case 'Shift':
        return event.shiftKey;
      case 'Meta':
        return event.metaKey;
      default:
        return event.key === modifier || event.code === modifier;
    }
  });
};

const createKeyEventHandler = (handlers, options = {}) => {
  const { enableLogging = false, context = 'general' } = options;
  
  return (event) => {
    // Check for function keys first (F3, F7, F8, F12)
    const functionKey = getFunctionKeyByCode(event.key);
    
    if (functionKey && isValidContext(functionKey.action, context)) {
      if (enableLogging) {
        console.log(`Function key pressed: ${event.key} -> ${functionKey.action}`);
      }
      
      if (functionKey.preventDefault) {
        event.preventDefault();
        event.stopPropagation();
      }
      
      const handler = handlers[functionKey.handler];
      if (handler && typeof handler === 'function') {
        handler(event, functionKey);
        return;
      }
    }
    
    // Check for alternative key combinations (Ctrl+R, Ctrl+Esc, etc.)
    const pressedKeys = [];
    if (event.ctrlKey) pressedKeys.push('Control');
    if (event.altKey) pressedKeys.push('Alt');
    if (event.shiftKey) pressedKeys.push('Shift');
    pressedKeys.push(event.code);
    
    const alternative = getAlternativeByKeySequence(pressedKeys);
    
    if (alternative && isValidContext(alternative.action, context)) {
      if (enableLogging) {
        console.log(`Alternative key combination pressed: ${alternative.keySequence} -> ${alternative.action}`);
      }
      
      if (alternative.preventDefault) {
        event.preventDefault();
        event.stopPropagation();
      }
      
      const handler = handlers[alternative.handler];
      if (handler && typeof handler === 'function') {
        handler(event, alternative);
        return;
      }
    }
  };
};

/**
 * createKeyboardEventHandler - External Function Key Event Handler Factory
 * 
 * Exported function for creating standardized keyboard event handlers that can
 * be used by individual React components for custom keyboard behavior. Provides
 * the same function key processing logic as the main hook but in a reusable form.
 * 
 * @param {Object} handlers - Object mapping handler names to callback functions
 * @param {Object} options - Configuration options for event handling
 * @returns {Function} Keyboard event handler function
 */
export const createKeyboardEventHandler = (handlers = {}, options = {}) => {
  const {
    context = 'general',
    enableLogging = false,
    preventDefaultKeys = ['F3', 'F7', 'F8', 'F12'],
    enableAlternatives = true
  } = options;
  
  return createKeyEventHandler(handlers, { enableLogging, context });
};

/**
 * createTouchDeviceHandlers - Touch Device Support Functions
 * 
 * Exported function for creating touch-friendly alternatives to keyboard shortcuts.
 * Provides gesture recognition, touch button handlers, and mobile navigation patterns
 * that replicate function key behavior on devices without physical keyboards.
 * 
 * @param {Object} handlers - Object mapping actions to callback functions
 * @param {Object} options - Touch configuration options
 * @returns {Object} Touch event handlers and configuration
 */
export const createTouchDeviceHandlers = (handlers = {}, options = {}) => {
  const {
    enableSwipeGestures = true,
    enableTouchButtons = true,
    swipeSensitivity = 50,
    buttonSize = 'medium'
  } = options;
  
  const deviceInfo = getDeviceType();
  
  // Swipe gesture handlers for pagination (F7/F8 replacement)
  const swipeHandlers = enableSwipeGestures ? {
    handleSwipeLeft: (event) => {
      if (handlers.handlePageForward) {
        handlers.handlePageForward(event, { action: 'PAGE_FORWARD', source: 'swipe' });
      }
    },
    
    handleSwipeRight: (event) => {
      if (handlers.handlePageBackward) {
        handlers.handlePageBackward(event, { action: 'PAGE_BACKWARD', source: 'swipe' });
      }
    },
    
    handleEdgeSwipeLeft: (event) => {
      if (handlers.handleExit) {
        handlers.handleExit(event, { action: 'EXIT', source: 'edge_swipe' });
      }
    }
  } : {};
  
  // Touch button configurations for function key alternatives
  const touchButtons = enableTouchButtons ? {
    exitButton: {
      ...TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.EXIT_BUTTON,
      onClick: (event) => {
        if (handlers.handleExit) {
          handlers.handleExit(event, { action: 'EXIT', source: 'touch_button' });
        }
      }
    },
    
    prevPageButton: {
      ...TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.PREV_PAGE_BUTTON,
      onClick: (event) => {
        if (handlers.handlePageBackward) {
          handlers.handlePageBackward(event, { action: 'PAGE_BACKWARD', source: 'touch_button' });
        }
      }
    },
    
    nextPageButton: {
      ...TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.NEXT_PAGE_BUTTON,
      onClick: (event) => {
        if (handlers.handlePageForward) {
          handlers.handlePageForward(event, { action: 'PAGE_FORWARD', source: 'touch_button' });
        }
      }
    },
    
    cancelButton: {
      ...TOUCH_DEVICE_EQUIVALENTS.TOUCH_BUTTONS.CANCEL_BUTTON,
      onClick: (event) => {
        if (handlers.handleCancel) {
          handlers.handleCancel(event, { action: 'CANCEL', source: 'touch_button' });
        }
      }
    }
  } : {};
  
  // Gesture detection utilities
  const gestureRecognizer = {
    startX: 0,
    startY: 0,
    threshold: swipeSensitivity,
    
    onTouchStart: (event) => {
      const touch = event.touches[0];
      gestureRecognizer.startX = touch.clientX;
      gestureRecognizer.startY = touch.clientY;
    },
    
    onTouchEnd: (event) => {
      const touch = event.changedTouches[0];
      const deltaX = touch.clientX - gestureRecognizer.startX;
      const deltaY = touch.clientY - gestureRecognizer.startY;
      
      // Horizontal swipes for pagination
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > gestureRecognizer.threshold) {
        if (deltaX > 0) {
          swipeHandlers.handleSwipeRight && swipeHandlers.handleSwipeRight(event);
        } else {
          swipeHandlers.handleSwipeLeft && swipeHandlers.handleSwipeLeft(event);
        }
      }
      
      // Edge swipe detection for exit functionality
      if (gestureRecognizer.startX < 20 && deltaX > gestureRecognizer.threshold) {
        swipeHandlers.handleEdgeSwipeLeft && swipeHandlers.handleEdgeSwipeLeft(event);
      }
    }
  };
  
  return {
    deviceInfo,
    swipeHandlers,
    touchButtons,
    gestureRecognizer,
    isTouch: deviceInfo.isTouch,
    isMobile: deviceInfo.isMobile,
    isTablet: deviceInfo.isTablet
  };
};

/**
 * handleAlternativeKeyCombinations - Alternative Key Sequence Processor
 * 
 * Exported function for processing alternative keyboard combinations when standard
 * function keys are not available or conflict with browser shortcuts. Handles
 * combinations like Ctrl+R for F5 and Ctrl+Esc for F12 with proper validation.
 * 
 * @param {KeyboardEvent} event - Browser keyboard event object
 * @param {Object} handlers - Object mapping actions to callback functions
 * @param {Object} options - Processing options and configuration
 * @returns {boolean} Whether an alternative combination was processed
 */
export const handleAlternativeKeyCombinations = (event, handlers = {}, options = {}) => {
  const { context = 'general', enableLogging = false } = options;
  
  // Extract pressed key combination
  const pressedKeys = [];
  if (event.ctrlKey) pressedKeys.push('Control');
  if (event.altKey) pressedKeys.push('Alt');
  if (event.shiftKey) pressedKeys.push('Shift');
  if (event.metaKey) pressedKeys.push('Meta');
  pressedKeys.push(event.code);
  
  // Check against defined alternative combinations
  const alternatives = [
    ALTERNATIVE_KEY_COMBINATIONS.CTRL_R_FOR_F5,
    ALTERNATIVE_KEY_COMBINATIONS.CTRL_ESC_FOR_F12,
    ...Object.values(ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS)
  ];
  
  for (const alternative of alternatives) {
    if (alternative.keys && alternative.keys.every(key => pressedKeys.includes(key))) {
      if (!isValidContext(alternative.action, context)) {
        continue;
      }
      
      if (enableLogging) {
        console.log(`Alternative combination processed: ${alternative.keySequence} -> ${alternative.action}`);
      }
      
      if (alternative.preventDefault) {
        event.preventDefault();
        event.stopPropagation();
      }
      
      const handler = handlers[alternative.handler];
      if (handler && typeof handler === 'function') {
        handler(event, alternative);
        return true;
      }
    }
  }
  
  return false;
};

/**
 * useKeyboardShortcuts - Main React Hook for Keyboard Navigation
 * 
 * Primary custom React hook providing comprehensive keyboard navigation functionality
 * for React components. Implements document-level keyboard event listeners with proper
 * cleanup, integrates with React Router for navigation, and provides touch device
 * support for consistent user experience across all device types.
 * 
 * This hook serves as the central keyboard navigation controller for the entire
 * CardDemo application, ensuring consistent function key behavior across all
 * React components while preserving the original CICS terminal user experience.
 * 
 * Key Features:
 * - Document-level keyboard event handling with automatic cleanup
 * - React Router integration for F3=Exit navigation
 * - Touch device detection and alternative input methods
 * - Browser compatibility with alternative key combinations  
 * - Component-specific keyboard context management
 * - Custom event dispatching for specialized component handling
 * - Session state preservation during navigation
 * 
 * @param {Object} options - Hook configuration options
 * @returns {Object} Keyboard navigation functions and state
 */
const useKeyboardShortcuts = (options = {}) => {
  const {
    enabled = true,
    context = 'general',
    enableLogging = false,
    enableTouchSupport = true,
    enableCustomEvents = true,
    preventDefaultBehavior = true
  } = options;
  
  // React Router hooks for navigation functionality
  const navigate = useNavigate();
  const location = useLocation();
  
  // Hook state for keyboard and touch management
  const [isKeyboardEnabled, setIsKeyboardEnabled] = useState(enabled);
  const [deviceInfo, setDeviceInfo] = useState(() => getDeviceType());
  const [activeKeys, setActiveKeys] = useState(new Set());
  
  // Refs for stable event handler references
  const keyboardHandlersRef = useRef({});
  const touchHandlersRef = useRef({});
  const cleanupFunctionsRef = useRef([]);
  
  /**
   * Core Keyboard Event Handlers
   * 
   * These handlers implement the primary function key behaviors matching
   * original CICS terminal patterns while integrating with modern React
   * Router navigation and state management.
   */
  
  // F3=Exit Handler - Universal exit functionality
  const handleExit = useCallback((event, keyInfo = {}) => {
    if (enableLogging) {
      console.log('F3=Exit key pressed', { currentRoute: location.pathname, keyInfo });
    }
    
    try {
      // Use NavigationHelpers for F3=Exit functionality
      const exitResult = NavigationHelpers.handleExit(navigate, location);
      
      // Dispatch custom event for component-specific handling
      if (enableCustomEvents) {
        window.dispatchEvent(new CustomEvent('keyboard-exit', {
          detail: {
            currentRoute: location.pathname,
            exitResult,
            keyInfo,
            timestamp: new Date().toISOString()
          }
        }));
      }
      
      // Preserve current screen state before navigation
      const currentScreenData = {
        route: location.pathname,
        state: location.state,
        timestamp: new Date().toISOString()
      };
      
      NavigationHelpers.preserveState(currentScreenData);
      
      return exitResult;
      
    } catch (error) {
      console.error('Error handling F3=Exit:', error);
      
      // Fallback to menu navigation
      navigate(ROUTES.MENU, { 
        replace: false,
        state: { 
          error: error.message,
          fallback: true,
          previousRoute: location.pathname
        }
      });
    }
  }, [navigate, location, enableLogging, enableCustomEvents]);
  
  // F7=Page Backward Handler - Previous page navigation
  const handlePageBackward = useCallback((event, keyInfo = {}) => {
    if (enableLogging) {
      console.log('F7=Page Backward key pressed', { currentRoute: location.pathname, keyInfo });
    }
    
    // Dispatch custom event for component-specific pagination handling
    if (enableCustomEvents) {
      window.dispatchEvent(new CustomEvent('page-backward', {
        detail: {
          currentRoute: location.pathname,
          keyInfo,
          timestamp: new Date().toISOString()
        }
      }));
    }
    
    // Components will listen for this event to handle their specific pagination logic
    return { action: 'PAGE_BACKWARD', handled: true };
    
  }, [location, enableLogging, enableCustomEvents]);
  
  // F8=Page Forward Handler - Next page navigation
  const handlePageForward = useCallback((event, keyInfo = {}) => {
    if (enableLogging) {
      console.log('F8=Page Forward key pressed', { currentRoute: location.pathname, keyInfo });
    }
    
    // Dispatch custom event for component-specific pagination handling
    if (enableCustomEvents) {
      window.dispatchEvent(new CustomEvent('page-forward', {
        detail: {
          currentRoute: location.pathname,
          keyInfo,
          timestamp: new Date().toISOString()
        }
      }));
    }
    
    // Components will listen for this event to handle their specific pagination logic
    return { action: 'PAGE_FORWARD', handled: true };
    
  }, [location, enableLogging, enableCustomEvents]);
  
  // F12=Cancel Handler - Cancel current operation
  const handleCancel = useCallback((event, keyInfo = {}) => {
    if (enableLogging) {
      console.log('F12=Cancel key pressed', { currentRoute: location.pathname, keyInfo });
    }
    
    // Dispatch custom event for component-specific cancel handling
    if (enableCustomEvents) {
      window.dispatchEvent(new CustomEvent('operation-cancel', {
        detail: {
          currentRoute: location.pathname,
          keyInfo,
          timestamp: new Date().toISOString()
        }
      }));
    }
    
    // For forms and edit operations, clear any pending changes
    try {
      // Clear form data from session storage
      const formKeys = Object.keys(sessionStorage).filter(key => key.startsWith('form_data_'));
      formKeys.forEach(key => sessionStorage.removeItem(key));
      
      // Navigate back using the same logic as F3=Exit but with cancel context
      return handleExit(event, { ...keyInfo, action: 'CANCEL' });
      
    } catch (error) {
      console.error('Error handling F12=Cancel:', error);
      return { action: 'CANCEL', error: error.message };
    }
  }, [handleExit, location, enableLogging, enableCustomEvents]);
  
  // Alternative key combination handlers
  const handleSave = useCallback((event, keyInfo = {}) => {
    if (enableLogging) {
      console.log('Save key combination pressed (Ctrl+R or Alt+S)', { keyInfo });
    }
    
    if (enableCustomEvents) {
      window.dispatchEvent(new CustomEvent('save-operation', {
        detail: {
          currentRoute: location.pathname,
          keyInfo,
          timestamp: new Date().toISOString()
        }
      }));
    }
    
    return { action: 'SAVE', handled: true };
  }, [location, enableLogging, enableCustomEvents]);
  
  /**
   * Keyboard Event Listener Setup
   * 
   * Sets up document-level keyboard event listeners with proper cleanup
   * using useEffect. Handles both function keys and alternative combinations
   * while managing browser compatibility and preventing default behaviors.
   */
  
  useEffect(() => {
    if (!isKeyboardEnabled) return;
    
    // Create consolidated keyboard handlers object
    keyboardHandlersRef.current = {
      handleExit,
      handlePageBackward,
      handlePageForward,
      handleCancel,
      handleSave
    };
    
    // Create main keyboard event handler
    const keyboardEventHandler = createKeyEventHandler(
      keyboardHandlersRef.current,
      { 
        enableLogging, 
        context,
        preventDefaultKeys: preventDefaultBehavior ? ['F3', 'F7', 'F8', 'F12'] : []
      }
    );
    
    // Key down event listener
    const handleKeyDown = (event) => {
      // Track active keys for combination detection
      setActiveKeys(prev => new Set([...prev, event.key]));
      
      // Process keyboard event
      keyboardEventHandler(event);
    };
    
    // Key up event listener for cleanup
    const handleKeyUp = (event) => {
      setActiveKeys(prev => {
        const newSet = new Set(prev);
        newSet.delete(event.key);
        return newSet;
      });
    };
    
    // Add event listeners to document
    document.addEventListener('keydown', handleKeyDown, { passive: false });
    document.addEventListener('keyup', handleKeyUp, { passive: true });
    
    // Store cleanup functions
    cleanupFunctionsRef.current.push(() => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('keyup', handleKeyUp);
    });
    
    // Cleanup function
    return () => {
      cleanupFunctionsRef.current.forEach(cleanup => cleanup());
      cleanupFunctionsRef.current = [];
    };
    
  }, [
    isKeyboardEnabled,
    handleExit,
    handlePageBackward, 
    handlePageForward,
    handleCancel,
    handleSave,
    enableLogging,
    context,
    preventDefaultBehavior
  ]);
  
  /**
   * Touch Device Support Setup
   * 
   * Initializes touch device detection and creates touch-friendly alternatives
   * for keyboard shortcuts when running on mobile or tablet devices.
   */
  
  useEffect(() => {
    if (!enableTouchSupport) return;
    
    // Update device info
    const currentDeviceInfo = getDeviceType();
    setDeviceInfo(currentDeviceInfo);
    
    if (currentDeviceInfo.isTouch) {
      // Create touch handlers for gesture support
      touchHandlersRef.current = createTouchDeviceHandlers(
        keyboardHandlersRef.current,
        {
          enableSwipeGestures: true,
          enableTouchButtons: true,
          swipeSensitivity: 50
        }
      );
      
      if (enableLogging) {
        console.log('Touch device detected, enabling gesture support', currentDeviceInfo);
      }
    }
    
  }, [enableTouchSupport, enableLogging]);
  
  /**
   * Window Focus Management
   * 
   * Handles window focus/blur events to ensure keyboard shortcuts only
   * work when the application window is active, preventing conflicts
   * with other applications or browser tabs.
   */
  
  useEffect(() => {
    const handleWindowFocus = () => {
      setIsKeyboardEnabled(enabled);
    };
    
    const handleWindowBlur = () => {
      setIsKeyboardEnabled(false);
      setActiveKeys(new Set()); // Clear active keys when window loses focus
    };
    
    window.addEventListener('focus', handleWindowFocus);
    window.addEventListener('blur', handleWindowBlur);
    
    return () => {
      window.removeEventListener('focus', handleWindowFocus);
      window.removeEventListener('blur', handleWindowBlur);
    };
  }, [enabled]);
  
  /**
   * Hook Return Object
   * 
   * Returns comprehensive keyboard navigation functionality and state
   * for consuming React components. Provides both direct handler access
   * and utility functions for custom keyboard behavior implementation.
   */
  
  return {
    // Primary function key handlers
    handleExit,
    handlePageBackward,
    handlePageForward,
    handleCancel,
    handleSave,
    
    // Utility functions
    enableKeyboard: () => setIsKeyboardEnabled(true),
    disableKeyboard: () => setIsKeyboardEnabled(false),
    toggleKeyboard: () => setIsKeyboardEnabled(prev => !prev),
    
    // State information
    isEnabled: isKeyboardEnabled,
    deviceInfo,
    activeKeys: Array.from(activeKeys),
    currentContext: context,
    
    // Touch support information
    touchHandlers: touchHandlersRef.current,
    isTouch: deviceInfo.isTouch,
    isMobile: deviceInfo.isMobile,
    isTablet: deviceInfo.isTablet,
    
    // Navigation information
    currentRoute: location.pathname,
    currentState: location.state,
    
    // Alternative combination handler
    processAlternativeKeys: (event) => handleAlternativeKeyCombinations(
      event,
      keyboardHandlersRef.current,
      { context, enableLogging }
    ),
    
    // Custom event dispatcher for advanced usage
    dispatchKeyboardEvent: (eventType, detail = {}) => {
      if (enableCustomEvents) {
        window.dispatchEvent(new CustomEvent(eventType, {
          detail: {
            ...detail,
            currentRoute: location.pathname,
            timestamp: new Date().toISOString()
          }
        }));
      }
    }
  };
};

// Export the main hook as default
export default useKeyboardShortcuts;