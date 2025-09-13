package com.example.dip.data.rv_adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dip.data.api.Valute
import com.example.dip.databinding.ItemConversionBinding
import java.text.DecimalFormat

class ConversionsAdapter(
    private val onConversionClick: (Valute) -> Unit
) : RecyclerView.Adapter<ConversionsAdapter.ConversionViewHolder>() {

    private val conversions = mutableListOf<Valute>()
    private val decimalFormat = DecimalFormat("#.####")

    inner class ConversionViewHolder(val binding: ItemConversionBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setConversions(newConversions: List<Valute>) {
        conversions.clear()
        conversions.addAll(newConversions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        val binding = ItemConversionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        val valute = conversions[position]
        holder.binding.textCurrency.text = valute.charCode
        holder.binding.textRate.text = decimalFormat.format(valute.valueDouble)
        holder.binding.root.setOnClickListener {
            onConversionClick(valute)
        }
    }

    override fun getItemCount(): Int = conversions.size
}