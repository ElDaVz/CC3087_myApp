package com.uvg.cc3087.myapp.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.uvg.cc3087.myapp.data.repository.DemoRequestRepository
import com.uvg.cc3087.myapp.ui.navigation.RequestDetailRoute
import com.uvg.cc3087.myapp.ui.screens.RequestDetailScreen
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import java.time.Instant

private val sample = DemoRequestRepository.requests.first()

class RequestDetailDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DemoRequestRepository
        setContent {
            MyappTheme {
                RequestDetailRoute(sample.formId, sample.responseId, repository, onBack = { finish() }, isDemo = true)
            }
        }
    }
}

@Preview(showBackground = true, locale = "es", widthDp = 390, heightDp = 844)
@Composable
private fun RequestDetailPreview() {
    MyappTheme(dynamicColor = false) {
        RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, sample), {}, {}, isDemo = true)
    }
}
