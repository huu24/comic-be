package com.example.comic.config;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class FallbackCacheManager implements CacheManager {

    private final CacheManager primary;
    private final CacheManager fallback;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public FallbackCacheManager(CacheManager primary, CacheManager fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> {
            Cache primaryCache = primary.getCache(n);
            Cache fallbackCache = fallback.getCache(n);
            if (primaryCache == null) {
                return fallbackCache;
            }
            if (fallbackCache == null) {
                return primaryCache;
            }
            return new FallbackCache(primaryCache, fallbackCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return primary.getCacheNames();
    }
}
