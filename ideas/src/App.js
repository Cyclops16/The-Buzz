// App.js
import React, { useState } from 'react';
import './App.css';
import TopBar from './components/TopBar';
import NavBar   from './components/NavBar';
import LoginPage from './components/LoginPage';
import PostMessage from './components/PostMessage';
import AccountPage from './components/AccountPage';

function App() {
  const [showForm, setShowForm] = useState(false);
  const [navVisible, setNavVisible]   = useState(false);
  // 'home', 'account', or 'login'
  const [currentPage, setCurrentPage] = useState('home');
  const [userToken, setUserToken] = useState(null); // store the credential token here

  const toggleNav = () => setNavVisible(visible => !visible);

  const togglePostForm = () => {
    setShowForm(prev => !prev);
  };

  const handleAccountClick = () => {
    setCurrentPage('account');
  };

  const handleLoginClick = () => {
    setCurrentPage('login');  
  };

  const handleHomeClick = () => {
    setCurrentPage('home');
  };

  return (
    <div className="App">
      <TopBar 
        onMenuClick={toggleNav}
        onAddClick={togglePostForm} 
        onAccountClick={handleAccountClick} 
        onHomeClick={handleHomeClick} 
        onLoginClick={handleLoginClick}
        currentPage={currentPage}
      />

      <NavBar
        visible={navVisible}           // or some state if you’re toggling it
        currentPage={currentPage}
        onAddClick={togglePostForm}
        onHomeClick={handleHomeClick}
        onLoginClick={handleLoginClick}
        onAccountClick={handleAccountClick}
      />

      {currentPage === 'home' && (
        <div>
          <h1>Home Page</h1>
          {/* Pass the userToken here as a prop */}
          <PostMessage showForm={showForm} credential={userToken} />
        </div>
      )}
      {currentPage === 'account' && <AccountPage />}
      {currentPage === 'login' && (
        <LoginPage onLoginSuccess={(token) => { 
          console.log("User logged in with token:", token);
          setUserToken(token);  // Save the credential (JWT token) to state.
          setCurrentPage('home');
        }} />
      )}
    </div>
  );
}

export default App;
