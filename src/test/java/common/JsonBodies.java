package common;

/** Request bodies shared by several tests. */
public final class JsonBodies {

    private JsonBodies() {
    }

    /** Body that refers to an existing object by id, as the relationship endpoints expect. */
    public static String idReference(String id) {
        return "{\"id\":\"" + id + "\"}";
    }
}
