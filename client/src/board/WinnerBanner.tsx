/*
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
*/

import React from "react";

interface WinnerBannerProps {
  winnerId: number;
}

export const WinnerBanner: React.FC<WinnerBannerProps> = ({ winnerId }) => (
  <div className="winner-banner">Player {winnerId} has won the game!</div>
);