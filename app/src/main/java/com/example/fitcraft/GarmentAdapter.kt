package com.example.fitcraft

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GarmentAdapter(private val groupedGarments: Map<String, List<Garment>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val BASE_URL = "http://192.168.1.4:5000/uploads/" // Adjust to your backend's image path
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_garment, parent, false)
                GarmentViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val garmentType = groupedGarments.keys.elementAt(position)
            holder.bind(garmentType)
        } else if (holder is GarmentViewHolder) {
            val garmentType = groupedGarments.keys.elementAt(position)
            val garmentsForType = groupedGarments[garmentType] ?: emptyList()
            val garment = garmentsForType[position % garmentsForType.size]
            holder.bind(garment)
        }
    }

    override fun getItemCount(): Int {
        return groupedGarments.size * 2 // Including headers for each garment type
    }

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) TYPE_HEADER else TYPE_ITEM
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.tvHeader)

        fun bind(garmentType: String) {
            headerText.text = garmentType.capitalize() // Capitalize "top" or "bottom"
        }
    }

    inner class GarmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val garmentName: TextView = itemView.findViewById(R.id.textGarmentName)
        private val garmentImage: ImageView = itemView.findViewById(R.id.imageGarment)

        fun bind(garment: Garment) {
            garmentName.text = garment.name
            // Combine the base URL with the image filename returned from the backend
            val imageUrl = BASE_URL + garment.image

            // Use Glide to load the image from the full URL
            Glide.with(itemView.context)
                .load(imageUrl)
                .into(garmentImage)
        }
    }
}
