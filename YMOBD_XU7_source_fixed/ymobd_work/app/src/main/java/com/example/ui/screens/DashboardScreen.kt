package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.VehicleData
import com.example.data.obd.ConnectionState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    vehicleData: VehicleData,
    connectionState: ConnectionState,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // OBD Connection status card (Matching YMOBD's top device card)
        item {
            ObdConnectionCard(
                connectionState = connectionState,
                onCardClick = onNavigateToSettings
            )
        }

        // Section Title: Dashboard with link
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDiagnostics() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF2979FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Dashboard",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "داشبورد زنده (Dashboard)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "بیشتر",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // 2x2 Grid exactly like the user's YMOBD screenshot:
        // Left column: Speed card + Average liters card
        // Right column: Rotating speed gauge (RPM) + Instant liters gauge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricNumberCard(
                        title = "Speed",
                        value = "${vehicleData.speed}",
                        unit = "km/h",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    MetricNumberCard(
                        title = "Average liters",
                        value = "${vehicleData.avgFuelRate}",
                        unit = "L",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = "Avg fuel",
                                tint = Color(0xFF43A047),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                // Right Column: Rotating Speed & Instant Liters Gauges
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArcSpeedGauge(
                        title = "Rotating speed",
                        value = vehicleData.rpm.toFloat(),
                        maxValue = 8000f,
                        unit = "rpm",
                        primaryColor = Color(0xFFFFA000)
                    )

                    ArcSpeedGauge(
                        title = "Instant liters",
                        value = vehicleData.instantFuelRate,
                        maxValue = 20f,
                        unit = "L/100km",
                        primaryColor = Color(0xFF2979FF)
                    )
                }
            }
        }

        // Vital XU7 Coolant Temperature Card with Head Gasket Alert
        item {
            CoolantTempCard(tempCelsius = vehicleData.coolantTemp)
        }

        // Additional Telemetry Data (Battery, MAP, Throttle, MIL status)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "پارامترهای زنده موتور XU7 (ایسیو زیمنس/کروز)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = "ولتاژ باتری/دینام",
                            value = "${vehicleData.batteryVoltage} V",
                            statusColor = if (vehicleData.batteryVoltage in 13.5f..14.8f) GreenSuccess else AmberWarning
                        )
                        TelemetryItem(
                            label = "فشار منیفولد (MAP)",
                            value = "${vehicleData.mapPressureKpa} kPa",
                            statusColor = BluePrimary
                        )
                        TelemetryItem(
                            label = "زاویه دریچه گاز (TPS)",
                            value = "${vehicleData.throttlePositionPercent.toInt()}%",
                            statusColor = CyanAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (vehicleData.milActive) RedDanger.copy(alpha = 0.15f) else GreenSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (vehicleData.milActive) "چراغ چک روشن (خطا دارد)" else "چراغ چک خاموش (عادی)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (vehicleData.milActive) RedDanger else GreenSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        TextButton(onClick = onNavigateToDiagnostics) {
                            Text("بررسی خطاها و دیاگ هوشمند")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    statusColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        )
    }
}
