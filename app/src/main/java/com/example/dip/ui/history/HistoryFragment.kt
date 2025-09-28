package com.example.dip.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dip.R

class HistoryFragment : Fragment(R.layout.fragment_history) {
    private lateinit var historyManager: HistoryManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var clearButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        clearButton = view.findViewById(R.id.button_clear_history)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        historyManager = HistoryManager(requireContext())

        adapter = HistoryAdapter { currency ->
        }

        recyclerView.adapter = adapter

        adapter.submitList(historyManager.getHistory())

        clearButton.setOnClickListener {
            historyManager.clearHistory()
            adapter.submitList(historyManager.getHistory())
        }
    }
}