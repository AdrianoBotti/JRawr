package rawr.data;

/** Thrown when a fetched item payload is not valid XML or does not contain an item. */
public class ItemXmlParseException extends Exception {

    public ItemXmlParseException(String message) {
        super(message);
    }

    public ItemXmlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
