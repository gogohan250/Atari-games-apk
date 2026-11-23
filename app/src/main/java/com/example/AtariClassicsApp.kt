package com.example

import android.app.Application
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.GameRepository

class AtariClassicsApp : Application() {
    lateinit var repository: GameRepository
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GameRepository(this)
        settingsDataStore = SettingsDataStore(this)
    }
}
