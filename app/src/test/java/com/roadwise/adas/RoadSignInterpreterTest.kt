package com.roadwise.adas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadSignInterpreterTest {
    @Test
    fun readsSpeedLimitWithLineBreaks() {
        val cue = RoadSignInterpreter.interpret("SPEED LIMIT\n80\nKM/H")

        assertEquals("SPEED LIMIT · 80 KM/H", cue?.title)
        assertTrue(cue?.spokenText.orEmpty().contains("80 kilometers per hour"))
    }

    @Test
    fun doesNotTreatAnUnlabelledNumberAsASpeedLimit() {
        assertNull(RoadSignInterpreter.interpret("80"))
    }

    @Test
    fun doesNotGuessTheUnitWhenTheSignDoesNotShowOne() {
        val cue = RoadSignInterpreter.interpret("SPEED LIMIT 80")

        assertEquals("SPEED LIMIT · 80 · UNIT UNCLEAR", cue?.title)
        assertTrue(cue?.spokenText.orEmpty().contains("verify the unit"))
    }

    @Test
    fun recognizesCommonSafetyText() {
        assertEquals("STOP SIGN", RoadSignInterpreter.interpret("STOP")?.title)
        assertEquals("YIELD SIGN", RoadSignInterpreter.interpret("GIVE WAY")?.title)
    }

    @Test
    fun readsShortRoadDirectionText() {
        assertEquals("EXIT 12", RoadSignInterpreter.interpret("EXIT 12")?.title)
        assertNull(RoadSignInterpreter.interpret("SALE TODAY"))
    }
}
