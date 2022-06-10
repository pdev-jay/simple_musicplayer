package com.example.simple_musicplayer

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simple_musicplayer.databinding.ActivityMainBinding
import com.example.simple_musicplayer.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val ALBUM_IMAGE_SIZE_LARGE = 240
    val ALBUM_IMAGE_SIZE_SMALL = 100

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val bottomSheetBinding: BottomSheetBinding by lazy {
        binding.bottomSheet
    }

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    lateinit var toggle: ActionBarDrawerToggle

    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val REQUEST_READ = 100

    val dbHelper: DBHelper by lazy {
        DBHelper(this)
    }

    lateinit var adapter: MainListAdapter

    //현재 재생되고 있는 음악 리스트, 현재 화면에 보여지는 음악 리스트를 분리
    var playMusicList: MutableList<Music> = mutableListOf()
    var displayMusicList: MutableList<Music> = mutableListOf()

    var typeOfList = Type.ALL

    var isPlaying = false

    //미디어플레이어
    private var mediaPlayer: MediaPlayer? = null

    //음악정보
    var currentMusic: Music? = null

    //Coroutine scope
    private var playerJob: Job? = null



    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Bottom Sheet을 표현할 레이아웃 지정
        sheetBehavior = BottomSheetBehavior.from<ConstraintLayout>(binding.bottomSheet.root)

        //Bottom sheet의 peekHeight때문에 리사이클러뷰가 가려짐
        //화면의 크기를 구하여 리사이클러뷰가 Bottom sheet의 바로 위까지만 나타나게 height 조절
        val insets =
            windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()) //상태바와 네비게이션바의 영역 크기
        val constraintLayoutParams = binding.recyclerView.layoutParams
        val toolbarParams = binding.toolbar.layoutParams
        constraintLayoutParams.height =
            windowManager.currentWindowMetrics.bounds.height() - sheetBehavior.peekHeight - insets.top - insets.bottom - toolbarParams.height

        //Bottom sheet setting
        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheetBinding.bottomSheetSmall.root.alpha = 1 - slideOffset
                if (bottomSheetBinding.bottomSheetSmall.root.alpha == 0f) {
                    bottomSheetBinding.bottomSheetSmall.root.visibility = View.INVISIBLE
                } else {
                    bottomSheetBinding.bottomSheetSmall.root.visibility = View.VISIBLE
                }

                bottomSheetBinding.bottomSheet.alpha = 0 + slideOffset
                if (bottomSheetBinding.bottomSheet.alpha == 0f) {
                    bottomSheetBinding.bottomSheet.visibility = View.INVISIBLE
                } else {
                    bottomSheetBinding.bottomSheet.visibility = View.VISIBLE
                }
            }
        })

        bottomSheetBinding.bottomSheetSmall.root.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        //Toolbar section
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        //파일 저장소 접근 권한 요청 후 actions
        if (isPermitted()) {
            startProcess()
        } else {
            ActivityCompat.requestPermissions(this, permission, REQUEST_READ)
        }

        //Bottom sheet의 seekBar action
        bottomSheetBinding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            //시크바를 터치하고 이동할 때
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {//user가 직접 interaction할 때
                    //progress 만큼 노래 재생시간 이동
                    mediaPlayer?.seekTo(progress)
                    bottomSheetBinding.tvDurationStart.text =
                        SimpleDateFormat("mm:ss").format(progress)
                }
            }

            //시크바를 터치할 때
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            //시크바 터치가 끝났을 때
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

    }

    //권한 요청 결과
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startProcess()
            }
        } else {
            Toast.makeText(this, "Unable to this app without external storage read permission", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        //Bottom sheet이 확장되었을 때 뒤로가기 버튼이 눌렸을 때
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        //navigationView가 확장되었을 때 뒤로가기 버튼이 눌렸을 때
        if (binding.drawerLayout.isOpen){
            binding.drawerLayout.closeDrawers()
            return
        }

        super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menuSongs -> {
                binding.collapssingToolbarLayout.title = "Songs"
                getFilteredMusicList(null, Type.ALL)
            }

            R.id.menuArtist -> {
                binding.collapssingToolbarLayout.title = "Artist"
                val bottomSheetDialog = BottomSheetDialog(Type.ARTIST)
                bottomSheetDialog.show(supportFragmentManager, bottomSheetDialog.TAG)
            }

            R.id.menuGenre -> {
                binding.collapssingToolbarLayout.title = "Genre"
                val bottomSheetDialog = BottomSheetDialog(Type.GENRE)
                bottomSheetDialog.show(supportFragmentManager, bottomSheetDialog.TAG)
            }

            R.id.menuFavorite -> {
                binding.collapssingToolbarLayout.title = "Favorite"
                getFilteredMusicList(null, Type.FAVORITE)
            }
        }

        binding.drawerLayout.closeDrawers()

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchMenu = menu?.findItem(R.id.search_item)
        val searchView = searchMenu?.actionView as SearchView

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            //검색 텍스트 변화시 검색 결과만 화면에 출력
            override fun onQueryTextChange(query: String?): Boolean {
                adapter.filter.filter(query)

                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }


    //외부 파일 읽기 권한 승인 여부
    private fun isPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission[0]
        ) == PackageManager.PERMISSION_GRANTED
    }

    //권한이 승인된 후 DB에서 음악 정보를 가져오기
    private fun startProcess() {
        displayMusicList = dbHelper.selectMusicAll()

        //음악 정보가 없다면 현재 기기의 저장소에서 음악 정보를 가져옴
        if (displayMusicList == null || displayMusicList.size <= 0) {
            displayMusicList = getMusicListFromMobile()
            for (i in 0 until displayMusicList!!.size) {
                val music = displayMusicList!![i]

                //저장소에서 가져온 음악 정보를 DB에 저장
                if (dbHelper.insertMusic(music) == false) {
                    Log.d("Log_debug", "삽입 오류 ${music}")
                }
            }
        }
        Log.d("Log_debug", "${displayMusicList}")

        val layoutManager = LinearLayoutManager(this)

        adapter = MainListAdapter(this, displayMusicList)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun getMusicListFromMobile(): MutableList<Music> {
        //외부 파일의 음악 정보 주소
        val listUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        //음원 정보 컬럼
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,     //음악 아이디
            MediaStore.Audio.Media.TITLE,    //음악 타이틀
            MediaStore.Audio.Media.ARTIST,    //가수
            MediaStore.Audio.Media.ALBUM_ID,    //앨범 이미지
            MediaStore.Audio.Media.GENRE,    //장르
            MediaStore.Audio.Media.DURATION,    //곡 길이
        )

        //contentResolver
        val cursor = contentResolver.query(listUri, proj, null, null, null)
        val musicList = mutableListOf<Music>()

        while (cursor?.moveToNext() == true) {
            val id = cursor.getString(0)
            val title = cursor.getString(1).replace("'", "")
            var artist = cursor.getString(2).replace("'", "")
            var albumId = cursor.getString(3)
            var genre = cursor.getString(4)
            var duration = cursor.getInt(5)

            val music = Music(id, title, artist, albumId, genre, duration, 0)

            musicList.add(music)
        }
        cursor?.close()

        return musicList
    }

    //네비게이션의 메뉴를 눌렀을 때 해당되는 항목을 가져오는 함수
    fun getFilteredMusicList(filter: String?, type: Type) {
        val filteredMusicList = dbHelper.selectFilter(filter, type)

        displayMusicList.clear()
        displayMusicList .addAll(filteredMusicList)

        adapter.initialise()
        adapter.notifyDataSetChanged()

        //즐겨찾기 리스트를 선택했을 때, 현재 음악 목록이 즐겨찾기 리스트인지 표시 -> 리사이클러뷰에서 즐겨찾기를 해제했을 때 현재 보여지는 목록이 즐겨찾기 목록일때만 음악이 사라지게 하기 위함
        typeOfList = type
    }

    fun nowPlaying(music: Music) {
        musicStop()

        //현재 보여지는 목록에서 한 음악을 재생했을 때 재생될 플레이리스트 설정

        playMusicList.clear()

        playMusicList.addAll(displayMusicList)
        currentMusic = playMusicList[playMusicList.indexOf(music)]

        bottomSheetUpdate(music)

        mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())

        if (mediaPlayer?.isPlaying == false){
            musicStart()
        }
    }

    //현재 재생되는 노래에 따른 Bottom sheet ui 업데이트
    fun bottomSheetUpdate(music: Music){
        val bitmapLarge: Bitmap? = music.getAlbumImage(this, ALBUM_IMAGE_SIZE_LARGE)
        val bitmapSmall: Bitmap? = music.getAlbumImage(this, ALBUM_IMAGE_SIZE_SMALL)

        if (bitmapLarge != null && bitmapSmall != null) {
            bottomSheetBinding.ivAlbumImageLarge.setImageBitmap(bitmapLarge)
            bottomSheetBinding.bottomSheetSmall.ivAlbumImageSmall.setImageBitmap(bitmapSmall)
        } else {
            bottomSheetBinding.ivAlbumImageLarge.setImageResource(R.drawable.ic_baseline_music_note_24)
            bottomSheetBinding.bottomSheetSmall.ivAlbumImageSmall.setImageResource(R.drawable.ic_baseline_music_note_24)
        }

        bottomSheetBinding.tvSingerLarge.text = music.artist
        bottomSheetBinding.tvTitleLarge.text = music.title

        bottomSheetBinding.bottomSheetSmall.tvSingerSmall.text = music.artist
        bottomSheetBinding.bottomSheetSmall.tvTitleSmall.text = music.title

        bottomSheetBinding.tvDurationEnd.text = SimpleDateFormat("mm:ss").format(music?.duration)

        bottomSheetBinding.seekBar.max = music?.duration ?: 0
    }

    fun onClick(view: View?) {
        if (currentMusic != null) {
            when (view?.id) {
                R.id.ivPlaySmall, R.id.ivPlayLarge -> {
                    if (mediaPlayer?.isPlaying == true) {
                        musicPause()
                    } else {
                        musicStart()
                    }
                }

                R.id.ivNextBottom -> {
                    musicNext()
                }

                R.id.ivPreviousBottom -> {
                    musicPrevious()
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    fun musicStop() {
        //mediaPlayer.release를 하면 다음 곡, 이전 곡 재생이 안됨
        mediaPlayer?.stop()
        playerJob?.cancel() //coroutine 정지
        mediaPlayer = null
        bottomSheetBinding.seekBar.progress = 0
        bottomSheetBinding.tvDurationStart.text = "00:00"
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    fun musicPause() {
        mediaPlayer?.pause()
        isPlaying = false
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.seekBar.progress = mediaPlayer?.currentPosition!!
    }

    fun musicStart() {
        mediaPlayer?.start()
        isPlaying = true
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_pause_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_pause_24)

        //음악 재생 시간, seekBar ui 업데이트를 위한 Coroutines
        //ui를 바꾸는 작업은 runOnUiThread { } 에서 처리
        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
        playerJob = backgroundScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                //현재 노래 재생 시간을 구하여 seekBar에 적용
                runOnUiThread {
                    var currentPosition = mediaPlayer?.currentPosition!!
                    bottomSheetBinding.seekBar.progress = currentPosition
                    bottomSheetBinding.tvDurationStart.text =
                        SimpleDateFormat("mm:ss").format(currentPosition)
                }

                try {
                    delay(500) //위의 runOnUiThread의 실행을 늦추기 위함
                } catch (e: Exception) {
                    Log.d("Log_debug", "delay error : ${e.printStackTrace()}")
                }
            }
            runOnUiThread {
                if (mediaPlayer!!.currentPosition >= (bottomSheetBinding.seekBar.max - 1000)) {
                    //musicNext()에 bottom sheet의 ui를 바꾸는 작업이 있어 UiThread에 넣어 놓음
                    musicNext()
                }
            }
        }
    }

    fun musicNext(){

        if (playMusicList.isEmpty()){
            Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }

        musicStop()

        var nextMusic: Music?

        //현재 곡이 마지막 곡이 아닐 때
        if (playMusicList.indexOf(currentMusic) != playMusicList.lastIndex){
            nextMusic = playMusicList[playMusicList.indexOf(currentMusic) + 1]
            mediaPlayer = MediaPlayer.create(this, nextMusic.getMusicUri())
            bottomSheetBinding.seekBar.max = nextMusic.duration ?: 0
        } else {
            //현재 곡이 마지막 곡이라면 첫번째 곡의 정보 가져옴
            nextMusic = playMusicList.first()
            mediaPlayer = MediaPlayer.create(this, nextMusic.getMusicUri())
            bottomSheetBinding.seekBar.max = nextMusic.duration ?: 0
        }

        if (nextMusic != null) {
            currentMusic = nextMusic
            bottomSheetUpdate(nextMusic)
        }

        //음악이 재생중일 때 next 버튼을 누르면 다음 곡의 정보를 가져오고 재생
        //음악이 멈춰있을 때 next 버튼을 누르면 다음 곡의 정보는 가져오지만 재생은 하지 않음
        if (isPlaying){
            musicStart()
        }
    }

    fun musicPrevious(){

        if (playMusicList.isEmpty()){
            Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }

        when (mediaPlayer?.currentPosition) {
            //재생 후 5초 이내에 이전 곡 버튼을 누르면 플레이리스트의 이전 곡이 재생
            //5초 이후에 누른다면 현재 재생중인 곡의 처음부터 재생
            in 0..5000 -> {
                musicStop()

                var previousMusic: Music?

                //현재 곡이 플레이리스트의 첫번째 곡이 아닐 때 플레이리스트에서 현재 곡의 이전 곡의 정보 가져옴
                if (playMusicList.indexOf(currentMusic) != 0){
                    previousMusic = playMusicList[playMusicList.indexOf(currentMusic) - 1]
                    mediaPlayer = MediaPlayer.create(this, previousMusic.getMusicUri())
                    bottomSheetBinding.seekBar.max = previousMusic.duration ?: 0
                } else {
                    //현재 곡이 플레이리스트의 첫번째 곡일 때 플레이리스트의 마지막 곡을 재생
                    previousMusic = playMusicList.last()
                    mediaPlayer = MediaPlayer.create(this, previousMusic.getMusicUri())
                    bottomSheetBinding.seekBar.max = previousMusic.duration ?: 0
                }

                if (previousMusic != null) {
                    currentMusic = previousMusic
                    bottomSheetUpdate(previousMusic)
                }
            }

            else -> {
                musicStop()
                mediaPlayer = MediaPlayer.create(this, currentMusic?.getMusicUri())
            }
        }

        //음악이 재생중일 때 previous 버튼을 누르면 이전 곡의 정보를 가져오고 재생(재생 후 5초가 지나지 않았다면)
        //음악이 멈춰있을 때 previous 버튼을 누르면 이전 곡의 정보는 가져오지만 재생은 하지 않음
        if (isPlaying) {
            musicStart()
        }
    }
}