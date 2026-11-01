package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CognitiveBenchmarkTest {
    @Test
    fun reactionTimePercentile_fasterIsHigherPercentile() {
        // Average is ~290ms
        val avg = CognitiveBenchmark.reactionTimePercentile(290L)
        assertTrue(avg in 48..52, "Average reaction should be ~50th percentile, was $avg")

        // Elite is 190ms (top percentile)
        val elite = CognitiveBenchmark.reactionTimePercentile(190L)
        assertTrue(elite >= 95, "190ms should be >= 95th percentile, was $elite")

        // Slower is lower percentile
        val slow = CognitiveBenchmark.reactionTimePercentile(400L)
        assertTrue(slow <= 15, "400ms should be <= 15th percentile, was $slow")
    }

    @Test
    fun workingMemoryPercentile_higherSpanIsHigherPercentile() {
        // Population average span is ~5.5 (span 5 is ~33rd percentile)
        val avg = CognitiveBenchmark.workingMemoryPercentile(span = 5)
        assertTrue(avg in 30..50, "Span 5 should be ~33rd percentile, was $avg")

        // Span 8 is top tier (>=90th percentile)
        val high = CognitiveBenchmark.workingMemoryPercentile(span = 8)
        assertTrue(high >= 90, "Span 8 should be >= 90th percentile, was $high")
    }

    @Test
    fun tierLabel_returnsAppropriateBracket() {
        assertEquals("Top 5% Elite", CognitiveBenchmark.tierLabel(percentile = 96))
        assertEquals("Top 15% Advanced", CognitiveBenchmark.tierLabel(percentile = 86))
        assertEquals("Top 30% Above Average", CognitiveBenchmark.tierLabel(percentile = 72))
        assertEquals("Average", CognitiveBenchmark.tierLabel(percentile = 50))
    }
}
