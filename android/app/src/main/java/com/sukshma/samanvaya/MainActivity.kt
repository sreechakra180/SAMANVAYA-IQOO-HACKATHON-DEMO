package com.sukshma.samanvaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sukshma.samanvaya.core.SamanvayaEngine
import com.sukshma.samanvaya.ui.theme.SamanvayaTheme
import com.sukshma.samanvaya.ui.navigation.SamanvayaNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var engine: SamanvayaEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = SamanvayaEngine(application)
        engine.initialize()

        enableEdgeToEdge()
        setContent {
            SamanvayaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SamanvayaNavGraph(engine = engine)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.shutdown()
    }
}
