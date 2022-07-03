package comanch.simpleplayer.interfaces

import androidx.lifecycle.LifecycleOwner
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.dataBase.MusicTrack

interface WorkMusicListFragment {

    val viewModel: WorkMusicListViewModel
    val stateViewModel: StateViewModel
    val lifecycleOwner: LifecycleOwner

    var notifyAdapterPosition: (i: Int) -> Unit
    var list: List<MusicTrack>?

    fun setMusicListControl(){

        viewModel.click.observe(lifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                it.active = if (it.active == 1) {
                    stateViewModel.removeMusicTrackFromList(it)
                    0
                } else {
                    stateViewModel.addMusicTrack(it)
                    1
                }
                notifyAdapterPosition(it.position)
            }
        }

        stateViewModel.containedInMusicList.observe(lifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {list ->
                list.forEach {
                    it.active = 1
                    notifyAdapterPosition(it.position)
                }
            }
        }

        stateViewModel.musicListIsEmpty.observe(lifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {isEmpty ->
                if (!isEmpty) {
                    list?.let { it -> stateViewModel.containedInMusicList(it) }
                }
            }
        }

        viewModel.setMusicActive.observe(lifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.musicListIsEmpty()
            }
        }
    }

    fun notifyItem(pos: Int)
}