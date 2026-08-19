package fm.libro.wearos.auth

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.wear.input.RemoteInputIntentHelper

class TextInputActivity {

    companion object {
        const val EXTRA_RESULT = "result"

        fun createIntent(label: String, key: String): Intent {
            val remoteInput = RemoteInput.Builder(key)
                .setLabel(label)
                .setAllowFreeFormInput(true)
                .build()

            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput));

            return intent
        }
    }
}
