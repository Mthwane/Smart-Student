package com.example.smartstudent

import android.app.Application
import com.example.smartstudent.util.SoundPlayer

class SmartStudentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SoundPlayer.init(this)
    }
}
