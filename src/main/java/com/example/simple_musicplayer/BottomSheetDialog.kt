package com.example.simple_musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.simple_musicplayer.databinding.BottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialog(val type: Type): BottomSheetDialogFragment() {

    lateinit var binding: BottomSheetDialogBinding

    val dbHelper: DBHelper by lazy {
        DBHelper(requireContext())
    }

    val TAG = "bottom_sheet_dialog"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetDialogBinding.inflate(inflater, container, false)

        val layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.HORIZONTAL)

        //네비게이션 메뉴에서 누른 아이템이 Artist 혹은 Genre 일 때
//        val adapter = if(type == Type.ARTIST){
//            dbHelper.selectMusicAll()?.let { BottomArtistRecyclerViewAdapter(context, it) }
//        } else  {
//            dbHelper.selectMusicAll()?.let { BottomGenreRecyclerViewAdapter(context, it) }
//        }
        val filteredList = mutableListOf<Music>()

        when (type){
            Type.ARTIST -> {
                filteredList.clear()

                val artistList = dbHelper.selectMusicAll()?.run{
                    this.distinctBy { it.artist }
                }.run{
                    this?.filter { it.artist != "null" }
                }

                if (artistList != null) {
                    filteredList.addAll(artistList)
                }
            }

            Type.GENRE -> {
                filteredList.clear()

                val genreList = dbHelper.selectMusicAll()?.run{
                    this.distinctBy { it.genre }
                }.run{
                    this?.filter { it.genre != "null" }
                }

                if (genreList != null) {
                    filteredList.addAll(genreList)
                }
            }
        }

        binding.bottomRecyclerView.adapter = FilterRecyclerViewAdapter(context, filteredList, type)
        binding.bottomRecyclerView.layoutManager = layoutManager

        return binding.root
    }
}