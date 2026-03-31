package com.babakjan.moneybag.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub for participant eligibility checks.
 * In production, this would integrate with payroll/HR systems to determine
 * if a participant is a high earner (FICA wages > $145,000 threshold for SECURE 2.0 §603).
 */
@Component
public class ParticipantEligibilityStub {

    private final Map<Long, Boolean> highEarnerMap = new ConcurrentHashMap<>();

    /**
     * Check if a participant is a high earner (FICA wages exceed threshold).
     * @param participantId participant identifier
     * @return true if participant is a high earner
     */
    public boolean isHighEarner(Long participantId) {
        return highEarnerMap.getOrDefault(participantId, false);
    }

    /**
     * Set a participant's high earner status (for testing/configuration).
     * @param participantId participant identifier
     * @param isHighEarner whether the participant is a high earner
     */
    public void setHighEarner(Long participantId, boolean isHighEarner) {
        highEarnerMap.put(participantId, isHighEarner);
    }

    /**
     * Clear all participant data.
     */
    public void clear() {
        highEarnerMap.clear();
    }
}
