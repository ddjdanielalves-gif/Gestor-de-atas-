package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AgendaRemindersScreen
import com.example.ui.screens.AtasHistoryScreen
import com.example.ui.screens.DashboardAlertsScreen
import com.example.ui.screens.DecisionsScreen
import com.example.ui.screens.UploadAtaScreen
import com.example.ui.theme.DueTodayAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.MainViewModel

enum class AppNavigationTab(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("Painel & Avisos", Icons.Default.NotificationsActive, "nav_dashboard"),
    UPLOAD_ATA("Receber Ata", Icons.Default.UploadFile, "nav_upload"),
    DECISIONS("Decisões", Icons.Default.Checklist, "nav_decisions"),
    AGENDA("Agenda", Icons.Default.CalendarMonth, "nav_agenda"),
    HISTORY("Atas Salvas", Icons.Default.FolderShared, "nav_history")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(AppNavigationTab.DASHBOARD) }
    val alertsSummary by viewModel.alertsSummary.collectAsStateWithLifecycle()

    val totalAlerts = alertsSummary.overdueCount + alertsSummary.dueTodayCount + alertsSummary.pendingRemindersTodayCount

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Gestor de Atas & Prazos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                actions = {
                    if (totalAlerts > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = if (alertsSummary.overdueCount > 0) OverdueRed else DueTodayAmber
                                ) {
                                    Text(
                                        text = totalAlerts.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            IconButton(onClick = { selectedTab = AppNavigationTab.DASHBOARD }) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alertas de Consulta",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                AppNavigationTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount = when (tab) {
                        AppNavigationTab.DASHBOARD -> if (totalAlerts > 0) totalAlerts else 0
                        AppNavigationTab.DECISIONS -> if (alertsSummary.totalPendingDecisions > 0) alertsSummary.totalPendingDecisions else 0
                        AppNavigationTab.AGENDA -> if (alertsSummary.pendingRemindersTodayCount > 0) alertsSummary.pendingRemindersTodayCount else 0
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (badgeCount > 0 && !isSelected) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = if (tab == AppNavigationTab.DASHBOARD && alertsSummary.overdueCount > 0) OverdueRed else NavyPrimary
                                        ) {
                                            Text(badgeCount.toString(), fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = NavyPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                AppNavigationTab.DASHBOARD -> DashboardAlertsScreen(
                    viewModel = viewModel,
                    onNavigateToUpload = { selectedTab = AppNavigationTab.UPLOAD_ATA },
                    onNavigateToDecisions = { selectedTab = AppNavigationTab.DECISIONS },
                    onNavigateToAgenda = { selectedTab = AppNavigationTab.AGENDA }
                )
                AppNavigationTab.UPLOAD_ATA -> UploadAtaScreen(
                    viewModel = viewModel,
                    onSavedSuccessfully = { selectedTab = AppNavigationTab.DASHBOARD }
                )
                AppNavigationTab.DECISIONS -> DecisionsScreen(
                    viewModel = viewModel
                )
                AppNavigationTab.AGENDA -> AgendaRemindersScreen(
                    viewModel = viewModel
                )
                AppNavigationTab.HISTORY -> AtasHistoryScreen(
                    viewModel = viewModel,
                    onNavigateToUpload = { selectedTab = AppNavigationTab.UPLOAD_ATA }
                )
            }
        }
    }
}
