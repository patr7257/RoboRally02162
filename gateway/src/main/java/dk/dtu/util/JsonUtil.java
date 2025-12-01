package dk.dtu.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */

public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {}

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @return Returns string Json representation of object
     */

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @return  JsonNode representation of string
     * @throws RuntimeException if the string is an invalid json.
     */

    public static JsonNode parser(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public static JsonNode toTree(Object list) {
        return mapper.valueToTree(list);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public static Map<String, String> toMap(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @return Object node to be used to create json tree
     */

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }
}
