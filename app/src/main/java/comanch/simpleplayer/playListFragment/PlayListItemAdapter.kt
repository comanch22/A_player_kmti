package comanch.simpleplayer.playListFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import comanch.simpleplayer.R
import comanch.simpleplayer.dataBase.PlayList
import comanch.simpleplayer.databinding.PlayListItemBinding


class PlayListItemListener(val clickListener: (item: PlayList) -> Unit) {
     fun onClick(item: PlayList) = clickListener(item)
}

class PlayListItemAdapter(private val clickListener: PlayListItemListener) : ListAdapter<DataItem, RecyclerView.ViewHolder>(
    SleepNightDiffCallback()
) {

    private var mRecyclerView: RecyclerView? = null

    fun setData(list: List<PlayList>?) {

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
                holder.bind(item.playList, clickListener, position)
            }
        }
    }

    class ViewHolder private constructor(private val binding: PlayListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    PlayListItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(item: PlayList, clickListener: PlayListItemListener, position: Int) {

            item.position = position
            binding.item = item
            binding.name.text = item.name
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
        return oldItem.playListId == newItem.playListId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class DataItem {

    abstract val playListId: Long

    data class MusicItem(val playList: PlayList) : DataItem() {
        override val playListId = playList.playListId
    }
}


