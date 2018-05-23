package com.mulimachibuye.remotesensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mTemperature;
    Bmx280 mBmx280;
    Bmx280SensorDriver mSensorDriver;
    SensorEventListener mListener;
    I2cDevice device;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
//        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
//            @Override
//            public void onDynamicSensorConnected(Sensor sensor) {
//                if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
//                    mSensorManager.registerListener(mListener, sensor,SensorManager.SENSOR_DELAY_NORMAL);
//
//                }
//            }
//        });



        try {
            //mSensorDriver = new Bmx280SensorDriver("I2C1",0x77);
            mBmx280 = new Bmx280("I2C1",0x77);
            mBmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            System.out.println(mBmx280.readTemperature());
            mBmx280.setHumidityOversampling(Bmx280.OVERSAMPLING_1X);
            System.out.println(mBmx280.readHumidity());
            mBmx280.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
            System.out.println(mBmx280.readPressure());
            //mSensorDriver.registerTemperatureSensor();


        } catch (IOException e) {
            // couldn't configure the device...
            Log.e("Initialization Failed","The sensor could not be initialized");
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        System.out.println("Accuracy changed");
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float temperature = event.values[0];
        System.out.println(temperature);
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);

    }

    protected Bmx280 initializeBmx(Bmx280 m){

        try {
            m = new Bmx280("I2C1",0x77);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return m;
    }
}
