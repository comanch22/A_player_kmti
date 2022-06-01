package comanch.simpleplayer.detailFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import comanch.simpleplayer.*
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.DetailFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val detailViewModel: DetailViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var adapter: DetailItemAdapter? = null
    private var list: List<MusicTrack>? = null
    private val args: DetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val action = DetailFragmentDirections.actionDetailFragmentToListFragment()
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@DetailFragment,
                action
            )
        }
        callback.isEnabled = true

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: DetailFragmentBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.detail_fragment, container, false
            )

        binding.list.setHasFixedSize(true)

        adapter = DetailItemAdapter(
            DetailItemListener { itemId ->
                detailViewModel.onItemClicked(itemId)
            })

        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        adapter?.setData(getMusicList())

        detailViewModel.toast.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        detailViewModel.click.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                it.active = if (it.active == 1) {
                    stateViewModel.removeMusicTrackFromList(it)
                    0
                } else {
                    stateViewModel.addMusicTrack(it)
                    1
                }
                adapter?.notifyItemChanged(it.position)
            }
        }

        stateViewModel.containedInMusicList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {list ->
                list.forEach {
                    it.active = 1
                    adapter?.notifyItemChanged(it.position)
                }
            }
        }

        stateViewModel.musicListIsEmpty.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {isEmpty ->
                if (!isEmpty) {
                    list?.let { it -> stateViewModel.containedInMusicList(it) }
                }
            }
        }

        detailViewModel.setMusicActive.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.musicListIsEmpty()
            }
        }

        binding.arrowBack.setOnClickListener {
            navigation.navigateToDestination(
                this@DetailFragment,
                DetailFragmentDirections.actionDetailFragmentToListFragment()
            )
        }
        return binding.root
    }

    override fun onResume() {

        super.onResume()
        detailViewModel.setMusicActive()
        soundPoolContainer.setTouchSound()
    }

    private fun getMusicList(): List<MusicTrack>? {

        list = context?.let { MusicList(it, args.relativePath).getMusicList() }
        return list
    }
}