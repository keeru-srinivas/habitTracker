package com.example.cozytrack.presentation.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val errorMessage = state.errorMessage
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧸☕",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "Welcome back!",
            color = AuthText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Let's continue your cozy journey 🧸",
            color = AuthBrown,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        CozyAuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email or username",
            leading = "✉️"
        )

        CozyAuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            leading = "🔒",
            trailing = if (passwordVisible) "🙈" else "👁️",
            onTrailingClick = { passwordVisible = !passwordVisible },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text(
                    text = "Remember me",
                    color = AuthText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Forgot password?",
                color = AuthBrown,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AuthBrown)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("🐾  Log in")
            }
        }

        AuthDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "G  Continue with Google",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp),
                color = AuthText,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(onClick = onSignUpClick) {
            Text(
                text = "Don't have an account?  Sign up",
                color = AuthBrown,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "🐾        🪴",
            modifier = Modifier.align(Alignment.Start),
            color = AuthAccent,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun CozyAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leading: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Text(leading) },
        trailingIcon = if (trailing != null) {
            {
                TextButton(onClick = { onTrailingClick?.invoke() }) {
                    Text(trailing)
                }
            }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuthAccent,
            unfocusedBorderColor = AuthBorder,
            focusedLabelColor = AuthBrown,
            unfocusedLabelColor = AuthMuted,
            focusedContainerColor = Color(0xFFFFFCF7),
            unfocusedContainerColor = Color(0xFFFFFCF7),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = Color.Black
        )
    )
}

@Composable
private fun AuthDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthBorder)
        )
        Text(
            text = "or",
            color = AuthText,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthBorder)
        )
    }
}

private val AuthBackground = Color(0xFFFFF3E3)
private val AuthBrown = Color(0xFFA96332)
private val AuthAccent = Color(0xFFD7A276)
private val AuthBorder = Color(0xFFF0C8AA)
private val AuthText = Color(0xFF3B2416)
private val AuthMuted = Color(0xFF9A7A64)
