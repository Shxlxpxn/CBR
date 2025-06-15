package com.example.dip.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dip.data.ValCurs
import com.example.dip.di.modules.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {

    private val _currencyMap = MutableLiveData<Map<String, Double>>()
    val currencyMap: LiveData<Map<String, Double>> = _currencyMap

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadCurrencyRates() {
        _loading.value = true
        _error.value = null

        RetrofitClient.instance.getCurrencyRates().enqueue(object : Callback<ValCurs> {
            override fun onResponse(call: Call<ValCurs>, response: Response<ValCurs>) {
                _loading.value = false
                if (response.isSuccessful) {
                    val valCurs = response.body()
                    val map = valCurs?.valuteList?.mapNotNull { valute ->
                        val valueNum = valute.value.replace(',', '.').toDoubleOrNull()
                        val nominal = valute.nominal.takeIf { it > 0 } ?: 1
                        if (valueNum != null) {
                            valute.charCode to (valueNum / nominal)
                        } else null
                    }?.toMap()?.toMutableMap() ?: mutableMapOf()

                    map["RUB"] = 1.0 // Добавляем рубль как базовую валюту

                    _currencyMap.value = map
                } else {
                    _error.value = "Ошибка загрузки: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<ValCurs>, t: Throwable) {
                _loading.value = false
                _error.value = "Ошибка: ${t.localizedMessage}"
            }
        })
    }
}
