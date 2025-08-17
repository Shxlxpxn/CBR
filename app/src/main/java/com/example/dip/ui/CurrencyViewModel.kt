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
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _currencies = MutableLiveData<List<CurrencyEntity>>() // текущая страница

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    val currencies: LiveData<List<CurrencyEntity>> get() = _currencies

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var allCurrencies: List<CurrencyEntity> = emptyList() // полный список
    private var currentPage = 0
    private val pageSize = 10 // количество элементов на странице

    fun loadCurrencies(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            allCurrencies = repository.getCurrencies(forceRefresh)
            currentPage = 0
            _currencies.value = getPage(0)
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoading.value == true) return
        val nextPage = currentPage + 1
        val startIndex = nextPage * pageSize
        if (startIndex < allCurrencies.size) {
            _isLoading.value = true
            val updatedList = _currencies.value.orEmpty() + getPage(nextPage)
            currentPage = nextPage
            _currencies.value = updatedList
            _isLoading.value = false
        }
    }

    private fun getPage(page: Int): List<CurrencyEntity> {
        val start = page * pageSize
        val end = minOf(start + pageSize, allCurrencies.size)
        return if (start < end) allCurrencies.subList(start, end) else emptyList()
    }
}