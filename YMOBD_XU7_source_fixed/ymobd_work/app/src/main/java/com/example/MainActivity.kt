package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.YmobdTopHeader
import com.example.ui.screens.*
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.ObdViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ObdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ObdViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val vehicleData by viewModel.vehicleData.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isSimulated by viewModel.isSimulatedMode.collectAsStateWithLifecycle()
    val storedDtcs by viewModel.storedDtcs.collectAsStateWithLifecycle()
    val selectedDtc by viewModel.selectedDtcForAi.collectAsStateWithLifecycle()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsStateWithLifecycle()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsStateWithLifecycle()
    val ecuConfigs by viewModel.ecuConfigs.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Status Message Toast
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Bluetooth permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            Toast.makeText(context, "جهت اتصال به دانگل بلوتوث، دسترسی لازم است", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            YmobdTopHeader(
                connectionState = connectionState,
                isSimulated = isSimulated,
                onSimulatedToggle = { viewModel.setSimulationMode(it) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppNavTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppNavTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "داشبورد") },
                    label = { Text("داشبورد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.DIAGNOSTICS,
                    onClick = { viewModel.selectTab(AppNavTab.DIAGNOSTICS) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "دیاگ هوشمند") },
                    label = { Text("دیاگ AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.ECU_CONFIG,
                    onClick = { viewModel.selectTab(AppNavTab.ECU_CONFIG) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "پیکربندی ECU") },
                    label = { Text("تنظیمات ECU", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.TERMINAL,
                    onClick = { viewModel.selectTab(AppNavTab.TERMINAL) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "ترمینال") },
                    label = { Text("ترمینال", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.BLUETOOTH,
                    onClick = { viewModel.selectTab(AppNavTab.BLUETOOTH) },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = "اتصال دانگل") },
                    label = { Text("دانگل OBD", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppNavTab.DASHBOARD -> {
                    DashboardScreen(
                        vehicleData = vehicleData,
                        connectionState = connectionState,
                        onNavigateToSettings = { viewModel.selectTab(AppNavTab.BLUETOOTH) },
                        onNavigateToDiagnostics = { viewModel.selectTab(AppNavTab.DIAGNOSTICS) }
                    )
                }

                AppNavTab.DIAGNOSTICS -> {
                    DiagnosticScreen(
                        storedDtcs = storedDtcs,
                        selectedDtc = selectedDtc,
                        aiAnalysisResult = aiAnalysisResult,
                        isAiAnalyzing = isAiAnalyzing,
                        onScanClick = { viewModel.scanTroubleCodes() },
                        onClearClick = { viewModel.clearTroubleCodes() },
                        onSelectDtc = { viewModel.selectDtcForAi(it) },
                        onCustomCodeAnalyze = { viewModel.analyzeCustomDtc(it) }
                    )
                }

                AppNavTab.ECU_CONFIG -> {
                    EcuConfigScreen(
                        ecuConfigs = ecuConfigs,
                        onToggleConfig = { viewModel.toggleEcuConfig(it) },
                        onRunActuatorTest = { testName ->
                            Toast.makeText(context, "برای $testName ابتدا Routine ID واقعی ECU را باید شناسایی کنیم؛ فرمان حدسی ارسال نشد.", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                AppNavTab.TERMINAL -> {
                    TerminalScreen(
                        terminalLogs = terminalLogs,
                        onSendCommand = { viewModel.sendTerminalCommand(it) }
                    )
                }

                AppNavTab.BLUETOOTH -> {
                    BluetoothScreen(
                        connectionState = connectionState,
                        isSimulated = isSimulated,
                        pairedDevices = viewModel.getPairedDevices(),
                        onToggleSimulated = { viewModel.setSimulationMode(it) },
                        onConnectDevice = { viewModel.connectRealDevice(it) },
                        onDisconnect = { viewModel.disconnect() }
                    )
                }
            }
        }
    }
}
