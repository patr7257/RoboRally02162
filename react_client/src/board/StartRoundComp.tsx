import React from "react";
import { sendMessage } from "../ws";

interface StartRoundProps {
  jsn: Record<string, any>; // or more specific type if you know the shape
}

export default function StartRound({ jsn }: StartRoundProps) {
  const handleClick = () => {
    sendMessage(jsn);
  };
  console.log(JSON.stringify(jsn));
  return (
    <div>
      <button onClick={handleClick}>Start Round</button>
    </div>
  );
}
