package com.example.dip

import android.app.Application
import com.example.dip.di.AppComponent
import com.example.dip.di.DaggerAppComponent

class App : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder()
            .applicationContext(this)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

    }
}