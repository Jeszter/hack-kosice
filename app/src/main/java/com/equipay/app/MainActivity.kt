package com.equipay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.equipay.app.navigation.AppNavHost
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.EquiPayTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            EquiPayTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Bg),
                    color = Bg
                ) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return

        if (data.scheme == "equipay" && data.host == "tatra") {
            val success = data.getQueryParameter("success")
            val error = data.getQueryParameter("error")

            if (success == "true") {
                println("Tatra connected successfully")
            } else {
                println("Tatra connection failed: $error")
            }
        }
    }
}