package com.example.dip.ui.details

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.example.dip.R
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.dip.data.api.Valute
import com.example.dip.databinding.FragmentDetailsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private var valuteArg: Valute? = null
    private val decimalFormat = DecimalFormat("#.####")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getColorByName(name: String, fallbackResId: Int): Int {
        val ctx = requireContext()
        val id = resources.getIdentifier(name, "color", ctx.packageName)
        return if (id != 0) ContextCompat.getColor(ctx, id) else ContextCompat.getColor(ctx, fallbackResId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Детали валюты"
        }
        setHasOptionsMenu(true)

        // читаем аргумент безопасно
        valuteArg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("valute", Valute::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("valute")
        }

        val provided = valuteArg
        if (provided == null) {
            binding.collapsingToolbar.title = "Детали"
            binding.detailTitle.text = "Нет данных"
            binding.detailText.text = "Выберите валюту в главном экране"
            binding.detailChart.visibility = View.GONE
            return
        }

        binding.collapsingToolbar.title = provided.name.ifBlank { provided.charCode }
        binding.detailTitle.text = provided.charCode

        lifecycleScope.launch {
            val code = provided.charCode
            if (code == "RUB") {
                val history = List(7) { 1.0 }
                updateDetailsAndChart(1, 1.0, 1.0, history)
                return@launch
            }

            val history = fetchHistoryRates(code, 7)
            val todayInfo = fetchValuteInfoForDate(code)

            val currentVal = todayInfo?.value ?: history.lastOrNull()
            val previousValFromArg = safeParseDouble(provided.previous)
            val previousVal = previousValFromArg ?: if (history.size >= 2) history[history.size - 2] else null

            if (!todayInfo?.name.isNullOrBlank()) {
                binding.collapsingToolbar.title = todayInfo?.name
            }

            val nominalToShow = todayInfo?.nominal ?: provided.nominal.takeIf { it > 0 } ?: 1

            updateDetailsAndChart(nominalToShow, currentVal, previousVal, history)
        }
    }

    private suspend fun updateDetailsAndChart(
        nominal: Int,
        current: Double?,
        previous: Double?,
        history: List<Double>,
    ) {
        withContext(Dispatchers.Main) {
            // составляем текст
            val detailsText = SpannableStringBuilder()
                .append(createBold("Номинал: ")).append("$nominal\n\n")
                .append(createBold("Текущее значение: "))
                .append(current?.let { decimalFormat.format(it) } ?: "—")
                .append("\n\n")
                .append(createBold("Предыдущее значение: "))
                .append(previous?.let { decimalFormat.format(it) } ?: "—")
                .append("\n\n")
            binding.detailText.text = detailsText

            if (history.isNotEmpty()) showChart(history)
            else binding.detailChart.visibility = View.GONE
        }
    }

    private fun safeParseDouble(s: String?): Double? = s?.replace(",", ".")?.toDoubleOrNull()

    private suspend fun fetchHistoryRates(code: String, days: Int): List<Double> {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val today = Calendar.getInstance()
        val rates = mutableListOf<Double>()

        repeat(days) {
            val date = formatter.format(today.time)
            val url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=$date"
            try {
                val xml = withContext(Dispatchers.IO) {
                    URL(url).readText(Charset.forName("windows-1251"))
                }
                parseRateFromXml(xml, code)?.let { rates.add(it) }
            } catch (e: Exception) {
                Log.e("DetailsFragment", "Ошибка загрузки курса на $date", e)
            }
            today.add(Calendar.DAY_OF_MONTH, -1)
        }
        return rates.reversed()
    }

    private fun parseRateFromXml(xml: String, code: String): Double? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var eventType = parser.eventType
            var inValute = false
            var currentCharCode = ""
            var currentValue = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "Valute") inValute = true
                        if (inValute && parser.name == "CharCode") {
                            parser.next(); currentCharCode = parser.text ?: ""
                        }
                        if (inValute && parser.name == "Value") {
                            parser.next(); currentValue = parser.text ?: ""
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Valute") {
                            if (currentCharCode == code) {
                                return currentValue.replace(",", ".").toDoubleOrNull()
                            }
                            inValute = false; currentCharCode = ""; currentValue = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Ошибка парсинга XML", e)
        }
        return null
    }

    private suspend fun fetchValuteInfoForDate(code: String) = withContext(Dispatchers.IO) {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val today = formatter.format(Calendar.getInstance().time)
            val url = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=$today"
            val xml = URL(url).readText(Charset.forName("windows-1251"))
            parseValuteInfoFromXml(xml, code)
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Ошибка fetchValuteInfoForDate", e)
            null
        }
    }

    private fun parseValuteInfoFromXml(xml: String, code: String): ValuteInfo? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var eventType = parser.eventType
            var inValute = false
            var currentCharCode = ""
            var currentValue = ""
            var currentNominal = ""
            var currentName = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "Valute" -> inValute = true
                            "CharCode" -> if (inValute) { parser.next(); currentCharCode = parser.text ?: "" }
                            "Value" -> if (inValute) { parser.next(); currentValue = parser.text ?: "" }
                            "Nominal" -> if (inValute) { parser.next(); currentNominal = parser.text ?: "" }
                            "Name" -> if (inValute) { parser.next(); currentName = parser.text ?: "" }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Valute") {
                            if (currentCharCode == code) {
                                val valueD = currentValue.replace(",", ".").toDoubleOrNull()
                                val nominalInt = currentNominal.toIntOrNull()
                                return ValuteInfo(name = currentName, nominal = nominalInt, value = valueD)
                            }
                            inValute = false
                            currentCharCode = ""; currentValue = ""; currentNominal = ""; currentName = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Ошибка parseValuteInfoFromXml", e)
        }
        return null
    }

    private data class ValuteInfo(val name: String?, val nominal: Int?, val value: Double?)

    private fun showChart(history: List<Double>) {
        val entries = history.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }

        val accent = getColorByName("color_secondary", R.color.color_on_background)
        val primary = getColorByName("color_primary", R.color.color_on_background)
        val onSurface = getColorByName("color_on_surface", android.R.color.white)
        val surface = getColorByName("color_surface", android.R.color.background_light)

        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val lineColor = if (isNight) accent else primary

        val gridAlpha = (if (isNight) 0.14f else 0.08f)
        val gridColor = ColorUtils.setAlphaComponent(onSurface, (255 * gridAlpha).toInt())

        val textColor = onSurface
        val dataSet = LineDataSet(entries, "Курс за ${history.size} дней").apply {
            setDrawValues(false)
            setDrawCircles(true)
            lineWidth = 2f
            color = lineColor
            circleRadius = 4f
            setCircleColor(lineColor)
            valueTextColor = textColor
            mode = LineDataSet.Mode.LINEAR
        }

        binding.detailChart.apply {
            setBackgroundColor(surface)
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = gridColor
            xAxis.textColor = textColor

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = gridColor
            axisLeft.textColor = textColor

            legend.textColor = textColor

            animateX(900)
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(400).start()
            invalidate()
        }
    }

    private fun createBold(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        return spannable
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            requireActivity().onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
