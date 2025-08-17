package com.example.dip.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dip.App
import com.example.dip.R
import com.example.dip.data.rv_adapters.ConversionsAdapter
import com.example.dip.databinding.FragmentHomeBinding
import com.example.dip.ui.viewmodel.CurrencyViewModel
import javax.inject.Inject

class HomeFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    // Если хочешь сохранить HomeViewModel — оставляем, но логика теперь локально использует currencyViewModel
    private lateinit var viewModel: HomeViewModel
    private lateinit var currencyViewModel: CurrencyViewModel

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val availableCurrencies = listOf(
        "RUB", "USD", "EUR", "CNY", "KZT", "GBP", "JPY", "CHF", "AUD", "CAD"
    )

    private var selectedBaseCurrency = "RUB"
    private val selectedCurrencies = mutableSetOf<String>()

    private lateinit var conversionsAdapter: ConversionsAdapter

    // пагинация
    private var currentPage = 0
    private val pageSize = 5
    private var conversionsList: List<Pair<String, Double>> = emptyList()

    // локальная мапа курсов (charCode -> value)
    private var ratesMap: Map<String, Double> = emptyMap()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as App).appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]
        currencyViewModel = ViewModelProvider(this, viewModelFactory)[CurrencyViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Spinner
        val baseAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, availableCurrencies)
        binding.spinnerFromCurrency.adapter = baseAdapter
        binding.spinnerFromCurrency.setSelection(availableCurrencies.indexOf(selectedBaseCurrency))
        binding.spinnerFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBaseCurrency = availableCurrencies[position]
                updateConversionsAndShowPage()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // RecyclerView + adapter
        conversionsAdapter = ConversionsAdapter()
        binding.recyclerViewConversions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversionsAdapter
        }

        // Выбор валют (мультивыбор)
        binding.buttonSelectCurrencies.setOnClickListener {
            val currenciesArray = availableCurrencies.toTypedArray()
            val tempSelected = selectedCurrencies.toMutableSet()
            val checkedItems = availableCurrencies.map { tempSelected.contains(it) }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Выберите валюты")
                .setMultiChoiceItems(currenciesArray, checkedItems) { _, which, isChecked ->
                    if (isChecked) tempSelected.add(currenciesArray[which])
                    else tempSelected.remove(currenciesArray[which])
                }
                .setPositiveButton("OK") { dialog, _ ->
                    selectedCurrencies.clear()
                    selectedCurrencies.addAll(tempSelected)
                    currentPage = 0
                    updateConversionsAndShowPage()
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // Кнопка "Загрузить ещё"
        binding.buttonLoadMore.setOnClickListener {
            val maxPage = if (conversionsList.isEmpty()) 0 else (conversionsList.size - 1) / pageSize
            if (currentPage < maxPage) {
                currentPage++
                showCurrentPage()
            }
        }

        // Наблюдаем за списком валют из CurrencyViewModel (Repository -> Room/API)
        currencyViewModel.currencies.observe(viewLifecycleOwner) { currencyList ->
            // обновляем локальную мапу курсов
            ratesMap = currencyList.associate { it.charCode to it.value }
            // обновляем UI
            updateConversionsAndShowPage()
        }

        // Ошибки
        currencyViewModel.error.observe(viewLifecycleOwner) { error ->
            binding.textHome.text = error?.let { "Ошибка: $it" } ?: ""
        }

        // Показать/скрыть индикатор в пагинации
        currencyViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressPagination.visibility = if (loading) View.VISIBLE else View.GONE
            // при загрузке скрываем кнопку загрузки
            binding.buttonLoadMore.visibility = if (!loading && hasMorePages()) View.VISIBLE else View.GONE
        }

        // Инициируем загрузку
        currencyViewModel.loadCurrencies(forceRefresh = true)
    }

    // строим conversionsList из выбранных валют и локальной ratesMap
    private fun updateConversionsAndShowPage() {
        if (selectedCurrencies.isEmpty()) {
            conversionsList = emptyList()
            binding.textHome.text = "Выберите хотя бы одну валюту для конвертации"
            conversionsAdapter.setConversions(emptyList())
            binding.buttonLoadMore.visibility = View.GONE
            return
        }

        val baseRate = ratesMap[selectedBaseCurrency] ?: 1.0

        conversionsList = selectedCurrencies.mapNotNull { currency ->
            val rate = ratesMap[currency]
            if (rate != null) currency to (rate / baseRate) else null
        }.sortedBy { it.first } // сортировка для стабильности

        currentPage = 0
        showCurrentPage()
    }

    private fun showCurrentPage() {
        val fromIndex = currentPage * pageSize
        val toIndex = minOf(fromIndex + pageSize, conversionsList.size)
        val pageData = if (fromIndex < toIndex) conversionsList.subList(fromIndex, toIndex) else emptyList()

        binding.textHome.text = if (conversionsList.isEmpty()) {
            "Нет данных для выбранных валют"
        } else {
            "Курсы валют относительно $selectedBaseCurrency (стр. ${currentPage + 1}/${maxPages()})"
        }

        conversionsAdapter.setConversions(pageData)

        // Показать/скрыть кнопку "Загрузить ещё"
        binding.buttonLoadMore.visibility = if (hasMorePages()) View.VISIBLE else View.GONE
    }

    private fun hasMorePages(): Boolean {
        val maxPage = if (conversionsList.isEmpty()) 0 else (conversionsList.size - 1) / pageSize
        return currentPage < maxPage
    }

    private fun maxPages(): Int {
        return if (conversionsList.isEmpty()) 1 else ((conversionsList.size - 1) / pageSize) + 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
