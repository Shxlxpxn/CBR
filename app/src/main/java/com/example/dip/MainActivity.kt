package com.example.dip

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.dip.data.ValCurs
import com.example.dip.databinding.ActivityMainBinding
import com.example.dip.di.modules.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        textView.text = "Загрузка..."

        // Делаем запрос
        RetrofitClient.instance.getCurrencyRates().enqueue(object : Callback<ValCurs> {
            override fun onResponse(call: Call<ValCurs>, response: Response<ValCurs>) {
                if (response.isSuccessful) {
                    val valCurs = response.body()
                    val usd = valCurs?.valuteList?.find { it.charCode == "USD" }

                    if (usd != null) {
                        textView.text = "Курс USD: ${usd.value} руб."
                    } else {
                        textView.text = "Курс USD не найден"
                    }
                } else {
                    textView.text = "Ошибка: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<ValCurs>, t: Throwable) {
                textView.text = "Ошибка загрузки"

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
        })}}