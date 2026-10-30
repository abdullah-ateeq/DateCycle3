package com.example.datecycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.datecycle.ui.navigation.DateCycleNavHost
import com.example.datecycle.ui.theme.DateCycleTheme
import com.example.datecycle.ui.viewmodel.DateCycleViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DateCycleViewModel by viewModels { DateCycleViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DateCycleTheme(themeMode = uiState.themeMode) {
                DateCycleNavHost(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}
