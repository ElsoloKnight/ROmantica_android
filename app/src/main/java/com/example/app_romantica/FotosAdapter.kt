package com.example.app_romantica

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONObject

class FotosAdapter(
    private val fotos: MutableList<JSONObject> = mutableListOf(),
    private val onLongClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imagenFoto)
    }

    fun actualizar(nuevas: List<JSONObject>) {
        fotos.clear()
        fotos.addAll(nuevas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = fotos[position]
        val url = "${Api.BASE_URL}${foto.optString("url")}"
        Glide.with(holder.imagen.context).load(url).centerCrop().into(holder.imagen)
        holder.imagen.setOnLongClickListener {
            onLongClick(foto)
            true
        }
    }

    override fun getItemCount(): Int = fotos.size
}
