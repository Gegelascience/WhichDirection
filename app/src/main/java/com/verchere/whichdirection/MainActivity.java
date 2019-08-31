package com.verchere.whichdirection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
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
    DirectionView direction;
    Button search;
    EditText address;
    private CameraView cameraView;
    private boolean isCamOpened = false;

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
            boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (gps_enabled){
                myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            else if(network_enabled){
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
            if (address != null && !address.equals("") && !address.equals(" ")) {
                addressList = coder.getFromLocationName(address, 5);
                if (addressList != null) {
                    Address directionAddress = addressList.get(0);
                    double x = Math.cos(Math.toRadians(myLocation.getLatitude())) * Math.sin(Math.toRadians(directionAddress.getLatitude())) - Math.sin(Math.toRadians(directionAddress.getLatitude())) * Math.cos(Math.toRadians(myLocation.getLatitude())) * Math.cos(Math.toRadians(directionAddress.getLongitude() - myLocation.getLongitude()));
                    double y = Math.cos(Math.toRadians(myLocation.getLatitude())) * Math.sin(Math.toRadians(directionAddress.getLongitude() - myLocation.getLongitude()));
                    orientationCoord = (float) Math.round(Math.toDegrees(Math.atan2(y, x)));
                    Log.e("angle direction", String.valueOf(orientationCoord));
                    setARView();
                } else {
                    TextView error = findViewById(R.id.error);
                    error.setVisibility(View.VISIBLE);
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
        cameraView = new CameraView((TextureView) findViewById(R.id.view), this);
        TextureView textureView = findViewById(R.id.direction);
        direction = new DirectionView(this);
        direction.init(textureView);
        isCamOpened = true;
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
            direction.mTextureView.setRotation(azimut - orientationCoord);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sens, int accuracy){
        if (sens.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (accuracy == 0 | accuracy == 1){
                Toast.makeText(MainActivity.this, "Please recalibrate your magnetometer", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_PERMISSIONS) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
                    boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if (gps_enabled){
                        myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    else if(network_enabled){
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


    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this,magneto);
        sensorManager.unregisterListener(this,accelero);
        if(isCamOpened){
            cameraView.stopBackgroundThread();
        }

    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this,magneto,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,accelero,SensorManager.SENSOR_DELAY_NORMAL);
        if (isCamOpened){
            cameraView.startBackgroundThread();
            cameraView.resumeCam();
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
