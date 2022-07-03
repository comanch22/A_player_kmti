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
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import comanch.simpleplayer.interfaces.WorkMusicListFragment
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.R
import comanch.simpleplayer.musicManagment.MusicList
import comanch.simpleplayer.databinding.DetailFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailFragment() : Fragment(), WorkMusicListFragment {

    override val viewModel: DetailViewModel by viewModels()
    override val stateViewModel: StateViewModel by activityViewModels()
    override val lifecycleOwner: LifecycleOwner by lazy { viewLifecycleOwner }
    override var list: List<MusicTrack>? = null
    override var notifyAdapterPosition: (Int) -> Unit = ::notifyItem

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private val args: DetailFragmentArgs by navArgs()
    private var adapter: DetailItemAdapter? = null

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
                viewModel.onItemClicked(itemId)
            })

        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        adapter?.setData(getMusicList())
        binding.list.itemAnimator = null


        setMusicListControl()

        viewModel.toast.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_LONG
                ).show()
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

    override fun notifyItem(pos: Int){
        adapter?.notifyItemChanged(pos)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setMusicActive()
    }

    private fun getMusicList(): List<MusicTrack>? {
        list = context?.let { MusicList(it, args.relativePath).getMusicList() }
        return list
    }
}