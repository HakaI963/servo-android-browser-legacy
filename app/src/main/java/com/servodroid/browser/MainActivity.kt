package com.servodroid.browser

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.servodroid.browser.model.Screen
import com.servodroid.browser.ui.BrowserApp
import com.servodroid.browser.ui.theme.ServoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BrowserViewModel(applicationContext) as T
                }
            }
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            ServoTheme(themeMode = viewModel.uiState.themeMode) {
                BrowserApp(viewModel)

                val message = viewModel.uiState.message
                LaunchedEffect(message) {
                    if (message != null) {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.uiState.screen != Screen.Browser -> viewModel.openScreen(Screen.Browser)
                    viewModel.uiState.menuOpen -> viewModel.closeMenu()
                    viewModel.uiState.isEditingAddress -> viewModel.stopAddressEditing()
                    viewModel.uiState.canGoBack -> viewModel.goBack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val url = when {
            Intent.ACTION_VIEW == intent.action -> intent.dataString
            else -> intent.getStringExtra("servoargs")?.takeIf { it.startsWith("http") }
        }
        if (!url.isNullOrBlank()) viewModel.navigate(url)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val unicode = event.unicodeChar
        viewModel.onKeyEvent(event.keyCode, unicode, event.action == KeyEvent.ACTION_DOWN)
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        viewModel.engine.onResume()
    }

    override fun onPause() {
        viewModel.engine.onPause()
        super.onPause()
    }
}
