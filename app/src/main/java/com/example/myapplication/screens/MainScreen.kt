package com.example.myapplication.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.CameraPreview

import com.example.myapplication.utils.hasRequiredPermissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    controller: LifecycleCameraController,
    firebaseref: DatabaseReference,
    storageRef: StorageReference,
    fusedLocationClient: FusedLocationProviderClient,
    onPhotoTaken: (Bitmap) -> Unit
) {
    // Navigation items for the drawer
    val items = listOf(
        NavigationItem("Home", "main", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem("Friends", "friends", Icons.Filled.Info, Icons.Outlined.Info),
        NavigationItem("Settings", "settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    // Drawer state and coroutine scope for handling UI state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val context = LocalContext.current
    // Main drawer
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                items.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(text = item.title) },
                        selected = false, // Logic for selection highlight (optional)
                        onClick = {
                            navController.navigate(item.route)  // Navigate to selected route
                            scope.launch { drawerState.close() } // Close the drawer after selection
                        },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Camera App") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxHeight()
            ) {
                // Camera preview content here
                CameraPreview(controller = controller, modifier = Modifier.fillMaxWidth()
                                                                          .fillMaxHeight(0.8f)
                            )
                IconButton(
                    onClick = {
                        controller.cameraSelector =
                            if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    modifier = Modifier
                        .offset(16.dp, 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera"
                    )
                }

                // Bottom buttons or actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(50.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = {
                        // Action for taking photo (integrate with your existing photo logic)
                        takePhoto(controller, storageRef, firebaseref, onPhotoTaken, context)
                        getLocation(fusedLocationClient,firebaseref, context)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo"
                        )
                    }


                }
            }
        }
    }
}

private fun takePhoto(
    controller: LifecycleCameraController,
    storageRef: StorageReference,
    firebaseref: DatabaseReference,
    onPhotoTaken: (Bitmap) -> Unit,
    context: Context
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )

                // Convert Bitmap to ByteArray
                val byteArrayOutputStream = ByteArrayOutputStream()
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // Upload the image to Firebase Storage
                val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
                val uploadTask: UploadTask = imageRef.putBytes(byteArray)

                uploadTask.addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Save the download URL to Firebase Database or use it as needed
                        Log.d("Firebase", "Image uploaded successfully: $uri")
                        Toast.makeText(context, "Photo uploaded", Toast.LENGTH_SHORT).show()

                        // Optionally, you can save the URL to Firebase Database if needed

                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error uploading image", exception)
                    Toast.makeText(context, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                }

                // Call onPhotoTaken with the Bitmap if needed
                onPhotoTaken(rotatedBitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Couldn't take photo: ", exception)
            }
        }
    )
}

private fun getLocation(
    fusedLocationClient: FusedLocationProviderClient,
    firebaseref: DatabaseReference,
    context: Context
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        // Request location permissions if not granted
        return
    }
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            firebaseref.setValue("latitude: ${location.latitude}, longitude: ${location.longitude}")
                .addOnCompleteListener {
                    Toast.makeText(context, "Location sent", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

private val CAMERAX_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
)

object AppPermissions {
    val CAMERAX_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}
data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
