package com.parkease.parking.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvailabilityCounterServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AvailabilityCounterServiceImpl counterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void initCounter_ShouldSetRedisKey() {
        counterService.initCounter(1L, 10);
        verify(valueOperations).set(eq("availability:lot:1"), eq("10"));
    }

    @Test
    void increment_ShouldCallRedisIncr() {
        counterService.increment(1L);
        verify(valueOperations).increment("availability:lot:1");
    }

    @Test
    void decrement_ShouldCallRedisDecr_WhenSpotsAvailable() {
        when(valueOperations.get("availability:lot:1")).thenReturn("5");

        counterService.decrement(1L);

        verify(valueOperations).decrement("availability:lot:1");
    }

    @Test
    void decrement_ShouldThrow_WhenNoSpotsAvailable() {
        when(valueOperations.get("availability:lot:1")).thenReturn("0");

        assertThatThrownBy(() -> counterService.decrement(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No available spots");
    }

    @Test
    void getAvailableCount_ShouldReturnParsedValue() {
        when(valueOperations.get("availability:lot:1")).thenReturn("7");

        int count = counterService.getAvailableCount(1L);

        assertThat(count).isEqualTo(7);
    }

    @Test
    void getAvailableCount_ShouldReturnZero_WhenKeyMissing() {
        when(valueOperations.get("availability:lot:1")).thenReturn(null);

        int count = counterService.getAvailableCount(1L);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenCountPositive() {
        when(valueOperations.get("availability:lot:1")).thenReturn("3");

        assertThat(counterService.isAvailable(1L)).isTrue();
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenCountZero() {
        when(valueOperations.get("availability:lot:1")).thenReturn("0");

        assertThat(counterService.isAvailable(1L)).isFalse();
    }

    @Test
    void deleteCounter_ShouldCallRedisDelete() {
        counterService.deleteCounter(1L);

        verify(redisTemplate).delete("availability:lot:1");
    }
}
