package com.terman37.triplogger.settings

import android.content.Context
import com.terman37.triplogger.data.RegisteredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * [SettingsRepository] persisted with SharedPreferences (the simplest Android
 * key-value store — fine for a handful of settings; product decision).
 *
 * The registered-device list is stored as one JSON array string. JSON is used
 * instead of a delimiter join because device names may contain any character.
 * (org.json is part of Android, no extra dependency.)
 */
class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _monitoringEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MONITORING, false),
    )
    override val monitoringEnabled: StateFlow<Boolean> = _monitoringEnabled

    private val _gracePeriodMinutes = MutableStateFlow(
        prefs.getInt(KEY_GRACE_MINUTES, SettingsRepository.DEFAULT_GRACE_MINUTES),
    )
    override val gracePeriodMinutes: StateFlow<Int> = _gracePeriodMinutes

    private val _registeredDevices = MutableStateFlow(loadDevices())
    override val registeredDevices: StateFlow<List<RegisteredDevice>> = _registeredDevices

    override fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING, enabled).apply()
        _monitoringEnabled.value = enabled
    }

    override fun setGracePeriodMinutes(minutes: Int) {
        // Clamp to the legal range instead of rejecting: the UI only offers
        // legal values, this guards against anything else.
        val clamped = SettingsRepository.clampGraceMinutes(minutes)
        prefs.edit().putInt(KEY_GRACE_MINUTES, clamped).apply()
        _gracePeriodMinutes.value = clamped
    }

    override fun addRegisteredDevice(device: RegisteredDevice) {
        val updated = SettingsRepository.withDeviceAdded(_registeredDevices.value, device)
        saveDevices(updated)
        _registeredDevices.value = updated
    }

    override fun removeRegisteredDevice(address: String) {
        val updated = SettingsRepository.withDeviceRemoved(_registeredDevices.value, address)
        saveDevices(updated)
        _registeredDevices.value = updated
    }

    // --- storage helpers --------------------------------------------------

    private fun loadDevices(): List<RegisteredDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(RegisteredDevice(obj.getString("address"), obj.getString("name")))
                }
            }
        } catch (_: org.json.JSONException) {
            // Corrupt saved list (should never happen): start empty rather
            // than crash the app.
            emptyList()
        }
    }

    private fun saveDevices(devices: List<RegisteredDevice>) {
        val array = JSONArray()
        devices.forEach { device ->
            array.put(org.json.JSONObject().put("address", device.address).put("name", device.name))
        }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
    }

    private companion object {
        const val KEY_MONITORING = "monitoring_enabled"
        const val KEY_GRACE_MINUTES = "grace_minutes"
        const val KEY_DEVICES = "registered_devices"
    }
}
