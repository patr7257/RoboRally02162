

import React from "react";

/**
* @author Weihao Mo
*/

interface WinnerBannerProps {
  winnerId: number;
}

export const WinnerBanner: React.FC<WinnerBannerProps> = ({ winnerId }) => (
  <div className="winner-banner">Player {winnerId} has won the game!</div>
);