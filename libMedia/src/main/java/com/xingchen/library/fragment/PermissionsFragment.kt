package com.xingchen.library.fragment

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.xingchen.library.MediaActivity.Companion.AUDIO_DESCRIPTION
import com.xingchen.library.MediaActivity.Companion.CAMERA_DESCRIPTION
import com.xingchen.library.MediaActivity.Companion.STORE_DESCRIPTION
import com.xingchen.library.R
import com.xingchen.library.databinding.FragmentPermissionBinding

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 */
class PermissionsFragment : BaseFragment() {
    private lateinit var fragmentPermissionBinding: FragmentPermissionBinding

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    private val appName: String by lazy {
        val pm: PackageManager = requireActivity().packageManager
        requireContext().applicationInfo.loadLabel(pm).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentPermissionBinding = FragmentPermissionBinding.inflate(inflater, container, false)
        return fragmentPermissionBinding.root
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        checkPermission()
    }

    private fun setDescription(title: String, content: String?) {
        if (content.isNullOrEmpty()) {
            fragmentPermissionBinding.root.visibility = View.GONE
        } else {
            fragmentPermissionBinding.root.visibility = View.VISIBLE
            fragmentPermissionBinding.tvTitle.text = "$appName$title"
            fragmentPermissionBinding.tvContent.text = content
        }
    }

    private fun checkPermission() {
        if (!hasPermission(requireContext(), Manifest.permission.CAMERA)) {
            requestCameraLauncher.launch(Manifest.permission.CAMERA)
            val description = arguments?.getString(CAMERA_DESCRIPTION)
            setDescription("申请获取相机权限", description)
        } else if (!hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            requestAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            val description = arguments?.getString(AUDIO_DESCRIPTION)
            setDescription("申请获取麦克风权限", description)
        } else if (!hasPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestStoreLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val description = arguments?.getString(STORE_DESCRIPTION)
            setDescription("申请获取存储权限", description)
        } else {
            navController.navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    private fun showPermission(permission: String) {
        fragmentPermissionBinding.root.visibility = View.GONE
        AlertDialog.Builder(requireContext())
            .setTitle("权限申请")
            .setMessage("在设置-应用-${appName}-权限中开启${permission}，已正常使用拍照、视频等功能")
            .setNegativeButton("取消") { _: DialogInterface?, _: Int ->
                activity?.finish()
            }
            .setPositiveButton("去设置") { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:" + activity?.packageName)
                activity?.startActivity(intent)
                activity?.finish()
            }
            .create()
            .show()
    }

    private val requestCameraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermission("相机权限")
            } else {
                checkPermission()
            }
        }

    private val requestAudioLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermission("麦克风权限")
            } else {
                checkPermission()
            }
        }

    private val requestStoreLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermission("存储权限")
            } else {
                checkPermission()
            }
        }

//    private val activityResultLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val permissionGranted = permissions.entries.all { it.value }
//            if (!permissionGranted) {
//                Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
//            } else {
//                navController.navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
//            }
//        }

    companion object {
        private var PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        /** Convenience method used to check if one permission required by this app are granted */
        fun hasPermission(context: Context, permission: String): Boolean {
            val isGranted = ContextCompat.checkSelfPermission(context, permission)
            return isGranted == PackageManager.PERMISSION_GRANTED
        }
    }
}