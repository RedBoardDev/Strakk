package com.strakk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.RootContent
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.auth.RootViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StrakkTheme {
                val rootState by rootViewModel.uiState.collectAsStateWithLifecycle()

                RootContent(state = rootState)
            }
        }
    }
}
