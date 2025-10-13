package com.example.dip.ui.details

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dip.R
import com.example.dip.data.api.Valute
import com.example.dip.databinding.FragmentDetailsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
    private var baseCurrency: String = "RUB"
    private val decimalFormat = DecimalFormat("#.####")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Чтение аргументов, отображение данных и загрузка графика.

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Детали валюты"
        }
        setHasOptionsMenu(true)

        // Читаем аргументы
        valuteArg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("valute", Valute::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("valute")
        }
        baseCurrency = arguments?.getString("baseCurrency", "RUB") ?: "RUB"

        val provided = valuteArg
        if (provided == null) {
            binding.detailTitle.text = "Нет данных"
            binding.detailText.text = "Выберите валюту"
            binding.detailChart.visibility = View.GONE
            return
        }

        // Заголовок с валютами
        binding.detailTitle.text = "${provided.charCode} относительно $baseCurrency"

        // Кнопка "Назад" (если присутствует в layout)
        binding.buttonBack?.apply {
            visibility = View.VISIBLE
            setOnClickListener { findNavController().navigateUp() }
        }

        // Загрузка исторических данных курса и построение графика
        lifecycleScope.launch {
            val targetHistory = fetchHistoryRates(provided.charCode, 7)
            val baseHistory = if (baseCurrency == "RUB") {
                List(targetHistory.size) { 1.0 } // RUB относительно RUB = 1
            } else {
                fetchHistoryRates(baseCurrency, 7)
            }

            if (targetHistory.isEmpty() || baseHistory.isEmpty() || targetHistory.size != baseHistory.size) {
                if (targetHistory.isEmpty()) {
                    binding.detailChart.visibility = View.GONE
                } else {
                    updateDetailsAndChart(provided.nominal, targetHistory.lastOrNull(), null, targetHistory)
                }
                return@launch
            }

            // Считаем относительные значения target/base
            val relativeHistory = targetHistory.zip(baseHistory).map { (t, b) ->
                if (b == 0.0) t else t / b
            }

            val currentVal = relativeHistory.lastOrNull()
            val previousVal = relativeHistory.getOrNull(relativeHistory.size - 2)
            updateDetailsAndChart(provided.nominal, currentVal, previousVal, relativeHistory)
        }
    }

    // Загружает курсы валюты за N дней с сайта ЦБ РФ.
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

    // Парсит XML-ответ и извлекает курс нужной валюты
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

    // Обновляет текстовую информацию и отображает график на экране.
    private suspend fun updateDetailsAndChart(
        nominal: Int,
        current: Double?,
        previous: Double?,
        history: List<Double>
    ) = withContext(Dispatchers.Main) {
        val detailsText = SpannableStringBuilder()
            .append(createBold("Базовая валюта: ")).append("$baseCurrency\n\n")
            .append(createBold("Номинал: ")).append("$nominal\n\n")
            .append(createBold("Текущее значение: "))
            .append(current?.let { decimalFormat.format(it) } ?: "—")
            .append("\n\n")
            .append(createBold("Предыдущее значение: "))
            .append(previous?.let { decimalFormat.format(it) } ?: "—")

        binding.detailText.text = detailsText

        if (history.isNotEmpty()) showChart(history)
        else binding.detailChart.visibility = View.GONE
    }

    // Отображает график изменения курса с учётом темы оформления.

    private fun showChart(history: List<Double>) {
        val entries = history.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }

        val accent = ContextCompat.getColor(requireContext(), R.color.color_secondary_text)
        val onSurface = ContextCompat.getColor(requireContext(), R.color.color_on_surface)
        val surface = ContextCompat.getColor(requireContext(), R.color.color_surface)

        val isNight =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val lineColor = if (isNight) accent else ContextCompat.getColor(requireContext(), R.color.color_accent)
        val gridAlpha = if (isNight) 0.14f else 0.08f
        val gridColor = ColorUtils.setAlphaComponent(onSurface, (255 * gridAlpha).toInt())

        val dataSet = LineDataSet(entries, "Курс за ${history.size} дней").apply {
            setDrawValues(false)
            setDrawCircles(true)
            lineWidth = 2f
            color = lineColor
            circleRadius = 4f
            setCircleColor(lineColor)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextColor = onSurface
        }

        binding.detailChart.apply {
            setBackgroundColor(surface)
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = gridColor
            xAxis.textColor = onSurface

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = gridColor
            axisLeft.textColor = onSurface

            legend.textColor = onSurface
            animateX(900)
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(400).start()
            invalidate()
        }
    }

    private fun createBold(text: String): SpannableStringBuilder {
        val s = SpannableStringBuilder(text)
        s.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        return s
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
