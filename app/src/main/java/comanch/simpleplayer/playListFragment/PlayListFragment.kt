package comanch.simpleplayer.playListFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.R
import comanch.simpleplayer.dataBase.PlayList
import comanch.simpleplayer.databinding.PlayListFragmentBinding
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.helpers.NavigationCorrespondent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayListFragment : Fragment() {

    private val playlistViewModel: PlayListViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var adapter: PlayListItemAdapter? = null
    private var playList: List<PlayList>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = PlayListFragmentDirections.actionPlayListFragmentToListFragment()
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@PlayListFragment,
                action
            )
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: PlayListFragmentBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.play_list_fragment, container, false
            )

        binding.list.setHasFixedSize(true)

        adapter = PlayListItemAdapter(
            PlayListItemListener { item ->
                playlistViewModel.onItemClicked(item)
            })

        binding.list.adapter = adapter
        binding.list.itemAnimator = null
        binding.lifecycleOwner = viewLifecycleOwner

        playlistViewModel.items.observe(viewLifecycleOwner) { list ->
            list?.let {
                playList = it
                adapter?.setData(it)
            }
        }

        playlistViewModel.click.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                it.active = if (it.active == 1) {
                    playlistViewModel.removePlayListFromList(it)
                    0
                } else {
                    playlistViewModel.addPlayList(it)
                    1
                }
                adapter?.notifyItemChanged(it.position)
            }
        }

        stateViewModel.navigationToPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                navigation.navigateToDestination(
                    this@PlayListFragment,
                    PlayListFragmentDirections.actionPlayListFragmentToPlayFragment(
                        NavigationCorrespondent.PlayListFragment
                    )
                )
            }
        }

        playlistViewModel.playList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.setPlaylistAndPlay(it.toList())
            }
        }

        playlistViewModel.playListDel.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.deletePlayList(it.toList())
            }
        }

        binding.arrowBack.setOnClickListener {
            navigation.navigateToDestination(
                this@PlayListFragment,
                PlayListFragmentDirections.actionPlayListFragmentToListFragment()
            )
        }

        binding.delete.setOnClickListener {
            playlistViewModel.delete()
        }

        binding.play.setOnClickListener {
            playlistViewModel.getPlaylists()
        }
        return binding.root
    }
}