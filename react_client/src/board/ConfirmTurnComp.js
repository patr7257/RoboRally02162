import { sendMessage } from "../ws";
function ConfirmTurn({ jsn }) {
  return (
    <div>
      {" "}
      {console.log(JSON.stringify(jsn))}{" "}
      <button
        onClick={(e) => {
          sendMessage(jsn);
        }}
      >
        {" "}
        Make Move{" "}
      </button>{" "}
    </div>
  );
}
export default ConfirmTurn;