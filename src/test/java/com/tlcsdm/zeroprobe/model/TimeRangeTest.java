package com.tlcsdm.zeroprobe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TimeRange enum.
 */
class TimeRangeTest {

    @Test
    void testAllValuesHavePositiveSeconds() {
        for (TimeRange range : TimeRange.values()) {
            assertTrue(range.getSeconds() > 0, range.name() + " should have positive seconds");
        }
    }

    @Test
    void testMaxDataPointsMatchesSeconds() {
        for (TimeRange range : TimeRange.values()) {
            assertEquals(range.getSeconds(), range.getMaxDataPoints(),
                range.name() + " maxDataPoints should equal seconds");
        }
    }

    @Test
    void testValuesAreOrdered() {
        TimeRange[] values = TimeRange.values();
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i].getSeconds() > values[i - 1].getSeconds(),
                values[i].name() + " should have more seconds than " + values[i - 1].name());
        }
    }

    @Test
    void testExpectedValueCount() {
        assertEquals(14, TimeRange.values().length);
    }

    @Test
    void testSpecificDurations() {
        assertEquals(60, TimeRange.MIN_1.getSeconds());
        assertEquals(300, TimeRange.MIN_5.getSeconds());
        assertEquals(3600, TimeRange.HOUR_1.getSeconds());
        assertEquals(86400, TimeRange.DAY_1.getSeconds());
        assertEquals(604800, TimeRange.DAY_7.getSeconds());
        assertEquals(2592000, TimeRange.MONTH_1.getSeconds());
    }

    @Test
    void testToStringNotNull() {
        for (TimeRange range : TimeRange.values()) {
            assertNotNull(range.toString(), range.name() + " toString should not be null");
            assertFalse(range.toString().isEmpty(), range.name() + " toString should not be empty");
        }
    }
}
