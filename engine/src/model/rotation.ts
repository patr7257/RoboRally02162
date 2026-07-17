/** Ported from dk.dtu.domain.model.Rotation. */
export const Rotation = {
  NONE: "NONE",
  LEFT: "LEFT",
  RIGHT: "RIGHT",
} as const;

export type Rotation = (typeof Rotation)[keyof typeof Rotation];
