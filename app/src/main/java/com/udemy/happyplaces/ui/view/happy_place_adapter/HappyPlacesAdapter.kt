package com.udemy.happyplaces.ui.view.happy_place_adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.udemy.happyplaces.R
import com.udemy.happyplaces.data.database.DatabaseHandler
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.ui.view.AddHappyPlaceActivity
import com.udemy.happyplaces.ui.view.MainActivity

class HappyPlacesAdapter(
    private val list: ArrayList<HappyPlaceModel>,
    private val onClick: (HappyPlaceModel) -> Unit,
    private val editItemLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<HappyPlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HappyPlaceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return HappyPlaceViewHolder(inflater.inflate(R.layout.item_happy_place, parent, false))
    }

    override fun onBindViewHolder(holder: HappyPlaceViewHolder, position: Int) {
        val happyPlace = list[position]
        holder.render(happyPlace, onClick)
    }

    fun notifyEditItem(activity: Activity, position: Int) {
        val intent = Intent(activity, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACES_DETAIL, list[position])
        editItemLauncher.launch(intent)
        notifyItemChanged(position)
    }

    fun notifyDeleteItem(activity: Activity,position: Int){
        val dbHandler = DatabaseHandler(activity)
        val result = dbHandler.deleteHappyPlace(list[position])
        if(result>0){
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}