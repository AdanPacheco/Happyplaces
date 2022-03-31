package com.udemy.happyplaces.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udemy.happyplaces.R
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.databinding.ActivityMapBinding


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private var mHappyPlace: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(MainActivity.EXTRA_PLACES_DETAIL)) {
            mHappyPlace = intent.getParcelableExtra(MainActivity.EXTRA_PLACES_DETAIL)!!
            if (mHappyPlace != null) {
                setupActionBar(mHappyPlace!!)
            }
        }

        val supportMapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(binding.mMap.id) as SupportMapFragment
        supportMapFragment.getMapAsync(this)
    }

    private fun setupActionBar(hp: HappyPlaceModel) {
        setSupportActionBar(binding.mapHappyPlaceToolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = hp.title
        binding.mapHappyPlaceToolBar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val position = LatLng(mHappyPlace!!.latitude,mHappyPlace!!.longitude)
        map.addMarker(MarkerOptions().position(position).title(mHappyPlace!!.title))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position,15f)
        map.animateCamera(newLatLngZoom)
    }
}