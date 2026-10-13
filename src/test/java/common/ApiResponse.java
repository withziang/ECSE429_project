package common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public record ApiResponse(int status, HttpHeaders headers, String body) {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    public JsonNode json() {
        try {
            return JSON_MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Response body is not valid JSON: " + body, e);
        }
    }

    /** Ids of the objects in a list response, e.g. idsIn("todos") for {"todos":[...]}. */
    public List<String> idsIn(String collection) {
        List<String> ids = new ArrayList<>();
        json().path(collection).forEach(object -> ids.add(object.path("id").asText()));
        return ids;
    }

    /** First object of a list response, e.g. the todo in GET /todos/:id. */
    public JsonNode firstIn(String collection) {
        return json().path(collection).path(0);
    }

    public Document xml() {
        try {
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(body)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Response body is not valid XML: " + body, e);
        }
    }
}
