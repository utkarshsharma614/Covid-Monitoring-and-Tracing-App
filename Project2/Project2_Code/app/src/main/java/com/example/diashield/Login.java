package com.example.diashield;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Login extends AppCompatActivity implements LocationListener  {

    Button buttonLogin;
    EditText username_text;
    EditText password_text;
    String username;
    public static final String EXTRA_MESSAGE = "com.example.diashield.MESSAGE";

    protected LocationManager locationManager;
    protected LocationListener locationListener;

    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Login Button/////////////////////////////////////////////////////////////
        buttonLogin = (Button)findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username_text = (EditText)findViewById(R.id.UserName);
                password_text = (EditText)findViewById(R.id.Password);

                username = username_text.getText().toString();
                String password = password_text.getText().toString();

                if(username.equals("utkarsh") && password.equals("1234567890"))
                    Login_action();
                else{
                    username_text.setText("Wrong Credentials");
                    password_text.setText("");
                }
            }
        });
        //Login Button/////////////////////////////////////////////////////////////

        //Database Logic //////////////////////////////////////////////////////////

        createDatabase();


        //Database Logic //////////////////////////////////////////////////////////

        //Location Logic//////////////////////////////////////////////////////////

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},10);
            return;
        }

        locationManager.requestLocationUpdates(GPS_PROVIDER, 2000, 0,  this);


        //Location Logic//////////////////////////////////////////////////////////


    }

    public void Login_action(){
        Intent loginIntent = new Intent(this, MainActivity.class);
        loginIntent.putExtra(EXTRA_MESSAGE,username);
        startActivity(loginIntent);
    }


    @Override
    public void onLocationChanged(Location location) {
        //Log.d("sdf","sdf");
        //Toast.makeText(getApplicationContext(),"Current Longitude: " + location.getLongitude() + " Current Latitude: " + location.getLatitude(), Toast.LENGTH_LONG).show();
        UpdateDatabase(location.getLongitude(),location.getLatitude());
        Log.d("Location","Current Longitude: " + location.getLongitude() + " Current Latitude: " + location.getLatitude());
    }

    public void createDatabase(){
        try {
            database = openOrCreateDatabase("utkarshDatabase.db", Context.MODE_PRIVATE, null);
            database.beginTransaction();
            try{
                database.execSQL("Create Table if not exists User_Database (" +
                        "record_id integer primary key autoincrement, " +
                        "time TIMESTAMP default 0.0, " +
                        "x_coordinate double default 0.0, " +
                        "y_coordinate double default 0.0, " +
                        "heartrate double default 0.0, " +
                        "respiratoryRate double default 0.0, " +
                        "nausea float default 0.0, " +
                        "headache float default 0.0, " +
                        "diarrhea float default 0.0, " +
                        "sorethroat float default 0.0, " +
                        "fever float default 0.0, " +
                        "muscleache float default 0.0, " +
                        "lossofsmellortaste float default 0.0, " +
                        "cough float default 0.0, " +
                        "shortnessofbreath float default 0.0, " +
                        "feelingtired float default 0.0);");
                database.setTransactionSuccessful();
            }catch (SQLiteException e){
                Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }finally {
                database.endTransaction();
            }
        }catch (SQLException e){
            Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void UpdateDatabase(double longitude, double latitude) {
        try{
            database = openOrCreateDatabase("utkarshDatabase.db", Context.MODE_PRIVATE, null);
            database.beginTransaction();
            try{
                database.execSQL("Insert Into User_Database (time, x_coordinate, y_coordinate) " +
                        "Values(CURRENT_TIMESTAMP,"+longitude+", "+latitude+");");
                database.setTransactionSuccessful();
              UploadTask up1 = new UploadTask();
//                Toast.makeText(getApplicationContext(),"Stating to Upload",Toast.LENGTH_LONG).show();
                up1.execute();
            }catch (SQLiteException e){
                Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }finally {
                database.endTransaction();
            }
        }catch (SQLException e){
            Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class UploadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {

//                String url = "http://10.218.107.121/cse535/upload_video.php";
                String url = "http://192.168.0.104:8080/upload_video.php";
                String charset = "UTF-8";
                String group_id = "40";
                String ASUid = "1200072576";
                String accept = "1";


//                File videoFile = new File(Environment.getExternalStorageDirectory()+"/my_folder/Action1.mp4");
                File videoFile = new File("/data/data/com.example.diashield/databases/utkarshDatabase.db");
                String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
                String CRLF = "\r\n"; // Line separator required by multipart/form-data.

                URLConnection connection;

                connection = new URL(url).openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (
                        OutputStream output = connection.getOutputStream();
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
                ) {
                    // Send normal accept.
                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"accept\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(accept).append(CRLF).flush();

                    // Send normal accept.
                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"id\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(ASUid).append(CRLF).flush();

                    // Send normal accept.
                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"group_id\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(group_id).append(CRLF).flush();


                    // Send video file.
                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + videoFile.getName() + "\"").append(CRLF);
                    writer.append("Content-Type: video/mp4; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                    writer.append(CRLF).flush();
                    FileInputStream vf = new FileInputStream(videoFile);
                    try {
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        while ((bytesRead = vf.read(buffer, 0, buffer.length)) >= 0)
                        {
                            output.write(buffer, 0, bytesRead);

                        }
                        //   output.close();
                        //Toast.makeText(getApplicationContext(),"Read Done", Toast.LENGTH_LONG).show();
                    }catch (Exception exception)
                    {


                        //Toast.makeText(getApplicationContext(),"output exception in catch....."+ exception + "", Toast.LENGTH_LONG).show();
                        Log.d("Error", String.valueOf(exception));
                        publishProgress(String.valueOf(exception));
                        // output.close();

                    }

                    output.flush(); // Important before continuing with writer!
                    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.


                    // End of multipart/form-data.
                    writer.append("--" + boundary + "--").append(CRLF).flush();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Request is lazily fired whenever you need to obtain information about response.
                int responseCode = ((HttpURLConnection) connection).getResponseCode();
                System.out.println(responseCode); // Should be 200

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onProgressUpdate(String... text) {
            Toast.makeText(getApplicationContext(), "In Background Task " + text[0], Toast.LENGTH_LONG).show();
        }

    }


}