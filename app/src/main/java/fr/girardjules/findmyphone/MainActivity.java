package fr.girardjules.findmyphone;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    ImageView mImageFineLocation;
    ImageView mImageBackgroundLocation;
    ImageView mImageReceiveSms;
    ImageView mImageSendSms;
    ImageView mImageGps;
    ImageView mImageNetwork;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageFineLocation = findViewById(R.id.main_activity_image_fine_location);
        mImageBackgroundLocation = findViewById(R.id.main_activity_image_background_location);
        mImageReceiveSms = findViewById(R.id.main_activity_image_receive_sms);
        mImageSendSms = findViewById(R.id.main_activity_image_send_sms);
        mImageGps = findViewById(R.id.main_activity_image_gps);
        mImageNetwork = findViewById(R.id.main_activity_image_network);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences_file_key), Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean("check_auto_start_enable", false)) {

            Log.d(TAG, "enable auto-start");

            //check marque du telephone pour activer l'autostart
            String manufacturer = android.os.Build.MANUFACTURER;
            try {
                Intent intent = new Intent();
                if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
                } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                }

                List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.size() > 0) {
                    startActivity(intent);
                }
                sharedPref.edit().putBoolean("check_auto_start_enable", true).apply();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

//        Log.d(TAG, "ACCESS_FINE_LOCATION : " + (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
//        Log.d(TAG, "ACCESS_BACKGROUND_LOCATION" + (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED));
//        Log.d(TAG, "RECEIVE_SMS" + (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED));
//        Log.d(TAG, "SEND_SMS" + (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED));


        // check autorisations
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "permission denied");

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
            }, 201);
        }

//        Intent intent = new Intent("android.provider.Telephony.SMS_RECEIVED");
//        List<ResolveInfo> infos = getPackageManager().queryBroadcastReceivers(intent, 0);
//        for (ResolveInfo info : infos) {
//            System.out.println("Receiver name:" + info.activityInfo.name + "; priority=" + info.priority);
//        }


    }


    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mImageFineLocation.setImageResource(R.drawable.ic_check);
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 29) {
            mImageBackgroundLocation.setImageResource(R.drawable.ic_check);
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            mImageReceiveSms.setImageResource(R.drawable.ic_check);
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            mImageSendSms.setImageResource(R.drawable.ic_check);
        }

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mImageGps.setImageResource(R.drawable.ic_check);
        }


        TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (mTelephonyManager.isSmsCapable()) {
            mImageNetwork.setImageResource(R.drawable.ic_check);
        }


    }
}

