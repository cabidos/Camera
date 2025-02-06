import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locationupdate.LocationService
import org.w3c.dom.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show a toast when the app sta
        ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java))
        // Check for permissions and request if not granted
        if (!checkPermission()) {
            requestPermission()
        } else {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if location permissions are granted
    private fun checkPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }
}

@Composable
fun MyButton(context: Context) {
    Button(onClick = {
        Toast.makeText(context, "Starting service", Toast.LENGTH_SHORT).show()
        ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
    }) {
        Text(text = "Start Service")
    }
}

@Composable
fun MyStopButton(context: Context) {
    Button(onClick = {
        Toast.makeText(context, "Stopping service", Toast.LENGTH_SHORT).show()
        context.stopService(Intent(context, LocationService::class.java))
    }) {
        Text(text = "Stop Service")
    }
}
