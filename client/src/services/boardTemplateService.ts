import { BoardTemplateInfo } from '../types/boardTypes';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

/**
 * Service for fetching board templates from Gateway API
 * @author Patrick Røbel
 */

/**
 * Fetches the list of available board templates from the Gateway
 */
export const fetchBoardTemplates = async (): Promise<BoardTemplateInfo[]> => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/templates/list`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
            },
        });

        if (!response.ok) {
            console.error('Failed to fetch board templates:', response.status);
            return getDefaultTemplates();
        }

        const templates: BoardTemplateInfo[] = await response.json();

        templates.forEach(template => {
            template.imageUrl = getTemplateImageUrl(template.name);
        });

        // Add "Random" as a special option
        templates.push({
            name: "Random",
            difficulty: "Variable",
            maxPlayers: 6,
            gameLength: "Medium",
            imageUrl: "/boardtemplates/random.png"
        });

        return templates;
    } catch (error) {
        console.error('Error fetching board templates:', error);
        return getDefaultTemplates();
    }
};

/**
 * Gets the image URL for a template based on its name
 */
const getTemplateImageUrl = (templateName: string): string => {
    const normalizedName = templateName.toLowerCase().trim();

    const imageMap: { [key: string]: string } = {
        "starter-course": "/boardtemplates/Starter-Course.png",
        "burnout": "/boardtemplates/burnout.png",
        "fractionation": "/boardtemplates/fractionation.png",
        "death-trap": "/boardtemplates/death-trap.png",
        "random": "/boardtemplates/random.png"
    };

    return imageMap[normalizedName] || "/boardtemplates/CustomMap.png";
};

/**
 * Returns default templates if API call fails
 */
const getDefaultTemplates = (): BoardTemplateInfo[] => {
    return [
        {
            name: "Custom Map",
            difficulty: "Variable",
            maxPlayers: 6,
            gameLength: "Medium",
            imageUrl: "/boardtemplates/CustomMap.png"
        }
    ];
};
