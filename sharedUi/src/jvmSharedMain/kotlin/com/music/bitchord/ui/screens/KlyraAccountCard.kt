package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.klyra.shared.KlyraAccount

/** Shared account controls; both platforms use the original Klyra Supabase protocol. */
@Composable
fun KlyraAccountCard(modifier: Modifier = Modifier) {
    val state by KlyraAccount.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(false) }
    var verify by remember { mutableStateOf(false) }
    var emailLink by remember { mutableStateOf("") }
    var changePassword by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth().padding(20.dp), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your Klyra account", style = MaterialTheme.typography.titleLarge)
            Text("Your library, across Android and Windows. This is separate from your music provider account.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!state.configured) {
                Text("Cloud accounts are not configured in this build. Local music and provider playback remain available.")
            } else if (state.email == null) {
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.busy)
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !state.busy)
                Button(onClick = {
                    if (signUp) KlyraAccount.signUp(email, password) else KlyraAccount.signIn(email, password)
                    password = ""
                }, enabled = !state.busy && email.isNotBlank() && password.length >= 8) { Text(if (signUp) "Create account" else "Sign in") }
                TextButton({ signUp = !signUp }, enabled = !state.busy) { Text(if (signUp) "Already have an account? Sign in" else "Create a Klyra account") }
                TextButton({ KlyraAccount.recover(email) }, enabled = !state.busy && email.isNotBlank()) { Text("Forgot password") }
                TextButton({ verify = !verify }, enabled = !state.busy) { Text("Verify email link") }
                if (verify) {
                    Text("Copy the confirmation or recovery link from your Klyra email before opening it. Paste it here to finish in the app.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(emailLink, { emailLink = it }, label = { Text("Email link") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Button({ KlyraAccount.verifyEmailLink(emailLink, email); emailLink = "" }, enabled = !state.busy && email.isNotBlank() && emailLink.isNotBlank()) { Text("Verify") }
                }
            } else {
                Text(state.email.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text("${state.records.count { it.kind == "playlist" }} playlists · ${state.records.count { it.kind == "like" }} favorites")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ KlyraAccount.sync() }, enabled = !state.busy) { Text("Sync library") }
                    TextButton({ KlyraAccount.signOut() }, enabled = !state.busy) { Text("Sign out") }
                }
                TextButton({ changePassword = !changePassword }, enabled = !state.busy) { Text("Change password") }
                if (changePassword || state.recovery) {
                    OutlinedTextField(password, { password = it }, label = { Text("New password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !state.busy)
                    Button({ KlyraAccount.updatePassword(password); password = ""; changePassword = false }, enabled = !state.busy && password.length >= 8) { Text("Update password") }
                }
                TextButton({ deleteConfirmation = true }, enabled = !state.busy) { Text("Delete Klyra account", color = MaterialTheme.colorScheme.error) }
            }
            if (state.conflicts.isNotEmpty()) {
                Text("${state.conflicts.size} library changes need your choice. This applies only to conflicting records.")
                TextButton({ KlyraAccount.resolveConflicts(true) }, enabled = !state.busy) { Text("Keep this device's edits") }
                TextButton({ KlyraAccount.resolveConflicts(false) }, enabled = !state.busy) { Text("Use cloud versions") }
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
    if (deleteConfirmation) AlertDialog(onDismissRequest = { deleteConfirmation = false },
        title = { Text("Delete your Klyra account?") },
        text = { Text("This permanently deletes your account, cloud favorites and cloud playlists. Music files and your provider account are not deleted.") },
        confirmButton = { TextButton({ deleteConfirmation = false; KlyraAccount.deleteAccount() }) { Text("Delete account", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ deleteConfirmation = false }) { Text("Cancel") } })
}
