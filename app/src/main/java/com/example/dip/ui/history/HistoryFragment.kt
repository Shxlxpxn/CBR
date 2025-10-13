package com.example.dip.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dip.R
import com.example.dip.data.api.Valute
import com.example.dip.databinding.FragmentHistoryBinding


class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyManager: HistoryManager
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Настройка RecyclerView, загрузка истории и обработка кнопок.

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        historyManager = HistoryManager(requireContext())

        // Адаптер списка истории с кликом и удалением
        adapter = HistoryAdapter(
            historyManager,
            onClick = { item ->
                // Переход на график валюты (DetailsFragment)
                val valute = Valute(
                    charCode = item.targetCurrency,
                    name = item.targetCurrency,
                    nominal = 1,
                    value = "0.0",
                    previous = "0.0"
                )
                val bundle = Bundle().apply {
                    putParcelable("valute", valute)
                    putString("baseCurrency", item.baseCurrency)
                }
                findNavController().navigate(
                    R.id.action_navigation_history_to_detailsFragment,
                    bundle
                )
            },
            onHistoryChanged = { loadHistory() } // при удалении обновляем UI
        )

        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHistory.adapter = adapter

        // Загрузка истории при старте
        loadHistory()

        // Очистка всей истории
        binding.buttonClearHistory.setOnClickListener {
            historyManager.clearHistory()
            loadHistory()
            Toast.makeText(requireContext(), "История очищена", Toast.LENGTH_SHORT).show()
        }
    }

    // Загружает список истории из SharedPreferences и обновляет адаптер.

    private fun loadHistory() {
        val historyList = historyManager.getHistory().reversed()
        adapter.submitList(historyList)
        binding.textHistoryEmpty?.visibility =
            if (historyList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


