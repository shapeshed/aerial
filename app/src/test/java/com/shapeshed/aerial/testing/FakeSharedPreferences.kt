package com.shapeshed.aerial.testing

import android.content.SharedPreferences

/**
 * In-memory [SharedPreferences] for JVM tests.
 *
 * A fake rather than a mock, so tests exercise the real read/write/edit/remove semantics without
 * an Android runtime or Robolectric.
 */
internal class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        values[key] as? Set<String> ?: defValues

    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = applyUpdate(key, value)

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor =
            applyUpdate(key, values)

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = applyUpdate(key, value)

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = applyUpdate(key, value)

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = applyUpdate(key, value)

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = applyUpdate(key, value)

        override fun remove(key: String): SharedPreferences.Editor = also {
            updates.remove(key)
            removals += key
        }

        override fun clear(): SharedPreferences.Editor = also { clearRequested = true }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() = applyChanges()

        private fun applyUpdate(key: String, value: Any?): SharedPreferences.Editor = also {
            removals -= key
            updates[key] = value
        }

        private fun applyChanges() {
            if (clearRequested) {
                values.clear()
                clearRequested = false
            }
            removals.forEach(values::remove)
            removals.clear()
            values.putAll(updates)
            updates.clear()
        }
    }
}
