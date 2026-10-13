package common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Snapshot of every todo, project and category, used to check side effects and that state was restored. */
public final class SystemState {

    private static final List<String> RESOURCES = List.of("todos", "projects", "categories");

    private final Map<String, String> objectsByPath;

    private SystemState(Map<String, String> objectsByPath) {
        this.objectsByPath = objectsByPath;
    }

    public static SystemState capture() {
        Map<String, String> objects = new TreeMap<>();
        for (String resource : RESOURCES) {
            ApiResponse response = ApiClient.get("/" + resource);
            assertEquals(200, response.status(), "could not read " + resource + ": " + response.body());
            response.json().path(resource).forEach(object ->
                    objects.put(resource + "/" + object.path("id").asText(), canonical(object)));
        }
        return new SystemState(objects);
    }

    /** Paths such as "todos/3" of the objects added, removed or changed between this snapshot and a later one. */
    public Set<String> changedObjects(SystemState later) {
        Set<String> paths = new TreeSet<>(objectsByPath.keySet());
        paths.addAll(later.objectsByPath.keySet());
        paths.removeIf(path -> Objects.equals(objectsByPath.get(path), later.objectsByPath.get(path)));
        return paths;
    }

    // Relationship lists come back in no fixed order, so arrays are compared as sorted lists.
    private static String canonical(JsonNode node) {
        if (node.isObject()) {
            Map<String, String> fields = new TreeMap<>();
            node.properties().forEach(field -> fields.put(field.getKey(), canonical(field.getValue())));
            return fields.toString();
        }
        if (node.isArray()) {
            List<String> items = new ArrayList<>();
            node.forEach(item -> items.add(canonical(item)));
            Collections.sort(items);
            return items.toString();
        }
        return node.asText();
    }
}
