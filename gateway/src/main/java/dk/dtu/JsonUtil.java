package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parser(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }
}
