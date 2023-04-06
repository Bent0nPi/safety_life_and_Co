package com.example.safetylife

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.navigationandmap.ui.NavigatorViewModel
import com.example.navigationandmap.ui.User
import com.example.navigationandmap.ui.models.PointsOnRoard
import com.example.navigationandmap.ui.models.QuadrantsOnMap
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.collect
import org.json.JSONException
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {
    var pref : SharedPreferences? = null
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val TAG = "MainActivity"
    var isActive = false


    private val navigatorViewModel: NavigatorViewModel by viewModels()
    private val locationRequest: LocationRequest = createLocationRequest()
    private lateinit var locationCallback: LocationCallback
    private var angle: Double = 0.2

    fun Double.format(digits: Int) = "%.${digits}f".format(this)

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        setContentView(R.layout.activity_main)

        getSupportActionBar()?.setTitle("Safety Life")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())
        getLoaction()

        createChannel("userRiskInteraction", "Опасные ситуации")
        val notification = NotificationCompat.Builder(this, "userRiskInteraction")
            .setContentText("Оглянись! Рядом дорога.")
            .setContentTitle("Safety Live")
            .setSmallIcon(R.drawable.notify_icon_foreground)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setOngoing(true)

        val a = findViewById<Button>(R.id.button2)
        val buttobState = findViewById<Button>(R.id.button2)
        val backgroundImage = findViewById<ImageView>(R.id.imageView2)

        fun active(){


            if(isActive){
                isActive = !isActive
                buttobState.setBackgroundResource(R.drawable.circle_onoff_button)
                backgroundImage.setImageResource(R.drawable.turnon)
                //notificationManager.cancel(1);
            } else{
                buttobState.setBackgroundResource(R.drawable.circle_onoff_button_active)
                backgroundImage.setImageResource(R.drawable.turnon_active)
                Toast.makeText(this,"Distance:${navigatorViewModel.uiState.dist.format(4
                )}\n" +
                        "inDangerous: ${navigatorViewModel.uiState.inDangerous}\n",Toast.LENGTH_SHORT).show()

                isActive = !isActive
            }
            //notificationManager.notify(1, notification.build()) // !1
        }
        a.setOnClickListener {
            active()
        }



        pref = getSharedPreferences("Pass", MODE_PRIVATE)
        var test = pref?.getString("pass", "hi")!!

        //для компаса
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,  IntentFilter(LocatyService.KEY_ON_SENSOR_CHANGED_ACTION))

        // поделючение JSON
        try{
            val jsonPointsString  = getJSONFromAssets("log_298.json")
            val points = Gson().fromJson(jsonPointsString, PointsOnRoard::class.java)
            val jsonQuadrantsString  = getJSONFromAssets("chunk_log_298.json")
            val quadrants = Gson().fromJson(jsonQuadrantsString, QuadrantsOnMap::class.java)
            navigatorViewModel.loadMapPointsAndQuadrants(points.points, quadrants.quadrants)
        } catch (e: JSONException){
            e.printStackTrace()
        }

        // создаем locationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0?: return
                for (location in p0.locations){
                    navigatorViewModel.updateCoordinates(location.latitude, location.longitude, angle)
                    if(navigatorViewModel.uiState.inDangerous){
                        notificationManager.notify(1, notification.build())
                    } else{
                        notificationManager.cancel(1)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startForegroundServiceForSensors()
        if (true) startLocationUpdates() // вообще вместо true стоит requestingLocationUpdate
    }

    private fun startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10101)
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun startForegroundServiceForSensors() {
        val locatyIntent = Intent(this, LocatyService::class.java)
        startService(locatyIntent)
    }

    fun openAct(v : View) {
        val intent = Intent(this, settings::class.java)
        startActivity(intent)
    }


    private fun getLoaction(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10101)
        }

        val lastLocation = fusedLocationProviderClient.lastLocation

        lastLocation.addOnSuccessListener {
            val address = geocoder.getFromLocation(it.latitude,it.longitude, 1)
            val text = findViewById<TextView>(R.id.sityText)
            text.text = address?.get(0)?.locality ?: "0"
        }
    }



    fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name,
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }

    fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        return locationRequest
    }

    // функция считывания JSON
    private fun getJSONFromAssets(fileName: String): String? {
        var json: String? = null
        val charset: Charset = Charsets.UTF_8
        try{
            val myJsonFile = assets.open(fileName)
            val size = myJsonFile.available()
            val buffer = ByteArray(size)
            myJsonFile.read(buffer)
            myJsonFile.close()
            json = String(buffer, charset)
            Log.d(TAG,"size $size, fileName $fileName")
        }catch(ex: IOException){
            ex.printStackTrace()
            return null
        }
        return json
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            angle = intent.getDoubleExtra(LocatyService.KEY_ANGLE,angle)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

}