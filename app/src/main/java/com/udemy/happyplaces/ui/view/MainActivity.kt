package com.udemy.happyplaces.ui.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udemy.happyplaces.data.database.DatabaseHandler
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.databinding.ActivityMainBinding
import com.udemy.happyplaces.ui.view.happy_place_adapter.HappyPlacesAdapter
import pl.kitek.rvswipetodelete.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLACES_DETAIL = "extra_places_detail"
    }

    private lateinit var binding: ActivityMainBinding
    private val addHappyPlaceForResultLauncher: ActivityResultLauncher<Intent> = addHappyPlaceResult()
    private val editItemResultLauncher: ActivityResultLauncher<Intent> = addHappyPlaceResult()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fabAddHappyPlaces.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            addHappyPlaceForResultLauncher.launch(intent)
        }
        getHappyPlacesFromDB()
    }

    private fun addHappyPlaceResult() = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            getHappyPlacesFromDB()
        }
    }

    private fun setupRecyclerViewHappyPlaces(happyPlacesList: ArrayList<HappyPlaceModel>) {
        binding.rvHappyPlacesList.layoutManager = LinearLayoutManager(this)
        binding.rvHappyPlacesList.setHasFixedSize(true)
        val happyPlacesAdapter =
            HappyPlacesAdapter(happyPlacesList, onClick = { HP -> onItemSelected(HP) }, editItemLauncher = editItemResultLauncher)
        binding.rvHappyPlacesList.adapter = happyPlacesAdapter

        val swipeRightHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition)
            }
        }

        val swipeToDeleteHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.notifyDeleteItem(this@MainActivity, viewHolder.adapterPosition)
            }
        }

        val editItemTouchHelper = ItemTouchHelper(swipeRightHandler)
        editItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)

        val deleteItemTouchHelper = ItemTouchHelper(swipeToDeleteHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)
    }

    private fun onItemSelected(hp: HappyPlaceModel) {
        val intent = Intent(this, DetailHappyPlace::class.java)
        intent.putExtra(EXTRA_PLACES_DETAIL, hp)
        startActivity(intent)
    }

    private fun getHappyPlacesFromDB() {
        val dbHandler = DatabaseHandler(this)
        val listHp = dbHandler.getHappyPlacesList()

        if (listHp.size > 0) {
            binding.rvHappyPlacesList.visibility = View.VISIBLE
            binding.tvMessage.visibility = View.GONE
            setupRecyclerViewHappyPlaces(listHp)
        }
    }

}