package com.tradebridge.common.event;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parsed representation of an event type string such as
 * {@code identity.user.registered.v1} → domain=identity,
 * name=user.registered, version=1.
 */
public record EventType(String domain, String name, int version) {

    private static final Pattern VERSION_SUFFIX = Pattern.compile(".*\\.v(\\d+)$");

    public EventType {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(name, "name");
        if (version <= 0) {
            throw new IllegalArgumentException("Event version must be >= 1");
        }
    }

    public static EventType parse(String rawType) {
        Objects.requireNonNull(rawType, "eventType");
        var matcher = VERSION_SUFFIX.matcher(rawType);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Event type must end with '.v<number>': " + rawType);
        }
        int version = Integer.parseInt(matcher.group(1));
        String withoutVersion = rawType.substring(0, rawType.lastIndexOf(".v"));
        int dot = withoutVersion.indexOf('.');
        if (dot <= 0) {
            throw new IllegalArgumentException("Event type must be '<domain>.<name>.v<n>': " + rawType);
        }
        String domain = withoutVersion.substring(0, dot);
        String name = withoutVersion.substring(dot + 1);
        return new EventType(domain, name, version);
    }

    public String canonical() {
        return domain + "." + name + ".v" + version;
    }

    @Override
    public String toString() {
        return canonical();
    }
}