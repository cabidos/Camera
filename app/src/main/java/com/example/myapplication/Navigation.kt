package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.screens.MainScreen
import com.example.myapplication.screens.Friends
import com.example.myapplication.screens.SettingsScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import androidx.camera.view.LifecycleCameraController
import android.graphics.Bitmap
import com.example.myapplication.screens.SettingsScreen

@Composable
fun Navigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    controller: LifecycleCameraController,
    firebaseref: DatabaseReference,
    storageRef: StorageReference,
    fusedLocationClient: FusedLocationProviderClient,
    onPhotoTaken: (Bitmap) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {

        composable("main") { MainScreen(
                controller = controller,
                firebaseref = firebaseref,
                storageRef = storageRef,
                fusedLocationClient = fusedLocationClient,
                onPhotoTaken = onPhotoTaken
            )
        }
        composable("settings") {SettingsScreen(modifier)
        }
        composable("friends") {Friends(modifier)
        }
    }
}