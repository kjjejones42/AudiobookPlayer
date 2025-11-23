//package com.kjjejones42.audiobookplayer.display
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.ComponentName
//import android.content.Intent
//import android.os.Bundle
//import android.support.v4.media.MediaBrowserCompat
//import android.support.v4.media.session.MediaControllerCompat
//import android.support.v4.media.session.PlaybackStateCompat
//import android.util.TypedValue
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.view.WindowInsets
//import android.widget.SearchView
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.content.res.AppCompatResources
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.work.OneTimeWorkRequest
//import androidx.work.WorkManager
//import com.kjjejones42.audiobookplayer.AudioBook
//import com.kjjejones42.audiobookplayer.R
//import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
//import com.kjjejones42.audiobookplayer.databinding.ActivityDisplayListBinding
//import com.kjjejones42.audiobookplayer.player.MediaPlaybackService
//import com.kjjejones42.audiobookplayer.player.PlayActivity.Companion.INTENT_AUDIOBOOK
//import com.kjjejones42.audiobookplayer.player.PlayActivity.Companion.INTENT_INDEX
//
//class DisplayListActivity : AppCompatActivity() {
//
//    companion object {
//        private val PERMISSIONS = arrayOf(
//            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.READ_MEDIA_AUDIO
//        )
//        const val INTENT_PLAY_FILE: String = "com.kjjejones42.audiobookplayer.PLAY"
//        const val INTENT_START_PLAYBACK: String = "com.kjjejones42.audiobookplayer.start"
//
//        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 3
//    }
//
//    private lateinit var mAdapter: DisplayListAdapter
//    private lateinit var viewbinding: ActivityDisplayListBinding
//    private var searchView: SearchView? = null
//    private var controller: MediaControllerCompat? = null
//    private lateinit var browser: MediaBrowserCompat
//
//    private val activityResultLauncher = registerForActivityResult(RequestMultiplePermissions()) { askUserForDirectory() }
//
//    fun chooseDirectory(item: MenuItem?) {
//        activityResultLauncher.launch(PERMISSIONS)
//    }
//
//    private fun askUserForDirectory() {
////        val message = "Loading. Please wait..."
//        val request = OneTimeWorkRequest.Builder(FileScannerWorker::class.java).build()
////        val dialog = ProgressDialog.show(this, "", message, true)
//        WorkManager.getInstance(this)
//            .enqueue(request)
////            .state
////            .observe(this) { if (it.javaClass != IN_PROGRESS::class.java) dialog.cancel() }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {
//            val allApproved = grantResults.all { it == 1 }
//            val result = if (allApproved) "All" else "Not all"
//            Toast.makeText(this, "$result permissions granted.", Toast.LENGTH_LONG).show()
//            if (allApproved) {
//                askUserForDirectory()
//            }
//        }
//    }
//
//    override fun onBackPressed() {
//        searchView?.takeIf { !it.isIconified }?.apply {
//            setQuery("", false)
//            clearFocus()
//            isIconified = true
//        } ?: super.onBackPressed()
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu, menu)
//        searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView?
//        searchView?.apply {
//            isSubmitButtonEnabled = false
//            setOnCloseListener {
//                mAdapter.filter(null)
//                false
//            }
//            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                override fun onQueryTextSubmit(query: String): Boolean {
//                    mAdapter.filter(query)
//                    return false
//                }
//
//                override fun onQueryTextChange(newText: String): Boolean {
//                    mAdapter.filter(newText)
//                    return false
//                }
//            })
//        }
//        return true
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        setTheme(R.style.AppTheme)
//        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        viewbinding = ActivityDisplayListBinding.inflate(layoutInflater)
//        setContentView(viewbinding.root)
//
//        ViewCompat.setOnApplyWindowInsetsListener(viewbinding.root) { view, insets ->
//            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//
//            // 2. Apply Insets to Child Views
//            // We adjust the view's padding or margins based on the insets.
//
//            // Example: Applying top inset as padding to the root view's content
//            view.setPadding(
//                view.paddingLeft,
//                systemBarsInsets.top, // Apply top inset here
//                view.paddingRight,
//                view.paddingBottom
//            )
//            insets
//        }
//
//        val model = ViewModelProvider(this)[DisplayListViewModel::class.java]
//        val serviceComponent = ComponentName(this, MediaPlaybackService::class.java)
//        browser = MediaBrowserCompat(this, serviceComponent, connectionCallbacks, null)
//
//        viewbinding.recyclerView.setLayoutManager(LinearLayoutManager(this))
//        mAdapter = DisplayListAdapter(model, viewbinding.recyclerView, this)
//        viewbinding.recyclerView.setAdapter(mAdapter)
//
//        getInstance(this).audiobookDao().allAndObserve.observe(this) { model.setBooks(it) }
//        model.savedBooks.observe(this) { this.updateScreen(it) }
//
//        askUserForDirectory()
//    }
//
//    private fun resumeMostRecentBook() {
//        getInstance(this).audiobookDao().mostRecentBook
//            ?.takeIf { it.lastSavedTimestamp > 0 }
//            ?.let {
//                val intent = Intent(this, MediaPlaybackService::class.java)
//                intent.putExtra(INTENT_AUDIOBOOK, it.displayName)
//                intent.putExtra(INTENT_INDEX, it.positionInTrackList)
//                startService(intent)
//            }
//    }
//
//    fun onFloatingActionButtonClick(@Suppress("unused") v: View?) {
//        controller?.apply {
//            if (playbackState == null || playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
//                resumeMostRecentBook()
//            } else {
//                transportControls.pause()
//            }
//        }
//    }
//
//    private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
//        object : MediaBrowserCompat.ConnectionCallback() {
//            override fun onConnected() {
//                super.onConnected()
//                controller = MediaControllerCompat(this@DisplayListActivity, browser.sessionToken)
//                controller!!.registerCallback(object : MediaControllerCompat.Callback() {
//                    override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
//                        val icon = if (state.state == PlaybackStateCompat.STATE_PLAYING) R.drawable.ic_pause else R.drawable.ic_play
//                        viewbinding.fab.setImageDrawable(AppCompatResources.getDrawable(baseContext, icon))
//                        super.onPlaybackStateChanged(state)
//                    }
//                })
//            }
//        }
//
//    @SuppressLint("Range")
//    fun updateScreen(list: List<AudioBook?>?) {
//        val listVisible = list != null && list.isEmpty()
//        viewbinding.recyclerView.visibility = if (listVisible) View.GONE else View.VISIBLE
//        viewbinding.emptyView.visibility = if (listVisible) View.VISIBLE else View.GONE
//    }
//
//
//    override fun onStart() {
//        super.onStart()
//        browser.connect()
//    }
//
//    override fun onStop() {
//        super.onStop()
//        browser.disconnect()
//    }
//}
//
