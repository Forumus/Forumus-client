package com.hcmus.forumus_client.ui.post.create

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.Place
import com.hcmus.forumus_client.R

class NearbyPlacesAdapter(
    private val places: List<Place>,
    private val onPlaceSelected: (Place) -> Unit
) : RecyclerView.Adapter<NearbyPlacesAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlaceName)
        val tvAddress: TextView = view.findViewById(R.id.tvPlaceAddress)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location_place, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.tvName.text = place.name
        holder.tvAddress.text = place.address

        if (selectedPosition == position) {
            holder.ivCheck.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD")) // Xanh nhạt
        } else {
            holder.ivCheck.visibility = View.GONE
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            onPlaceSelected(place)
        }
    }

    override fun getItemCount() = places.size
}