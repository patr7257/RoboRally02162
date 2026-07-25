import React from "react";

/**
 * @author Weihao Mo
 */

interface PostGamePanelProps {
  winnerId: number;
  robotIdToUsername?: Record<number, string>;
  /** Only the acting host can PUT the rematch state, so only the host tab
   *  gets the "Play again" button (issue #10). */
  isHost: boolean;
  onPlayAgain: () => void;
  onBackToMenu: () => void;
}

/**
 * Post-game overlay (issue #10): announces the winner, offers the host a
 * rematch on the same roster and board, and lets anyone leave to the main
 * menu. Kept in WinnerBanner.tsx (the file that used to be a bare winner
 * banner) to limit churn; the exported component is now PostGamePanel.
 */
export const PostGamePanel: React.FC<PostGamePanelProps> = ({
  winnerId,
  robotIdToUsername,
  isHost,
  onPlayAgain,
  onBackToMenu,
}) => {
  const displayName = robotIdToUsername?.[winnerId] ?? `Robot ${winnerId}`;
  return (
    <div className="postgame-overlay" role="dialog" aria-modal="true">
      <div className="postgame-modal">
        <p className="postgame-title">{displayName} has won the game!</p>
        <div className="postgame-buttons">
          {isHost && (
            <button className="metal-button" onClick={onPlayAgain}>
              Play again
            </button>
          )}
          <button className="metal-button" onClick={onBackToMenu}>
            Back to menu
          </button>
        </div>
      </div>
    </div>
  );
};
