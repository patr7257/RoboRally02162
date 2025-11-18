import React from 'react';
import { BoardTemplateInfo } from '../types/boardTypes';
import './board.css';

/**
 * Component for displaying and selecting board templates
 * @author Patrick Røbel
 */

interface BoardTemplateViewerProps {
    templates: BoardTemplateInfo[];
    selectedTemplate: string;
    onTemplateSelect: (templateName: string) => void;
    onClose: () => void;
}

export const BoardTemplateViewer: React.FC<BoardTemplateViewerProps> = ({
    templates,
    selectedTemplate,
    onTemplateSelect,
    onClose
}) => {
    // Define difficulty order for sorting
    const difficultyOrder: { [key: string]: number } = {
        "Beginner": 1,
        "Intermediate": 2,
        "Hard": 3,
        "Expert": 4,
        "Variable": 5  // Random always last
    };
    
    // Sort templates by difficulty
    const sortedTemplates = [...templates].sort((a, b) => {
        const orderA = difficultyOrder[a.difficulty] || 999;
        const orderB = difficultyOrder[b.difficulty] || 999;
        return orderA - orderB;
    });

    const handleConfirm = () => {
        onClose();
    };

    return (
        <div className="template-viewer-overlay" onClick={handleConfirm}>
            <div className="template-viewer-content" onClick={(e) => e.stopPropagation()}>
                <div className="template-viewer-header">
                    <h2>Choose Board Template</h2>
                    <button className="close-button" onClick={handleConfirm}>✕</button>
                </div>
                
                <div className="templates-grid">
                    {sortedTemplates.map((template) => (
                        <div
                            key={template.name}
                            className={`template-card ${selectedTemplate === template.name ? 'selected' : ''}`}
                            onClick={() => onTemplateSelect(template.name)}
                        >
                            <div className="template-image-container">
                                <img
                                    src={template.imageUrl}
                                    alt={template.displayName || template.name}
                                    className="template-image"
                                    onError={(e) => {
                                        console.log(`Image failed to load for ${template.name}: ${template.imageUrl}`);
                                        e.currentTarget.src = '/boardtemplates/random.png';
                                    }}
                                />
                                {selectedTemplate === template.name && (
                                    <div className="selected-overlay">✓</div>
                                )}
                            </div>
                            
                            <div className="template-info">
                                <h3 className="template-title">{template.displayName || template.name}</h3>
                                <div className="template-details">
                                    <span>Max Players: {template.maxPlayers}</span>
                                    <span>Difficulty: {template.difficulty}</span>
                                    <span>Game Length: {template.gameLength}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
                
                <div className="template-viewer-footer">
                    <button className="confirm-button" onClick={handleConfirm}>
                        Confirm Selection
                    </button>
                </div>
            </div>
        </div>
    );
};
