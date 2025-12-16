import React from 'react';
import { BoardTemplateInfo } from '../types/boardTypes';
import '../styles/gameview.css';
import '../styles/boardTemplates.css';

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
    const sortedTemplates = [...templates].sort((a, b) => {
        // 1. Starter-Course always first
        if (a.name === "Starter-Course") return -1;
        if (b.name === "Starter-Course") return 1;

        // 2. Random always last
        if (a.name === "Random") return 1;
        if (b.name === "Random") return -1;

        // 3. Custom maps after predefined templates
        const aIsCustom = a.imageUrl.endsWith('/CustomMap.png');
        const bIsCustom = b.imageUrl.endsWith('/CustomMap.png');
        if (aIsCustom && !bIsCustom) return 1;
        if (!aIsCustom && bIsCustom) return -1;
        const aName = (a.displayName || a.name).toLowerCase();
        const bName = (b.displayName || b.name).toLowerCase();

        return aName.localeCompare(bName);
    });

    const handleConfirm = () => {
        onClose();
    };

    return (
        <div className="template-viewer-overlay" onClick={handleConfirm}>
            <div className="template-viewer-content" onClick={(e) => e.stopPropagation()}>
                <div className="template-viewer-header">
                    <h2>Choose Board Template</h2>
                    <div className="header-buttons">
                        <button className="confirm-button" onClick={handleConfirm}>
                            Confirm Selection
                        </button>
                        <button className="close-button" onClick={handleConfirm}>✕</button>
                    </div>
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
                                        e.currentTarget.src = '/boardtemplates/CustomMap.png';
                                    }}
                                />
                                {selectedTemplate === template.name && (
                                    <div className="selected-overlay">✓</div>
                                )}
                            </div>

                            <div className="template-info">
                                <h3 className="template-title">{template.displayName || template.name}</h3>
                                <div className="template-details">
                                    <span>Difficulty: {template.difficulty}</span>
                                    <span>Game Length: {template.gameLength}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};
