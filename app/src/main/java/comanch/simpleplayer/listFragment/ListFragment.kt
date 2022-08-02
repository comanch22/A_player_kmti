package comanch.simpleplayer.listFragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.lifecycle.LifecycleOwner
import comanch.simpleplayer.R
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.databinding.ListFragmentBinding
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.helpers.NavigationCorrespondent
import comanch.simpleplayer.interfaces.WorkMusicListFragment
import comanch.simpleplayer.musicManagment.FoldersList
import comanch.simpleplayer.musicManagment.MusicList
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.preferences.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ListFragment : Fragment(), WorkMusicListFragment {

    override val viewModel: ListViewModel by viewModels()
    override val stateViewModel: StateViewModel by activityViewModels()
    override val lifecycleOwner: LifecycleOwner by lazy { viewLifecycleOwner }
    override var list: List<MusicTrack>? = null
    override var notifyAdapterPosition: (Int) -> Unit = ::notifyItem

    var adapter: ListItemAdapter? = null

    @Inject
    lateinit var preferences: DefaultPreference

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.window?.statusBarColor =
            resources.getColor(R.color.background, activity?.theme)

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: ListFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.list_fragment, container, false
        )

        binding.list.setHasFixedSize(true)
        binding.listViewModel = viewModel

        viewModel.startLoadFolders()

        adapter = ListItemAdapter(
            OpenFolderListener { item ->
                viewModel.openFolder(item)
            },
            ChooseFolderListener { item ->
                viewModel.onItemClicked(item)
            }
        )

        binding.list.adapter = adapter
        binding.list.itemAnimator = null
        binding.lifecycleOwner = viewLifecycleOwner

        setMusicListControl()

        binding.toolbar.inflateMenu(R.menu.app_menu)
        binding.toolbar.setOnMenuItemClickListener {
            menuNavigation(it)
        }

        binding.checkBox.isChecked = preferences.getBoolean(PreferenceKeys.addToList)

        viewModel.navigateToDetailFragment.observe(viewLifecycleOwner) { it ->
            it.getContentIfNotHandled()?.let {
                navigation.navigateToDestination(
                    this,
                    ListFragmentDirections.actionListFragmentToDetailFragment(it.relativePath)
                )
            }
        }

        viewModel.loadFolders.observe(viewLifecycleOwner) { it ->
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

        stateViewModel.navigationToPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                if (it < 0) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.empty_list),
                        Toast.LENGTH_LONG
                    ).show()
                    return@observe
                }
                navigation.navigateToDestination(
                    this,
                    ListFragmentDirections.actionListFragmentToPlayFragment(
                        NavigationCorrespondent.ListFragment
                    )
                )
            }
        }

        stateViewModel.musicList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let { contentList ->

                val resultList = mutableListOf<MusicTrack>()
                contentList.forEach {
                    if (it.isFolder) {
                        resultList.addAll(
                            MusicList(
                                requireContext(),
                                it.relativePath
                            ).getMusicList()
                        )
                    } else {
                        resultList.add(it)
                    }
                }
                stateViewModel.setCurrentPlaylist(resultList,
                    preferences.getBoolean(PreferenceKeys.addToList))
            }
        }

        binding.arrowBack.setOnClickListener {
            activity?.finish()
        }

        binding.playList.setOnClickListener {
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToPlayListFragment()
            )
        }

        binding.play.setOnClickListener {
            stateViewModel.getMusicList()
        }

        binding.checkBox.setOnCheckedChangeListener{ _, isChecked ->
           preferences.putBoolean(PreferenceKeys.addToList, isChecked)
        }

        return binding.root
    }

    override fun notifyItem(pos: Int) {
        adapter?.notifyItemChanged(pos)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setMusicActive()
    }

    private fun loadFolders() {
        list = context?.let { FoldersList(it).getFolders() }
        adapter?.setData(list)
    }

    private fun menuNavigation(it: MenuItem) = when (it.itemId) {
        R.id.license -> {
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToLicenseFragment()
            )
            true
        }
        R.id.about_app -> {
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToAboutAppFragment()
            )
            true
        }
        R.id.action_settings -> {
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToSettingsFragment()
            )
            true
        }
        R.id.action_images -> {
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToImageFragment()
            )
            true
        }
        else -> false
    }
}



