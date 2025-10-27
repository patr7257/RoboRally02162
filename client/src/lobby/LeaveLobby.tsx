/*
Author(s): Niklas
*/
   export {leaveLobby};
   const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

   const leaveLobby = async (
    lobbyId: string,
    userID: string|null, //to be changed to UUID
    setError: ((msg:string) =>void) | null //allow to not have error handling
   ):Promise<void> => {
    if (setError) setError("");
    if (userID === null) {
        throw new Error("Attempted to leave lobby without providing username");
    }
    try 
    {
      // TODO: ensure server verifies caller is the lobby owner before deleting
      // Right now anyone can delete any lobby 
      const response = await fetch(API_BASE_URL+"/api/lobby/leave", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify({lobbyID: lobbyId, userID:userID}),
      });
      if (!response.ok) {
        throw new Error("Server returned error when leaving lobby");
      }
     } catch (err) {
      console.error("leave lobby error:",err);
      if (setError) setError("Network error. Try Again.");
     }
  }