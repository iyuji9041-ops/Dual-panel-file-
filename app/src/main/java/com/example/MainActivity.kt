package com.example

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StoragePermission
import com.example.ui.DualFileManagerScreen
import com.example.ui.theme.DualFilesTheme
import com.example.viewmodel.DualFileViewModel

class MainActivity : ComponentActivity() {

    // Bumped every time the app returns to the foreground (e.g. back from the permission screen).
    private val resumeTick = mutableIntStateOf(0)

    // Android 10 and below use classic runtime permissions.
    private val legacyStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            resumeTick.intValue++
        }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val dualFileViewModel: DualFileViewModel = viewModel()
            val uiState by dualFileViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDarkTheme = uiState.darkThemeOverride ?: systemDark

            LaunchedEffect(effectiveDarkTheme) {
                if (effectiveDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    )
                }
            }

            // Real internal storage + SD card need "All files access".
            val tick by resumeTick
            var hasAccess by remember { mutableStateOf(StoragePermission.hasAllFilesAccess(this@MainActivity)) }
            var promptDismissed by remember { mutableStateOf(false) }
            LaunchedEffect(tick) {
                val granted = StoragePermission.hasAllFilesAccess(this@MainActivity)
                if (granted && !hasAccess) dualFileViewModel.reloadStorageRoots()
                hasAccess = granted
            }

            DualFilesTheme(
                darkTheme = effectiveDarkTheme,
                paletteStyle = uiState.paletteStyle
            ) {
                if (!hasAccess && !promptDismissed) {
                    AlertDialog(
                        onDismissRequest = { promptDismissed = true },
                        title = { Text("Allow access to all files") },
                        text = {
                            Text(
                                "To browse your phone storage and SD card, this file manager needs " +
                                    "\"All files access\". Everything stays on your device: the app has no " +
                                    "internet permission."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT >= 30) {
                                    startActivity(StoragePermission.allFilesAccessSettingsIntent(this@MainActivity))
                                } else {
                                    legacyStoragePermission.launch(
                                        arrayOf(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    )
                                }
                                promptDismissed = true
                            }) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = { promptDismissed = true }) { Text("Not now") }
                        }
                    )
                }
                DualFileManagerScreen(
                    viewModel = dualFileViewModel,
                    isDarkTheme = effectiveDarkTheme
                )
            }
        }
    }
}
