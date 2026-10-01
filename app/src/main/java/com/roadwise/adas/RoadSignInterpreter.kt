package com.roadwise.adas

import java.util.Locale

/**
 * Turns on-device OCR text into conservative, short spoken cues.
 * This is deliberately not a sign-shape detector: numbers without speed-sign context
 * are ignored so route numbers and unrelated scene text are not called speed limits.
 */
data class RoadSignCue(
    val key: String,
    val title: String,
    val spokenText: String,
    val detail: String,
)

object RoadSignInterpreter {
    private val speedWithContext = Regex(
        """(?:SPEED\s*LIMIT|SPEED|MAX(?:IMUM)?(?:\s*SPEED)?|LIMIT)\D{0,12}(\d{1,3})\s*(KM/?H|KPH|MPH)?""",
    )
    private val speedWithUnit = Regex("""\b(\d{1,3})\s*(KM/?H|KPH|MPH)\b""")
    private val speedSignWords = Regex("""\b(SPEED\s*LIMIT|MAX(?:IMUM)?\s*SPEED|KM/?H|KPH|MPH)\b""")
    private val roadMarker = Regex(
        """\b(EXIT|DETOUR|MERGE|LANE|ROUTE|TURN|ONLY|ONE\s+WAY|CLOSED|SLOW|PARKING|BRIDGE|MILE|KEEP\s+LEFT|KEEP\s+RIGHT)\b""",
    )

    fun interpret(rawText: String): RoadSignCue? {
        if (rawText.isBlank()) return null
        val normalized = rawText.uppercase(Locale.US).replace(Regex("\\s+"), " ").trim()

        val speedMatch = speedWithContext.find(normalized) ?: speedWithUnit.find(normalized)
        if (speedMatch != null) {
            // Both patterns put a contextual speed value in group 1 and the optional unit in group 2.
            val value = speedMatch.groupValues[1].toIntOrNull()
            val unit = speedMatch.groupValues.getOrNull(2).orEmpty().uppercase(Locale.US)
            val isMph = unit == "MPH"
            val plausible = value?.let { candidate ->
                if (isMph) candidate in 5..100 else candidate in 10..160
            } ?: false
            if (value != null && plausible) {
                if (unit.isBlank()) {
                    return RoadSignCue(
                        key = "speed:$value:unit-unknown",
                        title = "SPEED LIMIT · $value · UNIT UNCLEAR",
                        spokenText = "Possible speed limit sign reads $value. Please verify the unit and posted sign.",
                        detail = "OCR found a number but not its unit — verify the sign.",
                    )
                }
                val unitLabel = if (isMph) "MPH" else "KM/H"
                val spokenUnit = if (isMph) "miles per hour" else "kilometers per hour"
                return RoadSignCue(
                    key = "speed:$value:$unitLabel",
                    title = "SPEED LIMIT · $value $unitLabel",
                    spokenText = "Possible speed limit sign reads $value $spokenUnit.",
                    detail = "OCR reading only — verify the posted sign.",
                )
            }
        }

        if (speedSignWords.containsMatchIn(normalized)) {
            return RoadSignCue(
                key = "speed:unreadable",
                title = "POSSIBLE SPEED SIGN",
                spokenText = "Possible speed sign detected, but I cannot read the number. Please verify the posted sign.",
                detail = "The number was not clear enough to read.",
            )
        }

        fun cue(key: String, title: String, speech: String, detail: String = "Possible sign text — verify visually.") =
            RoadSignCue(key, title, speech, detail)

        when {
            Regex("\\bSTOP\\b").containsMatchIn(normalized) -> return cue(
                "stop", "STOP SIGN", "Possible stop sign detected. Please obey the posted sign.",
            )
            Regex("\\bYIELD\\b|\\bGIVE WAY\\b").containsMatchIn(normalized) -> return cue(
                "yield", "YIELD SIGN", "Possible yield sign detected. Please obey the posted sign.",
            )
            Regex("\\bSCHOOL\\b|\\bSCHOOL ZONE\\b").containsMatchIn(normalized) -> return cue(
                "school", "SCHOOL ZONE", "Possible school-zone sign detected. Watch for children and follow the posted limit.",
            )
            Regex("\\b(CROSSWALK|PEDESTRIAN|CROSSING)\\b").containsMatchIn(normalized) -> return cue(
                "crossing", "PEDESTRIAN CROSSING", "Possible pedestrian-crossing sign detected. Watch for people crossing.",
            )
            Regex("\\b(NO ENTRY|DO NOT ENTER|WRONG WAY)\\b").containsMatchIn(normalized) -> return cue(
                "no-entry", "NO ENTRY / WRONG WAY", "Possible no-entry or wrong-way sign detected. Check the road and obey posted signs.",
            )
            Regex("\\b(ROAD WORK|WORK ZONE|CONSTRUCTION)\\b").containsMatchIn(normalized) -> return cue(
                "work-zone", "ROAD WORK", "Possible road-work sign detected. Slow down and watch for workers.",
            )
            Regex("\\b(ROUNDABOUT|TRAFFIC CIRCLE)\\b").containsMatchIn(normalized) -> return cue(
                "roundabout", "ROUNDABOUT", "Possible roundabout sign detected. Follow local right-of-way rules.",
            )
        }

        // Read only short lines containing common road-sign language; arbitrary scene OCR is not spoken.
        val roadText = rawText.lineSequence()
            .map { it.replace(Regex("[^A-Za-z0-9' -]"), " ").replace(Regex("\\s+"), " ").trim() }
            .firstOrNull { line ->
                line.length in 4..48 && roadMarker.containsMatchIn(line.uppercase(Locale.US))
            }
            ?: return null
        val spokenLine = roadText.take(48)
        return RoadSignCue(
            key = "text:${spokenLine.uppercase(Locale.US)}",
            title = spokenLine.uppercase(Locale.US),
            spokenText = "Road sign text: $spokenLine.",
            detail = "On-device OCR reading — verify visually.",
        )
    }
}
