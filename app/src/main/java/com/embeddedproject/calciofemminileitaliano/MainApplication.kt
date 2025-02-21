package com.embeddedproject.calciofemminileitaliano

import android.app.Application
import android.speech.tts.TextToSpeech
import okhttp3.OkHttpClient
import okhttp3.Request

class MainApplication : Application() {
    lateinit var request: Request
    lateinit var query: String
    lateinit var url: String

    lateinit var hearRulesTextToSpeech: TextToSpeech

    lateinit var client: OkHttpClient
}