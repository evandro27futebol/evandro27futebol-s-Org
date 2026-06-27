package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ProjectViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

// --- Navigation Route Definitions ---
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_UPLOAD = "upload"
const val ROUTE_LIBRARY = "library"
const val ROUTE_EDITOR = "editor"
const val ROUTE_PREVIEW = "preview"
const val ROUTE_EXPORT = "export"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    FacelessAppNavigation()
                }
            }
        }
    }
}

@Composable
fun FacelessAppNavigation() {
    val navController = rememberNavController()
    val viewModel: ProjectViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = ROUTE_DASHBOARD,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToUpload = { navController.navigate(ROUTE_UPLOAD) },
                onNavigateToLibrary = { navController.navigate(ROUTE_LIBRARY) },
                onNavigateToEditor = { navController.navigate(ROUTE_EDITOR) }
            )
        }

        composable(ROUTE_UPLOAD) {
            UploadScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(ROUTE_EDITOR) {
                        popUpTo(ROUTE_DASHBOARD)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_EDITOR) {
            EditorScreen(
                viewModel = viewModel,
                onNavigateToPreview = { navController.navigate(ROUTE_PREVIEW) },
                onNavigateToExport = { navController.navigate(ROUTE_EXPORT) },
                onNavigateBack = {
                    viewModel.deselectProject()
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_PREVIEW) {
            PreviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_EXPORT) {
            ExportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
