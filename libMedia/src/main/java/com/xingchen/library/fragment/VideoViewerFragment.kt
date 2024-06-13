package com.xingchen.library.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.xingchen.library.R
import com.xingchen.library.databinding.FragmentVideoBinding


class VideoViewerFragment : BaseFragment() {
    private lateinit var videoViewerBinding: FragmentVideoBinding
    private val args: VideoViewerFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        videoViewerBinding = FragmentVideoBinding.inflate(inflater, container, false)
        return videoViewerBinding.root
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        val fileSize = getFileSizeFromUri(args.uri)
        if (fileSize == null || fileSize <= 0) return
        videoViewerBinding.videoViewer.setVideoURI(args.uri)
        videoViewerBinding.videoViewer.start()
    }

    override fun initListener(view: View, savedInstanceState: Bundle?) {
        super.initListener(view, savedInstanceState)
        // Set loop play
        videoViewerBinding.videoViewer.setOnCompletionListener {
            videoViewerBinding.videoViewer.seekTo(0);
            videoViewerBinding.videoViewer.start();
        }

        // Handle back button press
        videoViewerBinding.backButton.setOnClickListener {
            navController.navigateUp()
        }

        // Handle delete button press
        videoViewerBinding.deleteButton.setOnClickListener {
            deleteMediaFile(args.uri)
            navController.navigateUp()
        }

        // Handle confirm button press
        videoViewerBinding.confirmButton.setOnClickListener {
            activity?.setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("MEDIA_URI", args.uri)
            })
            activity?.finish()
        }
    }

    /**
     * delete file by ContentResolver
     */
    private fun deleteMediaFile(imageUri: Uri) {
        try {
            val contentResolver: ContentResolver? = context?.contentResolver
            contentResolver?.delete(imageUri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * A helper function to retrieve the captured file size.
     */
    private fun getFileSizeFromUri(contentUri: Uri): Long? {
        val cursor: Cursor? = requireContext().contentResolver
            .query(contentUri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        }
    }

    /**
     * A helper function to get the captured file location.
     */
    private fun getAbsolutePathFromUri(contentUri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = requireContext().contentResolver
            .query(contentUri, projection, null, null, null)
        return cursor?.use {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        }
    }
}