package com.udemy.happyplaces.ui.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.databinding.ActivityDetailHappyPlaceBinding

class DetailHappyPlace : AppCompatActivity() {

    private lateinit var binding: ActivityDetailHappyPlaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getExtraForDetail()
    }

    private fun getExtraForDetail() {
        var happyPlace: HappyPlaceModel? = null

        if (intent.hasExtra(MainActivity.EXTRA_PLACES_DETAIL)) {
            happyPlace = intent.getParcelableExtra(MainActivity.EXTRA_PLACES_DETAIL)!!
        }
        if (happyPlace != null) {
            setupActionBar(happyPlace)
        }
    }

    private fun setupActionBar(hp:HappyPlaceModel) {
        setSupportActionBar(binding.detailHappyPlaceToolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = hp.title
        binding.detailHappyPlaceToolBar.setNavigationOnClickListener{
            onBackPressed()
        }

        binding.ivPhoto.setImageURI(Uri.parse(hp.image))
        binding.tvTitle.text = hp.title
        binding.tvDescription.text = hp.description

        binding.btnViewMap.setOnClickListener{
            val intent = Intent(this,MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACES_DETAIL,hp)
            startActivity(intent)
        }
    }
}