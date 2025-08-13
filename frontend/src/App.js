import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './App.css';

// Import components (to be created)
// import SignOn from './components/screens/SignOn';
// import MainMenu from './components/screens/MainMenu';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <div className="App">
          <header className="App-header">
            <h1>CardDemo - COBOL to Java Migration</h1>
            <p>Credit Card Management System</p>
          </header>
          <main>
            <Routes>
              <Route path="/" element={
                <div>
                  <h2>Welcome to CardDemo</h2>
                  <p>This is the modernized version of the mainframe CardDemo application.</p>
                  <p>The system provides credit card management capabilities including:</p>
                  <ul>
                    <li>User Authentication and Authorization</li>
                    <li>Account Management</li>
                    <li>Transaction Processing</li>
                    <li>Credit Card Management</li>
                    <li>Report Generation</li>
                  </ul>
                </div>
              } />
              {/* Additional routes will be added as components are created */}
            </Routes>
          </main>
        </div>
      </Router>
    </ThemeProvider>
  );
}

export default App;
