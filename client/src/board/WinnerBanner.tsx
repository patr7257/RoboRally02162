/*
Author(s): Bjarke, Asger, Patrick
*/

import React from "react";

interface WinnerBannerProps {
  winnerId: number;
}

export const WinnerBanner: React.FC<WinnerBannerProps> = ({ winnerId }) => (
  <div className="winner-banner">Player {winnerId} has won the game!</div>
);