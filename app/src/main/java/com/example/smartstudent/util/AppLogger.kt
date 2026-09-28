package com.example.smartstudent.util

import android.util.Log

/**
 * Single place for technical logging. Every catch block in the app should log the
 * real exception here and show the user a short, friendly message instead — raw
 * exceptions, stack traces, and API error payloads (e.g. Gemini's JSON error body)
 * must never be bound directly into a Text() composable.
 *
 * View these logs in Android Studio's Logcat, filtered by tag "SmartStudent".
 */
object AppLogger {
    private const val TAG = "SmartStudent"

    fun e(context: String, throwable: Throwable) {
        Log.e(TAG, "[$context] ${throwable.javaClass.simpleName}: ${throwable.message}", throwable)
    }

    fun e(context: String, message: String) {
        Log.e(TAG, "[$context] $message")
    }

    fun w(context: String, message: String) {
        Log.w(TAG, "[$context] $message")
    }

    fun i(context: String, message: String) {
        Log.i(TAG, "[$context] $message")
    }
}
