package com.example.dip.data.rv_adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dip.R
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
        val binding = ItemConversionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        val valute = conversions[position]
        val ctx = holder.itemView.context
        val rateText = try {
            val numeric = valute.value.replace(",", ".").toDoubleOrNull()
            if (numeric != null) decimalFormat.format(numeric) else "—"
        } catch (e: Exception) {
            "—"
        }

        // 🔥 Подставляем тексты
        holder.binding.textCurrency.text = valute.charCode
        holder.binding.textRate.text = rateText
        val textColor = resolveAttrColor(ctx, android.R.attr.textColorPrimary, R.color.color_on_background)
        val subTextColor = resolveAttrColor(ctx, android.R.attr.textColorSecondary, R.color.color_surface)
        holder.binding.textCurrency.setTextColor(textColor)
        holder.binding.textRate.setTextColor(subTextColor)

        // 🔥 Клик
        holder.binding.root.setOnClickListener {
            onConversionClick(valute)
        }
    }

    override fun getItemCount(): Int = conversions.size
    private fun resolveAttrColor(context: Context, @AttrRes attr: Int, @ColorRes fallback: Int): Int {
        val tv = TypedValue()
        val ok = context.theme.resolveAttribute(attr, tv, true)
        return if (ok) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId)
            else tv.data
        } else {
            ContextCompat.getColor(context, fallback)
        }
    }
}