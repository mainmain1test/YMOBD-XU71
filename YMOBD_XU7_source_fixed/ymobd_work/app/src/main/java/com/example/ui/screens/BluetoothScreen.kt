package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.obd.ConnectionState
import com.example.ui.theme.*

@Composable
fun BluetoothScreen(
    connectionState: ConnectionState,
    isSimulated: Boolean,
    pairedDevices: List<BluetoothDevice>,
    onToggleSimulated: (Boolean) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Current Status Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "وضعیت ارتباط با دانگل OBD-II",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            is ConnectionState.Connected -> GreenSuccess
                                            is ConnectionState.Connecting -> AmberWarning
                                            else -> RedDanger
                                        }
                                    )
                            )

                            Column {
                                Text(
                                    text = when (connectionState) {
                                        is ConnectionState.Connected -> connectionState.deviceName
                                        is ConnectionState.Connecting -> "در حال تلاش برای اتصال..."
                                        is ConnectionState.Disconnected -> "ارتباط قطع است"
                                        is ConnectionState.Error -> "خطا در برقراری ارتباط"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = when (connectionState) {
                                        is ConnectionState.Connected -> connectionState.address
                                        else -> "جهت استفاده، دانگل را انتخاب کنید"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                        }

                        if (connectionState is ConnectionState.Connected) {
                            OutlinedButton(
                                onClick = onDisconnect,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
                            ) {
                                Text("قطع اتصال")
                            }
                        }
                    }
                }
            }
        }

        // Simulation Mode Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSimulated) BlueContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حالت شبیه‌ساز زنده (تست بدون خودرو)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "با فعال‌سازی این حالت، می‌توانید تمامی عقربه‌های داشبورد، دور موتور، تحلیل هوش مصنوعی و تنظیمات ECU را با داده‌های زنده تست نمایید.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )
                        )
                    }

                    Switch(
                        checked = isSimulated,
                        onCheckedChange = onToggleSimulated,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BluePrimary
                        )
                    )
                }
            }
        }

        // Paired Bluetooth Devices
        item {
            Text(
                text = "دستگاه‌های بلوتوث جفت‌شده گوشی (${pairedDevices.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (pairedDevices.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هیچ دانگل بلوتوثی در گوشی جفت نشده است.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ابتدا در تنظیمات بلوتوث اندروید دانگل OBDII را جفت کنید (پین: ۱۲۳۴ یا ۰۰۰۰).",
                            style = MaterialTheme.typography.labelSmall.copy(color = BluePrimary)
                        )
                    }
                }
            }
        } else {
            items(pairedDevices) { device ->
                @androidx.annotation.OptIn(androidx.core.os.BuildCompat.PrereleaseSdkCheck::class)
                val deviceName = try {
                    device.name ?: "دستگاه ناشناس"
                } catch (e: SecurityException) {
                    "دستگاه OBDII"
                }
                val isObd = deviceName.contains("OBD", ignoreCase = true) || deviceName.contains("ELM", ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConnectDevice(device) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isObd) Icons.Default.DeveloperBoard else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isObd) BluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Column {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                        }

                        Button(
                            onClick = { onConnectDevice(device) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("اتصال")
                        }
                    }
                }
            }
        }

        // Instructions for Iran Khodro Cars
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = BluePrimary)
                        Text(
                            text = "راهنمای اتصال به خودروهای ایران‌خودرو",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = """
                            📍 **محل پورت OBD-II ۱۶ پین در خودروهای ایران خودرو:**
                            • **پژو ۴۰۵ و پارس:** داخل محفظه جعبه فیوز سمت چپ زیر غربیلک فرمان (درپوش را باز کنید).
                            • **سمند، سورن و سورن پلاس:** زیر قاب داشبورد در امتداد پدال کلاچ.
                            • **تارا و دنا پلاس:** پنل کنسول زیر فرمان سمت چپ راننده.
                            
                            🔑 **مراحل راه‌اندازی دانگل ELM327:**
                            ۱. سوییچ خودرو را کامل باز کنید تا چراغ‌های پشت آمپر روشن شوند (موتور می‌تواند خاموش یا روشن باشد).
                            ۲. دانگل آبی رنگ ELM327 را محکم در سوکت جا بزنید (ال‌ای‌دی قرمز آن روشن می‌شود).
                            ۳. در بلوتوث گوشی جستجو کرده و با رمز ۱۲۳۴ یا ۰۰۰۰ جفت کنید.
                            ۴. به برنامه برگردید و دکمه اتصال را بزنید.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}
