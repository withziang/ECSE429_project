package common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Request bodies shared by several tests. */
public final class JsonBodies {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private JsonBodies() {
    }

    /** Body that refers to an existing object by id, as the relationship endpoints expect. */
    public static String idReference(String id) {
        return "{\"id\":\"" + id + "\"}";
    }

    /** JSON object with string fields, given as name/value pairs, e.g. object("title", "Lab 2"). */
    public static String object(String... namesAndValues) {
        ObjectNode object = JSON_MAPPER.createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            object.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return object.toString();
    }
}
