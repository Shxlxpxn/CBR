package com.example.dip

import android.app.Application
import com.example.dip.di.AppComponent

class App : Application() {

    // Инициализируем компонент Dagger один раз при запуске приложения
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Здесь можно инициализировать другие глобальные вещи
        // Dagger уже инициализирован через lazy
    }
}