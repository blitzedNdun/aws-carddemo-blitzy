/**
 * CardDemo - Custom React Hook for Keyboard Shortcuts
 * 
 * This module provides comprehensive keyboard shortcut handling that preserves
 * original mainframe function key behavior patterns while enabling modern web
 * application interaction patterns. Implements F3=Exit, F7=Page Up, F8=Page Down,
 * and F12=Cancel functionality with browser compatibility fallbacks.
 * 
 * Maintains exact functional equivalence with original CICS PF key processing
 * while providing touch device support and accessibility compliance for
 * enterprise-grade user experience preservation.
 * 
 * Key Features:
 * - CICS function key mapping (F3, F7, F8, F12) with exact original behavior
 * - Browser compatibility fallbacks for reserved function keys
 * - Touch device support with on-screen button equivalents
 * - Accessibility compliance with ARIA labels and keyboard navigation
 * - Document-level event handling with proper cleanup
 * - Mobile-first responsive design patterns
 * 
 * @module useKeyboardShortcuts
 * @version 1.0.0
 * @author Blitzy Development Team
 * @copyright 2024 CardDemo Application
 */

import { useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { NavigationHelpers } from '../utils/navigationHelpers.js';
import { ALTERNATIVE_KEY_COMBINATIONS } from '../constants/KeyboardConstants.ts';
import { ROUTES } from '../constants/NavigationConstants.ts';

// ===================================================================
// KEYBOARD EVENT HANDLER CREATION
// ===================================================================

/**
 * Creates keyboard event handler for function key processing
 * 
 * Generates document-level keyboard event handlers that map mainframe
 * function keys to modern web application actions while preserving
 * exact CICS PF key behavior patterns and user experience.
 * 
 * @param {Object} handlerConfig - Configuration for keyboard event handling
 * @param {Function} handlerConfig.onExit - Handler for F3 exit functionality
 * @param {Function} handlerConfig.onPageUp - Handler for F7 page up functionality
 * @param {Function} handlerConfig.onPageDown - Handler for F8 page down functionality
 * @param {Function} handlerConfig.onCancel - Handler for F12 cancel functionality
 * @param {Function} handlerConfig.onSave - Handler for F5 save functionality
 * @param {boolean} handlerConfig.enableAlternatives - Whether to enable alternative key combinations
 * @param {boolean} handlerConfig.preventDefaults - Whether to prevent default browser behavior
 * @returns {Function} Keyboard event handler function for document attachment
 * 
 * @example
 * // Create keyboard handler for mainframe function keys
 * const keyboardHandler = createKeyboardEventHandler({
 *   onExit: () => navigateToMenu(),
 *   onPageUp: () => goToPreviousPage(),
 *   onPageDown: () => goToNextPage(),
 *   onCancel: () => cancelCurrentOperation(),
 *   enableAlternatives: true,
 *   preventDefaults: true
 * });
 */
export function createKeyboardEventHandler(handlerConfig) {
  const {
    onExit,
    onPageUp,
    onPageDown,
    onCancel,
    onSave,
    enableAlternatives = true,
    preventDefaults = true
  } = handlerConfig;

  return function keyboardEventHandler(event) {
    try {
      // Store original event properties for validation
      const keyCode = event.keyCode;
      const ctrlKey = event.ctrlKey;
      const shiftKey = event.shiftKey;
      const altKey = event.altKey;
      const key = event.key;

      // Flag to track if we should prevent default behavior
      let shouldPreventDefault = false;

      // Process function keys with exact mainframe behavior
      switch (keyCode) {
        case 114: // F3 Key - Exit/Back Navigation
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onExit && typeof onExit === 'function') {
              onExit();
            }
          }
          break;

        case 116: // F5 Key - Save/Refresh (Browser Reserved)
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onSave && typeof onSave === 'function') {
              onSave();
            }
          }
          break;

        case 118: // F7 Key - Page Up/Backward
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onPageUp && typeof onPageUp === 'function') {
              onPageUp();
            }
          }
          break;

        case 119: // F8 Key - Page Down/Forward
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onPageDown && typeof onPageDown === 'function') {
              onPageDown();
            }
          }
          break;

        case 123: // F12 Key - Cancel Operation (Browser Reserved)
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onCancel && typeof onCancel === 'function') {
              onCancel();
            }
          }
          break;

        case 27: // Escape Key - Quick Exit
          if (!ctrlKey && !shiftKey && !altKey) {
            shouldPreventDefault = true;
            if (onExit && typeof onExit === 'function') {
              onExit();
            }
          }
          break;

        default:
          // Handle alternative key combinations if enabled
          if (enableAlternatives) {
            const alternativeAction = handleAlternativeKeyCombinations(event, {
              onExit,
              onPageUp,
              onPageDown,
              onCancel,
              onSave
            });
            if (alternativeAction) {
              shouldPreventDefault = true;
            }
          }
          break;
      }

      // Prevent default browser behavior if we handled the key
      if (shouldPreventDefault && preventDefaults) {
        event.preventDefault();
        event.stopPropagation();
      }

      // Log keyboard event for debugging in development
      if (process.env.NODE_ENV === 'development' && shouldPreventDefault) {
        console.log('Keyboard shortcut handled:', {
          key,
          keyCode,
          ctrlKey,
          shiftKey,
          altKey,
          action: getActionForKeyCode(keyCode)
        });
      }

    } catch (error) {
      console.error('Keyboard event handler error:', error);
    }
  };
}

/**
 * Handles alternative key combinations for browser compatibility
 * 
 * Processes alternative keyboard combinations that provide equivalent
 * functionality when standard function keys are reserved by browsers.
 * Ensures consistent behavior across different platforms and browsers.
 * 
 * @param {KeyboardEvent} event - Keyboard event object
 * @param {Object} handlers - Action handler functions
 * @param {Function} handlers.onExit - Exit handler function
 * @param {Function} handlers.onPageUp - Page up handler function
 * @param {Function} handlers.onPageDown - Page down handler function
 * @param {Function} handlers.onCancel - Cancel handler function
 * @param {Function} handlers.onSave - Save handler function
 * @returns {boolean} Whether an alternative combination was handled
 * 
 * @example
 * // Handle alternative key combinations
 * const wasHandled = handleAlternativeKeyCombinations(event, {
 *   onExit: () => navigateToMenu(),
 *   onCancel: () => cancelOperation(),
 *   onSave: () => saveData()
 * });
 */
export function handleAlternativeKeyCombinations(event, handlers) {
  const { onExit, onPageUp, onPageDown, onCancel, onSave } = handlers;
  
  try {
    // Check Ctrl+R for F5 Save/Refresh alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.CTRL_R_FOR_F5.keyCode &&
        event.ctrlKey === ALTERNATIVE_KEY_COMBINATIONS.CTRL_R_FOR_F5.ctrlKey &&
        !event.shiftKey && !event.altKey) {
      if (onSave && typeof onSave === 'function') {
        onSave();
        return true;
      }
    }

    // Check Ctrl+S for F5 Save alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.CTRL_S_FOR_F5.keyCode &&
        event.ctrlKey === ALTERNATIVE_KEY_COMBINATIONS.CTRL_S_FOR_F5.ctrlKey &&
        !event.shiftKey && !event.altKey) {
      if (onSave && typeof onSave === 'function') {
        onSave();
        return true;
      }
    }

    // Check Ctrl+Escape for F12 Cancel alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.CTRL_ESC_FOR_F12.keyCode &&
        event.ctrlKey === ALTERNATIVE_KEY_COMBINATIONS.CTRL_ESC_FOR_F12.ctrlKey &&
        !event.shiftKey && !event.altKey) {
      if (onCancel && typeof onCancel === 'function') {
        onCancel();
        return true;
      }
    }

    // Check Alt+Left for F3 Back alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.ALT_LEFT_FOR_F3.keyCode &&
        event.altKey === ALTERNATIVE_KEY_COMBINATIONS.ALT_LEFT_FOR_F3.altKey &&
        !event.ctrlKey && !event.shiftKey) {
      if (onExit && typeof onExit === 'function') {
        onExit();
        return true;
      }
    }

    // Check Alt+E for Exit alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_E_EXIT.keyCode &&
        event.altKey === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_E_EXIT.altKey &&
        !event.ctrlKey && !event.shiftKey) {
      if (onExit && typeof onExit === 'function') {
        onExit();
        return true;
      }
    }

    // Check Alt+S for Save alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_S_SAVE.keyCode &&
        event.altKey === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_S_SAVE.altKey &&
        !event.ctrlKey && !event.shiftKey) {
      if (onSave && typeof onSave === 'function') {
        onSave();
        return true;
      }
    }

    // Check Alt+C for Cancel alternative
    if (event.keyCode === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_C_CANCEL.keyCode &&
        event.altKey === ALTERNATIVE_KEY_COMBINATIONS.ALT_COMBINATIONS.ALT_C_CANCEL.altKey &&
        !event.ctrlKey && !event.shiftKey) {
      if (onCancel && typeof onCancel === 'function') {
        onCancel();
        return true;
      }
    }

    return false;

  } catch (error) {
    console.error('Alternative key combination handler error:', error);
    return false;
  }
}

/**
 * Creates touch device handlers for mobile and tablet compatibility
 * 
 * Generates touch event handlers that provide function key equivalents
 * for mobile and tablet devices. Implements swipe gestures and touch
 * buttons that replicate mainframe function key behavior patterns.
 * 
 * @param {Object} touchConfig - Configuration for touch device handlers
 * @param {Function} touchConfig.onExit - Handler for exit/back action
 * @param {Function} touchConfig.onPageUp - Handler for page up action
 * @param {Function} touchConfig.onPageDown - Handler for page down action
 * @param {Function} touchConfig.onCancel - Handler for cancel action
 * @param {Function} touchConfig.onSave - Handler for save action
 * @param {boolean} touchConfig.enableSwipeGestures - Whether to enable swipe gestures
 * @param {boolean} touchConfig.enableTouchButtons - Whether to enable touch buttons
 * @param {Object} touchConfig.swipeThresholds - Swipe gesture thresholds
 * @returns {Object} Touch device handlers and utilities
 * 
 * @example
 * // Create touch handlers for mobile devices
 * const touchHandlers = createTouchDeviceHandlers({
 *   onExit: () => navigateToMenu(),
 *   onPageUp: () => goToPreviousPage(),
 *   onPageDown: () => goToNextPage(),
 *   onCancel: () => cancelOperation(),
 *   enableSwipeGestures: true,
 *   enableTouchButtons: true
 * });
 */
export function createTouchDeviceHandlers(touchConfig) {
  const {
    onExit,
    onPageUp,
    onPageDown,
    onCancel,
    onSave,
    enableSwipeGestures = true,
    enableTouchButtons = true,
    swipeThresholds = {
      minDistance: 50,
      maxTime: 300,
      minVelocity: 0.3
    }
  } = touchConfig;

  // Touch state tracking
  let touchStartX = 0;
  let touchStartY = 0;
  let touchStartTime = 0;
  let touchEndX = 0;
  let touchEndY = 0;
  let touchEndTime = 0;

  /**
   * Handles touch start events
   * @param {TouchEvent} event - Touch event object
   */
  const handleTouchStart = (event) => {
    try {
      if (event.touches.length === 1) {
        const touch = event.touches[0];
        touchStartX = touch.clientX;
        touchStartY = touch.clientY;
        touchStartTime = Date.now();
      }
    } catch (error) {
      console.error('Touch start handler error:', error);
    }
  };

  /**
   * Handles touch end events with swipe gesture detection
   * @param {TouchEvent} event - Touch event object
   */
  const handleTouchEnd = (event) => {
    try {
      if (event.changedTouches.length === 1) {
        const touch = event.changedTouches[0];
        touchEndX = touch.clientX;
        touchEndY = touch.clientY;
        touchEndTime = Date.now();

        if (enableSwipeGestures) {
          processSwipeGesture();
        }
      }
    } catch (error) {
      console.error('Touch end handler error:', error);
    }
  };

  /**
   * Processes swipe gestures and triggers appropriate actions
   */
  const processSwipeGesture = () => {
    try {
      const deltaX = touchEndX - touchStartX;
      const deltaY = touchEndY - touchStartY;
      const deltaTime = touchEndTime - touchStartTime;
      const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      const velocity = distance / deltaTime;

      // Check if swipe meets minimum requirements
      if (distance < swipeThresholds.minDistance || 
          deltaTime > swipeThresholds.maxTime || 
          velocity < swipeThresholds.minVelocity) {
        return;
      }

      // Determine swipe direction
      const isHorizontal = Math.abs(deltaX) > Math.abs(deltaY);
      
      if (isHorizontal) {
        if (deltaX > 0) {
          // Swipe right - Exit/Back (F3 equivalent)
          if (onExit && typeof onExit === 'function') {
            onExit();
          }
        } else {
          // Swipe left - Forward navigation (F8 equivalent)
          if (onPageDown && typeof onPageDown === 'function') {
            onPageDown();
          }
        }
      } else {
        if (deltaY < 0) {
          // Swipe up - Previous page (F7 equivalent)
          if (onPageUp && typeof onPageUp === 'function') {
            onPageUp();
          }
        } else {
          // Swipe down - Next page (F8 equivalent)
          if (onPageDown && typeof onPageDown === 'function') {
            onPageDown();
          }
        }
      }

    } catch (error) {
      console.error('Swipe gesture processing error:', error);
    }
  };

  /**
   * Creates touch button elements for on-screen function key equivalents
   * @returns {Object} Touch button configuration
   */
  const createTouchButtons = () => {
    if (!enableTouchButtons) {
      return {};
    }

    return {
      exitButton: {
        position: 'top-left',
        icon: '←',
        label: 'Back',
        ariaLabel: 'Go back to previous screen (F3 equivalent)',
        onClick: onExit,
        style: {
          position: 'fixed',
          top: '10px',
          left: '10px',
          zIndex: 1000,
          backgroundColor: '#007bff',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px',
          fontSize: '16px',
          minWidth: '44px',
          minHeight: '44px',
          cursor: 'pointer',
          touchAction: 'manipulation'
        }
      },
      saveButton: {
        position: 'top-right',
        icon: '💾',
        label: 'Save',
        ariaLabel: 'Save current data (F5 equivalent)',
        onClick: onSave,
        style: {
          position: 'fixed',
          top: '10px',
          right: '10px',
          zIndex: 1000,
          backgroundColor: '#28a745',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px',
          fontSize: '16px',
          minWidth: '44px',
          minHeight: '44px',
          cursor: 'pointer',
          touchAction: 'manipulation'
        }
      },
      cancelButton: {
        position: 'bottom-left',
        icon: '✕',
        label: 'Cancel',
        ariaLabel: 'Cancel current operation (F12 equivalent)',
        onClick: onCancel,
        style: {
          position: 'fixed',
          bottom: '10px',
          left: '10px',
          zIndex: 1000,
          backgroundColor: '#dc3545',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px',
          fontSize: '16px',
          minWidth: '44px',
          minHeight: '44px',
          cursor: 'pointer',
          touchAction: 'manipulation'
        }
      },
      pageUpButton: {
        position: 'bottom-center-left',
        icon: '↑',
        label: 'Page Up',
        ariaLabel: 'Go to previous page (F7 equivalent)',
        onClick: onPageUp,
        style: {
          position: 'fixed',
          bottom: '10px',
          left: '50%',
          transform: 'translateX(-60px)',
          zIndex: 1000,
          backgroundColor: '#6c757d',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px',
          fontSize: '16px',
          minWidth: '44px',
          minHeight: '44px',
          cursor: 'pointer',
          touchAction: 'manipulation'
        }
      },
      pageDownButton: {
        position: 'bottom-center-right',
        icon: '↓',
        label: 'Page Down',
        ariaLabel: 'Go to next page (F8 equivalent)',
        onClick: onPageDown,
        style: {
          position: 'fixed',
          bottom: '10px',
          left: '50%',
          transform: 'translateX(16px)',
          zIndex: 1000,
          backgroundColor: '#6c757d',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px',
          fontSize: '16px',
          minWidth: '44px',
          minHeight: '44px',
          cursor: 'pointer',
          touchAction: 'manipulation'
        }
      }
    };
  };

  return {
    handleTouchStart,
    handleTouchEnd,
    createTouchButtons,
    swipeGestureConfig: {
      enabled: enableSwipeGestures,
      thresholds: swipeThresholds
    }
  };
}

// ===================================================================
// MAIN KEYBOARD SHORTCUTS HOOK
// ===================================================================

/**
 * Custom React hook for comprehensive keyboard shortcuts
 * 
 * Provides enterprise-grade keyboard navigation that preserves original
 * mainframe function key behavior patterns while enabling modern web
 * application interaction patterns. Integrates with React component
 * lifecycle for proper event handling and cleanup.
 * 
 * @param {Object} options - Configuration options for keyboard shortcuts
 * @param {boolean} options.enableKeyboardShortcuts - Whether to enable keyboard shortcuts
 * @param {boolean} options.enableTouchSupport - Whether to enable touch device support
 * @param {boolean} options.enableAlternativeKeys - Whether to enable alternative key combinations
 * @param {Function} options.onExit - Handler for F3 exit functionality
 * @param {Function} options.onPageUp - Handler for F7 page up functionality
 * @param {Function} options.onPageDown - Handler for F8 page down functionality
 * @param {Function} options.onCancel - Handler for F12 cancel functionality
 * @param {Function} options.onSave - Handler for F5 save functionality
 * @param {Object} options.navigationContext - Current navigation context
 * @param {boolean} options.globalScope - Whether to attach handlers to document
 * @returns {Object} Keyboard shortcut utilities and handlers
 * 
 * @example
 * // Use keyboard shortcuts in React component
 * const {
 *   keyboardHandlers,
 *   touchHandlers,
 *   isKeyboardSupported,
 *   isTouchDevice
 * } = useKeyboardShortcuts({
 *   enableKeyboardShortcuts: true,
 *   enableTouchSupport: true,
 *   onExit: () => navigateToMenu(),
 *   onPageUp: () => goToPreviousPage(),
 *   onPageDown: () => goToNextPage(),
 *   onCancel: () => cancelOperation()
 * });
 */
function useKeyboardShortcuts(options = {}) {
  const {
    enableKeyboardShortcuts = true,
    enableTouchSupport = true,
    enableAlternativeKeys = true,
    onExit,
    onPageUp,
    onPageDown,
    onCancel,
    onSave,
    navigationContext = {},
    globalScope = true
  } = options;

  const navigate = useNavigate();

  // Create default handlers if not provided
  const defaultHandlers = {
    handleExit: useCallback(() => {
      if (onExit && typeof onExit === 'function') {
        onExit();
      } else {
        // Default F3 exit behavior - navigate to menu or previous screen
        NavigationHelpers.handleExit(navigate, navigationContext);
      }
    }, [navigate, navigationContext, onExit]),

    handlePageUp: useCallback(() => {
      if (onPageUp && typeof onPageUp === 'function') {
        onPageUp();
      } else {
        // Default F7 page up behavior - scroll to top or previous page
        if (window.history.length > 1) {
          window.history.back();
        } else {
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      }
    }, [onPageUp]),

    handlePageDown: useCallback(() => {
      if (onPageDown && typeof onPageDown === 'function') {
        onPageDown();
      } else {
        // Default F8 page down behavior - scroll to bottom or next page
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
      }
    }, [onPageDown]),

    handleCancel: useCallback(() => {
      if (onCancel && typeof onCancel === 'function') {
        onCancel();
      } else {
        // Default F12 cancel behavior - navigate to menu
        NavigationHelpers.handleExit(navigate, navigationContext);
      }
    }, [navigate, navigationContext, onCancel]),

    handleSave: useCallback(() => {
      if (onSave && typeof onSave === 'function') {
        onSave();
      } else {
        // Default F5 save behavior - trigger form submission if available
        const activeForm = document.activeElement?.form;
        if (activeForm) {
          activeForm.dispatchEvent(new Event('submit', { bubbles: true }));
        }
      }
    }, [onSave])
  };

  // Create keyboard event handler
  const keyboardHandler = useCallback(
    createKeyboardEventHandler({
      onExit: defaultHandlers.handleExit,
      onPageUp: defaultHandlers.handlePageUp,
      onPageDown: defaultHandlers.handlePageDown,
      onCancel: defaultHandlers.handleCancel,
      onSave: defaultHandlers.handleSave,
      enableAlternatives: enableAlternativeKeys,
      preventDefaults: true
    }),
    [defaultHandlers, enableAlternativeKeys]
  );

  // Create touch device handlers
  const touchHandlers = createTouchDeviceHandlers({
    onExit: defaultHandlers.handleExit,
    onPageUp: defaultHandlers.handlePageUp,
    onPageDown: defaultHandlers.handlePageDown,
    onCancel: defaultHandlers.handleCancel,
    onSave: defaultHandlers.handleSave,
    enableSwipeGestures: enableTouchSupport,
    enableTouchButtons: enableTouchSupport
  });

  // Device detection
  const isKeyboardSupported = !('ontouchstart' in window) || window.navigator.maxTouchPoints === 0;
  const isTouchDevice = 'ontouchstart' in window && window.navigator.maxTouchPoints > 0;

  // Set up keyboard event listeners
  useEffect(() => {
    if (!enableKeyboardShortcuts) {
      return;
    }

    if (globalScope) {
      document.addEventListener('keydown', keyboardHandler);
    }

    return () => {
      if (globalScope) {
        document.removeEventListener('keydown', keyboardHandler);
      }
    };
  }, [keyboardHandler, enableKeyboardShortcuts, globalScope]);

  // Set up touch event listeners
  useEffect(() => {
    if (!enableTouchSupport || !isTouchDevice) {
      return;
    }

    if (globalScope) {
      document.addEventListener('touchstart', touchHandlers.handleTouchStart);
      document.addEventListener('touchend', touchHandlers.handleTouchEnd);
    }

    return () => {
      if (globalScope) {
        document.removeEventListener('touchstart', touchHandlers.handleTouchStart);
        document.removeEventListener('touchend', touchHandlers.handleTouchEnd);
      }
    };
  }, [touchHandlers, enableTouchSupport, isTouchDevice, globalScope]);

  // Return hook utilities
  return {
    // Handler functions
    keyboardHandler,
    touchHandlers,
    defaultHandlers,

    // Device detection
    isKeyboardSupported,
    isTouchDevice,

    // Configuration
    enableKeyboardShortcuts,
    enableTouchSupport,
    enableAlternativeKeys,

    // Utility functions
    createKeyboardEventHandler,
    createTouchDeviceHandlers,
    handleAlternativeKeyCombinations,

    // Touch button configuration
    touchButtons: touchHandlers.createTouchButtons(),

    // Status indicators
    isActive: enableKeyboardShortcuts || enableTouchSupport,
    hasHandlers: Boolean(onExit || onPageUp || onPageDown || onCancel || onSave)
  };
}

// ===================================================================
// UTILITY FUNCTIONS
// ===================================================================

/**
 * Gets action name for key code
 * @param {number} keyCode - Key code to lookup
 * @returns {string} Action name for the key code
 */
function getActionForKeyCode(keyCode) {
  const actionMap = {
    114: 'EXIT',     // F3
    116: 'SAVE',     // F5
    118: 'PAGE_UP',  // F7
    119: 'PAGE_DOWN', // F8
    123: 'CANCEL',   // F12
    27: 'QUICK_EXIT' // Escape
  };
  return actionMap[keyCode] || 'UNKNOWN';
}

/**
 * Detects if device supports keyboard input
 * @returns {boolean} Whether device has keyboard support
 */
function detectKeyboardSupport() {
  return !('ontouchstart' in window) || window.navigator.maxTouchPoints === 0;
}

/**
 * Detects if device is touch-enabled
 * @returns {boolean} Whether device supports touch input
 */
function detectTouchSupport() {
  return 'ontouchstart' in window && window.navigator.maxTouchPoints > 0;
}

// ===================================================================
// EXPORTS
// ===================================================================

export {
  createKeyboardEventHandler,
  createTouchDeviceHandlers,
  handleAlternativeKeyCombinations,
  detectKeyboardSupport,
  detectTouchSupport
};

export default useKeyboardShortcuts;