package com.example.comic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import java.util.concurrent.Callable;

@Slf4j
public class FallbackCache implements Cache {
    private final Cache primary;
    private final Cache fallback;
    private volatile boolean redisFailed = false;
    private volatile long lastAttemptTime = 0;
    private static final long RETRY_INTERVAL_MS = 60000; // 1 minute

    public FallbackCache(Cache primary, Cache fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    private Cache getActiveCache() {
        if (redisFailed) {
            long now = System.currentTimeMillis();
            if (now - lastAttemptTime > RETRY_INTERVAL_MS) {
                // Periodically retry primary
                return primary;
            }
            return fallback;
        }
        return primary;
    }

    private void handleException(Exception e) {
        if (!redisFailed) {
            log.warn("Redis cache operation failed, falling back to Caffeine local cache. Error: {}", e.getMessage());
            redisFailed = true;
        }
        lastAttemptTime = System.currentTimeMillis();
    }

    private void resetFallbackIfSuccessful() {
        if (redisFailed) {
            log.info("Redis cache operation succeeded, recovering from Caffeine fallback.");
            redisFailed = false;
        }
    }

    @Override
    public String getName() {
        return primary.getName();
    }

    @Override
    public Object getNativeCache() {
        Cache active = getActiveCache();
        try {
            Object nativeCache = active.getNativeCache();
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return nativeCache;
        } catch (Exception e) {
            handleException(e);
            return fallback.getNativeCache();
        }
    }

    @Override
    public ValueWrapper get(Object key) {
        Cache active = getActiveCache();
        try {
            ValueWrapper wrapper = active.get(key);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return wrapper;
        } catch (Exception e) {
            handleException(e);
            return fallback.get(key);
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Cache active = getActiveCache();
        try {
            T value = active.get(key, type);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return value;
        } catch (Exception e) {
            handleException(e);
            return fallback.get(key, type);
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Cache active = getActiveCache();
        try {
            T value = active.get(key, valueLoader);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return value;
        } catch (Exception e) {
            handleException(e);
            return fallback.get(key, valueLoader);
        }
    }

    @Override
    public void put(Object key, Object value) {
        Cache active = getActiveCache();
        try {
            active.put(key, value);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
        } catch (Exception e) {
            handleException(e);
            fallback.put(key, value);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Cache active = getActiveCache();
        try {
            ValueWrapper wrapper = active.putIfAbsent(key, value);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return wrapper;
        } catch (Exception e) {
            handleException(e);
            return fallback.putIfAbsent(key, value);
        }
    }

    @Override
    public void evict(Object key) {
        Cache active = getActiveCache();
        try {
            active.evict(key);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
        } catch (Exception e) {
            handleException(e);
            fallback.evict(key);
        }
    }

    @Override
    public boolean evictIfPresent(Object key) {
        Cache active = getActiveCache();
        try {
            boolean evicted = active.evictIfPresent(key);
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return evicted;
        } catch (Exception e) {
            handleException(e);
            return fallback.evictIfPresent(key);
        }
    }

    @Override
    public void clear() {
        Cache active = getActiveCache();
        try {
            active.clear();
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
        } catch (Exception e) {
            handleException(e);
            fallback.clear();
        }
    }

    @Override
    public boolean invalidate() {
        Cache active = getActiveCache();
        try {
            boolean invalidated = active.invalidate();
            if (active == primary) {
                resetFallbackIfSuccessful();
            }
            return invalidated;
        } catch (Exception e) {
            handleException(e);
            return fallback.invalidate();
        }
    }
}
