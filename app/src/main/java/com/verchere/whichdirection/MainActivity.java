package com.verchere.whichdirection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener {

    private double myLatitude, myLongitude, targetLatitude, targetLongitude;
    private SensorManager sensorManager;
    Sensor magneto,accelero;
    float[] magnetoVector=new float[3];
    float[] acceleroVector=new float[3];
    float azimut = 0f;
    float orientationCoord = 0f;
    DirectionView direction;
    Button search;
    EditText address,start;
    private CameraView cameraView;
    private boolean isCamOpened = false;
    private boolean isVisible = false;
    private boolean islocation = false;
    private boolean isTerminus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        magneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        search = findViewById(R.id.search);
        address = findViewById(R.id.address);
        start = findViewById(R.id.position);
        search.setOnClickListener(this);

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.search:
                latLngFromAddress(address.getText().toString(),start.getText().toString());
                break;
            default:
                break;
        }
    }

    public void latLngFromAddress(String address, String location){
        RequestQueue queue = Volley.newRequestQueue(this);
        String paramsDirection = address.trim().replace(" ", "+");
        String paramsStart = location.trim().replace(" ", "+");
        String urlDirection = "https://api-adresse.data.gouv.fr/search/?q="+ paramsDirection + "&limit=1";
        String urlStart = "https://api-adresse.data.gouv.fr/search/?q="+ paramsStart + "&limit=1";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlDirection, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TextView error = findViewById(R.id.error);
                try{
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject result = new JSONObject(jsonResponse.getJSONArray("features").getString(0));
                    JSONArray coordinates = result.getJSONObject("geometry").getJSONArray("coordinates");
                    targetLatitude = coordinates.getDouble(1);
                    targetLongitude = coordinates.getDouble(0);
                    isTerminus = true;
                    if (isTerminus && islocation){
                        setARView();
                    }

                }catch (JSONException e){
                    error.setVisibility(View.VISIBLE);
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView errorT = findViewById(R.id.error);
                errorT.setVisibility(View.VISIBLE);
            }
        });
        StringRequest stringRequest2 = new StringRequest(Request.Method.GET, urlStart, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TextView error = findViewById(R.id.error);
                try{
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject result = new JSONObject(jsonResponse.getJSONArray("features").getString(0));
                    JSONArray coordinates = result.getJSONObject("geometry").getJSONArray("coordinates");
                    myLatitude = coordinates.getDouble(1);
                    myLongitude = coordinates.getDouble(0);
                    islocation = true;
                    if (isTerminus && islocation){
                        setARView();
                    }

                }catch (JSONException e){
                    error.setVisibility(View.VISIBLE);
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView errorT = findViewById(R.id.error);
                errorT.setVisibility(View.VISIBLE);
            }
        });
        queue.add(stringRequest);
        queue.add(stringRequest2);
    }

    public void setARView(){
        double x = Math.cos(Math.toRadians(myLatitude))* Math.sin(Math.toRadians(targetLatitude)) - Math.sin(Math.toRadians(targetLatitude))* Math.cos(Math.toRadians(myLatitude))* Math.cos(Math.toRadians(targetLongitude-myLongitude));
        double y = Math.cos(Math.toRadians(myLatitude))* Math.sin(Math.toRadians(targetLongitude-myLongitude));
        orientationCoord =(float) Math.round(Math.toDegrees(Math.atan2(y,x)));
        Log.e("angle direction", String.valueOf(orientationCoord));
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
