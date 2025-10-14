package com.example.dip.ui.home

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavouritesManager(context: Context) {

    private val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_FAVOURITES = "favourites_list"

    /** Сохранить список избранных валют */
    fun saveFavourites(favourites: List<String>) {
        val json = gson.toJson(favourites)
        prefs.edit().putString(KEY_FAVOURITES, json).apply()
    }

    /** Загрузить список избранных валют */
    fun loadFavourites(): MutableList<String> {
        val json = prefs.getString(KEY_FAVOURITES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    /** Добавить валюту в избранное */
    fun addFavourite(currencyCode: String) {
        val list = loadFavourites()
        if (!list.contains(currencyCode)) {
            list.add(currencyCode)
            saveFavourites(list)
        }
    }

    /** Удалить валюту из избранного */
    fun removeFavourite(currencyCode: String) {
        val list = loadFavourites()
        if (list.remove(currencyCode)) {
            saveFavourites(list)
        }
    }

    /** Проверить, находится ли валюта в избранном */
    fun isFavourite(currencyCode: String): Boolean {
        return loadFavourites().contains(currencyCode)
    }
}