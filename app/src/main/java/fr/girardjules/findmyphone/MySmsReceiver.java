package fr.girardjules.findmyphone;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MySmsReceiver extends BroadcastReceiver {
    private static final String TAG = MySmsReceiver.class.getSimpleName();

    Location mLocation = null;
    boolean send = false;
    int timeout = 86400000; // 24 heures

    @Override
    public void onReceive(Context context, Intent intent) {

        for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {

            if (message != null) {

                if (message.getDisplayMessageBody().startsWith("GPS 0701")) {
                    Log.i(TAG, "message received");

                    if (message.getDisplayMessageBody().contains("critical")) {
                        Log.i(TAG, "critical mode enable");
                        timeout = 40000; // 40 secondes
                    }

                    LocationManager mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteres = new Criteria();
                    // la précision  : (ACCURACY_FINE pour une haute précision ou ACCURACY_COARSE pour une moins bonne précision)
                    criteres.setAccuracy(Criteria.ACCURACY_FINE);
                    // l'altitude
                    criteres.setAltitudeRequired(true);
                    //cap
                    criteres.setBearingRequired(true);
                    //vitesse
                    criteres.setSpeedRequired(true);

                    String fournisseur = mLocationManager.getBestProvider(criteres, true);

                    if (fournisseur != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            LocationListener mLocationListener = new LocationListener() {
                                @Override
                                public void onLocationChanged(@NonNull Location location) {
                                    Log.d(TAG, "location found");
                                    send = true;
                                    sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_success), location);
                                    mLocationManager.removeUpdates(this);

                                }
                            };

                            // on configure la mise à jour automatique : au moins 10 mètres et 15 secondes
                            mLocationManager.requestLocationUpdates(fournisseur, 1000, 2, mLocationListener);


                            // on attend 10 secondes
                            Thread mThread = new Thread() {
                                @Override
                                public void run() {
                                    long mTime = System.currentTimeMillis() + timeout;
                                    while (!send) {
                                        if (mTime < System.currentTimeMillis()) {
                                            mLocationManager.removeUpdates(mLocationListener);
                                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                mLocation = mLocationManager.getLastKnownLocation(fournisseur);
                                                if (mLocation != null) {
                                                    Log.i(TAG, "send last location");
                                                    sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_warn_last_position), mLocation);
                                                } else {
                                                    Log.w(TAG, "Last location is null");
                                                    sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_fail), mLocation);
                                                }
                                            } else {
                                                Log.w(TAG, "GPS permission denied");
                                                sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_fail_gps_permission_denied), null);
                                            }
                                            break;
                                        }
                                    }
                                }
                            };
                            mThread.start();

                        } else {
                            Log.w(TAG, "GPS permission denied");
                            sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_fail_gps_permission_denied), null);
                        }
                    } else {
                        Log.w(TAG, "GPS diseable");
                        sendLocation(context, message.getDisplayOriginatingAddress(), context.getString(R.string.response_fail_gps_disable), null);
                    }
                }
            }

        }

    }


    private void sendLocation(Context context, String number, String infos, Location location) {

        ArrayList<String> message = new ArrayList<>();
        if (infos != null) {
            message.addAll(Arrays.asList(infos.split(";;")));
        }
        if (location != null) {
            Date mDate = new Date(location.getTime());
            message.add(context.getString(R.string.response1, DateFormat.format("dd/MM/yyyy HH:mm:ss", mDate)));
            message.add(context.getString(R.string.response2, location.getLatitude(), location.getLongitude()));
            message.add(context.getString(R.string.response3, location.getAltitude(), location.getBearing(), location.getSpeed()));
        }
        Log.d(TAG, "SMS send : \n " + message.toString());

        SmsManager manager = SmsManager.getDefault();
        manager.sendMultipartTextMessage(number, null, message, null, null);
    }


}