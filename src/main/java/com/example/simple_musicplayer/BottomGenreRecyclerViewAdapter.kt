package com.example.simple_musicplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer_220603.databinding.BottomItemViewBinding

class BottomGenreRecyclerViewAdapter(val context: Context?, val musicList: MutableList<Music>): RecyclerView.Adapter<BottomGenreRecyclerViewAdapter.ViewHolder>() {
    val genreFilteredList = musicList.run {
        this.distinctBy { it.genre }
    }.run{
        this.filter { it.genre != "null" }//음악 파일에 장르 데이터가 없을 때 null로 가져옴. 후에 DB에서 가져올 때 getString으로 가져오기때문에 null도 String타입인 "null"로 가져온다.
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BottomGenreRecyclerViewAdapter.ViewHolder {
        val binding = BottomItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomGenreRecyclerViewAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        binding.textView.text = genreFilteredList[position].genre
        binding.root.setOnClickListener {
            genreFilteredList[position].genre?.let {
                (context as MainActivity).getFilteredMusicList(it, Type.GENRE)
            }
        }
    }

    override fun getItemCount(): Int {
        return genreFilteredList.size
    }

    inner class ViewHolder(val binding: BottomItemViewBinding): RecyclerView.ViewHolder(binding.root)
}