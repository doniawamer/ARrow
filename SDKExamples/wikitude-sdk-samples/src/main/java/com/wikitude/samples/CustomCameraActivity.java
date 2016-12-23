package com.wikitude.samples;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class CustomCameraActivity extends SampleCamActivity {

    @Override
    public void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        this.architectView.registerNativePlugins("wikitudePlugins", "customcamera");
        initNative();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onInputPluginInitialized() {
        Log.v(TAG, "onInputPluginInitialized");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    _wikitudeCamera2 = new WikitudeCamera2(CustomCameraActivity.this, 640, 480);
                    setFrameSize(_wikitudeCamera2.getFrameWidth(), _wikitudeCamera2.getFrameHeight());
                    setCameraFieldOfView((_wikitudeCamera2.getCameraFieldOfView()));

                    int imageSensorRotation = _wikitudeCamera2.getImageSensorRotation();
                    if (imageSensorRotation != 0) {
                        setImageSensorRotation(imageSensorRotation);
                    }
                }
                else
                {
                    _wikitudeCamera = new WikitudeCamera(640, 480);
                    setFrameSize(_wikitudeCamera.getFrameWidth(), _wikitudeCamera.getFrameHeight());

                    if(isCameraLandscape()) {
                        setDefaultDeviceOrientationLandscape(true);
                    }

                    int imageSensorRotation = _wikitudeCamera.getImageSensorRotation();
                    if (imageSensorRotation != 0) {
                        setImageSensorRotation(imageSensorRotation);
                    }
                }
            }
        });
    }

    public void onInputPluginPaused() {
        Log.v(TAG, "onInputPluginPaused");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
                {
                    _wikitudeCamera2.close();
                }
                else
                {
                    _wikitudeCamera.close();
                }
            }
        });
    }

    public void onInputPluginResumed() {
        Log.v(TAG, "onInputPluginResumed");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    CustomCameraActivity.this._wikitudeCamera2.start(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Image image = reader.acquireLatestImage();

                            if (null != image && null != image.getPlanes()) {
                                Image.Plane[] planes = image.getPlanes();

                                int widthLuminance = image.getWidth();
                                int heightLuminance = image.getHeight();

                                // 4:2:0 format -> chroma planes have half the width and half the height of the luma plane
                                int widthChrominance = widthLuminance / 2;
                                int heightChrominance = heightLuminance / 2;

                                int pixelStrideLuminance = planes[0].getPixelStride();
                                int rowStrideLuminance = planes[0].getRowStride();

                                int pixelStrideBlue = planes[1].getPixelStride();
                                int rowStrideBlue = planes[1].getRowStride();

                                int pixelStrideRed = planes[2].getPixelStride();
                                int rowStrideRed = planes[2].getRowStride();

                                notifyNewCameraFrame(
                                        widthLuminance,
                                        heightLuminance,
                                        getPlanePixelPointer(planes[0].getBuffer()),
                                        pixelStrideLuminance,
                                        rowStrideLuminance,
                                        widthChrominance,
                                        heightChrominance,
                                        getPlanePixelPointer(planes[1].getBuffer()),
                                        pixelStrideBlue,
                                        rowStrideBlue,
                                        getPlanePixelPointer(planes[2].getBuffer()),
                                        pixelStrideRed,
                                        rowStrideRed
                                );

                                image.close();
                            }
                        }
                    });
                }
                else
                {
                    _wikitudeCamera.start(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            notifyNewCameraFrameN21(data);
                        }
                    });
                    setCameraFieldOfView(_wikitudeCamera.getCameraFieldOfView());
                }
            }
        });
    }

    public void onInputPluginDestroyed() {
        Log.v(TAG, "onInputPluginDestroyed");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    _wikitudeCamera2.close();
                }
                else
                {
                    _wikitudeCamera.close();
                }
            }
        });
    }

    private byte[] getPlanePixelPointer(ByteBuffer pixelBuffer) {
        byte[] bytes;
        if (pixelBuffer.hasArray()) {
            bytes = pixelBuffer.array();
        } else {
            bytes = new byte[pixelBuffer.remaining()];
            pixelBuffer.get(bytes);
        }

        return bytes;
    }

    public boolean isCameraLandscape(){
        final Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final DisplayMetrics dm = new DisplayMetrics();
        final int rotation = display.getRotation();

        display.getMetrics(dm);

        final boolean is90off = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        final boolean isLandscape = dm.widthPixels > dm.heightPixels;

        return is90off ^ isLandscape;
    }

    private static final String TAG = "CustomCamera";

    private native void initNative();
    private native void notifyNewCameraFrame(int widthLuminance, int heightLuminance, byte[] pixelPointerLuminance, int pixelStrideLuminance, int rowStrideLuminance, int widthChrominance, int heightChrominance, byte[] pixelPointerChromaBlue, int pixelStrideBlue, int rowStrideBlue, byte[] pixelPointerChromaRed, int pixelStrideRed, int rowStrideRed);
    private native void notifyNewCameraFrameN21(byte[] frameData);
    private native void setCameraFieldOfView(double fieldOfView);
    private native void setFrameSize(int frameWidth, int frameHeight);
    private native void setDefaultDeviceOrientationLandscape(boolean isLandscape);
    private native void setImageSensorRotation(int rotation);

    private WikitudeCamera2 _wikitudeCamera2;
    private WikitudeCamera _wikitudeCamera;
}
