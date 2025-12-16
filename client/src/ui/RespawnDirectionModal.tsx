import React, { useEffect, useState } from 'react';
import { ArrowUp, ArrowDown, ArrowLeft, ArrowRight } from 'lucide-react';
import '../styles/RespawnModal.css';

/**
* @author Weihao Mo
*/
interface RespawnDirectionModalProps {
  isOpen: boolean;
  onSelectDirection: (direction: 'NORTH' | 'SOUTH' | 'EAST' | 'WEST') => void;
}

/**
 * @author Weihao Mo
 */
export const RespawnDirectionModal: React.FC<RespawnDirectionModalProps> = ({
  isOpen,
  onSelectDirection,
}) => {
  const [timeRemaining, setTimeRemaining] = useState(10);

  useEffect(() => {
    if (!isOpen) {
      setTimeRemaining(10);
      return;
    }

    const interval = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    const timeout = setTimeout(() => {
      const directions: Array<'NORTH' | 'SOUTH' | 'EAST' | 'WEST'> =
        ['NORTH', 'SOUTH', 'EAST', 'WEST'];
      const randomDirection = directions[Math.floor(Math.random() * directions.length)];
      onSelectDirection(randomDirection);
    }, 10000);

    return () => {
      clearInterval(interval);
      clearTimeout(timeout);
    };
  }, [isOpen, onSelectDirection]);

  if (!isOpen) return null;

  const directions: Array<{
    value: 'NORTH' | 'SOUTH' | 'EAST' | 'WEST';
    label: string;
    icon: React.ReactNode;
  }> = [
      { value: 'NORTH', label: 'North', icon: <ArrowUp size={32} /> },
      { value: 'EAST', label: 'East', icon: <ArrowRight size={32} /> },
      { value: 'SOUTH', label: 'South', icon: <ArrowDown size={32} /> },
      { value: 'WEST', label: 'West', icon: <ArrowLeft size={32} /> },
    ];

  return (
    <div className="respawn-modal-overlay">
      <div className="respawn-modal">
        <h2 className="respawn-modal-title">Your Robot Died!</h2>
        <p className="respawn-modal-subtitle">
          Choose a direction to respawn:
        </p>

        <div className="respawn-timer">
          Auto-selecting in: <strong>{timeRemaining}s</strong>
        </div>

        <div className="respawn-direction-grid">
          {directions.map((dir) => (
            <button
              key={dir.value}
              className="respawn-direction-btn"
              onClick={() => onSelectDirection(dir.value)}
            >
              <div className="respawn-direction-icon">{dir.icon}</div>
              <span className="respawn-direction-label">{dir.label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};