package utils;

import http.HttpRequest;
import http.HttpResponse;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    // 30 minutes expiration
    public static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    public SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Creates and registers a new Session.
     */
    public Session createSession() {
        if (sessions.size() > 2000) {
            cleanExpiredSessions();
        }
        Session session = new Session();
        sessions.put(session.getId(), session);
        return session;
    }

    /**
     * Retrieves an active session by ID. If expired, it is removed and returns null.
     */
    public Session getSession(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        Session session = sessions.get(id);
        if (session == null) {
            return null;
        }

        if (session.isExpired(timeoutMs)) {
            sessions.remove(id);
            return null;
        }

        session.access();
        return session;
    }

    /**
     * Gets an existing valid session or creates a new one.
     */
    public Session getOrCreateSession(String id) {
        Session session = getSession(id);
        if (session == null) {
            session = createSession();
        }
        return session;
    }

    /**
     * Manually invalidates/removes a session.
     */
    public void removeSession(String id) {
        if (id != null) {
            sessions.remove(id);
        }
    }

    /**
     * Purges all expired sessions from memory.
     */
    public void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(timeoutMs));
    }

    public int getActiveSessionsCount() {
        cleanExpiredSessions();
        return sessions.size();
    }

    public Map<String, Session> getAllSessions() {
        cleanExpiredSessions();
        return Collections.unmodifiableMap(sessions);
    }

    /**
     * Automatically handles reading cookie, resolving/creating session,
     * setting Set-Cookie header if new, and tracking visit counts.
     */
    public Session handleRequestSession(HttpRequest request, HttpResponse response) {
        if (request == null) return null;

        String sessionId = request.getCookie("session_id");
        Session session = getSession(sessionId);
        boolean isNew = false;

        if (session == null) {
            session = createSession();
            isNew = true;
        }

        // Track visits attribute
        Object visitsObj = session.getAttribute("visits");
        int visits = (visitsObj instanceof Number) ? ((Number) visitsObj).intValue() + 1 : 1;
        session.setAttribute("visits", visits);

        // If new session or cookie was not sent, add Set-Cookie to response
        if (isNew && response != null) {
            response.addCookie("session_id", session.getId());
        }

        return session;
    }
}
