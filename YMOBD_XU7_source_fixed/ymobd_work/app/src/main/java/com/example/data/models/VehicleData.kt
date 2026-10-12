package com.example.data.models

data class VehicleData(
    val speed: Int = 0,
    val rpm: Int = 0,
    val coolantTemp: Int = 88,
    val instantFuelRate: Float = 7.4f,
    val avgFuelRate: Float = 8.1f,
    val batteryVoltage: Float = 14.1f,
    val mapPressureKpa: Int = 34,
    val throttlePositionPercent: Float = 14.5f,
    val intakeAirTemp: Int = 32,
    val engineLoad: Float = 22.0f,
    val fuelLevel: Float = 65.0f,
    val milActive: Boolean = false,
    val dtcCount: Int = 0,
    val protocolName: String = "ISO 14230-4 KWP (Fast)",
    val ecuName: String = "Siemens / Crouse CIX48 (XU7/XU7P)"
) {
    val isTempWarning: Boolean
        get() = coolantTemp >= 95

    val isTempCritical: Boolean
        get() = coolantTemp >= 102
}
