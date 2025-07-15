/**
 * useKeyboardShortcuts.js
 * 
 * Custom React hook implementing mainframe function key behavior patterns through JavaScript
 * keydown event handlers. Provides F3=Exit, F7=Page Up, F8=Page Down, and F12=Cancel functionality
 * with browser compatibility fallbacks and touch device support for consistent navigation
 * across all React components.
 * 
 * This hook preserves the original CICS PF key functionality while providing modern web
 * application keyboard shortcuts. It implements document-level event listeners with proper
 * cleanup and supports alternative key combinations for browsers that reserve certain
 * function keys.
 * 
 * Key Features:
 * - Complete CICS PF key mapping (F3, F7, F8, F12) with exact behavior preservation
 * - Browser compatibility fallbacks for reserved function keys
 * - Touch device support with on-screen button equivalents
 * - Accessibility compliance with ARIA announcements and screen reader support
 * - React performance optimization with useCallback and proper cleanup
 * - Pseudo-conversational state management integration
 * - Session timeout handling and navigation state preservation
 * 
 * Technology Transformation: COBOL/CICS/BMS → React/JavaScript/Web
 * Original System: IBM 3270 Terminal Function Keys with BMS screen navigation
 * Target System: Modern web keyboard shortcuts with React event handling
 * 
 * Copyright (c) 2023 CardDemo Application - Mainframe Modernization
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { NavigationHelpers } from '../utils/navigationHelpers';
import { ALTERNATIVE_KEY_COMBINATIONS } from '../constants/KeyboardConstants';
import { ROUTES } from '../constants/NavigationConstants';

// ==========================================
// Core Hook Implementation - useKeyboardShortcuts
// ==========================================

/**
 * Primary keyboard shortcuts hook providing mainframe function key behavior
 * Implements F3=Exit, F7=Page Up, F8=Page Down, F12=Cancel with React patterns
 * 
 * @param {Object} options - Configuration options for keyboard behavior
 * @param {Function} options.onPageChange - Callback for pagination actions (F7/F8)
 * @param {Function} options.onExit - Custom exit handler (overrides default F3 behavior)
 * @param {Function} options.onCancel - Custom cancel handler (overrides default F12 behavior)
 * @param {boolean} options.enableTouchSupport - Enable touch device button support
 * @param {boolean} options.enableAccessibility - Enable accessibility features
 * @param {string} options.currentPath - Current route path for navigation context
 * @returns {Object} Hook interface with event handlers and touch support functions
 */
const useKeyboardShortcuts = (options = {}) => {
  // Extract options with defaults
  const {
    onPageChange = null,
    onExit = null,
    onCancel = null,
    enableTouchSupport = true,
    enableAccessibility = true,
    currentPath = window.location.pathname
  } = options;

  // React Router navigation hook
  const navigate = useNavigate();
  
  // State for tracking active shortcuts and touch support
  const [isShortcutsActive, setIsShortcutsActive] = useState(true);
  const [touchButtonsVisible, setTouchButtonsVisible] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);
  
  // Ref for cleanup and event listener management
  const eventListenersRef = useRef([]);
  const touchSupportRef = useRef(null);
  
  // ==========================================
  // Function Key Event Handlers - CICS PF Key Equivalents
  // ==========================================

  /**
   * Handle F3 - Exit/Back Navigation
   * Equivalent to CICS PF3 functionality with pseudo-conversational state preservation
   */
  const handleExitKey = useCallback((event) => {
    if (isProcessing) return;
    
    try {
      setIsProcessing(true);
      
      // Prevent default browser behavior
      event.preventDefault();
      event.stopPropagation();
      
      // Announce to screen readers
      if (enableAccessibility) {
        announceToScreenReader('Navigating back to previous screen');
      }
      
      // Use custom exit handler if provided
      if (onExit && typeof onExit === 'function') {
        onExit(currentPath);
      } else {
        // Default exit behavior using NavigationHelpers
        NavigationHelpers.handleExit(currentPath);
      }
      
      // Log navigation action for audit
      console.log('F3 Exit pressed:', { 
        currentPath, 
        timestamp: new Date().toISOString(),
        action: 'EXIT_SCREEN'
      });
      
    } catch (error) {
      console.error('Error handling F3 exit:', error);
      // Fallback to main menu
      navigate(ROUTES.MENU);
    } finally {
      setIsProcessing(false);
    }
  }, [currentPath, onExit, isProcessing, navigate, enableAccessibility]);

  /**
   * Handle F7 - Page Up/Backward Navigation
   * Equivalent to CICS PF7 functionality for paginated list navigation
   */
  const handlePageUpKey = useCallback((event) => {
    if (isProcessing) return;
    
    try {
      setIsProcessing(true);
      
      // Prevent default browser behavior
      event.preventDefault();
      event.stopPropagation();
      
      // Announce to screen readers
      if (enableAccessibility) {
        announceToScreenReader('Loading previous page');
      }
      
      // Use custom page change handler if provided
      if (onPageChange && typeof onPageChange === 'function') {
        onPageChange('PREVIOUS', currentPage);
      } else {
        // Default pagination behavior
        if (currentPage > 1) {
          const newPage = currentPage - 1;
          setCurrentPage(newPage);
          
          // Dispatch pagination event for components to handle
          const paginationEvent = new CustomEvent('pagination', {
            detail: { 
              action: 'PREVIOUS_PAGE',
              currentPage: newPage,
              direction: 'BACKWARD'
            }
          });
          document.dispatchEvent(paginationEvent);
        }
      }
      
      // Log navigation action for audit
      console.log('F7 Page Up pressed:', { 
        currentPath, 
        currentPage,
        timestamp: new Date().toISOString(),
        action: 'PREVIOUS_PAGE'
      });
      
    } catch (error) {
      console.error('Error handling F7 page up:', error);
    } finally {
      setIsProcessing(false);
    }
  }, [currentPath, onPageChange, currentPage, isProcessing, enableAccessibility]);

  /**
   * Handle F8 - Page Down/Forward Navigation
   * Equivalent to CICS PF8 functionality for paginated list navigation
   */
  const handlePageDownKey = useCallback((event) => {
    if (isProcessing) return;
    
    try {
      setIsProcessing(true);
      
      // Prevent default browser behavior
      event.preventDefault();
      event.stopPropagation();
      
      // Announce to screen readers
      if (enableAccessibility) {
        announceToScreenReader('Loading next page');
      }
      
      // Use custom page change handler if provided
      if (onPageChange && typeof onPageChange === 'function') {
        onPageChange('NEXT', currentPage);
      } else {
        // Default pagination behavior
        const newPage = currentPage + 1;
        setCurrentPage(newPage);
        
        // Dispatch pagination event for components to handle
        const paginationEvent = new CustomEvent('pagination', {
          detail: { 
            action: 'NEXT_PAGE',
            currentPage: newPage,
            direction: 'FORWARD'
          }
        });
        document.dispatchEvent(paginationEvent);
      }
      
      // Log navigation action for audit
      console.log('F8 Page Down pressed:', { 
        currentPath,
        currentPage,
        timestamp: new Date().toISOString(),
        action: 'NEXT_PAGE'
      });
      
    } catch (error) {
      console.error('Error handling F8 page down:', error);
    } finally {
      setIsProcessing(false);
    }
  }, [currentPath, onPageChange, currentPage, isProcessing, enableAccessibility]);

  /**
   * Handle F12 - Cancel Operation
   * Equivalent to CICS PF12 functionality with return to main menu
   */
  const handleCancelKey = useCallback((event) => {
    if (isProcessing) return;
    
    try {
      setIsProcessing(true);
      
      // Prevent default browser behavior
      event.preventDefault();
      event.stopPropagation();
      
      // Announce to screen readers
      if (enableAccessibility) {
        announceToScreenReader('Canceling operation and returning to main menu');
      }
      
      // Use custom cancel handler if provided
      if (onCancel && typeof onCancel === 'function') {
        onCancel(currentPath);
      } else {
        // Default cancel behavior - navigate to main menu
        navigate(ROUTES.MENU, {
          state: {
            cancelAction: true,
            fromPath: currentPath,
            timestamp: new Date().toISOString()
          }
        });
      }
      
      // Log navigation action for audit
      console.log('F12 Cancel pressed:', { 
        currentPath,
        timestamp: new Date().toISOString(),
        action: 'CANCEL_OPERATION'
      });
      
    } catch (error) {
      console.error('Error handling F12 cancel:', error);
      // Fallback to main menu
      navigate(ROUTES.MENU);
    } finally {
      setIsProcessing(false);
    }
  }, [currentPath, onCancel, isProcessing, navigate, enableAccessibility]);

  // ==========================================
  // Alternative Key Combinations - Browser Compatibility
  // ==========================================

  /**
   * Handle alternative key combinations for browser compatibility
   * Implements fallback shortcuts when function keys are reserved
   */
  const handleAlternativeKeyCombinations = useCallback((event) => {
    if (isProcessing) return;
    
    const { CTRL_R_FOR_F5, CTRL_ESC_FOR_F12 } = ALTERNATIVE_KEY_COMBINATIONS;
    
    // Check for Ctrl+R as alternative for F5 (refresh)
    if (event.key === CTRL_R_FOR_F5.key && 
        event.ctrlKey === CTRL_R_FOR_F5.ctrlKey && 
        !event.altKey && !event.shiftKey) {
      
      event.preventDefault();
      event.stopPropagation();
      
      // Announce to screen readers
      if (enableAccessibility) {
        announceToScreenReader('Refreshing screen data');
      }
      
      // Dispatch refresh event
      const refreshEvent = new CustomEvent('refresh', {
        detail: { 
          action: 'REFRESH_SCREEN',
          currentPath,
          timestamp: new Date().toISOString()
        }
      });
      document.dispatchEvent(refreshEvent);
      
      console.log('Ctrl+R refresh pressed:', { currentPath });
    }
    
    // Check for Ctrl+Esc as alternative for F12 (cancel)
    if (event.key === CTRL_ESC_FOR_F12.key && 
        event.ctrlKey === CTRL_ESC_FOR_F12.ctrlKey && 
        !event.altKey && !event.shiftKey) {
      
      // Use the F12 handler for consistency
      handleCancelKey(event);
    }
  }, [currentPath, isProcessing, handleCancelKey, enableAccessibility]);

  // ==========================================
  // Touch Device Support - Mobile and Tablet Compatibility
  // ==========================================

  /**
   * Create touch device handlers for mobile and tablet users
   * Provides on-screen buttons equivalent to function keys
   */
  const createTouchDeviceHandlers = useCallback(() => {
    // Check if device supports touch
    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    
    if (!isTouchDevice || !enableTouchSupport) {
      return null;
    }
    
    // Touch button configurations
    const touchButtons = {
      exitButton: {
        id: 'touch-f3-exit',
        label: 'Exit',
        icon: '←',
        action: handleExitKey,
        className: 'keyboard-shortcut-button keyboard-shortcut-button--exit',
        ariaLabel: 'Exit current screen (F3)',
        style: {
          position: 'fixed',
          bottom: '20px',
          left: '20px',
          zIndex: 1000,
          backgroundColor: '#007acc',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '44px',
          minHeight: '44px',
          display: touchButtonsVisible ? 'block' : 'none'
        }
      },
      
      pageUpButton: {
        id: 'touch-f7-pageup',
        label: 'Prev',
        icon: '↑',
        action: handlePageUpKey,
        className: 'keyboard-shortcut-button keyboard-shortcut-button--pageup',
        ariaLabel: 'Previous page (F7)',
        style: {
          position: 'fixed',
          bottom: '80px',
          left: '20px',
          zIndex: 1000,
          backgroundColor: '#28a745',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          padding: '8px 12px',
          fontSize: '14px',
          cursor: 'pointer',
          minWidth: '44px',
          minHeight: '44px',
          display: touchButtonsVisible ? 'block' : 'none'
        }
      },
      
      pageDownButton: {
        id: 'touch-f8-pagedown',
        label: 'Next',
        icon: '↓',
        action: handlePageDownKey,
        className: 'keyboard-shortcut-button keyboard-shortcut-button--pagedown',
        ariaLabel: 'Next page (F8)',
        style: {
          position: 'fixed',
          bottom: '80px',
          right: '20px',
          zIndex: 1000,
          backgroundColor: '#28a745',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          padding: '8px 12px',
          fontSize: '14px',
          cursor: 'pointer',
          minWidth: '44px',
          minHeight: '44px',
          display: touchButtonsVisible ? 'block' : 'none'
        }
      },
      
      cancelButton: {
        id: 'touch-f12-cancel',
        label: 'Cancel',
        icon: '×',
        action: handleCancelKey,
        className: 'keyboard-shortcut-button keyboard-shortcut-button--cancel',
        ariaLabel: 'Cancel operation (F12)',
        style: {
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          zIndex: 1000,
          backgroundColor: '#dc3545',
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '44px',
          minHeight: '44px',
          display: touchButtonsVisible ? 'block' : 'none'
        }
      }
    };
    
    return touchButtons;
  }, [enableTouchSupport, touchButtonsVisible, handleExitKey, handlePageUpKey, handlePageDownKey, handleCancelKey]);

  // ==========================================
  // Keyboard Event Handler - Main Event Processing
  // ==========================================

  /**
   * Create main keyboard event handler combining all function key behaviors
   * Implements document-level event listening with proper cleanup
   */
  const createKeyboardEventHandler = useCallback(() => {
    const keyboardEventHandler = (event) => {
      // Skip if shortcuts are disabled
      if (!isShortcutsActive) return;
      
      // Skip if target is an input field (unless it's a specific key combination)
      const isInputField = event.target.tagName === 'INPUT' || 
                          event.target.tagName === 'TEXTAREA' || 
                          event.target.isContentEditable;
      
      // Handle function keys
      switch (event.key) {
        case 'F3':
          if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
            handleExitKey(event);
          }
          break;
          
        case 'F7':
          if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
            handlePageUpKey(event);
          }
          break;
          
        case 'F8':
          if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
            handlePageDownKey(event);
          }
          break;
          
        case 'F12':
          if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
            handleCancelKey(event);
          }
          break;
          
        default:
          // Handle alternative key combinations
          handleAlternativeKeyCombinations(event);
          break;
      }
    };
    
    return keyboardEventHandler;
  }, [isShortcutsActive, handleExitKey, handlePageUpKey, handlePageDownKey, handleCancelKey, handleAlternativeKeyCombinations]);

  // ==========================================
  // Accessibility Support Functions
  // ==========================================

  /**
   * Announce messages to screen readers
   * Provides accessibility support for keyboard navigation
   */
  const announceToScreenReader = useCallback((message) => {
    if (!enableAccessibility) return;
    
    // Create or update ARIA live region
    let statusRegion = document.getElementById('keyboard-shortcut-status');
    if (!statusRegion) {
      statusRegion = document.createElement('div');
      statusRegion.id = 'keyboard-shortcut-status';
      statusRegion.setAttribute('aria-live', 'polite');
      statusRegion.setAttribute('aria-atomic', 'true');
      statusRegion.style.position = 'absolute';
      statusRegion.style.left = '-10000px';
      statusRegion.style.width = '1px';
      statusRegion.style.height = '1px';
      statusRegion.style.overflow = 'hidden';
      document.body.appendChild(statusRegion);
    }
    
    // Update the status region with the message
    statusRegion.textContent = message;
    
    // Clear the message after announcement
    setTimeout(() => {
      statusRegion.textContent = '';
    }, 1000);
  }, [enableAccessibility]);

  // ==========================================
  // Effect Hooks - Event Listener Management
  // ==========================================

  /**
   * Effect hook for setting up keyboard event listeners
   * Provides proper cleanup on unmount
   */
  useEffect(() => {
    const keyboardHandler = createKeyboardEventHandler();
    
    // Add event listener
    document.addEventListener('keydown', keyboardHandler);
    eventListenersRef.current.push({ type: 'keydown', handler: keyboardHandler });
    
    // Cleanup function
    return () => {
      document.removeEventListener('keydown', keyboardHandler);
      
      // Clean up event listeners array
      eventListenersRef.current = eventListenersRef.current.filter(
        listener => listener.handler !== keyboardHandler
      );
    };
  }, [createKeyboardEventHandler]);

  /**
   * Effect hook for touch device support
   * Manages touch button visibility and cleanup
   */
  useEffect(() => {
    if (!enableTouchSupport) return;
    
    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    
    if (isTouchDevice) {
      // Show touch buttons after a delay
      const showTimer = setTimeout(() => {
        setTouchButtonsVisible(true);
      }, 1000);
      
      // Hide touch buttons on mouse movement (desktop users)
      const handleMouseMove = () => {
        setTouchButtonsVisible(false);
      };
      
      document.addEventListener('mousemove', handleMouseMove);
      
      return () => {
        clearTimeout(showTimer);
        document.removeEventListener('mousemove', handleMouseMove);
      };
    }
  }, [enableTouchSupport]);

  /**
   * Effect hook for managing shortcuts active state
   * Handles focus management and dialog interactions
   */
  useEffect(() => {
    const handleFocusChange = () => {
      // Disable shortcuts when focus is in a modal or dialog
      const activeElement = document.activeElement;
      const isInModal = activeElement?.closest('[role="dialog"]') || 
                       activeElement?.closest('.modal') ||
                       activeElement?.closest('[aria-modal="true"]');
      
      setIsShortcutsActive(!isInModal);
    };
    
    // Listen for focus changes
    document.addEventListener('focusin', handleFocusChange);
    document.addEventListener('focusout', handleFocusChange);
    
    return () => {
      document.removeEventListener('focusin', handleFocusChange);
      document.removeEventListener('focusout', handleFocusChange);
    };
  }, []);

  // ==========================================
  // Hook Return Interface
  // ==========================================

  /**
   * Return hook interface with all functionality
   * Provides access to handlers and configuration
   */
  return {
    // Event handlers for manual use
    handleExitKey,
    handlePageUpKey,
    handlePageDownKey,
    handleCancelKey,
    handleAlternativeKeyCombinations,
    
    // Touch device support
    createTouchDeviceHandlers,
    touchButtonsVisible,
    setTouchButtonsVisible,
    
    // State management
    isShortcutsActive,
    setIsShortcutsActive,
    currentPage,
    setCurrentPage,
    isProcessing,
    
    // Accessibility support
    announceToScreenReader,
    
    // Utility functions
    createKeyboardEventHandler,
    
    // Configuration
    enableTouchSupport,
    enableAccessibility,
    currentPath
  };
};

// ==========================================
// Named Export Functions - Utility Functions
// ==========================================

/**
 * Create keyboard event handler function for manual implementation
 * Provides direct access to event handling without hook requirements
 */
export const createKeyboardEventHandler = (options = {}) => {
  const {
    onExit = null,
    onPageUp = null,
    onPageDown = null,
    onCancel = null,
    currentPath = window.location.pathname
  } = options;
  
  return (event) => {
    switch (event.key) {
      case 'F3':
        if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          event.stopPropagation();
          if (onExit) onExit(currentPath);
        }
        break;
        
      case 'F7':
        if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          event.stopPropagation();
          if (onPageUp) onPageUp();
        }
        break;
        
      case 'F8':
        if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          event.stopPropagation();
          if (onPageDown) onPageDown();
        }
        break;
        
      case 'F12':
        if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          event.stopPropagation();
          if (onCancel) onCancel(currentPath);
        }
        break;
    }
  };
};

/**
 * Create touch device handlers function for manual implementation
 * Provides touch button creation without hook requirements
 */
export const createTouchDeviceHandlers = (options = {}) => {
  const {
    onExit = null,
    onPageUp = null,
    onPageDown = null,
    onCancel = null,
    showButtons = true
  } = options;
  
  const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  
  if (!isTouchDevice) return null;
  
  return {
    exitButton: {
      onClick: onExit,
      style: { display: showButtons ? 'block' : 'none' }
    },
    pageUpButton: {
      onClick: onPageUp,
      style: { display: showButtons ? 'block' : 'none' }
    },
    pageDownButton: {
      onClick: onPageDown,
      style: { display: showButtons ? 'block' : 'none' }
    },
    cancelButton: {
      onClick: onCancel,
      style: { display: showButtons ? 'block' : 'none' }
    }
  };
};

/**
 * Handle alternative key combinations function for manual implementation
 * Provides fallback key combination handling without hook requirements
 */
export const handleAlternativeKeyCombinations = (event, options = {}) => {
  const { onRefresh = null, onCancel = null } = options;
  
  // Ctrl+R for refresh
  if (event.key === 'r' && event.ctrlKey && !event.altKey && !event.shiftKey) {
    event.preventDefault();
    event.stopPropagation();
    if (onRefresh) onRefresh();
  }
  
  // Ctrl+Esc for cancel
  if (event.key === 'Escape' && event.ctrlKey && !event.altKey && !event.shiftKey) {
    event.preventDefault();
    event.stopPropagation();
    if (onCancel) onCancel();
  }
};

// ==========================================
// Default Export - Main Hook
// ==========================================

export default useKeyboardShortcuts;