/*
Author(s): Bjarke, Asger, Patrick
*/

import React from "react";

export default function Layout({ children }) {
  return (
    <div className="App">
      <header className="App-header">
        <img
          src="/roborallyLogo.webp"
          alt="RoboRally Logo"
          className="logo"
        />

        <div className="page-content">
          {children}
        </div>
      </header>
    </div>
  );
}

