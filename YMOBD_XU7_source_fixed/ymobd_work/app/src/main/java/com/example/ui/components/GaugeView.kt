package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcSpeedGauge(
    title: String,
    value: Float,
    maxValue: Float,
    unit: String,
    primaryColor: Color = Color(0xFFFFA000),
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "gauge_progress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val startAngle = 140f
                    val sweepAngle = 260f

                    // Background arc
                    drawArc(
                        color = Color(0xFFE2E8F0),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )

                    // Tick dashes
                    val tickCount = 20
                    for (i in 0..tickCount) {
                        val angleDeg = startAngle + (sweepAngle * (i.toFloat() / tickCount))
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val rOuter = size.width / 2f
                        val rInner = rOuter - 5.dp.toPx()
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        val startX = cx + (rInner * cos(angleRad)).toFloat()
                        val startY = cy + (rInner * sin(angleRad)).toFloat()
                        val endX = cx + (rOuter * cos(angleRad)).toFloat()
                        val endY = cy + (rOuter * sin(angleRad)).toFloat()

                        drawColorTick(startX, startY, endX, endY)
                    }

                    // Progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )

                    // Needle pointer
                    val needleAngleDeg = startAngle + (sweepAngle * animatedProgress)
                    val needleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val needleLength = size.width / 2.3f

                    val needleEndX = cx + (needleLength * cos(needleRad)).toFloat()
                    val needleEndY = cy + (needleLength * sin(needleRad)).toFloat()

                    drawLine(
                        color = primaryColor,
                        start = Offset(cx, cy),
                        end = Offset(needleEndX, needleEndY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center pivot
                    drawCircle(
                        color = Color(0xFF0D47A1),
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${value.toInt()}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawColorTick(
    sx: Float,
    sy: Float,
    ex: Float,
    ey: Float
) {
    drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(sx, sy),
        end = Offset(ex, ey),
        strokeWidth = 1.5f
    )
}

@Composable
fun MetricNumberCard(
    title: String,
    value: String,
    unit: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = unit,
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
fun CoolantTempCard(
    tempCelsius: Int,
    modifier: Modifier = Modifier
) {
    val tempColor = when {
        tempCelsius < 75 -> BlueLight
        tempCelsius <= 92 -> GreenSuccess // Safe optimal zone for XU7
        tempCelsius <= 98 -> AmberWarning // Caution
        else -> RedDanger // Critical head gasket risk!
    }

    val statusTextFa = when {
        tempCelsius < 75 -> "موتور در حال گرم شدن"
        tempCelsius <= 92 -> "دمای نرمال و ایمن XU7"
        tempCelsius <= 98 -> "احتیاط: فن دور تند فعال"
        else -> "خطر جوش آوردن! خطر واشر سرسیلندر"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "دمای آب موتور",
                        tint = tempColor
                    )
                    Text(
                        text = "دمای مایع خنک‌کننده (ECT)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Surface(
                    color = tempColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$tempCelsius °C",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = tempColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Bar for XU7
            val progress = ((tempCelsius - 40) / 80f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = tempColor,
                trackColor = Color(0xFFE2E8F0),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "وضعیت: $statusTextFa",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = tempColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "سنسور زیمنس (سبز)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
