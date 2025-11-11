import React from "react";
import type { Board, Robot } from "../types/boardTypes";

/*
* @author Weihao Mo
*/

function getCheckpointNumbers(board: Board): number[] {
  const set = new Set<number>();
  board.tiles.forEach(row =>
    row.forEach(tile =>
      tile.effects?.forEach(e => {
        const kind = String((e as any).kind || "").toLowerCase();
        if (kind === "checkpoint") {
          const raw = (e as any).number;
          const num = typeof raw === "number" ? raw : Number(raw);
          if (Number.isFinite(num)) set.add(num);
        }
      })
    )
  );
  return [...set].sort((a, b) => a - b);
}

function hitsFromNext(nextCheckpoint: number): Set<number> {
  const n = Math.max(Number(nextCheckpoint) - 1, 0);
  return new Set(Array.from({ length: n }, (_, i) => i + 1));
}

export default function CheckpointChecklist({ board, robots }: { board: Board; robots: Robot[] }) {
  const checkpoints = getCheckpointNumbers(board);
  if (!checkpoints.length) {
    return <div className="ck-lists"><em>No checkpoints found.</em></div>;
  }

  return (
    <div className="ck-lists">
      {robots.map(r => {
        const hits = hitsFromNext(r.nextCheckpoint);
        return (
          <div key={r.id} className="ck-player">
            <div className="ck-player-name">Player {r.id}</div>
            <div className="ck-row">
              {checkpoints.map(n => (
                <label key={n} className="ck-item" title={`Player ${r.id} checkpoint ${n}`}>
                  <input type="checkbox" checked={hits.has(n)} readOnly />
                  <span> {n}</span>
                </label>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}
