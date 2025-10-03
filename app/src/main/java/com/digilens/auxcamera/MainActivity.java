package com.digilens.auxcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import com.digilens.digi_os_sdk.DigiOS_SDK;

public class MainActivity extends Activity {
    private static final String TAG = "AuxCamera";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private TextureView leftFishEye;
    private TextureView rightFishEye;

    protected CameraDevice cameraDeviceLeft;
    protected CameraDevice cameraDeviceRight;
    protected CameraCaptureSession cameraCaptureSessionsLeft;
    protected CameraCaptureSession cameraCaptureSessionsRight;
    protected CaptureRequest.Builder captureRequestBuilderLeft;
    protected CaptureRequest.Builder captureRequestBuilderRight;
    private Size mPreviewSizeRAW;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    private Handler mBackgroundHandlerLeft;
    private HandlerThread mBackgroundThreadLeft;
    private Handler mBackgroundHandlerRight;
    private HandlerThread mBackgroundThreadRight;

    Button leftEnable;
    Button rightEnable;
    Button bothEnable;

    boolean leftCameraStatus = false;
    boolean rightCameraStatus = false;
    boolean bothCameraStatus = false;

    private DigiOS_SDK digios;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        digios = new DigiOS_SDK();
        digios.allowAuxCameraAccess(getApplicationContext(), DigiOS_SDK.ARGO_AUX_CAMERA_BOTH_ALLOW_ACCESS, "com.digilens.stereotrackigcamerasample");

        leftFishEye = findViewById(R.id.leftAuxCamera);
        assert leftFishEye != null;

        rightFishEye = findViewById(R.id.rightAuxCamera);
        assert rightFishEye != null;

        leftEnable = findViewById(R.id.leftAuxCameraEnable);
        leftEnable.setOnClickListener(view -> handle_left_camera_enable());

        rightEnable = findViewById(R.id.rightAuxCameraEnable);
        rightEnable.setOnClickListener(view -> handle_right_camera_enable());

        bothEnable = findViewById(R.id.bothAuxCameraEnable);
        bothEnable.setOnClickListener(view -> {
            if (!bothCameraStatus) {
                Log.d(TAG, "bothAux Camera: enable");
                handle_left_camera_enable();
                handle_right_camera_enable();
                bothCameraStatus = true;
                leftEnable.setText(R.string.disable_left_aux_camera);
                rightEnable.setText(R.string.disable_right_aux_camera);
                bothEnable.setText(R.string.disable_both_aux_camera);
            } else {
                Log.d(TAG, "bothAux Camera: disable");
                enableAuxCameraLeft(false);
                enableAuxCameraRight(false);
                bothCameraStatus = false;
                leftEnable.setText(R.string.enable_left_aux_camera);
                rightEnable.setText(R.string.enable_right_aux_camera);
                bothEnable.setText(R.string.enable_both_aux_camera);
            }
        });
    }

    void handle_left_camera_enable() {
        if (!leftCameraStatus) {
            Log.d(TAG, "leftAux Camera: enable");
            enableAuxCameraLeft(true);
            leftEnable.setText(R.string.disable_left_aux_camera);
        } else {
            Log.d(TAG, "leftAux Camera: disable");
            enableAuxCameraLeft(false);
            leftEnable.setText(R.string.enable_left_aux_camera);
        }
    }
    void handle_right_camera_enable() {
        if (!rightCameraStatus) {
            Log.d(TAG, "rightAux Camera: enable");
            enableAuxCameraRight(true);
            rightEnable.setText(R.string.disable_right_aux_camera);
        } else {
            enableAuxCameraRight(false);
            Log.d(TAG, "rightAux Camera: disable");
            rightEnable.setText(R.string.enable_right_aux_camera);
        }
    }
    void enableAuxCameraLeft(boolean en) {
        if (en && !leftCameraStatus) {
            startBackgroundThread(mBackgroundThreadLeft, mBackgroundHandlerLeft,"Camera Left Background");

            if (leftFishEye != null) {
                if (leftFishEye.isAvailable()) {
                    openCamera(2, "Left");
                } else {
                    leftFishEye.setSurfaceTextureListener(textureListenerLeft);
                }
                leftCameraStatus = true;
            }
        } else if (!en && leftCameraStatus) {
            stopBackgroundThread(mBackgroundThreadLeft, mBackgroundHandlerLeft);
            if (null != cameraDeviceLeft) {
                cameraDeviceLeft.close();
                cameraDeviceLeft = null;
            }
            leftCameraStatus = false;
        }
    }
    void enableAuxCameraRight(boolean en) {
        if (en && !rightCameraStatus) {
            startBackgroundThread(mBackgroundThreadRight, mBackgroundHandlerRight,"Camera Right Background");
            if (rightFishEye != null) {
                if (rightFishEye.isAvailable()) {
                    openCamera(1, "Right");
                } else {
                    rightFishEye.setSurfaceTextureListener(textureListenerRight);
                }
                rightCameraStatus = true;
            }
        } else if (!en && rightCameraStatus) {
            stopBackgroundThread(mBackgroundThreadRight, mBackgroundHandlerRight);
            if (null != cameraDeviceRight) {
                cameraDeviceRight.close();
                cameraDeviceRight = null;
            }
            rightCameraStatus = false;
        }
    }

    TextureView.SurfaceTextureListener textureListenerLeft = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "Left Surface " + width + "    " + height);
            openCamera(2, "Left");
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.i(TAG, "Left Fish eye Surface updated");
        }
    };
    TextureView.SurfaceTextureListener textureListenerRight = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "Right Surface " + width + "    " + height);
            openCamera(1, "Right");
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.i(TAG, "Right Fish eye Surface updated");
        }
    };
    private final CameraDevice.StateCallback stateCallbackRight = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened CameraDevice Right");
            cameraDeviceRight = camera;
            createCameraPreviewRight(rightFishEye.getSurfaceTexture());
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "onDisconnected CameraDevice Right");
            cameraDeviceRight.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError CameraDevice Right");
            if (cameraDeviceRight != null) {
                cameraDeviceRight.close();
                cameraDeviceRight = null;
            }
        }
    };
    private final CameraDevice.StateCallback stateCallbackLeft = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened CameraDevice Left");
            cameraDeviceLeft = camera;
            createCameraPreviewLeft(leftFishEye.getSurfaceTexture());
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDeviceLeft.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            if (cameraDeviceLeft != null) {
                cameraDeviceLeft.close();
                cameraDeviceLeft = null;
            }
        }
    };

    protected void startBackgroundThread(HandlerThread bThread, Handler bhandler, String name) {
        bThread = new HandlerThread(name);
        bThread.start();
        bhandler = new Handler(bThread.getLooper());
    }
    protected void stopBackgroundThread(HandlerThread bThread, Handler bhandler) {
        if (bThread != null)
            bThread.quitSafely();
        try {
            if (bThread != null)
                bThread.join();
            bThread = null;
            bhandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException", e);
        }
    }

    protected void createCameraPreviewLeft(SurfaceTexture texture) {
        try {
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSizeRAW.getWidth(), mPreviewSizeRAW.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilderLeft = cameraDeviceLeft.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilderLeft.addTarget(surface);
            cameraDeviceLeft.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDeviceLeft) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessionsLeft = cameraCaptureSession;
                    updatePreview(cameraDeviceLeft, "Left", captureRequestBuilderLeft, mBackgroundHandlerLeft, cameraCaptureSessionsLeft);
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }

    protected void createCameraPreviewRight(SurfaceTexture texture) {
        try {
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSizeRAW.getWidth(), mPreviewSizeRAW.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilderRight = cameraDeviceRight.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilderRight.addTarget(surface);
            cameraDeviceRight.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDeviceRight) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessionsRight = cameraCaptureSession;
                    updatePreview(cameraDeviceRight, "Right", captureRequestBuilderRight, mBackgroundHandlerRight, cameraCaptureSessionsRight);
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }
    private void openCamera(int camId, String cam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                return;
            }
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] myList = manager.getCameraIdList();
            if (myList.length == 3 && camId <= 2) {
                Log.e(TAG, "is camera open " + camId + " for " + cam + " Camera length " + myList.length + " Camera Id " + myList[camId]);
                String cameraId = manager.getCameraIdList()[camId];
                mPreviewSizeRAW = new Size(WIDTH, HEIGHT);
                if (camId == 2) {
                    manager.openCamera(cameraId, stateCallbackLeft, null);
                } else if (camId == 1)
                    manager.openCamera(cameraId, stateCallbackRight, null);
            } else {
                String msg = "set property to enabled access to all 3 cameras";
                Log.e(TAG, msg);
                Log.e(TAG, "cameraId List " + myList.length);
                showCameraNotFoundMsg(msg);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }
    private void showCameraNotFoundMsg(String msg)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Stereo Camera Application");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
    protected void updatePreview(CameraDevice cameraDevice, String info, CaptureRequest.Builder builder, Handler handler, CameraCaptureSession cameraCaptureSession) {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview" + info + "error, return");
        }
        HandlerThread thread = new HandlerThread("CameraHighSpeedPreview"+info);
        thread.start();
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(builder.build(), null, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
    }
    private void closeCamera() {
        if (null != cameraDeviceRight) {
            cameraDeviceRight.close();
            cameraDeviceRight = null;
        }
        if (null != cameraDeviceLeft) {
            cameraDeviceLeft.close();
            cameraDeviceLeft = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "onResume");
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        enableAuxCameraLeft(false);
        enableAuxCameraRight(false);
        super.onPause();
    }
}
