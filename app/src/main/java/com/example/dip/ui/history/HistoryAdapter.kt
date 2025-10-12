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
    private val onClick: ((String) -> Unit)? = null,
    private val onHistoryChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(newList: List<String>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_currency)
        private val deleteButton: Button = itemView.findViewById(R.id.button_delete)

        init {
            // При нажатии на элемент — переход к графику
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = items[pos]
                    onClick?.invoke(item)
                    it.animateClick()
                }
            }

            deleteButton.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = items[pos]
                    historyManager.removeFromHistory(item)
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    onHistoryChanged?.invoke()
                    Toast.makeText(
                        itemView.context,
                        "Запись \"$item\" удалена",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        fun bind(currency: String) {
            text.text = currency
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(v)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}