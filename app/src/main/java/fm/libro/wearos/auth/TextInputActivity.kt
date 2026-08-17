package fm.libro.wearos.auth

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle

class TextInputActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Input"
        val key = intent.getStringExtra(EXTRA_KEY) ?: "result"

        val remoteInput = RemoteInput.Builder(key)
            .setLabel(label)
            .setAllowFreeFormInput(true)
            .build()

        val intent = Intent("android.support.wearable.input.action.REMOTE_INPUT").apply {
            putExtra(
                "android.support.wearable.input.extra.REMOTE_INPUTS",
                arrayOf(remoteInput),
            )
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && data != null) {
            val bundle = RemoteInput.getResultsFromIntent(data)
            val key = intent.getStringExtra(EXTRA_KEY) ?: "result"
            val text = bundle.getCharSequence(key)?.toString() ?: ""

            val resultIntent = Intent().apply {
                putExtra(EXTRA_RESULT, text)
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        const val EXTRA_LABEL = "label"
        const val EXTRA_KEY = "key"
        const val EXTRA_RESULT = "result"
        const val REQUEST_CODE = 9001

        fun createIntent(label: String, key: String): Intent {
            return Intent().setClassName(
                "fm.libro.wearos",
                "fm.libro.wearos.auth.TextInputActivity",
            ).apply {
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_KEY, key)
            }
        }
    }
}
