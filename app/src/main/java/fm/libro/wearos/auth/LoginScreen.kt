package fm.libro.wearos.auth

import android.app.Activity
import android.app.RemoteInput
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
) {
    val emailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        it.data?.let { data ->
            viewModel.email =
                RemoteInput.getResultsFromIntent(data).getCharSequence("email").toString()
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        it.data?.let { data ->
            viewModel.password =
                RemoteInput.getResultsFromIntent(data).getCharSequence("password").toString()
        }
    }

    ScalingLazyColumn (
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item{
            Text(
                text = "Libro.fm",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Button(
                onClick = {
                    emailLauncher.launch(
                        TextInputActivity.createIntent("Email", "email")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (viewModel.email.isNotBlank()) "Email: ${viewModel.email}" else "Enter email")
            }
        }

        item{
            Spacer(modifier = Modifier.height(8.dp))
        }

        item{
            Button(
                onClick = {
                    passwordLauncher.launch(
                        TextInputActivity.createIntent("Password", "password")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (viewModel.password.isNotBlank()) "Password: ****" else "Enter password")
            }
        }

        item{
            Spacer(modifier = Modifier.height(12.dp))
        }

        item{
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
        }

        item{
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
}
