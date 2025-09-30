import { sendMessage } from "../ws";

interface ConfirmTurnProps {
  jsn: any;
}

function ConfirmTurnComp({ jsn }: ConfirmTurnProps) {
  console.log(JSON.stringify(jsn))
  return (
    <div>
      
      <button
        onClick={() => {
          sendMessage(jsn);
        }}
      >
        Make Move
      </button>
    </div>
  );
}

export default ConfirmTurnComp;
