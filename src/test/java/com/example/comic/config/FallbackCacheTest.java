package com.example.comic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FallbackCacheTest {

    private Cache primary;
    private Cache fallback;
    private FallbackCache fallbackCache;

    @BeforeEach
    void setUp() {
        primary = mock(Cache.class);
        fallback = mock(Cache.class);
        fallbackCache = new FallbackCache(primary, fallback);
        when(primary.getName()).thenReturn("testCache");
        when(fallback.getName()).thenReturn("testCache");
    }

    @Test
    void getName_shouldReturnPrimaryName() {
        assertEquals("testCache", fallbackCache.getName());
    }

    @Test
    void get_whenPrimarySucceeds_shouldReturnPrimaryValue() {
        Cache.ValueWrapper mockWrapper = mock(Cache.ValueWrapper.class);
        when(primary.get("key")).thenReturn(mockWrapper);

        Cache.ValueWrapper result = fallbackCache.get("key");

        assertEquals(mockWrapper, result);
        verify(primary).get("key");
        verify(fallback, never()).get(any());
    }

    @Test
    void get_whenPrimaryFails_shouldFallbackToSecondaryAndMarkFailed() {
        when(primary.get("key")).thenThrow(new RuntimeException("Redis connection refused"));
        Cache.ValueWrapper mockWrapper = mock(Cache.ValueWrapper.class);
        when(fallback.get("key")).thenReturn(mockWrapper);

        Cache.ValueWrapper result = fallbackCache.get("key");

        assertEquals(mockWrapper, result);
        verify(primary).get("key");
        verify(fallback).get("key");

        // Subsequent call should skip primary immediately due to fast-fail flag
        reset(primary);
        fallbackCache.get("key");
        verify(primary, never()).get(any());
        verify(fallback, times(2)).get("key");
    }

    @Test
    void put_whenPrimaryFails_shouldFallbackToSecondary() {
        doThrow(new RuntimeException("Redis error")).when(primary).put("key", "value");

        assertDoesNotThrow(() -> fallbackCache.put("key", "value"));

        verify(primary).put("key", "value");
        verify(fallback).put("key", "value");
    }

    @Test
    void evict_whenPrimaryFails_shouldFallbackToSecondary() {
        doThrow(new RuntimeException("Redis error")).when(primary).evict("key");

        assertDoesNotThrow(() -> fallbackCache.evict("key"));

        verify(primary).evict("key");
        verify(fallback).evict("key");
    }

    @Test
    void clear_whenPrimaryFails_shouldFallbackToSecondary() {
        doThrow(new RuntimeException("Redis error")).when(primary).clear();

        assertDoesNotThrow(() -> fallbackCache.clear());

        verify(primary).clear();
        verify(fallback).clear();
    }

    @Test
    void getWithCallable_whenPrimaryFails_shouldFallbackToSecondary() {
        Callable<String> loader = () -> "newValue";
        when(primary.get(eq("key"), any(Callable.class))).thenThrow(new RuntimeException("Redis error"));
        when(fallback.get(eq("key"), any(Callable.class))).thenReturn("newValue");

        String result = fallbackCache.get("key", loader);

        assertEquals("newValue", result);
        verify(primary).get(eq("key"), any(Callable.class));
        verify(fallback).get(eq("key"), any(Callable.class));
    }
}
