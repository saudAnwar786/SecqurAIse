package com.sacoding.secquraise

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ktx.database

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.sacoding.secquraise.data.DataUi
import com.sacoding.secquraise.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryManager: BatteryManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private val viewModel : MainViewModel by viewModels()
    var captureCount = 0
    val imageRef = Firebase.storage.reference
    lateinit var imageUri: Uri
    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){
        binding.imageView.setImageURI(null)
        binding.imageView.setImageURI(imageUri)
        captureCount++
        uploadImageToStorage()
    }
    private val handler = Handler()
    private var updateTimeInterval = 2*60*1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        imageUri = getImageUri()!!
        contract.launch(imageUri)

        binding.apply {
            batteryChargeValue.text = "${ getbatteryPercentage()}%"
            batteryChargingValue.text = if(getBatteryStatus()) "ON" else "OFF"
            if(viewModel.hasInternetConnection()) connectivityValue.text = "ON"
            else connectivityValue.text = "OFF"
            dateAndTime.text = timestampToDateAndTime(System.currentTimeMillis().toLong())
            tvCaptureCount.text = captureCount.toString()

        }


        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            lifecycleScope.launchWhenStarted {
                val location =viewModel.getCurrentLocation()
                binding.tvLocationValue.text =  location
            }
        }
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))

        //Refreshing Ui Through Button click
//        uploadImageToStorage()

        binding.materialButton2.setOnClickListener {
            val time = binding.tvFreqValue.text.toString().toLong()
            updateTimeInterval = time
        }
        binding.materialButton.setOnClickListener {

            binding.materialButton.setOnClickListener {
                lifecycleScope.launchWhenStarted {
                    val location =viewModel.getCurrentLocation()
                   binding.tvLocationValue.text =  location
                }
                binding.apply {
                    batteryChargeValue.text = "${ getbatteryPercentage()}%"
                    batteryChargingValue.text = if(getBatteryStatus()) "ON" else "OFF"
                    if(viewModel.hasInternetConnection()) connectivityValue.text = "ON"
                    else connectivityValue.text = "OFF"
                    val timeStamp = System.currentTimeMillis()
                    dateAndTime.text = timestampToDateAndTime(timeStamp)
                    tvCaptureCount.text = captureCount.toString()
                }

                imageUri = getImageUri()!!
                contract.launch(imageUri)

            }
        }
    }

    private fun getbatteryPercentage(): Int  {
        val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batteryStatus
    }

    private fun getBatteryStatus():Boolean{
        val batteryStatus: Intent? = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status: Int? = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

        return isCharging
    }

    private fun timestampToDateAndTime(ms:Long) : String{
        val date = Date(ms)
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        return sdf.format(date)

    }

    @JvmName("functionOfKotlin")
    private fun getImageUri():Uri?{
        val image = File(applicationContext.filesDir,"camera_photo.png")
        return FileProvider.getUriForFile(applicationContext,
            "com.sacoding.secquraise.fileProvider",
            image)
    }

    private fun uploadImageToStorage() = CoroutineScope(Dispatchers.IO).launch {
        val filename = UUID.randomUUID().toString()
        try {
            imageUri?.let {
                imageRef.child("images/$filename").putFile(it).addOnSuccessListener { task->
                task.storage.downloadUrl.addOnSuccessListener {
                   val url = it.toString()
                    addDataToDatabase(url,binding.dateAndTime.toString(),
                        getbatteryPercentage(),getBatteryStatus(),binding.tvLocationValue.toString())
                }
            }

            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addDataToDatabase(image:String,date:String,batteryPercent:Int,
        batteryStatus:Boolean,location:String){
        val uuid = UUID.randomUUID().toString()
        val data = DataUi(image,date,location,batteryStatus,batteryPercent)
        val ref = Firebase.database.getReference("dataui/$uuid")

        ref.setValue(data).addOnSuccessListener {
            Toast.makeText(
                this@MainActivity, "Successfully uploaded  data & image",
                Toast.LENGTH_LONG
            ).show()
        }
            .addOnFailureListener {
                Toast.makeText(
                    this@MainActivity, "Failed to  uploaded  data",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onResume() {
        super.onResume()

        startUpdatingTask()

    }
    private fun startUpdatingTask() {
        // Schedule the task to run every 15 minutestime
         val captureTask = object : Runnable {
            override fun run() {
                // Collect location, battery, connectivity details

                lifecycleScope.launchWhenStarted {
                    val location =viewModel.getCurrentLocation()
                    binding.tvLocationValue.text =  location
                }
                binding.apply {
                    batteryChargeValue.text = "${ getbatteryPercentage()}%"
                    batteryChargingValue.text = if(getBatteryStatus()) "ON" else "OFF"
                    if(viewModel.hasInternetConnection()) connectivityValue.text = "ON"
                    else connectivityValue.text = "OFF"
                    val timeStamp = System.currentTimeMillis()
                    dateAndTime.text = timestampToDateAndTime(timeStamp)
                    tvCaptureCount.text = captureCount.toString()
                }
                imageUri = getImageUri()!!
                contract.launch(imageUri)

                //uploadImageToStorage()

                // Schedule the next capturing task after 15 minutes
                handler.postDelayed(this,  updateTimeInterval)
            }
        }
        handler.postDelayed(captureTask, updateTimeInterval)
    }
}