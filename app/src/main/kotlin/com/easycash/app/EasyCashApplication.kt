package com.easycash.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EasyCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.easycash.data.notification.scheduleFinancialAlerts(this)
    }
}
