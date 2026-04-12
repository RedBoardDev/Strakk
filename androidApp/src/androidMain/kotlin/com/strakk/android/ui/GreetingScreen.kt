package com.strakk.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.domain.model.Greeting
import com.strakk.shared.domain.model.Quote
import com.strakk.shared.presentation.GreetingUiState
import com.strakk.shared.presentation.GreetingViewModel
import com.strakk.shared.presentation.QuoteUiState
import com.strakk.shared.presentation.QuoteViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    greetingViewModel: GreetingViewModel = koinViewModel(),
    quoteViewModel: QuoteViewModel = koinViewModel(),
) {
    val greetingState by greetingViewModel.uiState.collectAsStateWithLifecycle()
    val quoteState by quoteViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        greetingViewModel.load()
        quoteViewModel.load()
    }

    HomeScreen(
        greetingState = greetingState,
        quoteState = quoteState,
        onRefreshQuote = quoteViewModel::refresh,
    )
}

@Composable
private fun HomeScreen(
    greetingState: GreetingUiState,
    quoteState: QuoteUiState,
    onRefreshQuote: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Greeting section
        when (greetingState) {
            is GreetingUiState.Loading -> CircularProgressIndicator()
            is GreetingUiState.Success -> GreetingContent(greeting = greetingState.greeting)
            is GreetingUiState.Error -> ErrorText(message = greetingState.message)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        // Quote section
        when (quoteState) {
            is QuoteUiState.Loading -> CircularProgressIndicator()
            is QuoteUiState.Success -> QuoteContent(quote = quoteState.quote)
            is QuoteUiState.Error -> ErrorText(message = quoteState.message)
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onRefreshQuote) {
            Text(text = "New Quote")
        }
    }
}

@Composable
private fun GreetingContent(greeting: Greeting) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = greeting.message,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Running on: ${greeting.platform}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuoteContent(quote: Quote) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\"${quote.text}\"",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "— ${quote.author}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
}
