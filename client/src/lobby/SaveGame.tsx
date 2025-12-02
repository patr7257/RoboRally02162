/** 
* @author Bjarke Søderhamn Petersen
* @author Karl Johannes Agerbo
* @author Benjamin Benyo Endahl Hansen
*/

export { saveGame };
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

/** 
* @author Bjarke Søderhamn Petersen
* @author Karl Johannes Agerbo
* @author Benjamin Benyo Endahl Hansen
*/
const saveGame = async (
  lobbyId: string
): Promise<void> => {
  try {
    const response = await fetch(API_BASE_URL + "/api/game/save", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
      },
      body: JSON.stringify({ lobbyID: lobbyId }),
    });
    if (!response.ok) {
      throw new Error("Server returned error when saving game");
    }
  } catch (err) {
    console.error("leave lobby error:", err);
  }
}