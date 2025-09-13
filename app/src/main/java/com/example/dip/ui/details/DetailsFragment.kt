package com.example.dip.ui.details

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
import androidx.appcompat.app.AppCompatActivity
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
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Стрелка "назад" в тулбаре
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Детали валюты"
        }
        setHasOptionsMenu(true)

        // Получаем аргумент (если есть)
        valuteArg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("valute", Valute::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("valute")
        }

        // Если аргумента нет — подсказываем пользователю
        val provided = valuteArg
        if (provided == null) {
            binding.collapsingToolbar.title = "Детали"
            binding.detailTitle.text = "Нет данных"
            binding.detailText.text = "Выберите валюту в главном экране"
            binding.detailChart.visibility = View.GONE
            return
        }

        // Показываем минимум сразу
        binding.collapsingToolbar.title = provided.name.ifBlank { provided.charCode }
        binding.detailTitle.text = provided.charCode

        // Выполняем загрузку: сначала получаем историю (для графика), затем при необходимости — детали (nominal/current)
        lifecycleScope.launch {
            val code = provided.charCode

            // Если это RUB — специальный случай (ЦБ не возвращает RUB в списке)
            if (code == "RUB") {
                val history = List(7) { 1.0 } // плоская линия 1.0
                updateDetailsAndChart(nominal = 1, current = 1.0, previous = 1.0, history = history)
                return@launch
            }

            // 1) Загружаем историю (7 дней) для графика и чтобы при необходимости определить previous
            val history = fetchHistoryRates(code, 7)

            // 2) Попробуем получить nominal/current/name из сегодняшнего XML (если доступно)
            val todayInfo = fetchValuteInfoForDate(code) // может вернуть null если нет
            val nominalFromApi = todayInfo?.nominal
            val currentFromApi = todayInfo?.value
            val nameFromApi = todayInfo?.name

            // 3) Определяем current/previous доверяясь в порядке приоритетов:
            //    current = currentFromApi ?: history.lastOrNull()
            //    previous = если есть в valuteArg.previous парсером -> безопасно, иначе берем из history (предпоследний)
            val currentVal = currentFromApi ?: history.lastOrNull()
            val previousValFromArg = safeParseDouble(provided.previous)
            val previousVal = previousValFromArg ?: if (history.size >= 2) history[history.size - 2] else null

            // 4) Если name из API присутствует — ставим его как заголовок
            if (!nameFromApi.isNullOrBlank()) {
                binding.collapsingToolbar.title = nameFromApi
            }

            // 5) Номинал: берем из API если есть, иначе из аргумента, иначе 1
            val nominalToShow = nominalFromApi ?: provided.nominal.takeIf { it > 0 } ?: 1

            // 6) Обновляем UI и рисуем график если есть история
            updateDetailsAndChart(nominalToShow, currentVal, previousVal, history)
        }
    }
    private suspend fun updateDetailsAndChart(
        nominal: Int,
        current: Double?,
        previous: Double?,
        history: List<Double>
    ) {
        withContext(Dispatchers.Main) {
            // Текст
            val detailsText = SpannableStringBuilder()
            detailsText.append(createBold("Номинал: ")).append("$nominal\n\n")
            detailsText.append(createBold("Текущее значение: "))
            detailsText.append(current?.let { decimalFormat.format(it) } ?: "—")
            detailsText.append("\n\n")
            detailsText.append(createBold("Предыдущее значение: "))
            detailsText.append(previous?.let { decimalFormat.format(it) } ?: "—")
            detailsText.append("\n\n")
            binding.detailText.text = detailsText

            // График
            if (history.isNotEmpty()) {
                showChart(history)
            } else {
                binding.detailChart.visibility = View.GONE
            }
        }
    }

    private fun safeParseDouble(s: String?): Double? {
        if (s.isNullOrBlank()) return null
        return try {
            s.replace(",", ".").toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

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
                val rate = parseRateFromXml(xml, code)
                rate?.let { rates.add(it) }
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
                            parser.next()
                            currentCharCode = parser.text ?: ""
                        }
                        if (inValute && parser.name == "Value") {
                            parser.next()
                            currentValue = parser.text ?: ""
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Valute") {
                            if (currentCharCode == code) {
                                return currentValue.replace(",", ".").toDoubleOrNull()
                            }
                            inValute = false
                            currentCharCode = ""
                            currentValue = ""
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

    private suspend fun fetchValuteInfoForDate(code: String): ValuteInfo? {
        return withContext(Dispatchers.IO) {
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
                            "CharCode" -> if (inValute) {
                                parser.next()
                                currentCharCode = parser.text ?: ""
                            }
                            "Value" -> if (inValute) {
                                parser.next()
                                currentValue = parser.text ?: ""
                            }
                            "Nominal" -> if (inValute) {
                                parser.next()
                                currentNominal = parser.text ?: ""
                            }
                            "Name" -> if (inValute) {
                                parser.next()
                                currentName = parser.text ?: ""
                            }
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
                            currentCharCode = ""
                            currentValue = ""
                            currentNominal = ""
                            currentName = ""
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
        val entries = history.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Курс за ${history.size} дней").apply {
            setDrawValues(false)
            setDrawCircles(true)
            lineWidth = 2f
            color = requireContext().getColor(android.R.color.holo_blue_light)
            circleRadius = 4f
            setCircleColor(requireContext().getColor(android.R.color.holo_blue_dark))
        }

        binding.detailChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.setDrawGridLines(false)
            xAxis.setDrawGridLines(false)
            animateX(1200)
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(600).start()
            invalidate()
        }
    }

    private fun createBold(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        return spannable
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}