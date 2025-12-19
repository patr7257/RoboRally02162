package dk.dtu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Service for managing board templates in the Gateway.
 * Loads templates from JSON files and provides them to lobbies.
 * 
 * @author Patrick Røbel
 */
@Service
public class BoardTemplateService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> templates = new HashMap<>();
    
    public BoardTemplateService() {
        loadTemplates();
    }
    
    // Loads all board templates from resources.
    // Automatically discovers all JSON files in the board-templates directory.
    private void loadTemplates() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:board-templates/*.json");
                        
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    JsonNode template = objectMapper.readTree(inputStream);
                    
                    // Extract filename without extension as key (this is also the template name)
                    String filename = resource.getFilename();
                    if (filename != null) {
                        String templateName = filename.replace(".json", "");
                        templates.put(templateName, template);
                        
                    }
                } catch (Exception e) {
                    System.err.println("Error loading template from " + resource.getFilename() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading board templates: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets a board template by name.
     * The name is the filename without the .json extension.
     * 
     * @param templateName Template name (e.g., "Starter-Course", "Burnout")
     * @return The template as JsonNode, or null if not found
     */
    public JsonNode getTemplate(String templateName) {
        return templates.get(templateName);
    }
    
    /**
     * Gets all available template names.
     * Template names are the filenames without .json extension.
     */
    public List<String> getAvailableTemplates() {
        return new ArrayList<>(templates.keySet());
    }
    
    /**
     * Gets template metadata for display purposes.
     * The template identifier (name) is the filename without .json extension.
     * The displayName comes from the JSON's internal "name" field for nice formatting.
     */
    public List<TemplateInfo> getAllTemplateInfo() {
        List<TemplateInfo> infos = new ArrayList<>();
        
        for (Map.Entry<String, JsonNode> entry : templates.entrySet()) {
            String templateKey = entry.getKey(); // filename without .json
            JsonNode template = entry.getValue();
            
            infos.add(new TemplateInfo(
                templateKey, // Use filename as the identifier
                template.has("displayName") ? template.get("displayName").asText() : templateKey, // Use displayName for display
                template.has("imageUrl") ? template.get("imageUrl").asText() : "",
                template.has("gameLength") ? template.get("gameLength").asText() : "Medium",
                template.has("difficulty") ? template.get("difficulty").asText() : "Intermediate",
                template.has("minPlayers") ? template.get("minPlayers").asInt() : 2,
                template.has("maxPlayers") ? template.get("maxPlayers").asInt() : 6
            ));
        }
        
        return infos;
    }
    
    /**
     * Record for template metadata.
     * @param name The template identifier (filename without .json) - used for lookups
     * @param displayName The pretty display name from the JSON file - used for UI
     */
    public record TemplateInfo(
        String name,
        String displayName,
        String imageUrl,
        String gameLength,
        String difficulty,
        int minPlayers,
        int maxPlayers
    ) {}
}
