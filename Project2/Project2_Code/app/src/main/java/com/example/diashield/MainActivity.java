package com.example.diashield;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;

import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabase;

import android.graphics.SurfaceTexture;
import android.graphics.Color;
import android.graphics.Bitmap;

import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCharacteristics;

import android.os.Bundle;
import android.os.Handler;

import android.util.Size;

import android.view.TextureView;
import android.view.View;
import android.view.Surface;

import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private CameraDevice camElement;
    private CameraCaptureSession camSession;
    private CaptureRequest.Builder recordElement;

    private TextureView viewTextLook;

    private Handler tempElement;
    private Size gridPoint;

    private int avgPrev;
    private int avgBeforePrev;
    private int avgCurr;

    private List<Long> maxNoOfBeat = new ArrayList<>();
    private int totalF = 0;
    private double heartRate;

    Button heartRateButton;
    TextView heartRateText;

    List<Float> a1 = new ArrayList<>();
    List<Float> a2 = new ArrayList<>();
    List<Float> a3 = new ArrayList<>();

    private double rateOfRespiration;
    private SensorManager accModule;
    private Sensor accDevice;

    Button respRateButton;
    TextView respRateText;
    Button buttonSymptoms;

    int symptomId;
    public static final String EXTRA_MESSAGE = "com.example.diashield.MESSAGE";

    SQLiteDatabase database;
    Button buttonUploadSigns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewTextLook = findViewById(R.id.textCamModule);
        viewTextLook.setSurfaceTextureListener(surfaceTextureListener);

        heartRateText = (TextView)findViewById(R.id.heartRateTextModule);
        heartRateButton = (Button)findViewById(R.id.heartRateButton);
        heartRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                heartRateText.setText("Measuring.....");
                onCamera();
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        camElement.close();
                        MeasureHeartRate();
                        heartRateText.setText("Heart Rate: " + String.format("%.4f", heartRate));
                    }
                }, 45000);
            }
        });


        accModule = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accDevice = accModule.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accModule.registerListener((SensorEventListener) this, accDevice, SensorManager.SENSOR_DELAY_NORMAL);

        respRateText = (TextView)findViewById(R.id.textViewMeasureRespiratoryRate);
        respRateButton = (Button)findViewById(R.id.respRateButton);
        respRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respRateText.setText("Measuring.....");
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MeasureRespiratoryRate();
                        respRateText.setText("Respiratory Rate: " + String.format("%.1f", rateOfRespiration));
                    }
                }, 45000);
            }
        });

        buttonUploadSigns = (Button)findViewById(R.id.buttonUploadSigns);
        buttonUploadSigns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadSigns();
                heartRateText.setText("Heart Rate uploaded");
                respRateText.setText("Respiratory Rate uploaded");
            }
        });

        buttonSymptoms = (Button)findViewById(R.id.buttonSymptoms);
        buttonSymptoms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickSymptoms();
            }
        });

    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {}

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) { return false; }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            Bitmap bitmapImage = viewTextLook.getBitmap();
            int redPixelsSum = 0;
            int[] imagePixels = new int[bitmapImage.getHeight() * bitmapImage.getWidth() / 4];
            bitmapImage.getPixels(imagePixels, 0, bitmapImage.getWidth()/2,bitmapImage.getWidth()/4, bitmapImage.getHeight()/4, bitmapImage.getWidth()/2, bitmapImage.getHeight()/2);
            for(int i=0;i<imagePixels.length;i++){
                redPixelsSum = redPixelsSum + Color.red(imagePixels[i]);
            }
            if(totalF > 50){
                if(totalF > 150){
                    avgCurr = (avgCurr * 100 + redPixelsSum) / 101;
                    if((avgCurr < avgPrev) && (avgPrev > avgBeforePrev)){
                        maxNoOfBeat.add(System.currentTimeMillis());
                    }
                }
                else{
                    avgCurr = (avgCurr * (totalF - 50) + redPixelsSum) / (totalF - 49);
                }
            }
            else{
                avgCurr = redPixelsSum;
            }

            totalF++;
            avgBeforePrev = avgPrev;
            avgPrev = avgCurr;
        }
    };

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            camElement = camera;
            cameraView();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            camElement = null;
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            camElement = null;
        }
    };

    private void cameraView() {
        try {
            SurfaceTexture surfaceTexture = viewTextLook.getSurfaceTexture();
            assert surfaceTexture != null;
            surfaceTexture.setDefaultBufferSize(gridPoint.getWidth(), gridPoint.getHeight());
            Surface surface = new Surface(surfaceTexture);
            recordElement = camElement.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            recordElement.addTarget(surface);
            camElement.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(camElement != null) {
                        try {
                            camSession = cameraCaptureSession;
                            recordElement.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            recordElement.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                            camSession.setRepeatingRequest(recordElement.build(), null, tempElement);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void onCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            gridPoint = configMap.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                heartRateText.setText("Measure Heart Rate");
                return;
            }
            cameraManager.openCamera(cameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void MeasureHeartRate(){
        double sumPeakBeats = 0.0;
        for(int i=0; i<maxNoOfBeat.size()-1; i++){
            sumPeakBeats += (maxNoOfBeat.get(i + 1) - maxNoOfBeat.get(i));
        }
        double peakRateAverage = sumPeakBeats / (maxNoOfBeat.size() - 1);
        heartRate = (60000 / (peakRateAverage));
    }

    public void onSensorChanged(SensorEvent sensorEvent){
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            a1.add(sensorEvent.values[0]);
            a2.add(sensorEvent.values[1]);
            a3.add(sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    private void MeasureRespiratoryRate(){
        int count = 0;
        for(int i=1;i<a2.size();i+=(a2.size())/40){
            if((a2.get(i-1)<a2.get(i))&&(a2.get(i+1)<a2.get(i))){
                count++;
            }
        }
        rateOfRespiration = 60000*count/45000;
        accModule.unregisterListener(this);
    }

    public void UploadSigns() {
        try{
            database = openOrCreateDatabase("utkarshDatabase.db", Context.MODE_PRIVATE, null);
            database.beginTransaction();
            try{
                database.execSQL("Update User_Database Set heartrate ="+heartRate+", respiratoryRate ="+rateOfRespiration+" Where record_id in (select record_id from User_Database order by record_id desc LIMIT 1);");

                database.setTransactionSuccessful();
            }catch (SQLiteException e){
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }finally {
                database.endTransaction();
            }
        }catch (SQLException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void ClickSymptoms() {
        Intent symptomIntent = new Intent(this, SymptomActivity.class);
        symptomIntent.putExtra(EXTRA_MESSAGE, symptomId);
        startActivity(symptomIntent);
    }


}