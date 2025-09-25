package com.example.dip.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dip.App
import com.example.dip.R
import com.example.dip.data.api.Valute
import com.example.dip.data.rv_adapters.ConversionsAdapter
import com.example.dip.databinding.FragmentHomeBinding
import com.example.dip.ui.details.DetailsFragment
import javax.inject.Inject

class HomeFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: HomeViewModel by activityViewModels { viewModelFactory }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedBaseCurrency: String = "RUB"
    private val selectedCurrencies = mutableSetOf<String>()
    private val favoriteCurrencies = mutableSetOf<String>()

    private lateinit var conversionsAdapter: ConversionsAdapter

    private var currentPage = 0
    private var pageSize: Int = 5
    private var conversionsList: List<Valute> = emptyList()

    private var ratesMap: Map<String, Double> = emptyMap()
    private var valutesList: List<Valute> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as App).appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        selectedBaseCurrency = prefs.getString("base_currency", "RUB") ?: "RUB"
        pageSize = prefs.getString("page_size", "5")?.toIntOrNull() ?: 5
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

        // RecyclerView
        conversionsAdapter = ConversionsAdapter { valute ->
            val bundle = Bundle().apply { putParcelable("valute", valute) }
            findNavController().navigate(R.id.action_navigation_home_to_detailsFragment, bundle)
        }
        binding.recyclerViewConversions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversionsAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // Наблюдение за курсами
        viewModel.currencyMap.observe(viewLifecycleOwner) { currencyMap ->
            ratesMap = currencyMap
            updateConversionsAndShowPage()
        }
        viewModel.valutes.observe(viewLifecycleOwner) { valutes ->
            valutesList = valutes
            val allCodes = listOf("RUB") + valutes.map { it.charCode }
            setupCurrencySpinner(allCodes)
        }

        binding.buttonShowChart.setOnClickListener {
            val selectedCode = selectedBaseCurrency
            val selectedValute: Valute? = if (selectedCode == "RUB") {
                Valute(
                    charCode = "RUB",
                    name = "Российский рубль",
                    nominal = 1,
                    value = "1.0",
                    previous = "1.0"
                )
            } else {
                valutesList.find { it.charCode == selectedCode }
            }

            if (selectedValute != null) {
                val bundle = Bundle().apply { putParcelable("valute", selectedValute) }
                findNavController().navigate(R.id.action_navigation_home_to_detailsFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Нет данных для $selectedCode", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка "Избранные"
        binding.buttonFavorites.setOnClickListener {
            if (favoriteCurrencies.isNotEmpty()) {
                selectedCurrencies.clear()
                selectedCurrencies.addAll(favoriteCurrencies)
                updateConversionsAndShowPage()
            } else {
                Toast.makeText(requireContext(), "Избранные валюты не выбраны", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка "Добавить в избранное"
        binding.buttonAddToFavorites.setOnClickListener {
            favoriteCurrencies.clear()
            favoriteCurrencies.addAll(selectedCurrencies)
            Toast.makeText(requireContext(), "Избранные валюты сохранены", Toast.LENGTH_SHORT).show()
        }

        // Кнопка "Сброс"
        binding.buttonReset.setOnClickListener {
            selectedCurrencies.clear()
            updateConversionsAndShowPage()
            Toast.makeText(requireContext(), "Выбранные валюты сброшены", Toast.LENGTH_SHORT).show()
        }

        // Кнопки пагинации
        binding.buttonLoadMore.setOnClickListener {
            if (currentPage < maxPageIndex()) {
                currentPage++
                showCurrentPage()
            }
        }

        binding.buttonLoadPrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                showCurrentPage()
            }
        }

        // Ошибки и загрузка
        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.textHome.text = error?.let { "Ошибка: $it" } ?: ""
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressPagination.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupCurrencySpinner(codes: List<String>) {
        val baseAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, codes)
        binding.spinnerFromCurrency.adapter = baseAdapter
        val defaultIndex = codes.indexOf(selectedBaseCurrency).takeIf { it >= 0 } ?: 0
        binding.spinnerFromCurrency.setSelection(defaultIndex)
        binding.spinnerFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBaseCurrency = codes[position]
                updateConversionsAndShowPage()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.buttonSelectCurrencies.setOnClickListener {
            val currenciesArray = codes.toTypedArray()
            val tempSelected = selectedCurrencies.toMutableSet()
            val checkedItems = codes.map { tempSelected.contains(it) }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Выберите валюты")
                .setMultiChoiceItems(currenciesArray, checkedItems) { _, which, isChecked ->
                    if (isChecked) tempSelected.add(currenciesArray[which])
                    else tempSelected.remove(currenciesArray[which])
                }
                .setPositiveButton("Выбрать") { dialog, _ ->
                    selectedCurrencies.clear()
                    selectedCurrencies.addAll(tempSelected)
                    updateConversionsAndShowPage()
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun updateConversionsAndShowPage() {
        binding.recyclerViewConversions.animate().alpha(0f).setDuration(200).withEndAction {
            if (ratesMap.isEmpty() || selectedCurrencies.isEmpty()) {
                conversionsList = emptyList()
                showCurrentPage()
                return@withEndAction
            }

            val baseRate = ratesMap[selectedBaseCurrency] ?: 1.0
            val conversions = selectedCurrencies.mapNotNull { code ->
                val rate = ratesMap[code]
                val valute = valutesList.find { it.charCode == code }
                rate?.let {
                    Valute(
                        charCode = code,
                        name = valute?.name ?: code,
                        nominal = valute?.nominal ?: 1,
                        value = (it / baseRate).toString(),
                        previous = valute?.previous ?: ""
                    )
                }
            }
            conversionsList = conversions
            currentPage = 0
            showCurrentPage()
            binding.recyclerViewConversions.animate().alpha(1f).setDuration(200).start()
        }.start()
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
        binding.buttonLoadMore.visibility = if (currentPage < maxPageIndex()) View.VISIBLE else View.GONE
        binding.buttonLoadPrev.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
    }

    private fun maxPages(): Int =
        if (conversionsList.isEmpty()) 1 else ((conversionsList.size - 1) / pageSize) + 1

    private fun maxPageIndex(): Int =
        if (conversionsList.isEmpty()) 0 else (conversionsList.size - 1) / pageSize

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

