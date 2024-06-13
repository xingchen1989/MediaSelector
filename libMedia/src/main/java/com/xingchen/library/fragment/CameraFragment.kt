package com.xingchen.library.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.xingchen.library.R
import com.xingchen.library.databinding.FragmentCameraBinding
import com.xingchen.library.utils.MediaStoreUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : BaseFragment() {
    private lateinit var fragmentCameraBinding: FragmentCameraBinding
    private lateinit var mediaStoreUtils: MediaStoreUtils
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var imageCapture: ImageCapture
    private lateinit var camera: Camera
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var recordEvent: VideoRecordEvent? = null
    private var recording: Recording? = null
    private var initialDistance: Float = 0f
    private var scaleFactor: Float = 1f
    private val maxZoomRatio: Float by lazy {
        camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
    }
    private val mainExecutor by lazy {
        ContextCompat.getMainExecutor(requireContext())
    }
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            imageCapture.targetRotation = view.display.rotation
        } ?: Unit
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        recordEvent = event
        if (event is VideoRecordEvent.Finalize) {
            val uri: Uri = event.outputResults.outputUri
            navController.navigate(CameraFragmentDirections.actionCaptureToVideoViewer(uri))
        }
    }

    /**
     * Make sure that all permissions are still present,
     * since the user could have removed them while the app was in paused state.
     */
    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            navController.navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        // Initialize MediaStoreUtils for fetching this app's images
        mediaStoreUtils = MediaStoreUtils(requireContext())
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
        // Set up the camera and its use cases
        lifecycleScope.launch { bindCameraUseCases() }
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun initListener(view: View, savedInstanceState: Bundle?) {
        // Stop recording when the finger is raised
        fragmentCameraBinding.cameraCaptureButton.onCaptureEnd = {
            recording?.stop()
        }

        // Handle takePhoto event
        fragmentCameraBinding.cameraCaptureButton.onTakePhoto = {
            // Create time stamped name and MediaStore entry.
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis())
            // Create contentValues for insert album
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
            }
            // Create output options object which contains file + metadata
            takePicture(
                ImageCapture.OutputFileOptions.Builder(
                    requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()
            )
        }

        // Handle video capture event
        fragmentCameraBinding.cameraCaptureButton.onCaptureStart = {
            if (recordEvent == null || recordEvent is VideoRecordEvent.Finalize) {
                // create MediaStoreOutputOptions for our recorder: resulting our recording!
                val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_TYPE)
                }
                val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                    requireActivity().contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                    .setContentValues(contentValues)
                    .build()
                // configure Recorder and Start recording to the mediaStoreOutput.
                recording = videoCapture.output
                    .prepareRecording(requireActivity(), mediaStoreOutput)
                    .withAudioEnabled()
                    .start(mainExecutor, captureListener)
            }
        }

        // Setup for button used to switch cameras
        fragmentCameraBinding.cameraSwitchButton.setOnClickListener {
            if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                lensFacing = CameraSelector.LENS_FACING_BACK
            } else {
                lensFacing = CameraSelector.LENS_FACING_FRONT
            }
            // Re-bind use cases to update selected camera
            lifecycleScope.launch { bindCameraUseCases() }
        }

        // Turn on or off the flash mode
        fragmentCameraBinding.flashModelButton.setOnClickListener {
            val flashModelButton = fragmentCameraBinding.flashModelButton
            if (imageCapture.flashMode == ImageCapture.FLASH_MODE_OFF) {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON)
                flashModelButton.setImageResource(R.drawable.ic_flash_on)
            } else {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF)
                flashModelButton.setImageResource(R.drawable.ic_flash_off)
            }
        }

        fragmentCameraBinding.viewFinder.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 将触摸点坐标转换为 Metering 点
                    val meteringFactory = fragmentCameraBinding.viewFinder.meteringPointFactory
                    val meteringPoint = meteringFactory.createPoint(event.x, event.y)
                    // 创建焦点和曝光参数
                    val meteringAction = FocusMeteringAction.Builder(meteringPoint).build()
                    // 执行对焦和曝光操作
                    camera.cameraControl.startFocusAndMetering(meteringAction)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount != 2) return@setOnTouchListener true
                    // 当有两个手指在屏幕上移动时，计算新的缩放级别
                    val currentDistance = calculateDistance(event)
                    // 根据需要调整缩放速度
                    val deltaDistance = (currentDistance - initialDistance) / 200
                    // 计算新的缩放级别并限制缩放级别范围
                    scaleFactor = max(1f, min(scaleFactor + deltaDistance, maxZoomRatio))
                    // 应用缩放级别到 CameraX
                    camera.cameraControl.setZoomRatio(scaleFactor)
                    // 更新初始距离为当前距离
                    initialDistance = currentDistance
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // 第二个手指按下时，记录两个手指之间的距离
                    initialDistance = calculateDistance(event)
                }
            }
            return@setOnTouchListener true
        }
    }

    /** Calculate the distance between the two touch points*/
    private fun calculateDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y))
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private suspend fun bindCameraUseCases() {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()
        // Get screen metrics used to setup camera for full screen resolution
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val resolutionStrategy = ResolutionStrategy(
            Size(displayMetrics.widthPixels, displayMetrics.heightPixels),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(resolutionStrategy).build()
        // Set up the preview use case to display camera preview.
        val preview: Preview = Preview.Builder()
            // Sets the resolution selector.
            // .setResolutionSelector(resolutionSelector)
            .build()
        // Set up the image capture use case to allow users to take photos.
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // Sets the resolution selector.
            .setResolutionSelector(resolutionSelector)
            .build()
        // Set up the video capture use case to allow users to record video.
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .setExecutor(cameraExecutor)
            .build()
        videoCapture = VideoCapture.Builder(recorder)
            .setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)
            .build()
        // Choose the camera by requiring a lens facing
        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing).build()
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()
        // Connect the preview use case to the previewView
        preview.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        // Attach use cases to the camera with the same lifecycle owner
        camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture, videoCapture)
    }

    private fun takePicture(outputFileOptions: ImageCapture.OutputFileOptions) {
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    // insert your code here.
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch {
                        val uri: Uri = outputFileResults.savedUri ?: Uri.parse("")
                        navController.navigate(CameraFragmentDirections.actionCameraToGallery(uri))
                    }
                }
            })
    }

    companion object {
        private const val VIDEO_TYPE = "video/mp4"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}