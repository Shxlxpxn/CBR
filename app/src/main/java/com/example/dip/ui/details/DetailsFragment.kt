package com.example.dip.ui.details

import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.dip.data.api.Valute
import com.example.dip.databinding.FragmentDetailsBinding

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настроим Toolbar как ActionBar и кнопку назад
        val activity = requireActivity() as? AppCompatActivity
        activity?.let {
            it.setSupportActionBar(binding.detailToolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.detailToolbar.setNavigationOnClickListener {
                // безопасный возврат
                if (!findNavController().popBackStack()) {
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Попытка безопасно получить Valute из аргументов (поддержка API 33+ и старых)
        val valute: Valute? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // getParcelable(key, class) доступен на API 33+
            arguments?.getParcelable("valute", Valute::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("valute") as? Valute
        }

        // Если не нашли в аргументах, пробуем (необязательно) — попытка через navArgs() закомментирована.
        // Если ты хочешь использовать Safe Args, раскомментируй и используй:
        // private val args: DetailsFragmentArgs by navArgs()
        // val valute = args.valute

        if (valute == null) {
            // Нет данных — показываем сообщение и уходим назад
            Log.w("DetailsFragment", "Valute is null in arguments")
            binding.collapsingToolbar.title = "Детали"
            binding.detailTitle.text = "Нет данных"
            binding.detailText.text = "Не удалось загрузить информацию об элементе."
            Toast.makeText(requireContext(), "Данные для детализации отсутствуют", Toast.LENGTH_SHORT).show()
            return
        }

        // Заполняем UI из valute
        binding.collapsingToolbar.title = valute.name
        binding.detailTitle.text = valute.name

        // Подпишем анимированное скрытие/появление изображения при скролле AppBarLayout
        // AppBarLayout в binding называется appBarLayout? В твоём XML id = app_bar_layout -> binding.appBarLayout
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val total = appBarLayout.totalScrollRange
            if (total > 0) {
                val fraction = kotlin.math.abs(verticalOffset).toFloat() / total.toFloat()
                // Плавно скрываем картинку по степени скролла
                binding.detailImage.alpha = 1f - fraction
            }
        }
        val detailsText = SpannableStringBuilder()

        fun appendBold(label: String) {
            val s = SpannableStringBuilder(label)
            s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, 0)
            detailsText.append(s)
        }

        appendBold("Код: ")
        detailsText.append("${valute.charCode}\n\n")

        appendBold("Номинал: ")
        detailsText.append("${valute.nominal}\n\n")

        appendBold("Текущее значение: ")
        // безопасное форматирование, округлим до 4 знаков
        detailsText.append("${String.format("%.4f", valute.valueDouble)}\n\n")

        // previous может быть пустым — проверяем
        appendBold("Предыдущее значение: ")
        val prevText = try {
            String.format("%.4f", valute.previousDouble)
        } catch (e: Exception) {
            "—"
        }
        detailsText.append("$prevText\n\n")
        binding.detailText.text = detailsText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}