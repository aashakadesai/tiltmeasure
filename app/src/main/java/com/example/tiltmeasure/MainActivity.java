package com.example.tiltmeasure;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    final double G = 9.81;

    private SensorManager sensorManager;
    private Sensor accl;
    private Sensor gyro;

    TextView xtilt;
    TextView ytilt;
    TextView ztilt;

    double timestamp = 0;
    double[] accl_angle;
    double[] gyro_angle;
    boolean enableAccl = false;
    boolean enableGyro = false;

    boolean measureBias = false;
    double[] accl_vals;
    double[] gyro_vals;
    int count_a = 0;
    int count_g = 0;

    double[] bias_accl;
    double[] bias_gyro;
    double print_time = 0;

    Button estimateB;
    Button accelero;
    Button gyrosco;

    Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xtilt = findViewById(R.id.xtilt);
        ytilt = findViewById(R.id.ytilt);
        ztilt = findViewById(R.id.ztilt);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accl = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        accl_angle = new double[3];
        gyro_angle = new double[3];
        accl_vals = new double[3];
        gyro_vals = new double[3];
        bias_accl = new double[3];
        bias_gyro = new double[3];

        for(int i = 0; i < 3; i++) {
            gyro_angle[i] = 0;
            bias_accl[i] = 0;
            bias_gyro[i] = 0;
        }

        accelero = findViewById(R.id.accelero);
        gyrosco = findViewById(R.id.gyrosco);
        estimateB = findViewById(R.id.bias);

        accelero.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (enableAccl == false){
                            enableAccl = true;
                            accelero.setText("Disable Accelerometer");
                        } else {
                            enableAccl = false;
                            accelero.setText("Enable Accelerometer");
                        }
                    }
                }
        );

        gyrosco.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (enableGyro == false){
                            enableGyro = true;
                            gyrosco.setText("Disable Gyroscope");
                        } else {
                            enableGyro = false;
                            gyrosco.setText("Enable Gyroscope");
                        }
                    }
                }
        );

        estimateB.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        measureBias = true;
                        enableAccl = true;
                        enableGyro = true;
                        estimateB.setEnabled(false);
                        accelero.setEnabled(false);
                        gyrosco.setEnabled(false);
                    }
                }
        );

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && enableAccl){

            if (measureBias == true){
                for (int i =0; i < 3; i++)
                    accl_vals[i] += event.values[i];
                count_a++;

            } else {

                float[] corrected = event.values.clone();

                for (int i = 0; i < 3; i++)
                    corrected[i] -= bias_accl[i];

                double norm = (corrected[0] * corrected[0]) + (corrected[1] * corrected[1]) + (corrected[2] * corrected[2]);
                norm = Math.sqrt(norm);

                double[] accl_tilt = new double[3];

                // USING TRIG
                accl_tilt[0] = Math.atan2(corrected[1], corrected[2]);
                accl_tilt[1] = Math.atan2(corrected[0], corrected[2]);
                //cannot measure yaw with accl
                accl_tilt[2] = 0;

                for (int i = 0; i < 3; i++) {
                    accl_angle[i] = accl_tilt[i];
                }
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && enableGyro){
            if (measureBias == true){
                for (int i =0; i < 3; i++)
                    gyro_vals[i] += event.values[i];

                count_g++;
            } else {
                if (timestamp != 0) {
                    double delta_t = (event.timestamp - timestamp) / 1000000000;

                    float[] corrected = event.values.clone();

                    for (int i = 0; i < 3; i++)
                        corrected[i] -= bias_gyro[i];

                    double[] gyro_tilt = new double[3];

                    for (int i = 0; i < 3; i++) {
                        gyro_tilt[i] = corrected[i] * delta_t;

                        if (enableAccl)
                            gyro_angle[i] = (0.98 * (gyro_angle[i] + gyro_tilt[i])) + (0.02 * accl_angle[i]);
                        else
                            gyro_angle[i] += gyro_tilt[i];
                    }
                }

                timestamp = event.timestamp;
            }
        }

        displayAngles(event.timestamp/1000000000);
    }

    public void displayAngles(double t){
       if (measureBias){

           xtilt.setText("MEASURING");
           ytilt.setText("BIAS");
           ztilt.setText("A: " + count_a + " G: " + count_g);

           if (count_a == 1000){
                for(int i = 0; i<3; i++)
                    bias_accl[i] = accl_vals[i]/count_a;
                //accounting for gravity
                bias_accl[2] -= 9.81;
                Log.d("accl_bias", bias_accl[0] + " " + bias_accl[1] + " " + bias_accl[2]);
           }

           if (count_g == 1000){
                for(int i = 0; i<3; i++)
                    bias_gyro[i] = gyro_vals[i]/count_g;

                Log.d("gyro_bias", bias_gyro[0] + " " + bias_gyro[1] + " " + bias_gyro[2]);
           }

           if (count_a > 1000 && count_g > 1000){
                measureBias = false;
                enableAccl = false;
                enableGyro = false;
                accelero.setEnabled(true);
                gyrosco.setEnabled(true);
           }
        } else {
           if (print_time == 0 && t != 0)
               print_time = t;

           if (!enableAccl && !enableGyro){
               xtilt.setText("NULL");
               ytilt.setText("NULL");
               ztilt.setText("NULL");
           }
           else if (enableGyro){
               xtilt.setText(String.format("%.2f", (gyro_angle[0]*180/Math.PI)));
               ytilt.setText(String.format("%.2f", (gyro_angle[1]*180/Math.PI)));
               ztilt.setText(String.format("%.2f", (gyro_angle[2]*180/Math.PI)));
               if (t - print_time > 1) {
                   if (enableAccl)
                       Log.d("fused", "" + gyro_angle[0] * 180 / Math.PI + "," + (gyro_angle[1] * 180 / Math.PI) + "," + (gyro_angle[2] * 180 / Math.PI));
                   else
                       Log.d("gyro", "" + gyro_angle[0] * 180 / Math.PI + "," + (gyro_angle[1] * 180 / Math.PI) + "," + (gyro_angle[2] * 180 / Math.PI));
                   print_time = t;
               }
           } else if (enableAccl){
               xtilt.setText(String.format("%.2f", (accl_angle[0]*180/Math.PI)));
               ytilt.setText(String.format("%.2f", (accl_angle[1]*180/Math.PI)));
               ztilt.setText("NULL");
               if (t - print_time > 1) {
                   Log.d("accl", "" + accl_angle[0]*180/Math.PI + "," + (accl_angle[1]*180/Math.PI) + "," + (accl_angle[2]*180/Math.PI));
                   print_time = t;
               }
           }
       }

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accl, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}