package comanch.simpleplayer.dialogFragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import comanch.simpleplayer.R
import comanch.simpleplayer.helpers.StringKey
import comanch.simpleplayer.preferences.DefaultPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DialogPlayList : DialogFragment() {

    @Inject
    lateinit var preferences: DefaultPreference

    var style: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = requireActivity().layoutInflater

        val customView: View = inflater.inflate(R.layout.dialog_play_list, null)

        val builder =
            AlertDialog.Builder(ContextThemeWrapper(
                requireContext(),
                R.style.AlertDialogCustom)
            )

        builder
            .setView(customView)
            .setPositiveButton(
                R.string.delete_ok
            ) { _, _ ->
                val result = customView.findViewById<EditText>(R.id.playListName).text
                setFragmentResult(
                    StringKey.dialogPlaylistKey,
                    bundleOf(
                        StringKey.dialogPlaylistKeyExtra to StringKey.ok,
                        StringKey.dialogPlaylistResult to result
                    )
                )
            }
            .setNegativeButton(
                R.string.delete_cancel
            ) { _, _ ->
            }

        return builder.create().apply {
            this.window?.decorView?.setBackgroundResource(R.drawable.rectangle_for_dialog)
        }
    }
}
