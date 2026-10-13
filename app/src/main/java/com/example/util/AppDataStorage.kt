package com.example.util

import android.content.Context
import com.example.model.LyricLine
import com.example.model.LyricWord
import com.example.model.PitchNote
import com.example.model.SeparationStatus
import com.example.model.SingingSessionRecord
import com.example.model.Song
import com.example.model.VoiceProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AppDataStorage {

    private const val PREF_NAME = "singsync_ai_prefs"
    private const val KEY_ACTIVE_SONG_ID = "active_song_id"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

    private const val FILE_SONGS = "persisted_songs.json"
    private const val FILE_PROFILES = "persisted_profiles.json"
    private const val FILE_SESSIONS = "persisted_sessions.json"

    // --- Active selection preferences ---
    fun saveActiveSongId(context: Context, id: String?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_SONG_ID, id)
            .apply()
    }

    fun loadActiveSongId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_SONG_ID, null)
    }

    fun saveActiveProfileId(context: Context, id: String?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_PROFILE_ID, id)
            .apply()
    }

    fun loadActiveProfileId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_PROFILE_ID, null)
    }

    // --- Songs Persistence ---
    fun saveSongs(context: Context, songs: List<Song>) {
        try {
            val jsonArray = JSONArray()
            for (song in songs) {
                val obj = JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("durationMs", song.durationMs)
                    put("bpm", song.bpm)
                    put("musicalKey", song.musicalKey)
                    put("genre", song.genre)
                    put("audioFilePath", song.audioFilePath ?: "")
                    put("audioUriString", song.audioUriString ?: "")
                    put("bgmFilePath", song.bgmFilePath ?: "")
                    put("vocalFilePath", song.vocalFilePath ?: "")
                    put("separationStatus", song.separationStatus.name)
                    put("isCustomImported", song.isCustomImported)

                    // Pitch notes
                    val notesArr = JSONArray()
                    for (note in song.pitchNotes) {
                        notesArr.put(JSONObject().apply {
                            put("startMs", note.startMs)
                            put("endMs", note.endMs)
                            put("midiNote", note.midiNote)
                            put("noteName", note.noteName)
                            put("lyric", note.lyric)
                        })
                    }
                    put("pitchNotes", notesArr)

                    // Lyrics
                    val lyricsArr = JSONArray()
                    for (line in song.lyrics) {
                        lyricsArr.put(JSONObject().apply {
                            put("startMs", line.startMs)
                            put("endMs", line.endMs)
                            put("text", line.text)
                        })
                    }
                    put("lyrics", lyricsArr)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_SONGS)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSongs(context: Context): List<Song> {
        val file = File(context.filesDir, FILE_SONGS)
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val result = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title")
                val artist = obj.optString("artist", "本地音乐")
                val durationMs = obj.optLong("durationMs", 45000L)
                val bpm = obj.optInt("bpm", 100)
                val musicalKey = obj.optString("musicalKey", "C Major")
                val genre = obj.optString("genre", "已导入")
                val audioFilePath = obj.optString("audioFilePath").takeIf { it.isNotBlank() && File(it).exists() }
                val audioUriString = obj.optString("audioUriString").takeIf { it.isNotBlank() }
                val bgmFilePath = obj.optString("bgmFilePath").takeIf { it.isNotBlank() && File(it).exists() }
                val vocalFilePath = obj.optString("vocalFilePath").takeIf { it.isNotBlank() && File(it).exists() }
                val sepStatusStr = obj.optString("separationStatus", SeparationStatus.READY.name)
                val status = try { SeparationStatus.valueOf(sepStatusStr) } catch (_: Exception) { SeparationStatus.READY }

                val notesList = mutableListOf<PitchNote>()
                val notesArr = obj.optJSONArray("pitchNotes")
                if (notesArr != null) {
                    for (n in 0 until notesArr.length()) {
                        val no = notesArr.getJSONObject(n)
                        notesList.add(PitchNote(
                            startMs = no.optLong("startMs"),
                            endMs = no.optLong("endMs"),
                            midiNote = no.optInt("midiNote", 60),
                            noteName = no.optString("noteName", "C4"),
                            lyric = no.optString("lyric", "")
                        ))
                    }
                }

                val lyricsList = mutableListOf<LyricLine>()
                val lyricsArr = obj.optJSONArray("lyrics")
                if (lyricsArr != null) {
                    for (l in 0 until lyricsArr.length()) {
                        val lo = lyricsArr.getJSONObject(l)
                        lyricsList.add(LyricLine(
                            startMs = lo.optLong("startMs"),
                            endMs = lo.optLong("endMs"),
                            text = lo.optString("text", "")
                        ))
                    }
                }

                result.add(Song(
                    id = id,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    bpm = bpm,
                    musicalKey = musicalKey,
                    genre = genre,
                    audioFilePath = audioFilePath,
                    audioUriString = audioUriString,
                    bgmFilePath = bgmFilePath,
                    vocalFilePath = vocalFilePath,
                    separationStatus = status,
                    pitchNotes = notesList,
                    lyrics = lyricsList,
                    isCustomImported = true
                ))
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Voice Profiles Persistence ---
    fun saveProfiles(context: Context, profiles: List<VoiceProfile>) {
        try {
            val jsonArray = JSONArray()
            for (p in profiles) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("subtitle", p.subtitle)
                    put("isUserCustom", p.isUserCustom)
                    put("dryAudioFilePath", p.dryAudioFilePath ?: "")
                    put("basePitchHz", p.basePitchHz.toDouble())
                    put("formantShift", p.formantShift.toDouble())
                    put("breathiness", p.breathiness.toDouble())
                    put("resonance", p.resonance.toDouble())
                    put("warmth", p.warmth.toDouble())
                    put("autoTuneStrength", p.autoTuneStrength.toDouble())
                    put("vibratoDepth", p.vibratoDepth.toDouble())
                    put("sampleDurationSec", p.sampleDurationSec)

                    val tagsArr = JSONArray()
                    for (t in p.tags) tagsArr.put(t)
                    put("tags", tagsArr)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_PROFILES)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadProfiles(context: Context, defaultList: List<VoiceProfile>): List<VoiceProfile> {
        val file = File(context.filesDir, FILE_PROFILES)
        if (!file.exists()) return defaultList
        return try {
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val result = mutableListOf<VoiceProfile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val subtitle = obj.optString("subtitle")
                val isUserCustom = obj.optBoolean("isUserCustom", false)
                val dryPath = obj.optString("dryAudioFilePath").takeIf { it.isNotBlank() && File(it).exists() }
                val basePitchHz = obj.optDouble("basePitchHz", 220.0).toFloat()
                val formantShift = obj.optDouble("formantShift", 1.0).toFloat()
                val breathiness = obj.optDouble("breathiness", 0.2).toFloat()
                val resonance = obj.optDouble("resonance", 0.5).toFloat()
                val warmth = obj.optDouble("warmth", 0.6).toFloat()
                val autoTuneStrength = obj.optDouble("autoTuneStrength", 0.85).toFloat()
                val vibratoDepth = obj.optDouble("vibratoDepth", 0.3).toFloat()
                val sampleDurationSec = obj.optInt("sampleDurationSec", 0)

                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.getString(t))
                    }
                }

                result.add(VoiceProfile(
                    id = id,
                    name = name,
                    subtitle = subtitle,
                    isUserCustom = isUserCustom,
                    dryAudioFilePath = dryPath,
                    basePitchHz = basePitchHz,
                    formantShift = formantShift,
                    breathiness = breathiness,
                    resonance = resonance,
                    warmth = warmth,
                    autoTuneStrength = autoTuneStrength,
                    vibratoDepth = vibratoDepth,
                    tags = tagsList,
                    sampleDurationSec = sampleDurationSec
                ))
            }
            if (result.isNotEmpty()) result else defaultList
        } catch (e: Exception) {
            e.printStackTrace()
            defaultList
        }
    }

    // --- Session Records Persistence ---
    fun saveSessions(context: Context, sessions: List<SingingSessionRecord>) {
        try {
            val jsonArray = JSONArray()
            for (s in sessions) {
                jsonArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("songTitle", s.songTitle)
                    put("artist", s.artist)
                    put("dateFormatted", s.dateFormatted)
                    put("score", s.score)
                    put("accuracyPercent", s.accuracyPercent)
                    put("ratingGrade", s.ratingGrade)
                    put("durationSec", s.durationSec)
                    put("voiceProfileName", s.voiceProfileName)
                })
            }
            File(context.filesDir, FILE_SESSIONS).writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSessions(context: Context): List<SingingSessionRecord> {
        val file = File(context.filesDir, FILE_SESSIONS)
        if (!file.exists()) return emptyList()
        return try {
            val jsonArray = JSONArray(file.readText())
            val result = mutableListOf<SingingSessionRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(SingingSessionRecord(
                    id = obj.optString("id"),
                    songTitle = obj.optString("songTitle"),
                    artist = obj.optString("artist"),
                    dateFormatted = obj.optString("dateFormatted"),
                    score = obj.optInt("score"),
                    accuracyPercent = obj.optInt("accuracyPercent"),
                    ratingGrade = obj.optString("ratingGrade", "A"),
                    durationSec = obj.optInt("durationSec"),
                    voiceProfileName = obj.optString("voiceProfileName")
                ))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
