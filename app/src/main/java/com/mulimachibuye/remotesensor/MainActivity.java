package com.mulimachibuye.remotesensor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.pio.I2cDevice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
public class MainActivity extends Activity {

    private SensorManager mSensorManager;
    private Sensor mTemperature, mHumidity, mPressure, mGPS;
    Bmx280 mBmx280;
    Bmx280SensorDriver bmx280SensorDriver;
    I2cDevice device;
    float temperature=0, humidity=0, pressure=0, last_Pressure=0;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
            @Override
            public void onDynamicSensorConnected(Sensor sensor) {
                if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    mSensorManager.registerListener(mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                }

                if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                    mSensorManager.registerListener(mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                }

                if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    mSensorManager.registerListener(mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                }

                if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    mSensorManager.registerListener(mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                }
            }


        });


        try {
            bmx280SensorDriver = new Bmx280SensorDriver("I2C1");

            bmx280SensorDriver.registerTemperatureSensor();
            bmx280SensorDriver.registerHumiditySensor();
            bmx280SensorDriver.registerPressureSensor();

        } catch (IOException e) {
            // Error configuring sensor
        }




    }


    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();

    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(mListener);
        bmx280SensorDriver.unregisterHumiditySensor();
        bmx280SensorDriver.unregisterPressureSensor();
        bmx280SensorDriver.unregisterTemperatureSensor();

    }

    protected Bmx280 initializeBmx(Bmx280 m){

        try {
            m = new Bmx280("I2C1",0x77);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return m;
    }



    private final SensorEventListener mListener = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

            // Do something here if sensor accuracy changes.
            System.out.println("Accuracy changed");
            System.out.println(sensor.getStringType());
        }


        @Override
        public void onSensorChanged(SensorEvent event) {

            if(event.sensor.getType()==Sensor.TYPE_AMBIENT_TEMPERATURE){
                temperature = event.values[0];
                System.out.println("Temp:"+temperature);
                HttpGetRequest getRequest = new HttpGetRequest();
                try{
                    String result = getRequest.execute("https://api.thingspeak.com/update?api_key=3W64QD86KH9VBVDC&field1="+temperature).get();
                    System.out.println(result);

                }catch(Exception e){
                    e.printStackTrace();

                }

            }

            if(event.sensor.getType()==Sensor.TYPE_RELATIVE_HUMIDITY){
                humidity = event.values[0];
                System.out.println("Humi:"+humidity);

                HttpGetRequest getRequest = new HttpGetRequest();
                try{
                    String result = getRequest.execute("https://api.thingspeak.com/update?api_key=3W64QD86KH9VBVDC&field2="+humidity).get();
                    System.out.println(result);

                }catch(Exception e){

                    e.printStackTrace();
                }
            }

            if(event.sensor.getType()==Sensor.TYPE_PRESSURE){
                pressure = event.values[0];
                System.out.println("Press:"+pressure);

                //only execute if the current pressure is not equal to the last recorded pressure
                if((last_Pressure-pressure)>0.01) {
                    HttpGetRequest getRequest = new HttpGetRequest();
                    try {
                        //update the last_pressure to the current pressure. The pressure value seems not to change much
                        //but isalways reported. Only report when there is a change in the value
                        last_Pressure = pressure;
                        String result = getRequest.execute("https://api.thingspeak.com/update?api_key=3W64QD86KH9VBVDC&field3=" + pressure).get();
                        System.out.println(result);

                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
            }




        }
    };

//    private class BackgroundRequest extends AsyncTask {
//
//        @Override
//        protected Object doInBackground(Object[] objects) {
//
//
//            return null;
//        }
//    }


    public static class HttpGetRequest extends AsyncTask<String, Void, String> {
        static final String REQUEST_METHOD = "GET";
        static final int READ_TIMEOUT = 15000;
        static final int CONNECTION_TIMEOUT = 15000;
        @Override
        protected String doInBackground(String... params){
            String stringUrl = params[0];
            String result;
            String inputLine;
            try {
                //Create a URL object holding our url
                URL myUrl = new URL(stringUrl);
                //Create a connection
                HttpURLConnection connection =(HttpURLConnection)
                        myUrl.openConnection();
                //Set methods and timeouts
                connection.setRequestMethod(REQUEST_METHOD);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECTION_TIMEOUT);

                //Connect to our url
                connection.connect();
                //Create a new InputStreamReader
                InputStreamReader streamReader = new
                        InputStreamReader(connection.getInputStream());
                //Create a new buffered reader and String Builder
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                //Check if the line we are reading is not null
                while((inputLine = reader.readLine()) != null){
                    stringBuilder.append(inputLine);
                }
                //Close our InputStream and Buffered reader
                reader.close();
                streamReader.close();
                //Set our result equal to our stringBuilder
                result = stringBuilder.toString();
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            }
            return result;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
        }
    }


}
