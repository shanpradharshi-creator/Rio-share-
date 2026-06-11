package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.RioShareAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ShareViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShareViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDark) {
                RioShareAppContent(viewModel = viewModel)
            }
        }
    }
}
