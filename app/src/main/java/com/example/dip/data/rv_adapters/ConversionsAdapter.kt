package com.example.dip.data.rv_adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dip.databinding.ItemConversionBinding
import java.text.DecimalFormat

class ConversionsAdapter : RecyclerView.Adapter<ConversionsAdapter.ConversionViewHolder>() {

    private val conversions = mutableListOf<Pair<String, Double>>()
    private val decimalFormat = DecimalFormat("#.####")

    inner class ConversionViewHolder(val binding: ItemConversionBinding) : RecyclerView.ViewHolder(binding.root)

    fun setConversions(newConversions: List<Pair<String, Double>>) {
        conversions.clear()
        conversions.addAll(newConversions)
        notifyDataSetChanged()
        Log.d("Conversions", "Обновляем данные: $conversions")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        val binding = ItemConversionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        val (currency, rate) = conversions[position]
        holder.binding.textCurrency.text = currency
        holder.binding.textRate.text = decimalFormat.format(rate)
        Log.d("ConversionsAdapter", "Отображаем валюту: ${conversions[position].first}, курс: ${conversions[position].second}")
    }

    override fun getItemCount(): Int = conversions.size

}