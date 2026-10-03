package com.abinet.gallerylight

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    private const val NAME = "gallerylight_prefs"
    const val KEY_AUTO_PLAY = "auto_play"
    const val KEY_LOOP = "loop"
    const val KEY_SPEED = "speed"
    const val KEY_BRIGHTNESS = "brightness"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun autoPlay(context: Context) = get(context).getBoolean(KEY_AUTO_PLAY, true)
    fun loop(context: Context) = get(context).getBoolean(KEY_LOOP, false)
    fun speed(context: Context) = get(context).getFloat(KEY_SPEED, 1.0f)
    fun brightness(context: Context) = get(context).getInt(KEY_BRIGHTNESS, 100)

    fun save(
        context: Context,
        autoPlay: Boolean,
        loop: Boolean,
        speed: Float,
        brightness: Int
    ) {
        get(context).edit {
            putBoolean(KEY_AUTO_PLAY, autoPlay)
            putBoolean(KEY_LOOP, loop)
            putFloat(KEY_SPEED, speed)
            putInt(KEY_BRIGHTNESS, brightness)
        }
    }
}