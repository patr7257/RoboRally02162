/**
 * @author Bjarke Søderhamn Petersen
 * @author Patrick Røbel
 * @author Asger Allin Jensen
 */

import React, { ReactNode } from "react";
import "./lobby.css";

interface LayoutProps {
  children: ReactNode;
}

export default function Layout({ children }: LayoutProps) {
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
