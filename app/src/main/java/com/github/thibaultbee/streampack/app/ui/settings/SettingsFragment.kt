/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.app.ui.settings

import android.media.MediaFormat
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.preference.*
import com.github.thibaultbee.streampack.app.R
import com.github.thibaultbee.streampack.app.models.EndpointFactory
import com.github.thibaultbee.streampack.app.models.EndpointType
import com.github.thibaultbee.streampack.app.models.FileExtension
import com.github.thibaultbee.streampack.app.utils.DialogUtils
import com.github.thibaultbee.streampack.utils.CameraStreamerConfigurationHelper
import com.github.thibaultbee.streampack.utils.getCameraList
import com.github.thibaultbee.streampack.utils.isFrameRateSupported
import java.io.IOException

class SettingsFragment : PreferenceFragmentCompat() {
    private val videoEnablePreference: SwitchPreference by lazy {
        this.findPreference(getString(R.string.video_enable_key))!!
    }

    private val videoSettingsCategory: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.video_settings_key))!!
    }

    private val videoEncoderListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.video_encoder_key))!!
    }

    private val videoResolutionListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.video_resolution_key))!!
    }

    private val videoFpsListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.video_fps_key))!!
    }

    private val videoBitrateSeekBar: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.video_bitrate_key))!!
    }

    private val audioEnablePreference: SwitchPreference by lazy {
        this.findPreference(getString(R.string.audio_enable_key))!!
    }

    private val audioSettingsCategory: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.audio_settings_key))!!
    }

    private val audioEncoderListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_encoder_key))!!
    }

    private val audioNumberOfChannelListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_number_of_channels_key))!!
    }

    private val audioBitrateListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_bitrate_key))!!
    }

    private val audioSampleRateListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_sample_rate_key))!!
    }

    private val endpointTypePreference: ListPreference by lazy {
        this.findPreference(getString(R.string.endpoint_type_key))!!
    }

    private val tsMuxerPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.ts_muxer_key))!!
    }

    private val rtmpEndpointPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.rtmp_server_key))!!
    }

    private val srtEndpointPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.srt_server_key))!!
    }

    private val fileEndpointPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.ts_endpoint_file_key))!!
    }

    private val serverIpPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.srt_server_ip_key))!!
    }

    private val serverPortPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.srt_server_port_key))!!
    }

    private val serverEnableBitrateRegulationPreference: SwitchPreference by lazy {
        this.findPreference(getString(R.string.server_enable_bitrate_regulation_key))!!
    }

    private val serverTargetVideoBitratePreference: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.server_video_target_bitrate_key))!!
    }

    private val serverMinVideoBitratePreference: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.server_video_min_bitrate_key))!!
    }

    private val fileNamePreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.file_name_key))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    private fun loadVideoSettings() {
        // Inflates video encoders
        val supportedVideoEncoderName =
            mapOf(
                MediaFormat.MIMETYPE_VIDEO_AVC to getString(R.string.video_encoder_h264),
                MediaFormat.MIMETYPE_VIDEO_HEVC to getString(R.string.video_encoder_h265)
            )

        val supportedVideoEncoder = CameraStreamerConfigurationHelper.Video.supportedEncoders
        videoEncoderListPreference.setDefaultValue(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoderListPreference.entryValues = supportedVideoEncoder.toTypedArray()
        videoEncoderListPreference.entries =
            supportedVideoEncoder.map { supportedVideoEncoderName[it] }.toTypedArray()

        loadVideoSettings(videoEncoderListPreference.value)
    }

    private fun loadVideoSettings(encoder: String) {
        videoSettingsCategory.isVisible = videoEnablePreference.isChecked
        videoEnablePreference.setOnPreferenceChangeListener { _, newValue ->
            videoSettingsCategory.isVisible = newValue as Boolean
            true
        }

        // Inflates video resolutions
        CameraStreamerConfigurationHelper.Video.getSupportedResolutions(
            requireContext(),
            encoder
        ).map { it.toString() }.toTypedArray().run {
            videoResolutionListPreference.entries = this
            videoResolutionListPreference.entryValues = this
        }

        // Inflates video fps
        val supportedFramerates = CameraStreamerConfigurationHelper.Video.getSupportedFramerates(
            requireContext(),
            encoder,
            "0"
        )
        videoFpsListPreference.entryValues.filter { fps ->
            supportedFramerates.any { it.contains(fps.toString().toInt()) }
        }.toTypedArray().run {
            videoFpsListPreference.entries = this
            videoFpsListPreference.entryValues = this
        }
        videoFpsListPreference.setOnPreferenceChangeListener { _, newValue ->
            val fps = (newValue as String).toInt()
            val unsupportedCameras = requireContext().getCameraList().filter {
                !requireContext().isFrameRateSupported(it, fps)
            }
            if (unsupportedCameras.isNotEmpty()) {
                DialogUtils.showAlertDialog(
                    requireContext(),
                    getString(R.string.warning),
                    resources.getQuantityString(
                        R.plurals.camera_frame_rate_not_supported,
                        unsupportedCameras.size,
                        unsupportedCameras.joinToString(", "),
                        fps
                    )
                )
            }
            true
        }

        // Inflates video bitrate
        CameraStreamerConfigurationHelper.Video.getSupportedBitrates(encoder).run {
            videoBitrateSeekBar.min = maxOf(videoBitrateSeekBar.min, lower / 1000) // to kb/s
            videoBitrateSeekBar.max = minOf(videoBitrateSeekBar.max, upper / 1000) // to kb/s
        }
    }

    private fun loadAudioSettings() {
        // Inflates audio encoders
        val supportedAudioEncoderName =
            mapOf(MediaFormat.MIMETYPE_AUDIO_AAC to getString(R.string.audio_encoder_aac))

        val supportedAudioEncoder = CameraStreamerConfigurationHelper.Audio.supportedEncoders
        audioEncoderListPreference.setDefaultValue(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoderListPreference.entryValues = supportedAudioEncoder.toTypedArray()
        audioEncoderListPreference.entries =
            supportedAudioEncoder.map { supportedAudioEncoderName[it] }.toTypedArray()

        loadAudioSettings(audioEncoderListPreference.value)
    }

    private fun loadAudioSettings(encoder: String) {
        audioSettingsCategory.isVisible = audioEnablePreference.isChecked
        audioEnablePreference.setOnPreferenceChangeListener { _, newValue ->
            audioSettingsCategory.isVisible = newValue as Boolean
            true
        }

        // Inflates audio number of channel
        val inputChannelRange =
            CameraStreamerConfigurationHelper.Audio.getSupportedInputChannelRange(encoder)
        audioNumberOfChannelListPreference.entryValues.filter {
            inputChannelRange.contains(it.toString().toInt())
        }.toTypedArray().run {
            audioNumberOfChannelListPreference.entries = this
            audioNumberOfChannelListPreference.entryValues = this
        }

        // Inflates audio bitrate
        val bitrateRange = CameraStreamerConfigurationHelper.Audio.getSupportedBitrates(encoder)
        audioBitrateListPreference.entryValues.filter {
            bitrateRange.contains(
                it.toString().toInt()
            )
        }.toTypedArray().run {
            audioBitrateListPreference.entries =
                this.map { "${it.toString().toInt() / 1000} Kbps" }.toTypedArray()
            audioBitrateListPreference.entryValues = this
        }

        // Inflates audio sample rate
        val sampleRates = CameraStreamerConfigurationHelper.Audio.getSupportedSampleRates(encoder)
        audioSampleRateListPreference.entryValues.filter {
            sampleRates.contains(
                it.toString().toInt()
            )
        }.toTypedArray().run {
            audioSampleRateListPreference.entries =
                this.map { "${"%.1f".format(it.toString().toFloat() / 1000)} kHz" }.toTypedArray()
            audioSampleRateListPreference.entryValues = this
        }
    }

    private fun loadEndpoint() {
        // Inflates endpoint
        val supportedEndpointName =
            mapOf(
                EndpointType.TS_FILE to getString(R.string.to_ts_file),
                EndpointType.FLV_FILE to getString(R.string.to_flv_file),
                EndpointType.SRT to getString(R.string.to_srt),
                EndpointType.RTMP to getString(R.string.to_rtmp),
            )
        val supportedEndpoint = EndpointType.values().map { "${it.id}" }.toTypedArray()
        endpointTypePreference.setDefaultValue(EndpointType.SRT.id)
        endpointTypePreference.entryValues = supportedEndpoint
        endpointTypePreference.entries =
            supportedEndpoint.map { supportedEndpointName[EndpointType.fromId(it.toInt())] }
                .toTypedArray()
        setEndpointType(endpointTypePreference.value.toInt())
        endpointTypePreference.setOnPreferenceChangeListener { _, newValue ->
            setEndpointType((newValue as String).toInt())
            true
        }

        serverIpPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        }

        serverPortPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.filters = arrayOf(InputFilter.LengthFilter(5))
        }

        serverTargetVideoBitratePreference.isVisible =
            serverEnableBitrateRegulationPreference.isChecked
        serverMinVideoBitratePreference.isVisible =
            serverEnableBitrateRegulationPreference.isChecked
        serverEnableBitrateRegulationPreference.setOnPreferenceChangeListener { _, newValue ->
            serverTargetVideoBitratePreference.isVisible = newValue as Boolean
            serverMinVideoBitratePreference.isVisible = newValue
            true
        }

        serverTargetVideoBitratePreference.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Int) < serverMinVideoBitratePreference.value) {
                serverMinVideoBitratePreference.value = newValue
            }
            true
        }

        serverMinVideoBitratePreference.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Int) > serverTargetVideoBitratePreference.value) {
                serverTargetVideoBitratePreference.value = newValue
            }
            true
        }
    }

    private fun setEndpointType(id: Int) {
        val endpoint = EndpointFactory(
            EndpointType.fromId(id)
        ).build()
        srtEndpointPreference.isVisible = endpoint.hasSrtCapabilities
        rtmpEndpointPreference.isVisible = endpoint.hasRtmpCapabilities
        fileEndpointPreference.isVisible = endpoint.hasFileCapabilities
        tsMuxerPreference.isVisible = endpoint.hasTSCapabilities

        // Update file extension
        if (endpoint.hasFileCapabilities) {
            // Remove previous extension
            FileExtension.values().forEach {
                fileNamePreference.text = fileNamePreference.text.removeSuffix(it.extension)
            }
            // Add correct extension
            fileNamePreference.text += when {
                endpoint.hasFLVCapabilities -> {
                    FileExtension.FLV.extension
                }
                endpoint.hasTSCapabilities -> {
                    FileExtension.TS.extension
                }
                else -> {
                    throw IOException("Unknown file type")
                }
            }
        }
    }

    private fun loadPreferences() {
        loadVideoSettings()
        loadAudioSettings()
        loadEndpoint()
    }
}