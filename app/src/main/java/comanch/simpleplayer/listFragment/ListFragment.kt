package comanch.simpleplayer.listFragment

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import comanch.simpleplayer.*
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.ListFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ListFragment : Fragment() {

    private val listViewModel: ListViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var adapter: ListItemAdapter? = null
    private var list: List<MusicTrack>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    loadFolders()
                } else {
                    Toast.makeText(context, "no access rights", Toast.LENGTH_LONG).show()
                }
            }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.finish()
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

        val binding: ListFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.list_fragment, container, false
        )

        binding.list.setHasFixedSize(true)
        binding.listViewModel = listViewModel

        listViewModel.startLoadFolders()

        adapter = ListItemAdapter(
            OpenFolderListener { item ->
                listViewModel.openFolder(item)
            },
            ChooseFolderListener { item ->
                listViewModel.click(item)
            }
        )

        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        binding.toolbar.inflateMenu(R.menu.app_menu)
        binding.toolbar.setOnMenuItemClickListener {
            menuNavigation(it)
        }

        listViewModel.navigateToDetailFragment.observe(viewLifecycleOwner) { it ->
            it.getContentIfNotHandled()?.let {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                navigation.navigateToDestination(
                    this,
                    ListFragmentDirections.actionListFragmentToDetailFragment(it.relativePath)
                )
            }
        }

        listViewModel.loadFolders.observe(viewLifecycleOwner) { it ->
            it.getContentIfNotHandled()?.let {
                when {
                    context?.let { it_ ->
                        ContextCompat.checkSelfPermission(
                            it_,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    } == PackageManager.PERMISSION_GRANTED -> {
                        loadFolders()
                    }
                    shouldShowRequestPermissionRationale("READ_EXTERNAL_STORAGE") -> {
                    }
                    else -> {
                        requestPermissionLauncher.launch(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    }
                }
            }
        }

        listViewModel.click.observe(viewLifecycleOwner) { content ->

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

        listViewModel.setFolderActive.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
               stateViewModel.musicListIsEmpty()
            }
        }

        stateViewModel.navigationToPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                navigation.navigateToDestination(
                    this,
                    ListFragmentDirections.actionListFragmentToPlayFragment())
            }
        }

        stateViewModel.musicList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let { contentList ->

                val resultList = mutableListOf<MusicTrack>()
                contentList.forEach {
                    if (it.isFolder){
                        resultList.addAll(MusicList(requireContext(), it.relativePath).getMusicList())
                    }else{
                        resultList.add(it)
                    }
                }
                    stateViewModel.setCurrentPlaylist(resultList)
            }
        }

        binding.arrowBack.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            activity?.finish()
        }

        binding.playList.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToPlayListFragment()
            )
        }

        binding.play.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            stateViewModel.getMusicList()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        listViewModel.setFolderActive()
        soundPoolContainer.setTouchSound()
    }

    private fun loadFolders() {

        list = context?.let { FoldersList(it).getFolders() }
        adapter?.setData(list)
    }

    private fun menuNavigation(it: MenuItem) = when (it.itemId) {
        R.id.about_app -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToAboutAppFragment()
            )
            true
        }
        R.id.action_settings -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToSettingsFragment()
            )
            true
        }
        R.id.action_images -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToImageFragment()
            )
            true
        }
        else -> false
    }
}



