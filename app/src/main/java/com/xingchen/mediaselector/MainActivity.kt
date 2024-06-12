/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingchen.mediaselector

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.xingchen.library.MediaActivity
import com.xingchen.mediaselector.databinding.ActivityMainBinding
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data: Uri? = result.data!!.getParcelableExtra("MEDIA_URI")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.btnStart.setOnClickListener {
            val storeDescription = resources.getString(R.string.store_description)
            val audioDescription = resources.getString(R.string.audio_description)
            val cameraDescription = resources.getString(R.string.camera_description)
            activityResultLauncher.launch(
                MediaActivity.newIntent(this, storeDescription, audioDescription, cameraDescription)
            )
        }
    }
}