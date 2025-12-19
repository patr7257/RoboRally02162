package dk.dtu.service;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.interfaces.GameDatabase;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @author Karl Johannes Agerbo
 */
@Service
public class DemoService {

    private final GameDatabase gameDatabase;

    public DemoService(DynamicGameDatabase gameDatabase) {
        this.gameDatabase = gameDatabase;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public JsonNode loadDemoTemplate(String demoName) throws IOException {
        Resource res = new PathMatchingResourcePatternResolver()
                .getResource("classpath:demo-templates/" + demoName + ".json");

        String json = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return JsonUtil.parser(json);
    }

    public List<String> getDemoTemplates() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:demo-templates/*.json");
        return Arrays.stream(resources).map(r -> r.getFilename().substring(0, r.getFilename().lastIndexOf("."))).toList();
    }

}
