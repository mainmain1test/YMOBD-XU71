package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.EcuConfigCategory
import com.example.data.models.EcuConfigItem
import com.example.ui.theme.*

@Composable
fun EcuConfigScreen(
    ecuConfigs: List<EcuConfigItem>,
    onToggleConfig: (String) -> Unit,
    onRunActuatorTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<EcuConfigCategory?>(null) }
    var itemToConfirm by remember { mutableStateOf<EcuConfigItem?>(null) }
    var showTechExplanation by remember { mutableStateOf(false) }

    val filteredList = if (selectedCategory == null) {
        ecuConfigs
    } else {
        ecuConfigs.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Investigation Report Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BlueDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                                )
                        }

                        Column {
                            Text(
                                text = "پیکربندی و تنظیمات تخصصی ECU و BCM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "قابلیت‌های اختصاصی تا شناسایی دقیق ECU غیرفعال هستند",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "فرمان‌های اختصاصی BCM و ECU فقط پس از شناسایی دقیق واحد کنترل و تأیید مستندات فنی فعال می‌شوند. در این نسخه هیچ Routine ID تأییدنشده‌ای ارسال نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { showTechExplanation = !showTechExplanation },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF80D8FF))
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showTechExplanation) "بستن توضیحات فنی پروتکل" else "مشاهده گزارش فنی قابلیت‌های دانگل OBD")
                    }

                    if (showTechExplanation) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = """
                                    🔬 **گزارش تحقیق در مورد دانگل ELM327 و خودروهای ایران‌خودرو:**
                                    • در خودروهای پژو پارس، سمند و ۴۰۵، دو بخش پردازشی وجود دارد:
                                      ۱. ایسیو موتور (Siemens / Crouse / SSAT)
                                      ۲. نود مرکزی بدنه و مالتی‌پلکس (BCM / CCN / SMS / Crouse)
                                    • پشتیبانی از K-Line، CAN و سرویس‌های اختصاصی باید از پاسخ واقعی ECU و مستندات همان نسخه تأیید شود؛ ELM327 به‌تنهایی تضمین‌کننده دسترسی به BCM نیست.
                                    • **قابلیت‌های قابل بررسی پس از تأیید ECU:**
                                      ۱. فعال/غیرفعال‌سازی تک‌بوق و آژیر دزدگیر فابریک با ریموت
                                      ۲. قفل خودکار کلیه درب‌ها در سرعت ۲۰ کیلومتر
                                      ۳. کاهش دمای روشن شدن فن رادیاتور (دور کند ۹۱° و دور تند ۹۶°) برای محافظت قطعی از واشر سرسیلندر XU7
                                      ۴. ریست و کالیبراسیون استپر موتور و دریچه گاز برقی
                                      ۵. پاک‌سازی مقادیر یادگیری شده حافظه تطبیقی (Fuel Trim) ایسیو
                                      ۶. اجرای تست عملگرها (تست مستقیم فن دور تند و کند، رله دوبل و چراغ چک)
                                """.trimIndent(),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.95f),
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            ScrollableTabRow(
                selectedTabIndex = when (selectedCategory) {
                    null -> 0
                    EcuConfigCategory.ALARM_SECURITY -> 1
                    EcuConfigCategory.COOLING_FAN -> 2
                    EcuConfigCategory.THROTTLE_IDLE -> 3
                    EcuConfigCategory.COMFORT_BCM -> 4
                    EcuConfigCategory.RESET_ADAPTATION -> 5
                },
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(selected = selectedCategory == null, onClick = { selectedCategory = null }, text = { Text("همه") })
                Tab(selected = selectedCategory == EcuConfigCategory.ALARM_SECURITY, onClick = { selectedCategory = EcuConfigCategory.ALARM_SECURITY }, text = { Text("دزدگیر و آژیر") })
                Tab(selected = selectedCategory == EcuConfigCategory.COOLING_FAN, onClick = { selectedCategory = EcuConfigCategory.COOLING_FAN }, text = { Text("دمای فن XU7") })
                Tab(selected = selectedCategory == EcuConfigCategory.THROTTLE_IDLE, onClick = { selectedCategory = EcuConfigCategory.THROTTLE_IDLE }, text = { Text("استپر و گاز") })
                Tab(selected = selectedCategory == EcuConfigCategory.COMFORT_BCM, onClick = { selectedCategory = EcuConfigCategory.COMFORT_BCM }, text = { Text("تجهیزات بدنه BCM") })
                Tab(selected = selectedCategory == EcuConfigCategory.RESET_ADAPTATION, onClick = { selectedCategory = EcuConfigCategory.RESET_ADAPTATION }, text = { Text("ریست ECU") })
            }
        }

        // Configuration Items List
        items(filteredList) { item ->
            ConfigItemCard(
                item = item,
                onToggleRequested = { itemToConfirm = item }
            )
        }

        // Actuator Tests Section
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
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = BluePrimary
                        )
                        Text(
                            text = "تست زنده عملگرهای موتور (Actuator Test)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "این بخش فقط پس از مشخص‌شدن Routine ID واقعی ECU قابل اجراست؛ برای جلوگیری از فعال‌کردن عملگر اشتباه، فرمان حدسی ارسال نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRunActuatorTest("تست فن دور کند رادیاتور") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تست فن دور کند")
                        }

                        OutlinedButton(
                            onClick = { onRunActuatorTest("تست فن دور تند رادیاتور") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تست فن دور تند")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRunActuatorTest("تست رله دوبل و پمپ بنزین") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تست رله دوبل")
                        }

                        OutlinedButton(
                            onClick = { onRunActuatorTest("تست چراغ چک پشت آمپر") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تست چراغ چک")
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before modifying ECU
    itemToConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToConfirm = null },
            title = { Text(item.titleFa) },
            text = {
                Column {
                    Text(
                        text = if (item.isEnabled)
                            "آیا قصد دارید این گزینه را غیرفعال نمایید؟"
                        else
                            "آیا قصد دارید این تنظیم را روی ECU / نود بدنه خودرو اعمال و ذخیره کنید؟",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.descriptionFa,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نکته ایمنی: ${item.safetyNotice}",
                        style = MaterialTheme.typography.labelSmall.copy(color = BluePrimary, fontWeight = FontWeight.SemiBold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = item.id
                        itemToConfirm = null
                        onToggleConfig(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("تایید و ارسال فرمان به خودرو")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToConfirm = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun ConfigItemCard(
    item: EcuConfigItem,
    onToggleRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleFa,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = item.titleEn,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }

                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = { onToggleRequested() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.descriptionFa,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "وضعیت جاری:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = item.currentValueDisplay.ifEmpty { if (item.isEnabled) "فعال" else "غیرفعال" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.isEnabled) GreenSuccess else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}
