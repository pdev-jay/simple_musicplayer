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
        val adapter = if(type == Type.ARTIST){
            BottomArtistRecyclerViewAdapter(context, dbHelper.selectMusicAll())
        } else  {
            BottomGenreRecyclerViewAdapter(context, dbHelper.selectMusicAll())
        }

        binding.bottomRecyclerView.adapter = adapter
        binding.bottomRecyclerView.layoutManager = layoutManager

        return binding.root
    }
}