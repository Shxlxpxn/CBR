package com.example.dip.ui.home

import com.example.dip.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dip.data.ValCurs
import com.example.dip.di.modules.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {


    private lateinit var textViewCurrency: TextView
    private lateinit var spinnerCurrency: Spinner
    private val availableCurrencies = listOf("USD", "EUR", "CNY", "KZT")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewCurrency = view.findViewById(R.id.text_home)
        spinnerCurrency = view.findViewById(R.id.spinnerCurrency)
        Log.d("HomeFragment", "Setting up Spinner with currencies: $availableCurrencies")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableCurrencies
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = availableCurrencies[position]
                loadCurrencyRates(selectedCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCurrencyRates(selectedCode: String) {
        textViewCurrency.text = "Загрузка..."

        RetrofitClient.instance.getCurrencyRates().enqueue(object : Callback<ValCurs> {
            override fun onResponse(call: Call<ValCurs>, response: Response<ValCurs>) {
                if (response.isSuccessful) {
                    val valCurs = response.body()
                    val currency = valCurs?.valuteList?.find { it.charCode == selectedCode }
                    if (currency != null) {
                        textViewCurrency.text = "Курс $selectedCode: ${currency.value} руб."
                    } else {
                        textViewCurrency.text = "Курс $selectedCode не найден"
                    }
                } else {
                    textViewCurrency.text = "Ошибка: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<ValCurs>, t: Throwable) {
                textViewCurrency.text = "Ошибка загрузки: ${t.localizedMessage}"
            }
        })
    }
}