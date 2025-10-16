package com.example.dip.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.dip.R

class HistoryAdapter(
    private val historyManager: HistoryManager,
    private val onClick: ((HistoryItem) -> Unit)? = null,
    private val onHistoryChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<HistoryItem>()

    fun submitList(newList: List<HistoryItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_currency)
        private val buttonDelete: Button = itemView.findViewById(R.id.button_delete)

        fun bind(item: HistoryItem) {
            text.text = "${item.baseCurrency} → ${item.targetCurrency}"

            itemView.setOnClickListener {
                onClick?.invoke(item)
                it.animateClick()
            }

            buttonDelete.setOnClickListener {
                historyManager.removeFromHistory(item)
                onHistoryChanged?.invoke()
                Toast.makeText(
                    itemView.context,
                    itemView.context.getString(R.string.history_item_deleted),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(v)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}