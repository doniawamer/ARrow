package com.wikitude.samples;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.wikitude.sdksamples.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceDetectionPluginActivity extends SampleCamActivity {

    private File _cascadeFile;
    private StrokedRectangle rectangle = new StrokedRectangle(StrokedRectangle.Type.FACE);
    private int _defaultOrientation;

    private final Object _projectionMatrixLock = new Object();
    private boolean _projectionMatrixUpdateRequired = false;
    private float[] _projectionMatrix = new float[16];

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.architectView.registerNativePlugins("wikitudePlugins", "face_detection");
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(R.raw.high_database);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            _cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(_cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            initNative(_cascadeFile.getAbsolutePath());

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }

        evaluateDeviceDefaultOrientation();
        if (_defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setIsBaseOrientationLandscape(true);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (rectangle == null) {

            rectangle = new StrokedRectangle(StrokedRectangle.Type.FACE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        rectangle = null;
    }

    public void evaluateDeviceDefaultOrientation() {
        WindowManager windowManager =  (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Configuration config = getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            _defaultOrientation = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            _defaultOrientation = Configuration.ORIENTATION_PORTRAIT;
        }
    }
    public void onFaceDetected(float[] modelViewMatrix) {
        if (rectangle != null) {

            rectangle.setViewMatrix(modelViewMatrix);
        }
    }

    public void onFaceLost() {

        if (rectangle != null) {

            rectangle.onFaceLost();
        }
    }

    public void onProjectionMatrixChanged(float[] projectionMatrix) {

        synchronized (_projectionMatrixLock) {
            _projectionMatrixUpdateRequired = true;
            _projectionMatrix = projectionMatrix;
        }
    }

    public void renderDetectedFaceAugmentation() {

        if (rectangle != null) {

            synchronized (_projectionMatrixLock) {
                if (_projectionMatrixUpdateRequired) {
                    rectangle.setProjectionMatrix(_projectionMatrix);
                    _projectionMatrixUpdateRequired = false;
                }
            }

            rectangle.onDrawFrame();
        }
    }

    private native void initNative(String casecadeFilePath);
    private native void setIsBaseOrientationLandscape(boolean isBaseOrientationLandscape_);
}
