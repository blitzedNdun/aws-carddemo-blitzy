# CardDemo React Frontend

## Project Overview

This is the React Single Page Application (SPA) frontend for the CardDemo system, representing a complete modernization from the original IBM mainframe COBOL/CICS 3270 terminal application to a modern web-based interface. The frontend maintains 100% functional parity with the original 18 BMS (Basic Mapping Support) screens while providing a contemporary user experience.

### Migration Context

The CardDemo React frontend is part of a comprehensive technology stack migration from:
- **From**: IBM mainframe COBOL/CICS with 3270 terminal screens
- **To**: Modern React 18.x Single Page Application with REST API integration
- **Objective**: Preserve all existing business logic and user workflows while providing a modern web interface

## Technology Stack

### Core Technologies
- **React 18.2.0** - Component-based UI framework with concurrent features
- **Node.js 20.x LTS** - JavaScript runtime environment for development and build
- **React Router 6.8.0** - Declarative client-side routing
- **Material-UI 5.14.x** - Modern component library for consistent design
- **Formik 2.4.x** - Declarative form state management
- **Yup 1.3.x** - Schema-based validation matching original COBOL field rules
- **Axios 1.6.x** - HTTP client for REST API communication

### Build and Development Tools
- **Vite/Webpack** - Module bundling and hot module replacement
- **CSS/SCSS** - Custom styling and responsive design
- **Development Server** - Hot reload for efficient development workflow

## Quick Start

### Prerequisites
- **Node.js 20.x LTS** (required for React 18.x compatibility)
- **npm 10.x** or **yarn 4.x**

### Installation and Setup

1. **Clone the repository and navigate to frontend directory:**
   ```bash
   git clone <repository-url>
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   # or
   yarn install
   ```

3. **Start development server:**
   ```bash
   npm start
   # or
   yarn start
   ```

4. **Access the application:**
   - Development server: `http://localhost:3000`
   - The application will automatically reload when you make changes

### Environment Configuration

Create a `.env.local` file in the frontend directory:
```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_SESSION_TIMEOUT=30
REACT_APP_PAGE_SIZE=10
```

## Component Architecture

### Screen Component Mapping

The React frontend provides a 1-to-1 mapping of the original BMS screens to React components, preserving the exact screen layouts and navigation patterns:

| Original BMS Screen | React Component | Transaction Code | Purpose |
|-------------------|-----------------|------------------|---------|
| COSGN00.bms | `COSGN00.jsx` | CC00 | Sign-On Screen |
| COMEN01.bms | `COMEN01.jsx` | CM00 | Main Menu |
| COADM01.bms | `COADM01.jsx` | CA00 | Admin Menu |
| COTRN00.bms | `COTRN00.jsx` | CT00 | Transaction List |
| COTRN01.bms | `COTRN01.jsx` | CT01 | Transaction View |
| COTRN02.bms | `COTRN02.jsx` | CT02 | Transaction Add |
| COACTVW.bms | `COACTVW.jsx` | CAVW | Account View |
| COACTUP.bms | `COACTUP.jsx` | CAUP | Account Update |
| COCRDLI.bms | `COCRDLI.jsx` | CCLI | Credit Card List |
| COCRDSL.bms | `COCRDSL.jsx` | CCDL | Credit Card View |
| COCRDUP.bms | `COCRDUP.jsx` | CCUP | Credit Card Update |
| COBIL00.bms | `COBIL00.jsx` | CB00 | Bill Payment |
| CORPT00.bms | `CORPT00.jsx` | CR00 | Transaction Reports |
| COUSR00.bms | `COUSR00.jsx` | CU00 | List Users |
| COUSR01.bms | `COUSR01.jsx` | CU01 | Add User |
| COUSR02.bms | `COUSR02.jsx` | CU02 | Update User |
| COUSR03.bms | `COUSR03.jsx` | CU03 | Delete User |

### Project Structure

```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── components/
│   │   ├── screens/              # BMS screen components
│   │   │   ├── COSGN00.jsx      # Sign-On Screen
│   │   │   ├── COMEN01.jsx      # Main Menu
│   │   │   ├── COTRN00.jsx      # Transaction List
│   │   │   └── ... (15 more)
│   │   ├── common/              # Shared components
│   │   │   ├── Header.jsx       # Standard screen header
│   │   │   ├── ErrorMessage.jsx # Error display component
│   │   │   └── Pagination.jsx   # PF7/PF8 pagination
│   │   └── forms/               # Form components
│   │       ├── UserForm.jsx
│   │       ├── AccountForm.jsx
│   │       └── TransactionForm.jsx
│   ├── services/                # API integration
│   │   ├── api.js              # Axios HTTP client configuration
│   │   ├── authService.js      # Authentication API calls
│   │   ├── accountService.js   # Account management APIs
│   │   ├── transactionService.js # Transaction APIs
│   │   └── userService.js      # User management APIs
│   ├── utils/                   # Utility functions
│   │   ├── validation.js       # Yup validation schemas
│   │   ├── keyboardHandler.js  # PF-key event handling
│   │   ├── formatters.js       # Data formatting utilities
│   │   └── constants.js        # Application constants
│   ├── hooks/                   # Custom React hooks
│   │   ├── useAuth.js          # Authentication state management
│   │   ├── useKeyboard.js      # PF-key handling hook
│   │   └── usePagination.js    # Pagination state management
│   ├── context/                 # React context providers
│   │   ├── AuthContext.js      # Authentication context
│   │   └── AppContext.js       # Global application state
│   ├── styles/                  # CSS and styling
│   │   ├── globals.css         # Global styles
│   │   ├── components.css      # Component-specific styles
│   │   └── themes.css          # Material-UI theme customization
│   ├── App.jsx                  # Main application component
│   ├── index.js                 # Application entry point
│   └── routes.js                # React Router configuration
├── package.json                 # Dependencies and scripts
├── .env.local                   # Environment configuration
└── README.md                    # This file
```

## API Integration Patterns

### REST Endpoint Mapping

The React frontend communicates with the Spring Boot backend through REST APIs that map one-to-one with the original CICS transaction codes:

| Legacy Transaction | REST Endpoint | HTTP Method | Purpose |
|-------------------|---------------|-------------|---------|
| CC00 (Sign On) | `/api/auth/signin` | POST | User authentication |
| CM00 (Main Menu) | `/api/menu/main` | GET | Menu display |
| CT00 (Transaction List) | `/api/transactions` | GET | Transaction browsing |
| CAUP (Account Update) | `/api/accounts/{id}` | PUT | Account management |
| CCLI (Card List) | `/api/cards` | GET | Credit card listing |
| CU00 (User List) | `/api/users` | GET | User management |

### Session Management

- **Server-side Sessions**: Spring Session with Redis backend maintains user context
- **HTTP Cookies**: `XSRF-TOKEN` and `JSESSIONID` cookie pair for secure session identification
- **Stateless UI**: React components maintain local state while server handles persistent context
- **Cross-Request Context**: Session attributes replicate COMMAREA functionality

### API Request/Response Format

**Standard Request Format:**
```javascript
{
  "transactionCode": "CT00",
  "requestData": {
    "accountId": "12345678901",
    "startDate": "2024-01-01",
    "pageSize": 10,
    "pageNumber": 1
  }
}
```

**Standard Response Format:**
```javascript
{
  "status": "SUCCESS",
  "transactionCode": "CT00",
  "responseData": {
    "transactions": [...],
    "totalCount": 150,
    "hasMorePages": true
  },
  "messages": [
    {"level": "INFO", "text": "15 transactions retrieved"}
  ]
}
```

## PF-Key Functionality Preservation

### Keyboard Event Mapping

Traditional mainframe PF-key functionality is preserved through JavaScript keyboard event handlers:

| PF Key | Function | React Implementation |
|--------|----------|---------------------|
| **F1** | Help | `useKeyboardHandler` hook with help dialog |
| **F3** | Exit/Return | React Router navigation to previous screen |
| **F4** | Clear | Form reset with confirmation |
| **F5** | Refresh | Data reload and screen refresh |
| **F7** | Page Up | Pagination API call (previous page) |
| **F8** | Page Down | Pagination API call (next page) |
| **F12** | Cancel | Form reset with confirmation dialog |
| **ENTER** | Submit | Form submission and validation |

### Implementation Example

```javascript
// Custom hook for PF-key handling
export const useKeyboardHandler = (component) => {
  const navigate = useNavigate();
  
  useEffect(() => {
    const handleKeyDown = (event) => {
      switch(event.keyCode) {
        case 114: // F3 - Exit
          navigate(-1);
          break;
        case 118: // F7 - Page Up
          handlePageUp();
          break;
        case 119: // F8 - Page Down
          handlePageDown();
          break;
        case 123: // F12 - Cancel
          handleCancel();
          break;
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [navigate]);
};
```

## Form Validation and Data Entry

### Yup Schema Validation

Client-side validation uses Yup schemas generated from original COBOL picture clauses:

```javascript
// Example: Account ID validation (COBOL: PIC 9(11))
const accountValidationSchema = yup.object({
  accountId: yup.string()
    .matches(/^\d{11}$/, 'Account ID must be exactly 11 digits')
    .required('Account ID is required'),
  
  // Date validation (COBOL: PIC X(8))
  accountOpenDate: yup.date()
    .max(new Date(), 'Date cannot be in the future')
    .required('Account open date is required'),
    
  // Amount validation (COBOL: PIC 9(7)V99)
  creditLimit: yup.number()
    .min(0, 'Credit limit must be positive')
    .max(9999999.99, 'Credit limit exceeds maximum')
    .required('Credit limit is required')
});
```

### Form Processing Flow

1. **Client-side Pre-validation**: Formik/Yup validation before API calls
2. **Field Format Checking**: Real-time validation with immediate feedback
3. **Cross-field Dependencies**: Custom validation for related fields
4. **Server-side Validation**: Business rule enforcement at backend
5. **Error Display**: Field-level and screen-level error messaging

## Development Workflow

### Available Scripts

```bash
# Development
npm start              # Start development server with hot reload
npm run dev            # Alternative development command

# Building
npm run build          # Create optimized production build
npm run build:dev      # Create development build with source maps

# Testing
npm test               # Run unit tests
npm run test:watch     # Run tests in watch mode
npm run test:coverage  # Generate test coverage report

# Code Quality
npm run lint           # Run ESLint code analysis
npm run lint:fix       # Fix auto-fixable linting issues
npm run format         # Format code with Prettier

# Analysis
npm run analyze        # Analyze bundle size and dependencies
npm run audit          # Security audit of dependencies
```

### Development Best Practices

1. **Component Naming**: Use original BMS screen names for traceability
2. **State Management**: Prefer React hooks over class components
3. **Error Handling**: Implement comprehensive error boundaries
4. **Accessibility**: Ensure keyboard navigation and screen reader support
5. **Performance**: Use React.memo for expensive re-renders
6. **Testing**: Write unit tests for all business logic components

### API Service Pattern

```javascript
// services/accountService.js
import axios from './api';

export const accountService = {
  // Get account details
  getAccount: (accountId) => 
    axios.get(`/api/accounts/${accountId}`),
    
  // Update account information
  updateAccount: (accountId, accountData) =>
    axios.put(`/api/accounts/${accountId}`, accountData),
    
  // Search accounts with pagination
  searchAccounts: (criteria, page = 1, size = 10) =>
    axios.get('/api/accounts/search', {
      params: { ...criteria, page, size }
    })
};
```

## Production Build and Deployment

### Build Configuration

The application builds for containerized deployment in Kubernetes environments:

```bash
# Production build
npm run build

# Build output
build/
├── static/
│   ├── css/           # Minified CSS files
│   ├── js/            # Minified JavaScript bundles
│   └── media/         # Optimized images and fonts
├── index.html         # Production HTML template
└── asset-manifest.json # Asset fingerprinting manifest
```

### Docker Integration

The frontend builds are served via CDN or static file server in the Kubernetes cluster:

```dockerfile
# Multi-stage build example
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Environment Variables

Production environment configuration:

```env
REACT_APP_API_BASE_URL=https://api.carddemo.company.com
REACT_APP_SESSION_TIMEOUT=30
REACT_APP_PAGE_SIZE=10
REACT_APP_ENABLE_DEBUG=false
REACT_APP_VERSION=$npm_package_version
```

## Screen Navigation Flow

### User Journey Mapping

```
Sign-On (COSGN00) 
    ↓
Main Menu (COMEN01) ← Admin Menu (COADM01)
    ↓
┌─ Account Functions ────────────────────┐
│  Account View (COACTVW) ↔ Account Update (COACTUP)
└────────────────────────────────────────┘
┌─ Card Functions ───────────────────────┐
│  Card List (COCRDLI) ↔ Card View (COCRDSL) ↔ Card Update (COCRDUP)
└────────────────────────────────────────┘
┌─ Transaction Functions ────────────────┐
│  Transaction List (COTRN00) ↔ Transaction View (COTRN01) ↔ Add Transaction (COTRN02)
└────────────────────────────────────────┘
┌─ Administrative Functions ─────────────┐
│  User List (COUSR00) ↔ Add User (COUSR01) ↔ Update User (COUSR02) ↔ Delete User (COUSR03)
└────────────────────────────────────────┘
```

### Navigation State Management

- **Browser History**: React Router manages navigation history
- **Session Context**: Server-side session maintains user context across screens
- **Return Navigation**: F3 key consistently returns to previous screen
- **Deep Linking**: URL-based routing supports direct screen access

## Testing Strategy

### Unit Testing

```javascript
// Example component test
import { render, screen, fireEvent } from '@testing-library/react';
import { COSGN00 } from '../components/screens/COSGN00';

test('validates user credentials on sign-on', async () => {
  render(<COSGN00 />);
  
  const userIdInput = screen.getByLabelText('User ID');
  const passwordInput = screen.getByLabelText('Password');
  const submitButton = screen.getByText('ENTER=Sign-on');
  
  fireEvent.change(userIdInput, { target: { value: 'USER001' } });
  fireEvent.change(passwordInput, { target: { value: 'PASSWORD' } });
  fireEvent.click(submitButton);
  
  expect(screen.getByText('Loading...')).toBeInTheDocument();
});
```

### Integration Testing

- **API Integration**: Mock backend services for testing
- **Navigation Testing**: Verify PF-key and router integration
- **Form Validation**: Test Yup schema validation rules
- **Session Management**: Test authentication and session flows

## Troubleshooting

### Common Issues

1. **PF-Key Not Working**: Check keyboard event listeners and focus management
2. **API Connection Errors**: Verify `REACT_APP_API_BASE_URL` environment variable
3. **Session Timeout**: Check session configuration and cookie settings
4. **Validation Errors**: Verify Yup schema matches backend validation rules
5. **Build Failures**: Check Node.js version compatibility (requires 20.x LTS)

### Debug Mode

Enable debug logging:
```env
REACT_APP_ENABLE_DEBUG=true
REACT_APP_LOG_LEVEL=debug
```

### Browser DevTools

- **Network Tab**: Monitor API requests and responses
- **Console**: Check for JavaScript errors and debug logs
- **Application Tab**: Inspect session cookies and local storage
- **React DevTools**: Analyze component state and props

## Performance Considerations

### Optimization Strategies

- **Code Splitting**: Lazy load screen components for faster initial load
- **Memoization**: Use React.memo for expensive component re-renders
- **API Caching**: Implement service worker for offline capability
- **Bundle Analysis**: Monitor and optimize JavaScript bundle size

### Performance Monitoring

- **Web Vitals**: Track Core Web Vitals metrics
- **API Response Times**: Monitor REST endpoint performance
- **User Experience**: Track user interaction patterns and errors

## Contributing

### Development Guidelines

1. **Follow BMS Naming**: Maintain original screen component names for traceability
2. **Test Coverage**: Maintain minimum 80% test coverage
3. **Documentation**: Update README for any new features or changes
4. **Code Review**: All changes require peer review before merge
5. **Accessibility**: Ensure all components meet WCAG 2.1 AA standards

### Git Workflow

```bash
# Feature development
git checkout -b feature/screen-component-name
git commit -m "feat: implement COTRN00 transaction list screen"
git push origin feature/screen-component-name

# Create pull request with detailed description
```

## Support and Maintenance

For technical support or questions about the CardDemo React frontend:

1. **Check Documentation**: Review this README and inline code comments
2. **Search Issues**: Check existing GitHub issues for similar problems
3. **Create Issue**: Submit detailed bug reports or feature requests
4. **Contact Team**: Reach out to the development team for urgent issues

---

**Note**: This React frontend maintains 100% functional parity with the original mainframe COBOL/CICS application while providing a modern, accessible, and performant web interface. All business logic and user workflows are preserved during the technology stack migration.