package com.ipafetch.app.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.ErrorLine
import com.ipafetch.app.ui.components.ExpressiveTextField
import com.ipafetch.app.ui.components.PrimaryButton
import com.ipafetch.app.ui.components.SecondaryButton
import com.ipafetch.app.ui.theme.IpafetchTheme

@Composable
private fun AuthShell(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val colors = IpafetchTheme.colors
    AppBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) { content() }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Encrypted on device · AES-256-GCM",
                color = colors.textSecondary.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SetupScreen(
    onCreate: (String) -> Unit,
    error: String?
) {
    val colors = IpafetchTheme.colors
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AuthShell(
        title = "Create your vault",
        subtitle = "One master password protects every token, key and link. It never leaves this device."
    ) {
        ExpressiveTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = "Master password",
            placeholder = "At least 8 characters",
            isSecret = true
        )
        Spacer(Modifier.height(12.dp))
        ExpressiveTextField(
            value = confirm,
            onValueChange = { confirm = it; localError = null },
            label = "Confirm password",
            placeholder = "Repeat the password",
            isSecret = true
        )
        Spacer(Modifier.height(6.dp))
        ErrorLine(localError ?: error)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            label = "Create vault",
            onClick = {
                val problem = when {
                    password.length < 8 -> "Use at least 8 characters"
                    password != confirm -> "Passwords do not match"
                    else -> null
                }
                localError = problem
                if (problem == null) onCreate(password)
            }
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Biometric unlock becomes available once the vault exists.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun UnlockScreen(
    error: String?,
    biometricAvailable: Boolean,
    onSubmitPassword: (String) -> Unit,
    onBiometric: () -> Unit
) {
    val colors = IpafetchTheme.colors
    var password by remember { mutableStateOf("") }
    var triedBiometric by remember { mutableStateOf(false) }

    AuthShell(
        title = "Welcome back",
        subtitle = "Unlock to reach your tokens, API keys and links."
    ) {
        ExpressiveTextField(
            value = password,
            onValueChange = { password = it },
            label = "Master password",
            placeholder = "Enter master password",
            isSecret = true
        )
        Spacer(Modifier.height(6.dp))
        ErrorLine(error)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(label = "Unlock", onClick = { onSubmitPassword(password) })
        if (biometricAvailable) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                label = if (triedBiometric) "Retry biometric unlock" else "Use biometrics",
                onClick = { triedBiometric = true; onBiometric() }
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Locks again after the auto-lock timeout",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}
