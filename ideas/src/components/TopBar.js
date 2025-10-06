import React from 'react';
import './TopBar.css';
import './NavBar.css';
import NavBar from './NavBar';

function TopBar({ onAddClick, onAccountClick, onHomeClick, onLoginClick, currentPage}) {
  const [navbarVisible, setNavbarVisible] = React.useState(false);

  const toggleNavbar = () => {
    setNavbarVisible(prev => !prev);
  };

  return (
    <div>
      <div className="TopBar">
        <button className="ToggleButton" onClick={toggleNavbar}>☰</button>
        <h2 className="TopBarTitle">Home</h2>
      </div>
      {/* Pass navbarVisible and callbacks to NavBar */}
      <NavBar 
        visible={navbarVisible}
        currentPage={currentPage} 
        onAddClick={onAddClick} 
        onAccountClick={onAccountClick} 
        onHomeClick={onHomeClick}
        onLoginClick={onLoginClick}
        
      />
    </div>
  );
}

export default TopBar;
