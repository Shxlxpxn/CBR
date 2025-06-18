package com.example.dip

import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.dip.databinding.ActivityMainBinding
import com.example.dip.ui.home.HomeViewModel
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: HomeViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as App).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем Toolbar как ActionBar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Можно динамически менять заголовок (например, по навигации)
        supportActionBar?.title = "Главная"
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // если нужна стрелка назад - включить

        // Твой остальной код
        binding.textView.text = "Загрузка..."
        viewModel.currencyMap.observe(this) { ratesMap ->
            val usdRate = ratesMap["USD"]
            if (usdRate != null) {
                binding.textView.text = "Курс USD: $usdRate руб."
            } else {
                binding.textView.text = "Курс USD не найден"
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                binding.textView.text = "Ошибка: $errorMsg"
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            if (isLoading) {
                binding.textView.text = "Загрузка..."
            }
        }

        viewModel.getCurrencyRates()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}

