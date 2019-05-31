package com.verchere.whichdirection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener {

    public LocationManager locationManager;
    private Location myLocation;
    private SensorManager sensorManager;
    private static final int REQUEST_PERMISSIONS = 1;
    Sensor magneto,accelero;
    float[] magnetoVector=new float[3];
    float[] acceleroVector=new float[3];
    float azimut = 0f;
    float orientationCoord = 0f;
    TextView direction;
    Button search;
    EditText address;
    private TextureView camView;
    String cameraId;
    protected CameraDevice cameraDevice;
    private Size imageDimension;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraCaptureSession cameraCaptureSessions;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private boolean isCamOpened = false;
    private boolean isVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        magneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        search = findViewById(R.id.search);
        address = findViewById(R.id.address);
        search.setOnClickListener(this);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
            boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(network_enabled){
                myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else {
                Toast.makeText(MainActivity.this, "Network not enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        }
        float LOCATION_REFRESH_DISTANCE = 1;
        long LOCATION_REFRESH_TIME = 100;
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);

    }
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            myLocation = location;
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.search:
                latLngFromAddress(address.getText().toString());
                break;
            default:
                break;
        }
    }

    public void latLngFromAddress(String address){
        Geocoder coder = new Geocoder(this);
        List<Address> addressList;
        try {
            if (address != null && !address.equals("") && !address.equals(" ")){
                addressList = coder.getFromLocationName(address,5);
                if (addressList != null){
                    Address directionAddress = addressList.get(0);
                    double x = Math.cos(Math.toRadians(myLocation.getLatitude()))* Math.sin(Math.toRadians(directionAddress.getLatitude())) - Math.sin(Math.toRadians(directionAddress.getLatitude()))* Math.cos(Math.toRadians(myLocation.getLatitude()))* Math.cos(Math.toRadians(directionAddress.getLongitude()-myLocation.getLongitude()));
                    double y = Math.cos(Math.toRadians(myLocation.getLatitude()))* Math.sin(Math.toRadians(directionAddress.getLongitude()-myLocation.getLongitude()));
                    orientationCoord =(float) Math.round(Math.toDegrees(Math.atan2(y,x)));
                    Log.e("angle direction", String.valueOf(orientationCoord));
                    setARView();
                }
            }

        }catch (IOException e){
            Log.e("fail found address",e.getMessage());
            TextView error = findViewById(R.id.error);
            error.setVisibility(View.VISIBLE);
        }
    }

    public void setARView(){
        setContentView(R.layout.ar_layout);
        camView = findViewById(R.id.view);
        direction = findViewById(R.id.direction);
        isCamOpened = true;
        assert camView!=null;
        camView.setSurfaceTextureListener(textureListener);


    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
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
        }
    };

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = camView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e("app", "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e("app", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        Log.e("app", "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException se){
            se.printStackTrace();
        } catch (NullPointerException npe){
            npe.printStackTrace();
        }
        Log.e("app", "openCamera X");
    }



    @Override
    public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            magnetoVector=e.values;
        }
        else if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
            acceleroVector = e.values;
        }
        updateAzimut();
    }

    public void updateAzimut(){
        float R[] = new float[9];
        SensorManager.getRotationMatrix(R, null, magnetoVector, acceleroVector);

        float[] camR = new float[9];
        SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, camR);

        float[] orientation=new float[3];
        SensorManager.getOrientation(camR, orientation);

        azimut = (float) -Math.round(Math.toDegrees(orientation[0]));
        if (isCamOpened){
            if (azimut> (orientationCoord-5) && azimut < (orientationCoord +5)){
                if (!isVisible){
                    direction.setVisibility(View.VISIBLE);
                    isVisible = true;
                }

            } else {
                if(isVisible){
                    direction.setVisibility(View.INVISIBLE);
                    isVisible = false;
                }

            }
        }
        // angular.setText(String.valueOf(azimut));
    }

    @Override
    public void onAccuracyChanged(Sensor sens, int accuracy){
    }

    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_PERMISSIONS) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
                    boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if(network_enabled){
                        myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    } else {
                        Toast.makeText(MainActivity.this, "Network not enabled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(MainActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this,magneto);
        sensorManager.unregisterListener(this,accelero);
        stopBackgroundThread();
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this,magneto,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,accelero,SensorManager.SENSOR_DELAY_NORMAL);
        startBackgroundThread();
        if (camView != null){
            if (camView.isAvailable()) {
                openCamera();
            } else {
                camView.setSurfaceTextureListener(textureListener);
            }
        }

    }

    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isCamOpened){
                isCamOpened = false;
                setContentView(R.layout.activity_main);
                search = findViewById(R.id.search);
                address = findViewById(R.id.address);
                search.setOnClickListener(this);
            }
            else {
                finish();
            }
            return true;
        }
        return false;
    }
}
