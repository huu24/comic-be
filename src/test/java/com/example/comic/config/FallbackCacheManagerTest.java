package com.example.comic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FallbackCacheManagerTest {

    private CacheManager primary;
    private CacheManager fallback;
    private FallbackCacheManager fallbackCacheManager;

    @BeforeEach
    void setUp() {
        primary = mock(CacheManager.class);
        fallback = mock(CacheManager.class);
        fallbackCacheManager = new FallbackCacheManager(primary, fallback);
    }

    @Test
    void getCache_shouldCreateFallbackCache() {
        Cache primaryCache = mock(Cache.class);
        Cache fallbackCache = mock(Cache.class);
        when(primaryCache.getName()).thenReturn("test");
        when(fallbackCache.getName()).thenReturn("test");
        when(primary.getCache("test")).thenReturn(primaryCache);
        when(fallback.getCache("test")).thenReturn(fallbackCache);

        Cache result = fallbackCacheManager.getCache("test");

        assertNotNull(result);
        assertInstanceOf(FallbackCache.class, result);
        assertEquals("test", result.getName());
    }

    @Test
    void getCache_whenPrimaryIsNull_shouldReturnFallbackCacheDirectly() {
        Cache fallbackCache = mock(Cache.class);
        when(fallback.getCache("test")).thenReturn(fallbackCache);
        when(primary.getCache("test")).thenReturn(null);

        Cache result = fallbackCacheManager.getCache("test");

        assertEquals(fallbackCache, result);
    }

    @Test
    void getCache_whenFallbackIsNull_shouldReturnPrimaryCacheDirectly() {
        Cache primaryCache = mock(Cache.class);
        when(primary.getCache("test")).thenReturn(primaryCache);
        when(fallback.getCache("test")).thenReturn(null);

        Cache result = fallbackCacheManager.getCache("test");

        assertEquals(primaryCache, result);
    }

    @Test
    void getCacheNames_shouldReturnPrimaryCacheNames() {
        when(primary.getCacheNames()).thenReturn(Collections.singleton("test"));

        assertEquals(Collections.singleton("test"), fallbackCacheManager.getCacheNames());
    }
}
