package com.example.data.models

enum class EcuConfigCategory {
    ALARM_SECURITY,
    COOLING_FAN,
    THROTTLE_IDLE,
    COMFORT_BCM,
    RESET_ADAPTATION
}

data class EcuConfigItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val category: EcuConfigCategory,
    val isEnabled: Boolean,
    val supportedOnElm: Boolean = true,
    val commandRoutine: String,
    val safetyNotice: String,
    val currentValueDisplay: String = ""
)
