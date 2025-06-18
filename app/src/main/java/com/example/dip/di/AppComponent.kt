package com.example.dip.di

import com.example.dip.MainActivity
import com.example.dip.di.modules.NetworkModule
import com.example.dip.ui.ViewModel.ViewModelModule
import com.example.dip.ui.home.HomeFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class, ViewModelModule::class])
interface AppComponent {
    fun inject(fragment: HomeFragment)
    fun inject(activity: MainActivity)
}