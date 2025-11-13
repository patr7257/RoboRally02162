import React from "react";
import type { TileEffect } from "../types/boardTypes";

type K = TileEffect["kind"];
const registry = new Map<string, (effect: TileEffect) => React.ReactNode>();

/**
 * @author William Pii Jæger
 */
export function registerEffect<K extends TileEffect["kind"]>(kind: K, Comp: React.FC<{ effect: Extract<TileEffect, { kind: K }> }>) {
  const key = String(kind).toLowerCase();
  const bridged = (effect: TileEffect) =>
    effect.kind.toLowerCase() === key ? <Comp effect={effect as any} /> : null;
  registry.set(key, bridged);
}

/**
 * @author William Pii Jæger
 */
export function renderEffect(effect: TileEffect) {
  const key = effect.kind.toLowerCase();
  const renderer = registry.get(key);
  if (!renderer) return <div className={`effect ${key}`} />;
  return renderer(effect);
}
