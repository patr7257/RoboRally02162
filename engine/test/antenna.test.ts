import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Phase } from "../src/core/phase.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { Antenna } from "../src/effects/antenna.js";
import { initEmptyBoard } from "./util/boardTestUtils.js";

/** Ported from dk.dtu.AntennaTest. */
describe("Antenna", () => {
  it("orders by distance", () => {
    const b = initEmptyBoard(9, 9);
    const antenna = new Antenna(Direction.N);
    const ant = b.getTile(3, 3);

    const r1 = new Robot(1, 4, 3, Direction.E);
    const r2 = new Robot(2, 4, 4, Direction.E);
    const r3 = new Robot(3, 3, 7, Direction.E);
    const r4 = new Robot(4, 0, 4, Direction.E);

    const api = new BoardApiImpl(b, [r1, r2, r3, r4]);
    antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

    const order = api.getRobotsByPriority().map((r) => r.getId());
    expect(order).toEqual([1, 2, 3, 4]);
  });

  it("orders by clockwise", () => {
    const b = initEmptyBoard(7, 7);
    const antenna = new Antenna(Direction.N);
    const ant = b.getTile(3, 3);

    const r = new Robot(1, 3, 2, Direction.E);
    const r2 = new Robot(2, 3, 4, Direction.E);
    const r3 = new Robot(3, 4, 3, Direction.E);
    const r4 = new Robot(4, 2, 3, Direction.E);
    const r5 = new Robot(5, 6, 3, Direction.E);

    const api = new BoardApiImpl(b, [r, r2, r3, r4, r5]);
    antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

    const order = api.getRobotsByPriority().map((x) => x.getId());
    expect(order).toEqual([1, 3, 2, 4, 5]);
  });

  it("diagonals are ordered by true angle clockwise", () => {
    const b = initEmptyBoard(7, 7);
    const antenna = new Antenna(Direction.N);
    const ant = b.getTile(3, 3);

    const r1 = new Robot(1, 4, 2, Direction.E);
    const r2 = new Robot(2, 4, 4, Direction.E);
    const r3 = new Robot(3, 2, 4, Direction.E);
    const r4 = new Robot(4, 2, 2, Direction.E);

    const api = new BoardApiImpl(b, [r1, r2, r3, r4]);
    antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

    const order = api.getRobotsByPriority().map((r) => r.getId());
    expect(order).toEqual([1, 2, 3, 4]);
  });

  it("mixed distance tie east", () => {
    const b = initEmptyBoard(9, 9);
    const antenna = new Antenna(Direction.E);
    const ant = b.getTile(4, 4);

    const r1 = new Robot(1, 5, 4, Direction.N);
    const r2 = new Robot(2, 4, 3, Direction.N);
    const r3 = new Robot(3, 4, 6, Direction.N);
    const r4 = new Robot(4, 7, 4, Direction.N);

    const api = new BoardApiImpl(b, [r1, r2, r3, r4]);
    antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

    const order = api.getRobotsByPriority().map((r) => r.getId());
    expect(order).toEqual([1, 2, 3, 4]);
  });

  it("mixed distance tie south", () => {
    const b = initEmptyBoard(9, 9);
    const antenna = new Antenna(Direction.S);
    const ant = b.getTile(4, 4);

    const r1 = new Robot(1, 4, 5, Direction.N);
    const r2 = new Robot(2, 5, 4, Direction.N);
    const r3 = new Robot(3, 3, 4, Direction.N);
    const r4 = new Robot(4, 4, 7, Direction.N);

    const api = new BoardApiImpl(b, [r1, r2, r3, r4]);
    antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

    const order = api.getRobotsByPriority().map((r) => r.getId());
    expect(order).toEqual([1, 3, 2, 4]);
  });
});
