package com.music.bitchord.auth

import android.content.SharedPreferences

/** KLYRA fallback when the Keystore is unavailable: process memory only, never plaintext disk. */
internal class SessionPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    @Synchronized override fun getAll(): MutableMap<String, *> = values.mapValues { (_, value) -> if (value is Set<*>) value.toSet() else value }.toMutableMap()
    @Synchronized override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    @Synchronized override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues?.toMutableSet()
    @Synchronized override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
    @Synchronized override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
    @Synchronized override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
    @Synchronized override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
    @Synchronized override fun contains(key: String?) = values.containsKey(key)
    @Synchronized override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { listener?.let(listeners::add) }
    @Synchronized override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { listeners.remove(listener) }
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clear = false
        private fun put(key: String?, value: Any?): SharedPreferences.Editor = apply { if (key != null) changes[key] = value }
        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)
        override fun remove(key: String?) = put(key, null)
        override fun clear(): SharedPreferences.Editor = apply { clear = true }
        override fun apply() { commit() }
        override fun commit(): Boolean {
            val changed = mutableSetOf<String>()
            val notify: List<SharedPreferences.OnSharedPreferenceChangeListener>
            synchronized(this@SessionPreferences) {
                if (clear) { changed += values.keys; values.clear() }
                changes.forEach { (key, value) ->
                    if (values[key] != value) changed += key
                    if (value == null) values.remove(key) else values[key] = value
                }
                notify = listeners.toList()
            }
            changed.forEach { key -> notify.forEach { it.onSharedPreferenceChanged(this@SessionPreferences, key) } }
            return true
        }
    }
}
