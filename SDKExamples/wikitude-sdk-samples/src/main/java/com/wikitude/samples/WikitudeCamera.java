package com.wikitude.samples;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

public class WikitudeCamera implements Camera.ErrorCallback {

    public WikitudeCamera(int frameWidth_, int frameHeight_)
    {
        _frameWidth = frameWidth_;
        _frameHeight = frameHeight_;

    }

    public void start(Camera.PreviewCallback previewCallback)
    {
        try
        {
            _camera = Camera.open(getCamera());
            _camera.setErrorCallback(this);
            _camera.setPreviewCallback(previewCallback);
            _cameraParameters = _camera.getParameters();
            _cameraParameters.setPreviewFormat(ImageFormat.NV21);
            Camera.Size cameraSize = getCameraSize(_frameWidth, _frameHeight);
            _cameraParameters.setPreviewSize(cameraSize.width, cameraSize.height);
            _fieldOfView = _cameraParameters.getHorizontalViewAngle();
            _camera.setParameters(_cameraParameters);
            _texture = new SurfaceTexture(0);
            _camera.setPreviewTexture((SurfaceTexture) _texture);
            _camera.startPreview();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (RuntimeException ex)
        {
            Log.e(TAG, "Camera not found: " + ex);
        }
    }

    public void close()
    {
        try
        {
            if (_camera != null)
            {
                _camera.setPreviewCallback(null);
                _camera.stopPreview();
                _camera.release();
                _camera = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        if (_camera != null) {
            _camera.release();
            _camera = null;
        }
    }

    private Camera.Size getCameraSize(int desiredWidth, int desiredHeight){
        for (Camera.Size size : _cameraParameters.getSupportedPreviewSizes()) {
            if (size.width<=desiredWidth && size.height<=desiredHeight) {
                return size;
            }
        }
        return _cameraParameters.getSupportedPreviewSizes().get(0);
    }

    private int getCamera()
    {
        try {
            int numberOfCameras = Camera.getNumberOfCameras();
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
                Camera.getCameraInfo(cameraId, cameraInfo);

                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return cameraId;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    public int getImageSensorRotation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        int cameraId = getCamera();

        if (cameraId != -1) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            int imageSensorRotation = cameraInfo.orientation;
            return 360 - imageSensorRotation; // the android API returns CW values (WHY?), 360 - X to have CCW
        } else {
            throw new RuntimeException("The getCamera function failed to return a valid camera ID. The image sensor rotation could therefore not be evaluated.");
        }
    }

    public double getCameraFieldOfView() { return _fieldOfView; }
    public int getFrameWidth() { return _frameWidth; }
    public int getFrameHeight() { return _frameHeight; }

    private static final String TAG = "WikitudeCamera";
    private int _frameWidth;
    private int _frameHeight;
    private double _fieldOfView;
    private Camera _camera;
    private Camera.Parameters _cameraParameters;
    private Object _texture;


}