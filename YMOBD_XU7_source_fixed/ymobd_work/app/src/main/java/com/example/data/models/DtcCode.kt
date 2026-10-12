package com.example.data.models

enum class DtcSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class DtcCode(
    val code: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val xu7SpecificCause: String,
    val severity: DtcSeverity,
    val symptoms: List<String>,
    val repairSteps: List<String>,
    val isPending: Boolean = false,
    val aiAnalysis: String? = null
)
