package com.xingchen.library

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.xingchen.library.databinding.ActivityMediaBinding

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MediaActivity : AppCompatActivity() {
    private lateinit var activityMediaBinding: ActivityMediaBinding

    /** Host's navigation controller
     * Warning: When creating a NavHostFragment with FragmentContainerView or manually adding a NavHostFragment to an activity using FragmentTransaction,
     * you may encounter issues.If you do this, it may cause Navigation.
     * findNavController(Activity, @IdRes int) to fail when attempting to retrieve the NavController in onCreate().
     * You should instead retrieve the NavController directly from the NavHostFragment.*/
    private val navController: NavController by lazy {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        (navHostFragment as NavHostFragment).navController
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fix the screen orientation for this sample to focus on cameraX API
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activityMediaBinding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(activityMediaBinding.root)
        initView(savedInstanceState)
    }

    private fun initView(savedInstanceState: Bundle?) {
        // 将nav_graph.xml设置为NavController的导航图
        navController.setGraph(R.navigation.nav_graph, intent?.extras)
        // hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, activityMediaBinding.root).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        const val STORE_DESCRIPTION = "store_description"
        const val AUDIO_DESCRIPTION = "audio_description"
        const val CAMERA_DESCRIPTION = "camera_description"

        @JvmStatic
        fun newIntent(context: Context?, store: String?, audio: String?, camera: String?): Intent? {
            if (context == null) return null
            return Intent(context, MediaActivity::class.java).apply {
                putExtra(STORE_DESCRIPTION, store ?: "")
                putExtra(AUDIO_DESCRIPTION, audio ?: "")
                putExtra(CAMERA_DESCRIPTION, camera ?: "")
            }
        }
    }
}