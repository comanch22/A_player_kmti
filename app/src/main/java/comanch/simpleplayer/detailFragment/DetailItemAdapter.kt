package comanch.simpleplayer.detailFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import comanch.simpleplayer.R
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.DetailItemBinding


class DetailItemListener(val clickListener: (item: MusicTrack) -> Unit) {
     fun onClick(item: MusicTrack) = clickListener(item)
}

class DetailItemAdapter(private val clickListener: DetailItemListener) : ListAdapter<DataItem, RecyclerView.ViewHolder>(
    SleepNightDiffCallback()
) {

    private var mRecyclerView: RecyclerView? = null

    fun setData(list: List<MusicTrack>?) {

        val items = list?.map { DataItem.MusicItem(it) }
        submitList(items)
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
                holder.bind(item.musicTrack, clickListener, position)
            }
        }
    }

    class ViewHolder private constructor(private val binding: DetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    DetailItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(item: MusicTrack, clickListener: DetailItemListener, position: Int) {

            item.position = position
            binding.item = item
            binding.artist.text = item.artist
            binding.title.text = item.title
            binding.duration.text = item.duration
            binding.clickListener = clickListener

            if (item.active == 1){
                binding.itemLayout.setBackgroundResource(R.drawable.rectangle_with_stroke)
            }else{
                binding.itemLayout.setBackgroundResource(R.drawable.rectangle_for_folder_name)
            }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}

class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.musicTrackId == newItem.musicTrackId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class DataItem {

    abstract val musicTrackId: Long

    data class MusicItem(val musicTrack: MusicTrack) : DataItem() {
        override val musicTrackId = musicTrack.musicTrackId
    }
}


