package com.mrousavy.camera.utils

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Log
import android.view.Surface
import com.mrousavy.camera.CameraQueues
import com.mrousavy.camera.parsers.parseHardwareLevel

enum class SessionType {
  REGULAR,
  HIGH_SPEED;

  fun toSessionType(): Int {
    // TODO: Use actual enum when we are on API Level 28
    return when(this) {
      REGULAR -> 0 /* CameraDevice.SESSION_OPERATION_MODE_NORMAL */
      HIGH_SPEED -> 1 /* CameraDevice.SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED */
    }
  }
}

enum class OutputType {
  PHOTO,
  VIDEO,
  PREVIEW,
  VIDEO_AND_PREVIEW;

  fun toOutputType(): Long {
    // TODO: Use actual enum when we are on API Level 28
    return when(this) {
      PHOTO -> 0x2 /* CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE */
      VIDEO -> 0x3 /* CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD */
      PREVIEW -> 0x1 /* CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW */
      VIDEO_AND_PREVIEW -> 0x4 /* CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL */
    }
  }
}

data class SurfaceOutput(val surface: Surface,
                         val outputType: OutputType,
                         val dynamicRangeProfile: Long? = null) {
  val isRepeating: Boolean
    get() = outputType == OutputType.VIDEO || outputType == OutputType.PREVIEW || outputType == OutputType.VIDEO_AND_PREVIEW
}

fun supportsOutputType(characteristics: CameraCharacteristics, outputType: OutputType): Boolean {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val availableUseCases = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES)
    if (availableUseCases != null) {
      if (availableUseCases.contains(outputType.toOutputType())) {
        return true
      }
    }
  }
  // See https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture
  // According to the Android Documentation, devices with LEVEL_3 or FULL support can do 4 use-cases.
  // LIMITED or LEGACY devices can't do it.
  val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
  if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
    return true
  }

  return false
}

private const val TAG = "CreateCaptureSession"

fun CameraDevice.createCaptureSession(cameraManager: CameraManager,
                                      sessionType: SessionType,
                                      outputs: List<SurfaceOutput>,
                                      callback: CameraCaptureSession.StateCallback,
                                      queue: CameraQueues.CameraQueue) {
  val characteristics = cameraManager.getCameraCharacteristics(this.id)
  val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
  Log.i(TAG, "Creating Capture Session.. Hardware Level: ${parseHardwareLevel(hardwareLevel)}")

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    // API >= 24
    val outputConfigurations = arrayListOf<OutputConfiguration>()
    for (output in outputs) {
      if (!output.surface.isValid) {
        Log.w(TAG, "Tried to add ${output.outputType} output, but Surface was invalid! Skipping this output..")
        continue
      }
      val result = OutputConfiguration(output.surface)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (output.dynamicRangeProfile != null) {
          result.dynamicRangeProfile = output.dynamicRangeProfile
          Log.i(TAG, "Using dynamic range profile ${result.dynamicRangeProfile} for ${output.outputType} output.")
        }
        if (supportsOutputType(characteristics, output.outputType)) {
          result.streamUseCase = output.outputType.toOutputType()
          Log.i(TAG, "Using optimized stream use case ${result.streamUseCase} for ${output.outputType} output.")
        }
      }
      outputConfigurations.add(result)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // API >=28
      val config = SessionConfiguration(sessionType.toSessionType(), outputConfigurations, queue.executor, callback)
      this.createCaptureSession(config)
    } else {
      // API >=24
      this.createCaptureSessionByOutputConfigurations(outputConfigurations, callback, queue.handler)
    }
  } else {
    // API <24
    this.createCaptureSession(outputs.map { it.surface }, callback, queue.handler)
  }
}
