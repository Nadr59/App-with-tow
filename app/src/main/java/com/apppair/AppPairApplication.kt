package com.apppair

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppPairApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
