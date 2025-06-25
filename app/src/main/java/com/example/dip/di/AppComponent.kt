package com.example.dip.di

import android.content.Context
import com.example.dip.MainActivity
import com.example.dip.di.modules.DatabaseModule
import com.example.dip.di.modules.NetworkModule
import com.example.dip.ui.ViewModel.ViewModelModule
import com.example.dip.ui.home.HomeFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(
    modules = [
        NetworkModule::class,
        ViewModelModule::class,
        DatabaseModule::class
    ]
)
interface AppComponent {

    fun inject(fragment: HomeFragment)
    fun inject(activity: MainActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationContext(context: Context): Builder

        fun build(): AppComponent
    }
}