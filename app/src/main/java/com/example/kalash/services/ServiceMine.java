package com.example.kalash.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.kalash.R;
import com.example.kalash.ui.MainActivity;
import com.github.tbouron.shakedetector.library.ShakeDetector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServiceMine extends Service {

    boolean isRunning = false;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    static final String PHONE_NUMBER = "phoneNumber";
    static final String SHARED_PREF_NAME = "WomenSafetyApp";
    String message="I'm in Trouble!!";
    static final String SMS_MESSAGE="sms_message";



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    SmsManager manager = SmsManager.getDefault();
    private LocationManager locationManager;
    private LocationListener locationListener;
    String myLocation;
    int count =0;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();


        // Get the location manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Define the criteria how to select the location provider -> use
        // default
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);

        // Get the last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);

        // Log the location
        if (location != null) {
            Log.d("LAST LOCATION", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());

              myLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
        } else {
              myLocation = "Unable to Find Location :(";
              locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }


        ShakeDetector.create(this, () -> {


                    Iterator<String> i = getPhoneNumber().iterator();
                    while (i.hasNext())
                        if (count < 3) {
                            manager.sendTextMessage(i.next(), null, getSOSMessage()+"\nSending My Location :\n" + myLocation, null, null);
                            count+=1;
                        }
                }
        );

    }

    public Set<String> getPhoneNumber() {
        Set<String> mutableSet = new HashSet<String>();
        return sharedPreferences.getStringSet(PHONE_NUMBER, mutableSet);
    }
    public String getSOSMessage() {
        return sharedPreferences.getString(SMS_MESSAGE, message);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

            if (intent.getAction().equalsIgnoreCase("STOP")) {
                if(isRunning) {
                    this.stopForeground(true);
                    this.stopSelf();
                }
            } else {


                Intent notificationIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("MYID", "CHANNELFOREGROUND", NotificationManager.IMPORTANCE_DEFAULT);

                    NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    m.createNotificationChannel(channel);

                    Notification notification = new Notification.Builder(this, "MYID")
                            .setContentTitle("Women Safety")
                            .setContentText("Shake Device to Send SOS")
                            .setSmallIcon(R.drawable.girl_vector)
                            .setContentIntent(pendingIntent)
                            .build();
                    this.startForeground(115, notification);
                    isRunning = true;
                    return START_NOT_STICKY;
                }
            }

        return super.onStartCommand(intent,flags,startId);


    }




    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
