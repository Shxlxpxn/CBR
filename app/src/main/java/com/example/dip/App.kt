package com.example.dip

import android.app.Application
import android.content.Context
import com.example.dip.di.AppComponent
import com.example.dip.di.DaggerAppComponent
import com.example.dip.utils.LocaleHelper

class App : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
            .applicationContext(this)
            .build()
        LocaleHelper.applyLocale(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { LocaleHelper.wrap(it) })
    }
}