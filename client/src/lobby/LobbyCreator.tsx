import Reac, { useState, useEffect } from "react";
import Layout from "./Layout";       // import the shared layout
import { useNavigate } from "react-router-dom";
import { BoardTemplateInfo } from '../types/boardTypes';
import { BoardTemplateViewer } from '../board/BoardTemplateViewer';
import { fetchBoardTemplates } from '../services/boardTemplateService';
import "./lobby.css";
import "../styles/lobbyCreator.css";

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

/**
 *@author: Niklas Emil Lysdal
 *@author: Patrick Røbel
 */
export default function LobbyCreator() { //change this name
    const navigate = useNavigate();
    const [lobbyName, setLobbyName] = useState("");
    const [capacity, setCapacity] = useState("");
    const [error, setError] = useState("");
    const [templates, setTemplates] = useState<BoardTemplateInfo[]>([]);
    const [selectedTemplate, setSelectedTemplate] = useState<string>("Random");
    const [showTemplateViewer, setShowTemplateViewer] = useState<boolean>(false);

    useEffect(() => {
        const loadTemplates = async () => {
            const availableTemplates = await fetchBoardTemplates();
            setTemplates(availableTemplates);
        };
        loadTemplates();
    }, []);

    const handleTemplateSelect = (templateName: string) => {
        setSelectedTemplate(templateName);
        // Don't close the viewer - let user confirm with button
    };
    
    const handleTemplateConfirm = () => {
        setShowTemplateViewer(false);
    };
    
    /**
     *@author: Niklas Emil Lysdal
     */
    const finishCreation = async () => {


        try {
            const response = await fetch(API_BASE_URL + "/api/lobby/create", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
                },
                body: JSON.stringify({ capacity: capacity, lobbyName: lobbyName, boardTemplate: selectedTemplate  }),
            });
            //console.log("Reached backend. Status:", response.status);
            if (!response.ok) {

                const errorCode = await response.text();
                switch (errorCode) {
                    case "INVALID_TOKEN":
                        setError("Could not authenticate request (invalid token).");
                        break;
                    case "MISSING_WEBSOCKET_CONNECTION":
                        setError("Missing connection to server - please try login again.");
                        break;
                    case "MISSING_LOBBY_NAME":
                        setError("Please enter a lobby name.");
                        break;
                    case "MISSING_CAPACITY":
                        setError("Please enter a player limit.");
                        break;
                    case "INVALID_CAPACITY":
                        setError("Invalid player limit (must be 1-6).");
                        break;
                    case "LOBBY_NAME_ALREADY_EXISTS":
                        setError("Lobby name already in use. Please choose another. ")
                        break;
                    default:
                        setError("Something went wrong, please try again.")



                }
                return;
            }
            const data = await response.text();

            //TODO: use reponse body to navigate to lobby scene.
            sessionStorage.setItem("id", data);

            //parse data to lobbyInfo

            //sessionStorage.setItem("lobbyInfo",JSON.stringify(data));
            navigate("/lobbyCreationScene")
            return;
        } catch (err) {
            console.error("create lobby error:", err);
            if (setError) setError("Network error. Try Again.");
        }
    }
    //number input accepts 'e' for scientific notation, this function prevents that.
    /**
     *@author: Niklas Emil Lysdal
     */
    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        const { key, metaKey, ctrlKey, altKey } = event;
        if (key.length > 1 || metaKey || ctrlKey || altKey) { //don't intercept editing, only input
            return;
        }
        if (/\d/.test(key)) { //if digit, allow input
            return;
        }
        event.preventDefault(); //all other cases, don't allow input
    };
    //number input accepts 'e' for scientific notation.
    /**
     * 
     *@author: Niklas Emil Lysdal
     */
    const handleCapacityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value: string = e.target.value
        if (value === "") {
            setCapacity("");
            return;
        }
        const num: number = parseInt(value, 10);
        if (num > 6 || num < 1) {
            setError("Player limit must be in range 1-6");
            return;
        }
        setError("");
        setCapacity(value);
    }

    return (
        <Layout>
            <h1> Create Lobby</h1>
            <div className="lobby-settings-panel">
                <div className="lobby-settings" >


                    <div className="form">
                        <div className="form-row">
                            <label htmlFor="lobbyName">Name</label>
                            <input
                                type="text"
                                value={lobbyName}
                                maxLength={15} // Limit lobby name length
                                onChange={(e) => setLobbyName(e.target.value)}
                                placeholder="Enter lobby name"

                            />
                        </div>
                        <div className="form-row">
                            <label htmlFor="lobbyName">Player limit</label>
                            <input
                                type="number"
                                value={capacity}
                                max="6"
                                min="1"
                                onChange={(e) => handleCapacityChange(e)}
                                onKeyDown={handleKeyDown}
                                placeholder="Enter capacity (1-6)"
                            />
                        </div>
                        
                        <div className="form-row">
                            <label htmlFor="boardTemplate">Map</label>
                            <div className="template-selector">
                                <span className="selected-template-name">
                                    {templates.find(t => t.name === selectedTemplate)?.displayName || selectedTemplate}
                                </span>
                                <button 
                                    type="button"
                                    className="choose-template-button"
                                    onClick={() => setShowTemplateViewer(true)}
                                >
                                    Choose Template
                                </button>
                            </div>
                        </div>

                    </div>
                </div>


                <div className="settings-buttons">

                    <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
                        Cancel
                    </button>

                    <button className="metal-button" onClick={() => finishCreation()}>
                        Finish
                    </button>

                </div>

                <div className="error-text">
                    <label> {error} </label>
                </div>
            </div>
            
            {showTemplateViewer && (
                <BoardTemplateViewer
                    templates={templates}
                    selectedTemplate={selectedTemplate}
                    onTemplateSelect={handleTemplateSelect}
                    onClose={handleTemplateConfirm}
                />
            )}
        </Layout>

    );
}