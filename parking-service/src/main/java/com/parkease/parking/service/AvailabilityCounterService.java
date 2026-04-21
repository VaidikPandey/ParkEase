package com.parkease.parking.service;

public interface AvailabilityCounterService {
    void initCounter(Long lotId, int availableSpots);
    void increment(Long lotId);
    void decrement(Long lotId);
    int getAvailableCount(Long lotId);
    void deleteCounter(Long lotId);
    boolean isAvailable(Long lotId);
}