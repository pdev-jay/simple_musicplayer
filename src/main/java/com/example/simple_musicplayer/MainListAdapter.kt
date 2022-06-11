package com.example.simple_musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.simple_musicplayer.databinding.MainItemViewBinding

class MainListAdapter(val context: Context, var musicList: MutableList<Music>):
    RecyclerView.Adapter<MainListAdapter.ViewHolder>(), Filterable {

    val mainActivity: MainActivity = (context as MainActivity)

    val dbHelper: DBHelper by lazy {
        DBHelper(context)
    }

    //recyclerView adapter가 받은 데이터의 원본인 musicList는 검색 결과에 상관 없이 유지
    var filteredMusic = arrayListOf<Music>()
    var itemFilter = ItemFilter()
    //
    init {
        initialise()
    }

    val ALBUM_IMAGE_SIZE = 150

    //musicList가 달라질 때 filteredMusic을 초기화할 수 있는 함수
    fun initialise(){
        filteredMusic.clear()
        filteredMusic.addAll(musicList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListAdapter.ViewHolder {
        val binding = MainItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainListAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        binding.tvRVArtist.text = filteredMusic[holder.adapterPosition].artist
        binding.tvRVTitle.text = filteredMusic[holder.adapterPosition].title

        //곡이 즐겨찾기에 추가되었는지에 따라 아이콘 구분
        when(filteredMusic[holder.adapterPosition].favorite){
            0 -> {
                binding.ivRVFavorite.setImageResource(R.drawable.ic_baseline_star_border_24)
            }

            1 -> {
                binding.ivRVFavorite.setImageResource(R.drawable.ic_baseline_star_24)
            }
        }

        //앨범 커버 이미지
        val bitmap: Bitmap? = filteredMusic[holder.adapterPosition].getAlbumImage(context, ALBUM_IMAGE_SIZE)
        if (bitmap != null){
            holder.binding.ivRVAlbumImage.setImageBitmap(bitmap)
        } else {
            holder.binding.ivRVAlbumImage.setImageResource(R.drawable.ic_baseline_music_note_24)
        }

        //즐겨찾기 아이콘을 눌렀을 때
        binding.ivRVFavorite.setOnClickListener {
            when(filteredMusic[position].favorite){
                //해당 음악이 즐겨찾기 되어있지 않으면
                0 -> {
                    //즐겨찾기에 추가
                    filteredMusic[position].favorite = 1

                    //DB에 해당 음악의 즐겨찾기 상태 업데이트
                    if (dbHelper.updateFavorite(filteredMusic[position])){
                        Toast.makeText(context, "Successfully added to your favorites", Toast.LENGTH_SHORT).show()
                    }
                    notifyItemChanged(position)
                }

                //해당 음악이 즐겨찾기에 등록되어있다면
                1 -> {
                    //즐겨찾기 해제
                    filteredMusic[position].favorite = 0

                    //즐겨찾기 상태 업데이트
                    if (dbHelper.updateFavorite(filteredMusic[position])){
                        Toast.makeText(context, "Removed from your favorites", Toast.LENGTH_SHORT).show()

                        //현재 즐겨찾기 목록을 보고있다면 즐겨찾기 목록에서 사라지게 설정
                        if (mainActivity.typeOfList == Type.FAVORITE){

                            //현재 목록에서 해당 음악 제거 -> nowPlaying에 의해 playMusicList에 적용
                            mainActivity.displayMusicList?.remove(filteredMusic[position])

                            //삭제된 음악의 다음 곡 재생
                            if (mainActivity.currentMusic?.equals(filteredMusic[position]) == true){
                                mainActivity.nowPlaying(filteredMusic[position + 1])
                            }

                            //recyclerView UI 업데이트
                            filteredMusic.remove(filteredMusic[position])
                        }
                    }
                    notifyDataSetChanged()
                }
            }
        }

        //현재 재생 곡에 해당하는 뷰의 백그라운드 컬러 변경
        if (mainActivity.currentMusic?.equals(filteredMusic[position]) == true){
            binding.root.setBackgroundResource(R.drawable.main_item_view_shape_focused)
        } else {
            binding.root.setBackgroundResource(R.drawable.main_item_view_shape)
        }

        //해당 곡을 터치하면 재생
        binding.root.setOnClickListener {
            mainActivity.nowPlaying(filteredMusic[position])
            notifyDataSetChanged()
        }

    }

    override fun getItemCount(): Int {
        return filteredMusic.size
    }

    //Filter를 상속받은 클래스를 위한 getter
    override fun getFilter(): Filter {
        return itemFilter
    }


    inner class ViewHolder(val binding: MainItemViewBinding): RecyclerView.ViewHolder(binding.root)

    //SearchView의 검색 함수와 같이 사용할 수 있는 Filter
    inner class ItemFilter: Filter(){

        //SearchView의 입력값에 따라 결과를 리턴해주는 함수
        override fun performFiltering(charSequence: CharSequence?): FilterResults {
            val filterString = charSequence.toString()
            val results = FilterResults()

            //검색이 필요없을 경우를 위해 원본 배열을 복제
            val filteredList: ArrayList<Music> = ArrayList<Music>()
            //공백제외 아무런 값이 없을 경우 -> 원본 배열
            if (filterString.isBlank()) {
                results.values = musicList
                results.count = musicList.size

                return results
            } else {
                for (music in musicList) {
                    //
                    if (music.title?.contains(filterString) == true || music.artist?.contains(filterString) == true) {
                        filteredList.add(music)
                    }
                }
            }

            results.values = filteredList
            results.count = filteredList.size

            return results
        }

        //검색 결과값을 리턴받아 호출되는 함수
        override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
            filteredMusic.clear()
            filteredMusic.addAll(filterResults.values as ArrayList<Music>)
            notifyDataSetChanged()
        }
    }
}