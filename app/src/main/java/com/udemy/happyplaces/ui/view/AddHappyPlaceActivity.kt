package com.udemy.happyplaces.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.udemy.happyplaces.R
import com.udemy.happyplaces.core.DatePickerFragment
import com.udemy.happyplaces.core.GetAddresFromLatLng
import com.udemy.happyplaces.data.database.DatabaseHandler
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.databinding.ActivityAddHappyPlaceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }

    private lateinit var binding: ActivityAddHappyPlaceBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val openGalleryLauncher: ActivityResultLauncher<Intent> = openGalleryResultLauncher()
    private val openCameraLauncher: ActivityResultLauncher<Intent> = openCameraResultLauncher()
    private val openMapsLauncher: ActivityResultLauncher<Intent> = openPlacesResultLauncher()

    private var imageUriToSave: Uri? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var happyPlaceDetail: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActionBar()
        initListeners()
        initPlaces()
        checkForExtras()

    }

    private fun initPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(this, resources.getString(R.string.apiPlacesKey))
        }
    }

    private fun checkForExtras() {
        if (intent.hasExtra(MainActivity.EXTRA_PLACES_DETAIL)) {
            happyPlaceDetail = intent.getParcelableExtra(MainActivity.EXTRA_PLACES_DETAIL)
            if (happyPlaceDetail != null) {
                supportActionBar?.title = "EDIT HAPPY PLACE"
                binding.ieTitle.setText(happyPlaceDetail!!.title)
                binding.ieDescription.setText(happyPlaceDetail!!.description)
                binding.ieDate.setText(happyPlaceDetail!!.date)
                binding.ieLocation.setText(happyPlaceDetail!!.location)
                latitude = happyPlaceDetail!!.latitude
                longitude = happyPlaceDetail!!.longitude

                imageUriToSave = Uri.parse(happyPlaceDetail!!.image)
                binding.ivPhoto.setImageURI(imageUriToSave)
                binding.btnSave.text = "UPDATE"
            }
        }
    }

    private fun initListeners() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding.ieDate.setOnClickListener { showDatePickerDialog() }
        binding.tvAddImage.setOnClickListener { showPictureAlertDialog() }
        binding.btnSave.setOnClickListener { checkForEmptyFields() }
        binding.ieLocation.setOnClickListener { showPlacesScreen() }
        binding.btnCurrentLocation.setOnClickListener { getCurrentLocation() }
    }

    private fun getCurrentLocation() {
        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multipleReport: MultiplePermissionsReport?) {
                    if (multipleReport!!.areAllPermissionsGranted()) {
                        requestNewLocationData()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    showRationalDialogFromPermissions()
                }
            }).onSameThread().check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            numUpdates = 1
        }


        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation: Location = locationResult.lastLocation
                latitude = lastLocation.latitude
                longitude = lastLocation.longitude
                val addressTask = GetAddresFromLatLng(this@AddHappyPlaceActivity, latitude, longitude)
                addressTask.setCustomAddressListener(object : GetAddresFromLatLng.AddressListener {
                    override fun onAddressFound(address: String) {
                        binding.ieLocation.setText(address)
                    }

                    override fun onError() {
                        Log.e("Get address:: ", "onError: Something went wrong")
                    }
                })

                lifecycleScope.launch(Dispatchers.IO) {
                    addressTask.launchBackgroundProcessForRequest()
                }

            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)
    }


    private fun openPlacesResultLauncher() = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_CANCELED) {
            val place: Place = Autocomplete.getPlaceFromIntent(result.data!!)
            binding.ieLocation.setText(place.address)
            latitude = place.latLng!!.latitude
            longitude = place.latLng!!.longitude
        }

    }

    private fun showPlacesScreen() {
        try {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
            openMapsLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkForEmptyFields() {
        when {
            binding.ieTitle.text.isNullOrEmpty() -> binding.ilTitle.error = "Please enter title"
            binding.ieDescription.text.isNullOrEmpty() -> binding.ilDescription.error = "Please enter description"
            binding.ieDate.text.isNullOrEmpty() -> binding.ilDate.error = "Please enter date"
            binding.ieLocation.text.isNullOrEmpty() -> binding.ilLocation.error = "Please enter location"
            imageUriToSave == null -> Toast.makeText(this, "Please select an Image", Toast.LENGTH_SHORT).show()
            else -> addHappyPlaceToDatabase()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun addHappyPlaceToDatabase() {
        val hp = HappyPlaceModel(
            if (happyPlaceDetail == null) 0 else happyPlaceDetail!!.id,
            binding.ieTitle.text.toString(),
            binding.ieDescription.text.toString(),
            binding.ieDate.text.toString(),
            binding.ieLocation.text.toString(),
            latitude,
            longitude,
            imageUriToSave.toString()
        )

        val dbHandler = DatabaseHandler(this)

        val result = if (happyPlaceDetail == null) {
            dbHandler.addHappyPlace(hp)
        } else {
            dbHandler.updateHappyPlace(hp).toLong()
        }

        if (result > 0) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { year, month, day -> onDateSelected(year, month, day) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun showPictureAlertDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val dialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(dialogItems) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> capturePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun openCameraResultLauncher() = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val thumbnail: Bitmap = result.data?.extras?.get("data") as Bitmap
            imageUriToSave = saveImageOnDevice(thumbnail)
            binding.ivPhoto.setImageBitmap(thumbnail)
        }
    }

    private fun capturePhotoFromCamera() {
        Dexter.withContext(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(multipleReport: MultiplePermissionsReport?) {
                if (multipleReport!!.areAllPermissionsGranted()) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    openCameraLauncher.launch(cameraIntent)
                }
            }

            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                showRationalDialogFromPermissions()
            }


        }).onSameThread().check()
    }

    private fun openGalleryResultLauncher() = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            var thumbnail: Bitmap? = null
            if (Build.VERSION.SDK_INT >= 29) {
                val source = result.data?.data?.let { ImageDecoder.createSource(this.contentResolver, it) }
                try {
                    thumbnail = source?.let { ImageDecoder.decodeBitmap(it) }!!
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                thumbnail = MediaStore.Images.Media.getBitmap(this.contentResolver, result.data?.data)
            }
            imageUriToSave = saveImageOnDevice(thumbnail!!)
            binding.ivPhoto.setImageBitmap(thumbnail)
        }
    }

    private fun choosePhotoFromGallery() {
        Dexter.withContext(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(multipleReport: MultiplePermissionsReport?) {
                if (multipleReport!!.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(galleryIntent)
                }
            }

            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                showRationalDialogFromPermissions()
            }


        }).onSameThread().check()
    }


    private fun showRationalDialogFromPermissions() {
        val alert = AlertDialog.Builder(this)
        alert.setMessage(
            "It looks like you have turned off permissions required for this feature. It can be enabled under the application settings"
        )
        alert.setPositiveButton("GO TO SETTINGS") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
        alert.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        alert.show()
    }

    private fun saveImageOnDevice(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun onDateSelected(year: Int, month: Int, day: Int) {
        val date = "$day/${month + 1}/$year"
        binding.ieDate.setText(date)
    }

    private fun initActionBar() {
        setSupportActionBar(binding.addHappyPlaceToolBar)
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding.addHappyPlaceToolBar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}