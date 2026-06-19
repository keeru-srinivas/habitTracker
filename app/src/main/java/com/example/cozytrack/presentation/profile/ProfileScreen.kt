package com.example.cozytrack.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cozytrack.presentation.components.BrownPrimary
import com.example.cozytrack.presentation.components.CreamCard
import com.example.cozytrack.presentation.components.DeepText
import com.example.cozytrack.presentation.components.DeleteRed
import com.example.cozytrack.presentation.components.MutedText
import com.example.cozytrack.presentation.components.ScreenBackground
import com.example.cozytrack.presentation.components.prettyDate
import com.example.cozytrack.presentation.habits.HabitListViewModel

@Composable
fun ProfileScreen(
    viewModel: HabitListViewModel,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLoggedOut()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            color = DeepText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )

        ProfileHeaderCard(
            userName = state.userName,
            habitCount = state.habits.count { !it.isArchived },
            serverDate = state.utcCalendarDate
        )

        SettingsSection(title = "Preferences") {
            SettingsToggleRow(
                label = "Show archived habits",
                description = "Include archived habits on Home and Habits",
                checked = state.includeArchived,
                onCheckedChange = viewModel::onIncludeArchivedChange
            )
        }

        SettingsSection(title = "Account") {
            SettingsInfoRow(
                label = "Signed in as",
                value = if (state.userName.isBlank()) "CozyTrack user" else state.userName
            )
            SettingsInfoRow(
                label = "Active habits",
                value = "${state.habits.count { !it.isArchived }}"
            )
            SettingsInfoRow(
                label = "Server date (UTC)",
                value = state.utcCalendarDate.prettyDate()
            )
        }

        Button(
            onClick = viewModel::logout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeleteRed,
                contentColor = Color.White
            )
        ) {
            Text("Log out")
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    userName: String,
    habitCount: Int,
    serverDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrownPrimary, Color(0xFFD1A06F))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🐻",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (userName.isBlank()) "Your profile" else userName,
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "$habitCount active habits · ${serverDate.prettyDate()}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = DeepText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = DeepText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = DeepText,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
