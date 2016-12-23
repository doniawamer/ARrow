package com.wikitude.samples;

/**
 * Created by danielguttenberg on 23/03/16.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.ImageReader;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

@TargetApi(22)
public class WikitudeCamera2
{
    public WikitudeCamera2(Context context_, int frameWidth_, int frameHeight_)
    {
        _context = context_;

        _frameWidth = frameWidth_;
        _frameHeight = frameHeight_;

        _manager = (CameraManager) _context.getSystemService(Context.CAMERA_SERVICE);
        _fieldOfView = getCameraFieldOfViewInternal();
        _imageSensorRotation = getImageSensorRotationInternal();

        _closeCalled = false;
    }

    public void start(ImageReader.OnImageAvailableListener onImageAvailableListener_)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= 23 && _context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                Log.e(TAG, "Camera Permission has been denied by the user. Aborting initialization.");
                throw new SecurityException();
            }

            _manager.openCamera(getCamera(), cameraStateCallback, null);
            _imageReader = ImageReader.newInstance(_frameWidth, _frameHeight, ImageFormat.YUV_420_888, 2);
            _imageReader.setOnImageAvailableListener(onImageAvailableListener_, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    public void close()
    {
        synchronized (_cameraClosedLock) {
            _closeCalled = true;
            try {
                if (_cameraCaptureSession != null && _cameraDevice != null) {
                    _closeCalled = false;
                }

                if (_cameraCaptureSession != null) {
                    _cameraCaptureSession.abortCaptures();
                    _cameraCaptureSession.close();
                    _cameraCaptureSession = null;
                }

                if (_cameraDevice != null) {
                    _cameraDevice.close();
                    _cameraDevice = null;
                }

                if (_imageReader != null) {
                    _imageReader.close();
                    _imageReader = null;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /*private void process(Image image)
    {
        Log.i(TAG, "width: " + image.getWidth());
        Log.i(TAG, "height: " + image.getHeight());
        Log.i(TAG, "format: " + image.getFormat());
        Log.i(TAG, "timestamp: " + image.getTimestamp());

        Image.Plane[] YCbCr = image.getPlanes();
        ByteBuffer Y = YCbCr[0].getBuffer();
        ByteBuffer Cb = YCbCr[1].getBuffer();
        ByteBuffer Cr = YCbCr[2].getBuffer();

        // do something with YCbCr image data
    }*/

    private String getCamera()
    {
        try
        {
            for (String cameraId : _manager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = _manager.getCameraCharacteristics(cameraId);

                int cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK)
                {
                    float sensorWidth = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth();
                    float focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
                    _fieldOfView = Math.toDegrees(2 * Math.atan(0.5 * sensorWidth / focalLength));

                    return cameraId;
                }
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private double getCameraFieldOfViewInternal()
    {
        try
        {
            for (String cameraId : _manager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = _manager.getCameraCharacteristics(cameraId);

                int cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK)
                {
                    float sensorWidth = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth();
                    float focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
                    return Math.toDegrees(2 * Math.atan(0.5 * sensorWidth / focalLength));
                }
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        return 0.0f;
    }

    private int getImageSensorRotationInternal()
    {
        try
        {
            if (_manager.getCameraIdList().length == 0)
            {
                throw new RuntimeException("The camera manager returned an empty list of available cameras. The image sensor rotation could not be evaluated.");
            }
            else
            {
                for (String cameraId : _manager.getCameraIdList())
                {
                    CameraCharacteristics cameraCharacteristics = _manager.getCameraCharacteristics(cameraId);

                    int cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK)
                    {
                        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        return 360 - sensorOrientation; // the android API returns CW values (WHY?), 360 - X to have CCW
                    }
                    else
                    {
                        throw new RuntimeException("No back facing camera found. The image sensor rotation could not be evaluated.");
                    }
                }
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        // 90, 180, 270, 360 are valid values
        // as this function return an angle in degrees that is used to rotate the camera image
        // a visually easily perceivable values is chosen. Using -1 might go unnoticed as
        // a rotation this small is visually insignificant.
        return 45;
    }

    private CaptureRequest createCaptureRequest()
    {
        try
        {
            CaptureRequest.Builder builder = _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(_imageReader.getSurface());
            return builder.build();
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(CameraDevice camera) {
            synchronized (_cameraClosedLock) {
                if (!_closeCalled) {
                    _cameraDevice = camera;
                    try {
                        _cameraDevice.createCaptureSession(Arrays.asList(_imageReader.getSurface()), sessionStateCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    _closeCalled = false;
                }
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera)
        {
            Log.e(TAG, "Callback function onDisconnected called.");
        }

        @Override
        public void onError(CameraDevice camera, int error)
        {
            if (_closeCalled) {
                _closeCalled = false;
            }

            String errorString = "";
            switch (error)
            {
                case ERROR_CAMERA_DEVICE:
                    errorString = "ERROR_CAMERA_DEVICE received, indicating that the camera device has encountered a fatal error.";
                    break;
                case ERROR_CAMERA_DISABLED:
                    errorString = "ERROR_CAMERA_DISABLED received, indicating that the camera device could not be opened due to a device policy.";
                    break;
                case ERROR_CAMERA_IN_USE:
                    errorString = "ERROR_CAMERA_IN_USE received, indicating that the camera device is in use already.";
                    break;
                case ERROR_CAMERA_SERVICE:
                    errorString = "ERROR_CAMERA_SERVICE received, indicating that the camera service has encountered a fatal error.";
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    errorString = "ERROR_MAX_CAMERAS_IN_USE received, indicating that the camera device could not be opened because there are too many other open camera devices.";
                    break;
            }

            Log.e(TAG, "Callback function onError called." + errorString);
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback()
    {
        @Override
        public void onConfigured(CameraCaptureSession session)
        {
            synchronized (_cameraClosedLock) {
                if (!_closeCalled) {
                    _cameraCaptureSession = session;
                    try {
                        session.setRepeatingRequest(createCaptureRequest(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    _closeCalled = false;
                }
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session)
        {
            if (_closeCalled) {
                _closeCalled = false;
            }
        }
    };

    public double getCameraFieldOfView() { return _fieldOfView; }
    public int getImageSensorRotation() { return _imageSensorRotation; }
    public int getFrameWidth() { return _frameWidth; }
    public int getFrameHeight() { return _frameHeight; }

    private static final String TAG = "WikitudeCamera2";
    private Context _context;
    private int _frameWidth;
    private int _frameHeight;
    CameraManager _manager;
    private CameraCaptureSession _cameraCaptureSession;
    private CameraDevice _cameraDevice;
    private ImageReader _imageReader;
    private double _fieldOfView;
    private int _imageSensorRotation;
    private boolean _closeCalled;
    private final Object _cameraClosedLock = new Object();
}