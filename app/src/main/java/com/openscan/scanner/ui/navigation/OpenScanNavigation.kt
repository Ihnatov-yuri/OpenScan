package com.openscan.scanner.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openscan.scanner.ui.screens.CameraScreen
import com.openscan.scanner.ui.screens.DocumentReviewScreen
import com.openscan.scanner.ui.screens.HomeScreen
import com.openscan.scanner.ui.screens.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun OpenScanNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME
    ) {
        composable(NavigationRoutes.HOME) {
            HomeScreen(
                onNavigateToCamera = {
                    navController.navigate(NavigationRoutes.CAMERA)
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationRoutes.SETTINGS)
                },
                onNavigateToDocumentReview = { documentPath ->
                    val encodedPath = URLEncoder.encode(documentPath, StandardCharsets.UTF_8.toString())
                    navController.navigate("${NavigationRoutes.DOCUMENT_REVIEW}/$encodedPath")
                }
            )
        }
        
        composable(NavigationRoutes.CAMERA) {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToReview = { documentPath ->
                    val encodedPath = URLEncoder.encode(documentPath, StandardCharsets.UTF_8.toString())
                    navController.navigate("${NavigationRoutes.DOCUMENT_REVIEW}/$encodedPath")
                }
            )
        }
        
        composable("${NavigationRoutes.DOCUMENT_REVIEW}/{documentPath}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("documentPath") ?: ""
            val documentPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            DocumentReviewScreen(
                documentPath = documentPath,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavigationRoutes.HOME) {
                        popUpTo(NavigationRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateToCamera = {
                    navController.navigate(NavigationRoutes.CAMERA)
                }
            )
        }
        
        composable(NavigationRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

object NavigationRoutes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val DOCUMENT_REVIEW = "document_review"
    const val SETTINGS = "settings"
} 