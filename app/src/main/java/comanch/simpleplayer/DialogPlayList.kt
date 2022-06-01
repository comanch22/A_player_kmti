package comanch.simpleplayer

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DialogPlayList() : DialogFragment() {

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = requireActivity().layoutInflater

        val customView: View = inflater.inflate(R.layout.dialog_play_list, null)

        val builder =
            AlertDialog.Builder(ContextThemeWrapper(requireContext(), getStyle()))

        builder
            .setView(customView)
            .setPositiveButton(R.string.delete_ok
            ) { _, _ ->
                val result = customView.findViewById<EditText>(R.id.playListName).text
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                setFragmentResult(
                    StringKey.dialogPlaylistKey,
                    bundleOf(StringKey.dialogPlaylistKeyExtra to StringKey.ok,
                        StringKey.dialogPlaylistResult to result)
                )
            }
            .setNegativeButton(R.string.delete_cancel
            ) { _, _ ->
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            }

        return builder.create().apply {

            val colorBackground = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryVariant, colorBackground, true)

            val colorText = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, colorText, true)

            setOnShowListener {

                this.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    this.setTextColor(colorText.data)
                    this.isSoundEffectsEnabled = false
                }
                this.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                    this.setTextColor(colorText.data)
                    this.isSoundEffectsEnabled = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    private fun getStyle(): Int{
        return R.style.AlertDialogCustom
    }
}
