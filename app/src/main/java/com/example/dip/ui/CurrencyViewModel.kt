package com.example.dip.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dip.data.CurrencyEntity
import com.example.dip.data.CurrencyRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository) : ViewModel() {
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    private val _currencies = MutableLiveData<List<CurrencyEntity>>()
    val currencies: LiveData<List<CurrencyEntity>> get() = _currencies

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadCurrencies(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getCurrencies(forceRefresh)
                _currencies.value = data
                _error.value = null
            } catch (e: Exception) {
                _currencies.value = emptyList()
                _error.value = e.message ?: "Неизвестная ошибка"
            } finally {
                _isLoading.value = false
            }
        }
    }
}