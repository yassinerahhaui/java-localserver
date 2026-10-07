package utils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private final String id;
    private final long creationTime;
    private volatile long lastAccessedTime;
    private final Map<String, Object> attributes;

    public Session() {
        this(UUID.randomUUID().toString());
    }

    public Session(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.attributes = new ConcurrentHashMap<>();
    }

    public String getId() {
        return id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void access() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public Object getAttribute(String name) {
        if (name == null) return null;
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (name == null) return;
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    public Object removeAttribute(String name) {
        if (name == null) return null;
        return attributes.remove(name);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean isExpired(long maxInactiveIntervalMillis) {
        return (System.currentTimeMillis() - lastAccessedTime) > maxInactiveIntervalMillis;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", creationTime=" + creationTime +
                ", lastAccessedTime=" + lastAccessedTime +
                ", attributes=" + attributes +
                '}';
    }
}
