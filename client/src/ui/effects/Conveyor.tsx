import { registerEffect } from "../effectRegistry";

// Author(s): William Pii Jæger

// Left an example here of how a conveyor would look with our new method
export default function Conveyor({ effect }: { effect: { kind:"conveyor"; id:string; dir:string; speed:1|2 }}) {
  return <div className={`conveyor dir-${effect.dir}`}>{effect.speed}x</div>;
}
registerEffect("conveyor", Conveyor);