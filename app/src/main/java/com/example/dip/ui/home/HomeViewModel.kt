package com.example.dip.ui.home


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dip.data.api.CbrApi
import kotlinx.coroutines.launch
import javax.inject.Inject


class HomeViewModel @Inject constructor(
    private val cbrApi: CbrApi
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _currencyMap = MutableLiveData<Map<String, Double>>()
    val currencyMap: LiveData<Map<String, Double>> = _currencyMap

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getCurrencyRates() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val valCurs = cbrApi.getCurrencyRates()
                val ratesMap = valCurs.valute.associate { it.charCode to it.valueDouble }
                _currencyMap.value = ratesMap
                _error.value = null
            } catch (e: Exception) {
                _currencyMap.value = emptyMap()
                _error.value = e.message ?: "Неизвестная ошибка"
            } finally {
                _loading.value = false
            }
        }
    }


    fun setCurrencyMap(map: Map<String, Double>) {
        _currencyMap.value = map
    }

}
