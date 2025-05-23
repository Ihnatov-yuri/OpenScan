package com.openscan.scanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.openscan.scanner.ui.navigation.OpenScanNavigation
import com.openscan.scanner.ui.theme.OpenScanTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.d("OpenScan", "OpenCV 4.11.0 loaded successfully")
        } else {
            Log.e("OpenScan", "OpenCV initialization failed!")
        }
        
        setContent {
            OpenScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenScanNavigation()
                }
            }
        }
    }
} 