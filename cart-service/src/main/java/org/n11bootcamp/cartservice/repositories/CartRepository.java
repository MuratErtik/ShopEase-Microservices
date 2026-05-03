package org.n11bootcamp.cartservice.repositories;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.cartservice.models.CartItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepository {

    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${cart.ttl-minutes:30}")
    private long ttlMinutes;

    // cart:{userId} → {productId: CartItem}
    private String buildKey(UUID userId) {
        return CART_KEY_PREFIX + userId.toString();
    }

    public void saveItem(UUID userId, CartItem item) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().put(key, item.getProductId().toString(), item);
        refreshTtl(key);
        log.debug("Cart item saved. userId={}, productId={}", userId, item.getProductId());
    }

    public void removeItem(UUID userId, UUID productId) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().delete(key, productId.toString());
        refreshTtl(key);
        log.debug("Cart item removed. userId={}, productId={}", userId, productId);
    }

    public CartItem findItem(UUID userId, UUID productId) {
        String key = buildKey(userId);
        Object raw = redisTemplate.opsForHash().get(key, productId.toString());
        if (raw == null) return null;
        return redisObjectMapper.convertValue(raw, CartItem.class);
    }

    public List<CartItem> findAllItems(UUID userId) {
        String key = buildKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return Collections.emptyList();

        return entries.values().stream()
                .map(raw -> redisObjectMapper.convertValue(raw, CartItem.class))
                .collect(Collectors.toList());
    }

    public void clearCart(UUID userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        log.debug("Cart cleared. userId={}", userId);
    }

    public boolean cartExists(UUID userId) {
        String key = buildKey(userId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public boolean itemExists(UUID userId, UUID productId) {
        String key = buildKey(userId);
        return redisTemplate.opsForHash().hasKey(key, productId.toString());
    }

    public long getItemCount(UUID userId) {
        String key = buildKey(userId);
        return redisTemplate.opsForHash().size(key);
    }

    private void refreshTtl(String key) {
        redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
    }
}
