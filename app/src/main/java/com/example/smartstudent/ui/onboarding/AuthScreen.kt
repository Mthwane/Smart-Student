package com.example.smartstudent.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.StudentGray200
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.SecondaryPillButton
import com.example.smartstudent.ui.components.StudentTextField
import com.example.smartstudent.ui.components.TextLinkButton

enum class AuthMode { SIGN_UP, LOG_IN }

data class SignUpDetails(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

@Composable
fun AuthScreen(
    initialMode: AuthMode,
    onBack: () -> Unit,
    onSignUp: (SignUpDetails) -> Unit,
    onLogIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    googleSignInAvailable: Boolean,
    loading: Boolean = false,
    errorMessage: String? = null
) {
    var mode by remember { mutableStateOf(initialMode) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isSignUp = mode == AuthMode.SIGN_UP
    val canSubmit = if (isSignUp) {
        firstName.isNotBlank() && lastName.isNotBlank() && email.contains("@") && password.length >= 8
    } else {
        email.contains("@") && password.isNotBlank()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (isSignUp) "Create your account" else "Welcome back",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(20.dp))

                SecondaryPillButton(
                    text = "Continue with Google",
                    onClick = onGoogleSignIn,
                    enabled = googleSignInAvailable
                )
                if (!googleSignInAvailable) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Google Sign-In isn't configured yet (needs a SHA-1 fingerprint added in Firebase).",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudentGray600
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = StudentGray200)
                    Text("  or use email  ", style = MaterialTheme.typography.labelMedium, color = StudentGray600)
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = StudentGray200)
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (isSignUp) {
                    StudentTextField(value = firstName, onValueChange = { firstName = it }, label = "First name")
                    Spacer(modifier = Modifier.height(14.dp))
                    StudentTextField(value = lastName, onValueChange = { lastName = it }, label = "Last name")
                    Spacer(modifier = Modifier.height(14.dp))
                }

                StudentTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email address",
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(14.dp))
                StudentTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = if (isSignUp) "Password (min. 8 characters)" else "Password",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    Text(
                        if (isSignUp) "Already have an account? " else "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudentGray600
                    )
                    TextLinkButton(
                        text = if (isSignUp) "Log in" else "Sign up",
                        onClick = { mode = if (isSignUp) AuthMode.LOG_IN else AuthMode.SIGN_UP }
                    )
                }
            }

            PrimaryPillButton(
                text = if (isSignUp) "Sign up" else "Log in",
                onClick = {
                    if (isSignUp) {
                        onSignUp(SignUpDetails(firstName, lastName, email, password))
                    } else {
                        onLogIn(email, password)
                    }
                },
                enabled = canSubmit,
                loading = loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}
