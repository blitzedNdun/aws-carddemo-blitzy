const { defineConfig } = require('cypress');

module.exports = defineConfig({
  // E2E Testing Configuration
  e2e: {
    // Base URL configuration pointing to React frontend
    baseUrl: 'http://localhost:3000',
    
    // Backend API endpoint for full-stack testing
    env: {
      apiUrl: 'http://localhost:8080/api',
      backendUrl: 'http://localhost:8080'
    },
    
    // Viewport settings matching 3270 terminal screen dimensions
    // Traditional 3270 screens: 80 columns x 24 rows
    // Converted to modern pixel dimensions with readable font size
    viewportWidth: 1280,
    viewportHeight: 768,
    
    // Test execution configuration
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.js',
    fixturesFolder: 'cypress/fixtures',
    screenshotsFolder: 'cypress/screenshots',
    videosFolder: 'cypress/videos',
    
    // Network and timeout settings optimized for E2E testing
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    pageLoadTimeout: 30000,
    
    // Retry configuration for flaky test management
    retries: {
      runMode: 2,
      openMode: 0
    },
    
    // Video and screenshot capture
    video: true,
    screenshotOnRunFailure: true,
    
    // Test isolation configuration
    testIsolation: true,
    
    // Custom configuration for 3270 terminal behavior emulation
    experimentalStudio: true,
    experimentalWebKitSupport: false,
    
    // Setup tasks and events
    setupNodeEvents(on, config) {
      // Task for PF-key simulation and custom commands
      on('task', {
        // Simulate PF-key press events that map to 3270 function keys
        simulatePFKey(keyCode) {
          // PF key mappings for 3270 terminal emulation:
          // PF1 = F1 (Help), PF3 = F3 (Exit), PF7 = F7 (Page Up), PF8 = F8 (Page Down)
          // PF12 = F12 (Cancel), Enter = Enter key
          const pfKeyMappings = {
            'PF1': 'F1',     // Help
            'PF3': 'F3',     // Exit/Return
            'PF7': 'F7',     // Page Up
            'PF8': 'F8',     // Page Down
            'PF12': 'F12',   // Cancel
            'ENTER': 'Enter', // Enter/Submit
            'CLEAR': 'Escape' // Clear screen
          };
          
          console.log(`Simulating PF Key: ${keyCode} -> ${pfKeyMappings[keyCode] || keyCode}`);
          return null;
        },
        
        // Task for test data management
        loadTestFixtures(fixtureName) {
          console.log(`Loading test fixture: ${fixtureName}`);
          return null;
        },
        
        // Task for session state management (COMMAREA equivalent)
        setSessionState(sessionData) {
          console.log('Setting session state for COMMAREA emulation:', sessionData);
          return null;
        }
      });
      
      // Browser configuration for testing
      on('before:browser:launch', (browser = {}, launchOptions) => {
        // Chrome-specific optimizations for E2E testing
        if (browser.family === 'chromium' && browser.name !== 'electron') {
          launchOptions.args.push('--disable-dev-shm-usage');
          launchOptions.args.push('--disable-gpu');
          launchOptions.args.push('--no-sandbox');
          launchOptions.args.push('--disable-web-security');
        }
        
        // Firefox-specific configurations
        if (browser.family === 'firefox') {
          launchOptions.preferences['dom.ipc.processCount'] = 1;
        }
        
        return launchOptions;
      });
      
      return config;
    }
  },
  
  // Component testing configuration (for React components in isolation)
  component: {
    devServer: {
      framework: 'react',
      bundler: 'webpack'
    },
    specPattern: 'src/components/**/*.cy.{js,jsx,ts,tsx}',
    viewportWidth: 1280,
    viewportHeight: 768,
    
    // Component test specific settings
    supportFile: 'cypress/support/component.js'
  },
  
  // Global Cypress configuration
  // Performance and reliability settings
  numTestsKeptInMemory: 10,
  watchForFileChanges: false,
  chromeWebSecurity: false,
  
  // Reporter configuration for test results
  reporter: 'mochawesome',
  reporterOptions: {
    reportDir: 'cypress/reports',
    overwrite: false,
    html: true,
    json: true,
    charts: true,
    reportPageTitle: 'CardDemo E2E Test Results',
    embeddedScreenshots: true,
    inlineAssets: true
  },
  
  // Environment variables for test configuration
  env: {
    // Frontend configuration
    frontendUrl: 'http://localhost:3000',
    
    // Backend configuration  
    apiUrl: 'http://localhost:8080/api',
    backendUrl: 'http://localhost:8080',
    
    // Test user credentials for authentication testing
    testUsers: {
      admin: {
        userId: 'TESTADM',
        password: 'TESTPASS'
      },
      user: {
        userId: 'TESTUSER', 
        password: 'TESTPASS'
      }
    },
    
    // 3270 screen dimensions for viewport validation
    terminal: {
      columns: 80,
      rows: 24,
      fontSize: '12px',
      fontFamily: 'Courier New, monospace'
    },
    
    // Transaction codes for testing CICS transaction mapping
    transactionCodes: {
      signOn: 'CC00',
      mainMenu: 'CM00', 
      accountView: 'CA00',
      transactionList: 'CT00',
      cardManagement: 'CC01',
      userAdmin: 'CU00'
    },
    
    // Performance test thresholds
    performance: {
      maxResponseTime: 200,  // milliseconds - matches 200ms SLA requirement
      maxLoadTime: 3000,     // milliseconds for page load
      maxApiResponseTime: 200 // milliseconds for API calls
    },
    
    // Test data configuration
    testData: {
      accountIds: ['000000001', '000000002', '000000003'],
      customerIds: ['000000001', '000000002', '000000003'],
      cardNumbers: ['4532123456789012', '4532123456789013', '4532123456789014']
    },
    
    // Validation rules matching BMS field definitions
    validation: {
      userIdMaxLength: 8,      // From COSGN00.bms USERID field
      passwordMaxLength: 8,    // From COSGN00.bms PASSWD field
      maxMenuOptions: 12,      // From COMEN01.bms OPTN001-OPTN012
      maxTransactionRows: 10   // From COTRN00.bms SEL0001-SEL0010
    }
  }
});