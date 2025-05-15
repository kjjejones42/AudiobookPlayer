package com.kjjejones42.audiobookplayer.display

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.AudioBook.Companion.statusMap
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
import com.kjjejones42.audiobookplayer.display.DisplayListAdapter.MyViewHolder
import com.kjjejones42.audiobookplayer.display.ListItem.AudioBookContainer
import com.kjjejones42.audiobookplayer.display.ListItem.Heading
import com.kjjejones42.audiobookplayer.player.PlayActivity
import java.util.Locale
import java.util.concurrent.TimeUnit

class DisplayListAdapter internal constructor(
    private var model: DisplayListViewModel,
    private var rcv: RecyclerView,
    private val activity: DisplayListActivity
) :
    RecyclerView.Adapter<MyViewHolder>() {
    private var selectedPos = RecyclerView.NO_POSITION
    private val onClickListener = View.OnClickListener { v ->
        val position = rcv.getChildLayoutPosition(v!!)
        val items = checkNotNull(
            model.listItems.value
        )
        if (items[position].headingOrItem == ListItem.TYPE_ITEM) {
            val book = (items[position] as AudioBookContainer).book
            selectedPos = position
            notifyItemChanged(position)
            startAudioBook(book)
        }
    }
    private var currentItems: List<ListItem>? = null
    private val onLongClickListener = OnLongClickListener { v ->
        val items = model.listItems.value
        val statuses = statusMap!!.values.toTypedArray<String>()
        checkNotNull(items)
        val dao = getInstance(v.context).audiobookDao()
        val container = (items[rcv.getChildLayoutPosition(v)] as AudioBookContainer)
        val bookId = container.book.displayName
        AlertDialog.Builder(v.context)
            .setSingleChoiceItems(
                statuses,
                dao.getStatus(bookId)
            ) { dialog, which ->
                val book = dao.findByName(bookId)
                book!!.setStatus(which)
                dao.update(book)
                dialog.dismiss()
            }.setTitle("Choose this book's status.")
            .setNegativeButton(
                "Cancel"
            ) { dialog, _ -> dialog.dismiss() }
            .show()
        false
    }

    init {
        model.listItems.observe(activity) { newItems -> this.selectivelyNotify(newItems) }
        setHasStableIds(true)
    }

    private fun startAudioBook(book: AudioBook) {
//        AudiobookDatabase.getInstance(activity).audiobookDao()
//                .getAllAndObserve().removeObservers(activity);
        val intent = Intent(activity, PlayActivity::class.java)
        intent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, book.displayName)
        intent.putExtra(DisplayListActivity.INTENT_START_PLAYBACK, true)
        activity.startActivity(intent)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun selectivelyNotify(newItems: List<ListItem>) {
        val oldItems = currentItems
        currentItems = newItems
        if (newItems == oldItems) {
            return
        }
        if (oldItems == null) {
            notifyItemRangeInserted(0, newItems.size)
            return
        }
        if (oldItems.size == newItems.size) {
            notifyDataSetChanged()
            return
        }
        val remove = oldItems.size > newItems.size
        val larger = if (remove) oldItems else newItems
        val smaller = if (remove) newItems else oldItems
        val indexes: MutableList<Int> = ArrayList()
        for (i in larger.indices) {
            if (!smaller.contains(larger[i])) {
                indexes.add(i)
            }
        }
        indexes.sortWith { o1, o2 -> o2 - o1 }
        if (remove) {
            for (i in indexes) {
                notifyItemRemoved(i)
            }
        } else {
            for (i in indexes) {
                notifyItemInserted(i)
            }
        }
        rcv.scrollToPosition(0)
    }

    private val items: List<ListItem>
        get() {
            if (currentItems != null) {
                return currentItems as List<ListItem>
            }
            return model.listItems.value ?: emptyList()
        }

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].headingOrItem
    }


    fun filter(filterTerm: String?) {
        val books = model.savedBooks.value
        if (books != null && filterTerm != null) {
            val filtered: MutableList<AudioBook> = ArrayList()
            for (book in books) {
                if (book.toString().uppercase(Locale.getDefault()).contains(
                        filterTerm.uppercase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    filtered.add(book)
                }
            }
            model.setFilteredListItems(filtered)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v: View
        if (viewType == ListItem.TYPE_HEADING) {
            v = LayoutInflater.from(parent.context)
                .inflate(R.layout.display_list_group, parent, false)
            return MyViewHolder(v, false)
        } else {
            v = LayoutInflater.from(parent.context)
                .inflate(R.layout.display_list_item, parent, false)
            v.setOnClickListener(onClickListener)
            v.setOnLongClickListener(onLongClickListener)
            return MyViewHolder(v, true)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        when (items[position].headingOrItem) {
            ListItem.TYPE_ITEM -> {
                val book = (items[position] as AudioBookContainer).book
                holder.duration!!.text = msToReadableDuration(book.totalDuration)
                holder.textView!!.text = book.displayName
                holder.artist!!.text = book.author
                holder.textView!!.isSelected = selectedPos == position
                Thread {
                    val thumbnail = book.getThumbnail(activity)
                    holder.v.post {
                        holder.image!!.setImageBitmap(thumbnail)
                        holder.image!!.visibility = View.VISIBLE
                    }
                }.start()
            }

            ListItem.TYPE_HEADING -> {
                val title = (items[position] as Heading).headingTitle
                holder.textView!!.text = title
            }
        }
    }

    private fun msToReadableDuration(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
        }
        return String.format(Locale.getDefault(), "%02dm", minutes)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class MyViewHolder(val v: View, isItem: Boolean) : RecyclerView.ViewHolder(v) {
        var textView: TextView? = null
        var artist: TextView? = null

        var duration: TextView? = null
        var image: ImageView? = null

        init {
            if (isItem) {
                textView = v.findViewById(R.id.listItemText)
                image = v.findViewById(R.id.listImageView)
                artist = v.findViewById(R.id.artist)
                duration = v.findViewById(R.id.listItemDuration)
                image!!.visibility = View.INVISIBLE
            } else {
                textView = v as TextView
            }
        }
    }
}
