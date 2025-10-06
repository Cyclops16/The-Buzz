import React, { useState } from 'react';
import Button from './Button';
import './NavBar.css';


function NavBar({ onAddClick, visible, onAccountClick, onHomeClick, onLoginClick, currentPage }) {
  console.log('NavBar visible prop:', visible);

  const [hovered, setHovered] = useState(null);

   // Map each key to its two public‑folder URLs
   const icons = {
    home: {
      normal:  `${process.env.PUBLIC_URL}/buttons/home-button.png`,
      selected:`${process.env.PUBLIC_URL}/buttons/home-button-selected.png`
    },
    login: {
      normal:  `${process.env.PUBLIC_URL}/buttons/login-button.png`,
      selected:`${process.env.PUBLIC_URL}/buttons/login-button-selected.png`
    },
    account: {
      normal:  `${process.env.PUBLIC_URL}/buttons/account-button.png`,
      selected:`${process.env.PUBLIC_URL}/buttons/account-button-selected.png`
    },
  };

  // choose selected if hovered _or_ it matches currentPage
  const pickSrc = key =>
    (hovered === key || currentPage === key)
      ? icons[key].selected
      : icons[key].normal;

  return (
    <nav className={`navbar ${visible ? 'visible' : ''}`}>
      <ul>
        <li>
          <Button onClick={onAddClick} className="add-button">
            +
          </Button>
        </li>

        <li>
          <Button onClick={onHomeClick} className="home-button" onMouseEnter={() => setHovered('home')} onMouseLeave={() => setHovered(null)}>
            <img src={pickSrc('home')} alt="Home" className="home-icon"/>
          </Button>
        </li>


        <li>
          <Button onClick={onLoginClick} className="login-button" onMouseEnter={() => setHovered('login')} onMouseLeave={() => setHovered(null)}>
            <img src={pickSrc('login')} alt="Login" className="login-icon"/>
          </Button>
        </li>

        <li>
          <Button onClick={onAccountClick} className="my-account-button" onMouseEnter={() => setHovered('account')} onMouseLeave={() => setHovered(null)}>
            <img src={pickSrc('account')} alt="My Account" className="my-account-icon"/>
          </Button>
        </li>

        
      </ul>
    </nav>
  );
}

export default NavBar;
