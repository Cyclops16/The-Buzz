// src/components/Common/LoginButton.jsx

import React from "react";
import { GoogleLogin } from "@react-oauth/google";
import { useUser } from "../../context/UserContext";
import { useNavigate } from "react-router-dom"; // to redirect user after login
import "./LoginButton.css";

const LoginButton = () => {
  const { user, setUser } = useUser();
  const navigate = useNavigate();

  const handleSuccess = (credentialResponse) => {
    const jwt = credentialResponse.credential;

    // decode jwt to access the user's email
    const base64Url = jwt.split(".")[1];
    const base64 = base64Url.replace(/-g/, "+").replace(/_/g, "/");
    const decodedPayload = JSON.parse(window.atob(base64));

    const email = decodedPayload.email;
    const domain = email.split("@")[1];

    if (domain !== "lehigh.edu") {
      alert("Only Lehigh users are allowed");
      return;
    }

    console.log("Google Login Successful:", credentialResponse);

    // FIXME: send to backend to verify and fetch user info
    // mock it for now
    const mockUser = {
      uId: 99,
      firstName: "Ben",
      lastName: "Hoody",
      displayName: "bhoody009",
      uEmail: "ben@example.com",
      uGI: "He/Him",
      uSO: "Straight",
      note: "Ugh",
      access: true,
    };

    setUser(mockUser);
    navigate("/"); // redirect to home after login
  };

  /*
  const handleError = () => {
    console.log("Google Login Failed");
  };
  */

  return (
    <div>
      {user ? (
        <div>
          <span>Welcome, {user.displayName}!</span>
          <button onClick={() => setUser(null)}>Logout</button>
        </div>
      ) : (
        <GoogleLogin
          onSuccess={handleSuccess}
          onError={() => console.log("Login Failed")}
          /* onError={handleError} 
          useOneTap */
          hosted_domain="lehigh.edu"
        />
      )}
    </div>
  );
};

export default LoginButton;
