package com.xingchen.library.utils

import android.app.Activity
import androidx.fragment.app.Fragment
import com.xingchen.library.MediaActivity

class MediaSelector(
    private var storeDesc: String? = null,
    private var audioDesc: String? = null,
    private var cameraDesc: String? = null,
) {

    fun setStoreDescription(description: String) {
        storeDesc = description
    }

    fun setAudioDescription(description: String) {
        audioDesc = description
    }

    fun setCameraDescription(description: String) {
        cameraDesc = description
    }

    fun start(activity: Activity?, requestCode: Int) {
        val intent = MediaActivity.newIntent(activity, storeDesc, audioDesc, cameraDesc)
        intent?.also { activity?.startActivityForResult(intent, requestCode) }
    }

    fun start(fragment: Fragment?, requestCode: Int) {
        val intent = MediaActivity.newIntent(fragment?.context, storeDesc, audioDesc, cameraDesc)
        intent?.also { fragment?.startActivityForResult(intent, requestCode) }
    }

    companion object {
        const val REQ_MEDIA: Int = 100
        const val MEDIA_URI: String = "MEDIA_URI"
    }
}