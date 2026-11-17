package com.swordfish.lemuroid.app.mobile.feature.product

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local single-player register; no ROM, save, account, or database changes. */
internal class GemWebsiteStore(context: Context) {
    private val preferences = context.getSharedPreferences("cartdeck_gem_websites_v1", Context.MODE_PRIVATE)

    fun read(): List<GemWebsiteSource> {
        val json = JSONArray(preferences.getString("sources", "[]") ?: "[]")
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            GemWebsiteSource(
                id = item.getString("id"),
                name = item.getString("name"),
                home = item.getString("home"),
                category = item.getString("category"),
                template = item.getString("template"),
                enabled = item.getBoolean("enabled"),
            ).also { require(it.validationError() == null) }
        }.also { sources ->
            require(sources.size <= 100 && sources.map { it.id }.distinct().size == sources.size)
        }
    }

    fun write(sources: List<GemWebsiteSource>): Boolean {
        if (sources.size > 100 || sources.any { it.validationError() != null }) return false
        val json = JSONArray()
        sources.forEach { source ->
            json.put(
                JSONObject()
                    .put("id", source.id)
                    .put("name", source.name)
                    .put("home", source.home)
                    .put("category", source.category)
                    .put("template", source.template)
                    .put("enabled", source.enabled),
            )
        }
        return preferences.edit().putString("sources", json.toString()).commit()
    }
}
