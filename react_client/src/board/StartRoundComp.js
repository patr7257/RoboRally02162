import { sendMessage } from "../ws";

function StartRound({ jsn }) {
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
        start round{" "}
      </button>{" "}
    </div>
  );
}
export default StartRound;