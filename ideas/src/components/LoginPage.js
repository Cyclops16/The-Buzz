// LoginPage.js
import React, { useEffect } from 'react';
import './LoginPage.css';

const LoginPage = ({ onLoginSuccess }) => {
  // Callback function to handle the response from Google.
  const handleCredentialResponse = async (response) => {
    // The response.credential holds the JWT token from Google
    console.log("Encoded JWT ID token:", response.credential);

    try {
      // Send the token to your backend.
      // Ensure the URL is correct (remove the extra slash from before 'https://')
      const backendResponse = await fetch('https://cse216-sp25-team-02.onrender.com/login', 
        {        
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        // Sending the token under the "credential" key as your backend expects.
        body: JSON.stringify({ credential: response.credential }),
        // Include credentials so that session cookies are passed along, if needed.
      });

      if (!backendResponse.ok) {
        throw new Error(`Backend responded with status ${backendResponse.status}`);
      }

      const result = await backendResponse.json();
      console.log("Backend login response:", result);

      // Pass the Google JWT token to App.js state on success
      if (onLoginSuccess) {
        onLoginSuccess(response.credential);
      }

    } catch (error) {
      console.error("Error sending token to backend:", error);
    }
  };

  useEffect(() => {
    // Check if the Google Identity Services SDK is available.
    if (window.google) {
      // Initialize the Google Sign-In client with your Client ID and callback.
      window.google.accounts.id.initialize({
        client_id: '1084206230118-b15atqgqeh8ggsrok2gt54em9e03m43g.apps.googleusercontent.com', // Replace with your actual client ID.
        callback: handleCredentialResponse,
      });

      // Render the Google Sign-In button into the container with id "googleSignInDiv".
      window.google.accounts.id.renderButton(
        document.getElementById("googleSignInDiv"),
        { theme: "outline", size: "large" }
      );

      // Optionally, you may call prompt() for One Tap sign-in.
      // window.google.accounts.id.prompt();
    } else {
      console.error("Google Identity Services SDK not found. Ensure you have added the Google script to your HTML.");
    }
  }, []);

  return (
    <div className="login-page">
      <h2>Login with Google</h2>
      {/* Container where the Google sign-in button will be rendered */}
      <div id="googleSignInDiv"></div>
    </div>
  );
};

export default LoginPage;
