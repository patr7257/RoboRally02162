import React from "react";

/**
* @author Weihao Mo
*/

interface WinnerBannerProps {
  winnerId: number;
  robotIdToUsername?: Record<number, string>;
}

export const WinnerBanner: React.FC<WinnerBannerProps> = ({ winnerId, robotIdToUsername }) => {
  const displayName = robotIdToUsername?.[winnerId];
  return (
    <div className="winner-banner">{displayName} has won the game!</div>
  );
};