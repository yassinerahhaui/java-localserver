package http;

/**
 * Tracks the current state of the HTTP parser when reading asynchronous network data.
 */
public enum ParserState {
    READING_REQUEST_LINE,
    READING_HEADERS,
    READING_BODY,
    DONE,
    ERROR
}