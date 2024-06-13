package com.xingchen.library.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.xingchen.library.R
import com.xingchen.library.databinding.FragmentGalleryBinding

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment : BaseFragment() {
    private lateinit var fragmentGalleryBinding: FragmentGalleryBinding
    private val args: GalleryFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentGalleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGalleryBinding.root
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        Glide.with(this).load(args.uri).into(fragmentGalleryBinding.imageView)
    }

    override fun initListener(view: View, savedInstanceState: Bundle?) {
        super.initListener(view, savedInstanceState)
        // Handle back button press
        fragmentGalleryBinding.backButton.setOnClickListener {
            navController.navigateUp()
        }

        // Handle delete button press
        fragmentGalleryBinding.deleteButton.setOnClickListener {
            deleteMediaFile(args.uri)
            navController.navigateUp()
        }

        // Handle confirm button press
        fragmentGalleryBinding.confirmButton.setOnClickListener {
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
}