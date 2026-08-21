package com.bskai.ui.screens.agreements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.PermContactCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.BskApp

@Composable
fun AgreementsScreen(
    app: BskApp,
    onAgreeAll: () -> Unit,
    onSkipToMain: () -> Unit
) {
    val viewModel: AgreementsViewModel = viewModel(factory = AgreementsViewModel.Factory(app))
    var showContent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF0A0914))
    ) {
        if (!showContent) {
            SplashScreen(onAgree = { showContent = true })
        } else {
            ContentScreen(
                agreedToTerms = viewModel.agreedToTerms,
                agreedToOSS = viewModel.agreedToOSS,
                onToggleTerms = { viewModel.toggleTerms() },
                onToggleOSS = { viewModel.toggleOSS() },
                onAgreeAll = {
                    viewModel.agreeAll()
                    onAgreeAll()
                },
                onSkip = onSkipToMain
            )
        }
    }
}

@Composable
private fun SplashScreen(onAgree: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(0.5f, 0.5f),
                        radius = 160f
                    ),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("\u26A1", fontSize = 36.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "BSK AI",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "v1.0.8",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Claude Code style coding assistant",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onAgree,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Continuing means you agree to the Terms of Service and Open Source Licenses",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ContentScreen(
    agreedToTerms: Boolean,
    agreedToOSS: Boolean,
    onToggleTerms: () -> Unit,
    onToggleOSS: () -> Unit,
    onAgreeAll: () -> Unit,
    onSkip: () -> Unit
) {
    val allAgreed = agreedToTerms && agreedToOSS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agreements", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x301A1730))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PermContactCalendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("User Agreement", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Welcome to BSK AI. By using this application you agree to the following terms:\n\n" +
                        "1. This application is for personal learning and research purposes only.\n" +
                        "2. Users are responsible for any risks arising from the use of this application.\n" +
                        "3. We are not liable for any direct or indirect damages arising from the use of this application.\n" +
                        "4. We reserve the right to modify or terminate this application at any time.\n" +
                        "5. Your use of this application must comply with applicable laws and regulations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("I have read and agree", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = agreedToTerms, onCheckedChange = { onToggleTerms() })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x301A1730))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Open Source Licenses", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This application uses the following open source components. Thank you to all contributors:\n\n" +
                        "- AndroidX Compose - Apache 2.0\n" +
                        "- Material Design 3 - Apache 2.0\n" +
                        "- OkHttp - Apache 2.0\n" +
                        "- Kotlin Coroutines - Apache 2.0\n\n" +
                        "Full license information can be viewed in the app settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("I understand", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = agreedToOSS, onCheckedChange = { onToggleOSS() })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onAgreeAll,
                enabled = allAgreed,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allAgreed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            ) {
                if (allAgreed) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.width(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agree and Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Please complete the agreements above first", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            androidx.compose.material3.TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Skip for now", fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
