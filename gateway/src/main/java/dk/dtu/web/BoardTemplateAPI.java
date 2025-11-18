package dk.dtu.web;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.service.BoardTemplateService;
import dk.dtu.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for board templates.
 * 
 * @author Patrick Røbel
 */
@RestController
@RequestMapping("/api")
public class BoardTemplateAPI {
    
    private final BoardTemplateService boardTemplateService;
    
    public BoardTemplateAPI(BoardTemplateService boardTemplateService) {
        this.boardTemplateService = boardTemplateService;
    }
    
    // Get all available board templates with metadata.
    @GetMapping("/templates/list")
    public ResponseEntity<String> listTemplates() {
        List<BoardTemplateService.TemplateInfo> templates = boardTemplateService.getAllTemplateInfo();
        String response = JsonUtil.toJson(templates);
        return ResponseEntity.ok(response);
    }
    
    // Get a specific board template by name.
    @PostMapping("/templates/get")
    public ResponseEntity<String> getTemplate(@RequestBody JsonNode request) {
        if (!request.has("templateName")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MISSING_TEMPLATE_NAME");
        }
        
        String templateName = request.get("templateName").asText();
        JsonNode template = boardTemplateService.getTemplate(templateName);
        
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TEMPLATE_NOT_FOUND");
        }
        
        String response = JsonUtil.toJson(template);
        return ResponseEntity.ok(response);
    }
}
