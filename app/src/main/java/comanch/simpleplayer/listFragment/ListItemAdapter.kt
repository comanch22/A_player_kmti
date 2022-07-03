package comanch.simpleplayer.listFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import comanch.simpleplayer.R
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.ListItemBinding

class ChooseFolderListener(val chooseFolderListener: (item: MusicTrack) -> Unit) {
    fun onClick(item: MusicTrack) = chooseFolderListener(item)
}

class OpenFolderListener(val openFolderListener: (item: MusicTrack) -> Unit) {
    fun onClick(item: MusicTrack) = openFolderListener(item)
}

class ListItemAdapter(
    private val clickListener: OpenFolderListener,
    private val chooseFolderListener: ChooseFolderListener
) : ListAdapter<DataItem, RecyclerView.ViewHolder>(
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is ViewHolder -> {
                val item = getItem(position) as DataItem.MusicItem
                holder.bind(item.musicTrack, clickListener, chooseFolderListener, position)
            }
        }
    }

    class ViewHolder private constructor(private val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    ListItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(
            item: MusicTrack,
            clickListener: OpenFolderListener,
            chooseFolderListener: ChooseFolderListener,
            position: Int
        ) {

            item.position = position
            binding.item = item
            binding.folderName.text = item.relativePathShort
            binding.clickListener = clickListener
            binding.chooseFolderListener = chooseFolderListener

            if (item.active == 1) {
                binding.folderName.setBackgroundResource(R.drawable.rectangle_with_stroke)
            } else {
                binding.folderName.setBackgroundResource(R.drawable.rectangle_without_stroke)
            }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}

class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.relativePathShort == newItem.relativePathShort
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class DataItem {

    abstract val relativePathShort: String

    data class MusicItem(val musicTrack: MusicTrack) : DataItem() {
        override val relativePathShort = musicTrack.relativePathShort
    }
}


