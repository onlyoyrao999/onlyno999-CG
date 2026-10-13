package com.example

import com.example.model.PitchNote
import com.example.model.SampleSongs
import com.example.model.VoiceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun pitchNote_frequencyCalculation_isAccurate() {
        // A4 (MIDI 69) should equal 440 Hz
        val a4 = PitchNote(0L, 1000L, 69, "A4", "啦")
        assertEquals(440f, a4.frequencyHz, 0.5f)

        // C4 (MIDI 60) should be around 261.63 Hz
        val c4 = PitchNote(0L, 1000L, 60, "C4", "唱")
        assertEquals(261.63f, c4.frequencyHz, 0.5f)
    }

    @Test
    fun sampleSongs_builtInFakeSongsRemoved() {
        val songs = SampleSongs.songs
        // Built-in fake songs removed as requested, ready for user MP3
        assertTrue(songs.isEmpty())
    }

    @Test
    fun defaultVoiceProfiles_haveCustomClonedVoice() {
        val profiles = SampleSongs.defaultProfiles
        assertTrue("Should have default voice profiles", profiles.isNotEmpty())

        val customProfile = profiles.find { it.isUserCustom }
        assertNotNull("Should have default user cloned profile", customProfile)
        assertTrue(customProfile!!.basePitchHz > 0)
    }
}
