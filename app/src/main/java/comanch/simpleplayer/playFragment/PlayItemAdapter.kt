package comanch.simpleplayer.playFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import comanch.simpleplayer.R
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.PlayItemBinding


class PlayItemListener(val clickListener: (item: MusicTrack) -> Unit) {
    fun onClick(item: MusicTrack) = clickListener(item)
}

class ButtonPlayListener(val playClickListener: (item: MusicTrack) -> Unit) {
    fun onPlayClick(item: MusicTrack) = playClickListener(item)
}

class PlayItemAdapter(
    private val clickListener: PlayItemListener,
    private val longClickListenerButton: ButtonPlayListener,
    private val backgroundPlayColor: Int
) : ListAdapter<DataItem, RecyclerView.ViewHolder>(
    SleepNightDiffCallback()
) {

    private var mRecyclerView: RecyclerView? = null

    fun setData(list: List<MusicTrack>?) {

        val items = list?.map { DataItem.MusicItem(it) }
        submitList(items)
    }

    fun mGetItemId(position: Int?): Long? {

        if (position == null){
            return null
        }
        return if (position >= 0 && position <= currentList.size.minus(1)) {
            super.getItem(position).musicTrackId
        }else{
            null
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<DataItem>,
        currentList: MutableList<DataItem>
    ) {
        notifyItemRangeChanged(0, itemCount)
        super.onCurrentListChanged(previousList, currentList)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {

        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is ViewHolder -> {
                val item = getItem(position) as DataItem.MusicItem
                holder.bind(
                    item.musicTrack,
                    clickListener,
                    longClickListenerButton,
                    position,
                    backgroundPlayColor
                )
            }
        }
    }

    class ViewHolder private constructor(private val binding: PlayItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    PlayItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(
            item: MusicTrack,
            clickListener: PlayItemListener,
            longClickListenerButton: ButtonPlayListener,
            position: Int,
            backgroundPlayColor: Int
        ) {
            item.position = position
            binding.item = item
            binding.artist.text = item.artist
            binding.title.text = item.title
            binding.duration.text = item.duration
            binding.clickListener = clickListener
            binding.longClickListener = longClickListenerButton

            if (item.active == 1) {
                binding.itemLayout.setBackgroundResource(R.drawable.rectangle_with_stroke)
            } else {
                binding.itemLayout.setBackgroundResource(R.drawable.rectangle_without_stroke)
            }
            if(item.isButtonPlayVisible == 1){
                binding.play.visibility = View.VISIBLE
            }else{
                binding.play.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}

class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.musicTrackId == newItem.musicTrackId || oldItem.musicId == newItem.musicId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.musicId == newItem.musicId
    }
}

sealed class DataItem {

    abstract val musicTrackId: Long
    abstract val musicId: String

    data class MusicItem(val musicTrack: MusicTrack) : DataItem() {
        override val musicTrackId = musicTrack.musicTrackId
        override val musicId = musicTrack.musicId
    }
}


