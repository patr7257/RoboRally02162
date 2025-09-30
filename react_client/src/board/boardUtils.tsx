export const getFacingArrow = (facing: string): string =>
  ({
    N: "↑",
    E: "→",
    S: "↓",
    W: "←",
  }[facing] || "●");

export const getRobotColor = (id: number): string => {
  const colors = [
    "#ff4444",
    "#44ff44",
    "#4444ff",
    "#ffff44",
    "#ff44ff",
    "#44ffff",
  ];
  return colors[(id - 1) % colors.length];
};
