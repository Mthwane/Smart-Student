package com.example.smartstudent

import android.app.Application
import com.example.smartstudent.data.ocr.OcrHelper
import com.example.smartstudent.util.SoundPlayer

class SmartStudentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SoundPlayer.init(this)
        // ML Kit's text recognizer downloads its model via Google Play Services the
        // FIRST time it's used on a device — that download (not the actual OCR or the
        // Gemini call) is usually what makes a user's very first scan feel like it's
        // hanging for minutes. Kicking it off here, at app launch, moves that download
        // into the background so it's already warm by the time someone taps "Scan".
        OcrHelper.warmUp()
    }
}
