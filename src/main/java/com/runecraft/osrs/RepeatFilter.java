package com.runecraft.osrs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lets a message through to each key (a player) only when it differs from the last one let through, or once {@code windowMc}
 * Minecraft ticks have passed since then (round 43: the single-way refusal line on the action bar, which a held attack key
 * re-sent twice a second). A clock that went backwards (a self-test's skewed clock) lets the message through.
 */
public final class RepeatFilter<K> {
    private record Shown(String message, long at) {
    }

    private final long windowMc;
    private final Map<K, Shown> last = new HashMap<>();

    public RepeatFilter(long windowMc) {
        this.windowMc = windowMc;
    }

    /** Whether {@code message} should be shown to {@code key} now ({@code nowMc}); if so, it is remembered as shown. */
    public synchronized boolean fresh(K key, String message, long nowMc) {
        Shown s = last.get(key);
        if (s != null && Objects.equals(s.message(), message) && nowMc >= s.at() && nowMc - s.at() < windowMc) {
            return false;
        }
        last.put(key, new Shown(message, nowMc));
        return true;
    }

    public synchronized void forget(K key) {
        last.remove(key);
    }
}
