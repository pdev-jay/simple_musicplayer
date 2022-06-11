package com.example.simple_musicplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.simple_musicplayer.databinding.BottomItemViewBinding

class FilterRecyclerViewAdapter(val context: Context?, val filteredList: MutableList<Music>, val type: Type): RecyclerView.Adapter<FilterRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilterRecyclerViewAdapter.ViewHolder {
        val binding = BottomItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterRecyclerViewAdapter.ViewHolder, position: Int) {
        val binding = holder.binding


        when(type){
            Type.ARTIST -> {
                binding.textView.text = filteredList[position].artist
            }

            Type.GENRE -> {
                binding.textView.text = filteredList[position].genre
            }
        }


        binding.root.setOnClickListener {
            when(type){
                Type.ARTIST -> {
                    filteredList[position].artist?.let {
                        (context as MainActivity).getFilteredMusicList(it, Type.ARTIST)
                    }
                    (context as MainActivity).binding.collapssingToolbarLayout.title = "Artist"
                }

                Type.GENRE -> {
                    filteredList[position].genre?.let {
                        (context as MainActivity).getFilteredMusicList(it, Type.GENRE)
                    }
                    (context as MainActivity).binding.collapssingToolbarLayout.title = "Genre"
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    inner class ViewHolder(val binding: BottomItemViewBinding): RecyclerView.ViewHolder(binding.root)
}