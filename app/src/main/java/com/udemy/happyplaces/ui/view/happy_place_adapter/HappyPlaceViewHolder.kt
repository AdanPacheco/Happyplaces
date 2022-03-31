package com.udemy.happyplaces.ui.view.happy_place_adapter

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.udemy.happyplaces.data.model.models.HappyPlaceModel
import com.udemy.happyplaces.databinding.ItemHappyPlaceBinding

class HappyPlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = ItemHappyPlaceBinding.bind(view)

    fun render(happyPlace: HappyPlaceModel,onClick:(HappyPlaceModel)->Unit) {
        binding.civImage.setImageURI(Uri.parse(happyPlace.image))
        binding.tvTitle.text = happyPlace.title
        binding.tvDescription.text = happyPlace.description
        itemView.setOnClickListener{
            onClick(happyPlace)
        }
    }
}
