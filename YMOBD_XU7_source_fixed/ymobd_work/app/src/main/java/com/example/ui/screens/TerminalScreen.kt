package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.obd.TerminalLog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard

@Composable
fun TerminalScreen(
    terminalLogs: List<TerminalLog>,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }

    val presetCommands = listOf(
        "ATZ" to "ریست دانگل",
        "ATRV" to "ولتاژ باتری",
        "ATE0" to "اکو خاموش",
        "ATSP5" to "پروتکل KWP Fast",
        "ATDP" to "پروتکل فعال",
        "ATH1" to "نمایش هدر ECU",
        "0100" to "PIDs پشتیبانی شده",
        "10 81" to "جلسه تشخیصی KWP",
        "010C" to "دور موتور RPM",
        "010D" to "سرعت خودرو",
        "0105" to "دمای آب",
        "03" to "خواندن خطاها",
        "04" to "پاک کردن خطاها"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Info
        Text(
            text = "ترمینال فرامین مستقیم ELM327",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = "ارسال مستقیم دستورات AT و کدهای هگزادسیمال OBD-II به ایسیو زیمنس",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset command chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetCommands) { (cmd, desc) ->
                SuggestionChip(
                    onClick = {
                        commandInput = cmd
                        onSendCommand(cmd)
                    },
                    label = {
                        Column {
                            Text(cmd, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input field and send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it.uppercase() },
                placeholder = { Text("دستور مثل: 010C یا ATZ") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onSendCommand(commandInput.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "ارسال")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Black Terminal Output Screen
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 80.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (terminalLogs.isEmpty()) {
                    item {
                        Text(
                            text = "> کنسول ترمینال آماده دریافت و ارسال دستورات است...\n> برای شروع، یک دستور را وارد کرده یا از چیپ‌های بالا انتخاب نمایید.",
                            color = Color(0xFF64B5F6),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(terminalLogs) { log ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[${log.timestamp}] TX: > ${log.command}",
                                color = Color(0xFF81D4FA),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RX: < ${log.response}",
                                color = if (log.isSuccess) Color(0xFF69F0AE) else Color(0xFFFF5252),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
