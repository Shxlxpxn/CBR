package com.example.dip.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dip.R
import com.example.dip.data.rv_adapters.ConversionsAdapter
import com.example.dip.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val availableCurrencies = listOf(
        "RUB", "USD", "EUR", "CNY", "KZT", "GBP", "JPY", "CHF", "AUD", "CAD"
    )

    private var selectedBaseCurrency = "RUB"
    private val selectedCurrencies = mutableSetOf<String>()

    private lateinit var conversionsAdapter: ConversionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Спиннер базовой валюты
        val baseAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, availableCurrencies)
        binding.spinnerFromCurrency.adapter = baseAdapter
        binding.spinnerFromCurrency.setSelection(availableCurrencies.indexOf(selectedBaseCurrency))
        binding.spinnerFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedBaseCurrency = availableCurrencies[position]
                updateConversions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Адаптер для курсов валют
        conversionsAdapter = ConversionsAdapter()
        binding.recyclerViewConversions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversionsAdapter
        }

        // Кнопка выбора валют с диалогом
        binding.buttonSelectCurrencies.setOnClickListener {
            val currenciesArray = availableCurrencies.toTypedArray()
            val tempSelected = selectedCurrencies.toMutableSet()
            val checkedItems = availableCurrencies.map { tempSelected.contains(it) }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Выберите валюты")
                .setMultiChoiceItems(currenciesArray, checkedItems) { _, which, isChecked ->
                    if (isChecked) {
                        tempSelected.add(currenciesArray[which])
                    } else {
                        tempSelected.remove(currenciesArray[which])
                    }
                }
                .setPositiveButton("OK") { dialog, _ ->
                    selectedCurrencies.clear()
                    selectedCurrencies.addAll(tempSelected)
                    updateConversions()
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // Обновляем список при изменениях данных
        viewModel.currencyMap.observe(viewLifecycleOwner) { ratesMap ->
            if (ratesMap != null) {
                updateConversions()
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.textHome.text = error?.let { "Ошибка: $it" } ?: ""
        }

        viewModel.loadCurrencyRates()
    }

    private fun updateConversions() {
        val ratesMap = viewModel.currencyMap.value ?: return

        if (selectedCurrencies.isEmpty()) {
            binding.textHome.text = "Выберите хотя бы одну валюту для конвертации"
            conversionsAdapter.setConversions(emptyList())
            return
        }

        val baseRate = ratesMap[selectedBaseCurrency] ?: 1.0

        val conversions = selectedCurrencies.mapNotNull { currency ->
            val rate = ratesMap[currency]
            if (rate != null) {
                currency to (rate / baseRate)
            } else null
        }

        binding.textHome.text = "Курсы валют относительно $selectedBaseCurrency"
        conversionsAdapter.setConversions(conversions)
        Log.d("Conversions", "Данные для обновления: $conversions")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


