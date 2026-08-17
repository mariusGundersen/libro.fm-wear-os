package fm.libro.wearos.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
) {
    var email by remember { mutableStateOf(viewModel.email) }
    var password by remember { mutableStateOf(viewModel.password) }

    val emailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringExtra(TextInputActivity.EXTRA_RESULT) ?: ""
            email = text
            viewModel.email = text
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringExtra(TextInputActivity.EXTRA_RESULT) ?: ""
            password = text
            viewModel.password = text
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Libro.fm",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                emailLauncher.launch(
                    TextInputActivity.createIntent("Email", "email")
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (email.isNotBlank()) "Email: $email" else "Enter email")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                passwordLauncher.launch(
                    TextInputActivity.createIntent("Password", "password")
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (password.isNotBlank()) "Password: ****" else "Enter password")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(36.dp))
        } else {
            Button(
                onClick = {
                    viewModel.login(onSuccess = onLoginSuccess)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log in")
            }
        }

        viewModel.error?.let { errorMsg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
