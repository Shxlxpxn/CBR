package com.example.dip.ui.home

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavouritesManager(context: Context) {

    private val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_FAVOURITES = "favourites_list"

    fun saveFavourites(favourites: List<String>) {
        val json = gson.toJson(favourites)
        prefs.edit().putString(KEY_FAVOURITES, json).apply()
    }

    fun loadFavourites(): MutableList<String> {
        val json = prefs.getString(KEY_FAVOURITES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}