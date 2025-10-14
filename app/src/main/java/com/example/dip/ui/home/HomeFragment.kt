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
import com.example.dip.ui.history.HistoryItem
import com.example.dip.ui.history.HistoryManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject

class HomeFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: HomeViewModel by activityViewModels { viewModelFactory }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedBaseCurrency: String = "RUB"
    private val selectedCurrencies = mutableSetOf<String>()
    private lateinit var favouritesManager: FavouritesManager

    private lateinit var conversionsAdapter: ConversionsAdapter
    private lateinit var historyManager: HistoryManager

    private var currentPage = 0
    private var pageSize: Int = 5
    private var conversionsList: List<Valute> = emptyList()

    private var ratesMap: Map<String, Double> = emptyMap()
    private var valutesList: List<Valute> = emptyList()

    // Dagger
    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as App).appComponent.inject(this)
        historyManager = HistoryManager(context)
        favouritesManager = FavouritesManager(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        selectedBaseCurrency = prefs.getString("base_currency", "RUB") ?: "RUB"
        pageSize = prefs.getString("page_size", "5")?.toIntOrNull() ?: 5
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversionsAdapter = ConversionsAdapter { valute ->
            val bundle = Bundle().apply {
                putParcelable("valute", valute)
                putString("baseCurrency", selectedBaseCurrency)
            }
            findNavController().navigate(R.id.action_navigation_home_to_detailsFragment, bundle)
        }

        binding.recyclerViewConversions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversionsAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // Подписки на ViewModel
        viewModel.currencyMap.observe(viewLifecycleOwner) { currencyMap ->
            ratesMap = currencyMap
            updateConversionsAndShowPage()
        }
        viewModel.valutes.observe(viewLifecycleOwner) { valutes ->
            valutesList = valutes
            val codes = valutes.map { it.charCode }.distinct().sorted()
            val allCodes = listOf("RUB") + codes
            setupCurrencySpinner(allCodes)
        }

        // Кнопка "Показать график"
        binding.buttonShowChart.setOnClickListener {
            if (selectedCurrencies.isEmpty()) {
                Toast.makeText(requireContext(), "Выберите валюту для просмотра графика", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetCode = selectedCurrencies.first()
            val targetValute = valutesList.find { it.charCode == targetCode }
                ?: Valute(charCode = targetCode, name = targetCode, nominal = 1, value = "1.0", previous = "1.0")

            val baseValute = valutesList.find { it.charCode == selectedBaseCurrency }
                ?: Valute(charCode = selectedBaseCurrency, name = selectedBaseCurrency, nominal = 1, value = "1.0", previous = "1.0")

            historyManager.addToHistory(HistoryItem(baseValute.charCode, targetValute.charCode))

            val bundle = Bundle().apply {
                putParcelable("valute", targetValute)
                putString("baseCurrency", selectedBaseCurrency)
            }
            findNavController().navigate(R.id.action_navigation_home_to_detailsFragment, bundle)
        }

        // 🔹 Избранное
        binding.buttonFavorites.setOnClickListener {
            val favs = favouritesManager.loadFavourites()
            if (favs.isNotEmpty()) {
                selectedCurrencies.clear()
                selectedCurrencies.addAll(favs)
                updateConversionsAndShowPage()
            } else {
                Toast.makeText(requireContext(), "Избранные валюты не выбраны", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonAddToFavorites.setOnClickListener {
            favouritesManager.saveFavourites(selectedCurrencies.toList())
            Toast.makeText(requireContext(), "Избранные валюты сохранены", Toast.LENGTH_SHORT).show()
        }

        binding.buttonReset.setOnClickListener {
            selectedCurrencies.clear()
            updateConversionsAndShowPage()
            Toast.makeText(requireContext(), "Выбранные валюты сброшены", Toast.LENGTH_SHORT).show()
        }

        // Пагинация
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

        // Ошибки / загрузка
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

            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme)
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

            val baseRate = if (selectedBaseCurrency == "RUB") 1.0 else (ratesMap[selectedBaseCurrency] ?: 1.0)

            val conversions = selectedCurrencies.mapNotNull { code ->
                val rate = if (code == "RUB") 1.0 else ratesMap[code]
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
            }.sortedBy { it.charCode }

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

    private fun maxPages(): Int = if (conversionsList.isEmpty()) 1 else ((conversionsList.size - 1) / pageSize) + 1
    private fun maxPageIndex(): Int = if (conversionsList.isEmpty()) 0 else (conversionsList.size - 1) / pageSize

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
