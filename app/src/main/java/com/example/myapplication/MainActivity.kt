package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.Button
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ContentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var storageRef: StorageReference
    private lateinit var selectedImageUri: Uri
    private lateinit var firebaseref: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var contentBinding: ContentMainBinding
    private lateinit var database: DatabaseReference  // Référence Firebase

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Initialisation du binding pour content_main.xml
        contentBinding = ContentMainBinding.bind(findViewById(R.id.app_bar_main))

        // Initialisation de Firebase Database
        firebaseref = FirebaseDatabase.getInstance().getReference("Position")
        database = FirebaseDatabase.getInstance().reference.child("users")

        setSupportActionBar(binding.appBarMain.toolbar)

        storageRef = FirebaseStorage.getInstance().reference.child("uploads")

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.appBarMain.fab.setOnClickListener {
            selectImage()
        }

        // Listener pour le bouton d'envoi de la localisation
        contentBinding.buttonLocation.setOnClickListener {
            startLocationService()
        }
        contentBinding.buttonLocation2.setOnClickListener {
            stopLocationService()
        }

        // Listener pour le bouton d'enregistrement du nom
        contentBinding.buttonSave.setOnClickListener {
            val name = contentBinding.editTextName.text.toString().trim()
            if (name.isNotEmpty()) {
                saveNameToFirebase(name)
            } else {
                Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let {
                selectedImageUri = it
                uploadImage()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Service de localisation démarré", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Service de localisation arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        val location = fusedLocationClient.lastLocation
        location.addOnSuccessListener {
            if (it != null) {
                firebaseref.setValue("latitude: ${it.latitude}, longitude: ${it.longitude}")
                    .addOnCompleteListener {
                        Toast.makeText(this, "Localisation envoyée", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    private fun CheckPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                )== PackageManager.PERMISSION_GRANTED

    }
    private fun saveNameToFirebase(name: String) {
        val userId = database.push().key
        if (userId != null) {
            database.child(userId).setValue(name)
                .addOnSuccessListener {
                    Toast.makeText(this, "Nom enregistré avec succès !", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadImage() {
        if (!::selectedImageUri.isInitialized) {
            Toast.makeText(this, "Sélectionnez une image d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        val fileRef = storageRef.child("${UUID.randomUUID()}.jpg")

        fileRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    Toast.makeText(this, "Upload réussi : $uri", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Échec de l'upload : ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}