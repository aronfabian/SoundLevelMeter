package wavrecorder.com.fabian.aron.wavrecorder;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.TimedText;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private PhoneDB db;
    private final MainActivity target = this;
    private ProgressBar progressBar;
    private TextView dBAText;
    private TextView dBCText;
    private TextView lAeqText;
    private TextView u3Text;
    private TextView b314Text;
    private TextView b1418Text;
    private EditText timeText;


    @NeedsPermission(Manifest.permission.READ_PHONE_STATE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startButton = (Button) findViewById(R.id.btn_start);
        Button stopButton = (Button) findViewById(R.id.btn_stop);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        dBAText = (TextView) findViewById(R.id.text_dba);
        dBCText = (TextView) findViewById(R.id.text_dbc);
        lAeqText = (TextView) findViewById(R.id.text_laeq);
        u3Text = (TextView) findViewById(R.id.text_u3_recom);
        b314Text = (TextView) findViewById(R.id.text_3_14_recom);
        b1418Text = (TextView) findViewById(R.id.text_14_18_recom);
        timeText = (EditText) findViewById(R.id.text_time);
        timeText.setFocusable(false);
        MainActivityPermissionsDispatcher.getPhoneInfoWithPermissionCheck(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.READ_PHONE_STATE)
    void showRationaleForReadPhoneState(PermissionRequest request) {
        showRationaleDialog(R.string.perm_phone_state_read, request);
    }

    @OnPermissionDenied(Manifest.permission.READ_PHONE_STATE)
    void onDeniedForReadPhoneState() {
        Toast.makeText(this, R.string.phonestate_denied, Toast.LENGTH_SHORT).show();
    }


    @NeedsPermission(Manifest.permission.READ_PHONE_STATE)
    public void getPhoneInfo() {
        db = new PhoneDB(this);
        DeviceName.with(this).request(new DeviceName.Callback() {
            @Override
            public void onFinished(DeviceName.DeviceInfo info, Exception error) {
                Constants.deviceModel = info.model;
                if (info.marketName.equals(info.model)) {
                    Constants.deviceMarketName = null;
                } else {
                    Constants.deviceMarketName = info.manufacturer + " " + info.marketName;
                }
                Constants.deviceUniqueID = getDeviceIMEI();

                if ((info.marketName.equals(Build.MODEL)) && !isOnline()) {
                    moreDeviceInfo();
                }

            }
        });
    }

    private void moreDeviceInfo() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(R.string.more_device_info)
                .show();

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    List<String> LAeqHistory = new ArrayList<>();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case Constants.ACTION.DBA_DBC_BROADCAST_ACTION:
                    double dBA = intent.getDoubleExtra("dBA", 0);
                    double dBC = intent.getDoubleExtra("dBC_max", 0);
                    dBCText.setText((int) dBC + "dBCmax");

                    // progressbar value checks
                    if (dBA < 0) {
                        dBA = 0;
                    }
                    if (dBA > progressBar.getMax()) {
                        dBA = progressBar.getMax();
                    }
                    ObjectAnimator.ofInt(progressBar, "progress", (int) dBA).start();
                    dBAText.setText(String.valueOf((int) dBA) + "dBA");
                    Log.d("MAIN", String.valueOf(dBA));
                    break;
                case Constants.ACTION.LAEQ_BROADCAST_ACTION:
                    double lAeq = intent.getDoubleExtra("LAeq", 0);
                    lAeqText.setText(String.format("LAeq: %.1fdB",lAeq));
                    //lAeqText.setText("LAeq: " + (int) lAeq + "dB");
                    LAeqHistory.add(String.format("%.1f",lAeq));
                    setRecommendationTexts(lAeq);
                    int measLengthSec = intent.getIntExtra("measLength",0);
                    int min = measLengthSec / 60;
                    int sec = measLengthSec - (min*60);
                    int h = min / 60;
                    min = min - (h*60);
                    timeText.setText(String.format("%02d:%02d:%02d",h,min,sec));
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    if (isOnline()) {
                        MainActivityPermissionsDispatcher.getPhoneInfoWithPermissionCheck(target);
                    }
                    break;
                case Constants.ACTION.RECORDERSTOPPED_ACTION:
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor prefEditor = prefs.edit();
                    prefEditor.putString(Constants.LAEQ_HISTORY,LAeqHistory.toString());
                    int spl_rms = Math.round(Float.valueOf(LAeqHistory.get(LAeqHistory.size()-1)));
                    Log.d("spl_rms",String.valueOf(spl_rms));
                    prefEditor.putString(Constants.LAEQ_LAST,String.valueOf(spl_rms));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        prefEditor.apply();
                    } else {
                        prefEditor.commit();
                    }
                    LAeqHistory.clear();
                    Intent formIntent = new Intent(context, FormActivity.class);
                    startActivity(formIntent);
                    break;
                default:
                    break;
            }

        }
    };

    private void setRecommendationTexts(double lAeq) {
        if(lAeq <= 75){
            u3Text.setText(R.string.table_no_limit);
            b314Text.setText(R.string.table_no_limit);
            b1418Text.setText(R.string.table_no_limit);
            lAeqText.setBackgroundColor(Color.GREEN);
        }
        if(lAeq > 75 && lAeq <= 80){
            u3Text.setText(R.string.table_notrecomm);
            b314Text.setText(R.string.table_max_2);
            b1418Text.setText(R.string.table_no_limit);
            lAeqText.setBackgroundColor(Color.YELLOW);
        }
        if(lAeq > 80 && lAeq <= 85){
            u3Text.setText(R.string.table_notrecomm);
            b314Text.setText(R.string.table_max_45);
            b1418Text.setText(R.string.table_no_limit);
            lAeqText.setBackgroundColor(Color.RED);
        }
        if(lAeq > 85 && lAeq <= 90){
            u3Text.setText(R.string.table_notrecomm);
            b314Text.setText(R.string.table_notrecomm);
            b1418Text.setText(R.string.table_max_2);
        }
        if(lAeq > 90 && lAeq <= 95){
            u3Text.setText(R.string.table_notrecomm);
            b314Text.setText(R.string.table_notrecomm);
            b1418Text.setText(R.string.table_max_45);
        }
        if(lAeq > 95){
            u3Text.setText(R.string.table_notrecomm);
            b314Text.setText(R.string.table_notrecomm);
            b1418Text.setText(R.string.table_notrecomm);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                if (!RecorderService.isAlive) {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.settings_info, Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.DBA_DBC_BROADCAST_ACTION);
        filter.addAction(Constants.ACTION.LAEQ_BROADCAST_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Constants.ACTION.RECORDERSTOPPED_ACTION);
        registerReceiver(broadcastReceiver, filter);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Group u3Group = (Group) findViewById(R.id.group_u3);
        Group b314Group = (Group) findViewById(R.id.group_3_14);
        Group b1418Group = (Group) findViewById(R.id.group_14_18);

        if(prefs.getBoolean("u3",true)){
            u3Group.setVisibility(View.VISIBLE);
        } else {
            u3Group.setVisibility(View.GONE);
        }
        if(prefs.getBoolean("b314",true)){
            b314Group.setVisibility(View.VISIBLE);
        } else {
            b314Group.setVisibility(View.GONE);
        }
        if(prefs.getBoolean("b1418",true)){
            b1418Group.setVisibility(View.VISIBLE);
        }else {
            b1418Group.setVisibility(View.GONE);
        }
        String calibPref = prefs.getString(Constants.CALIBTYPE,CalibrationType.NOT_CALIBRATED.toString());
        Constants.calibrationType = CalibrationType.valueOf(calibPref);
        TextView calibText = (TextView) findViewById(R.id.text_calib);
        switch(Constants.calibrationType){
            case NOT_CALIBRATED:
                calibText.setText(R.string.not_calib);
                break;
            case MODELLY_CALIBRATED:
                calibText.setText(R.string.model_calib);
                break;
            case UNIQUELY_CALIBRATED:
                calibText.setText(R.string.uniq_calib);
                break;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private void showRationaleDialog(@StringRes int messageResId, final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("DENY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(messageResId)
                .show();
    }

    @Override
    public void onClick(View v) {
        MainActivityPermissionsDispatcher.buttonHandlingWithPermissionCheck(this, v);
    }

    @NeedsPermission({Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void buttonHandling(View v) {

        switch (v.getId()) {
            case R.id.btn_start:
                if (!RecorderService.isAlive) {
                    Intent startIntent = new Intent(MainActivity.this, RecorderService.class);
                    startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startService(startIntent);

                } else {
                    Toast.makeText(this, R.string.rec_started, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_stop:
                if (RecorderService.isAlive) {
                    Intent stopIntent = new Intent(MainActivity.this, RecorderService.class);
                    stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    startService(stopIntent);
                } else {
                    Toast.makeText(this, R.string.rec_stopped, Toast.LENGTH_LONG).show();
                }
                break;


            default:
                break;
        }
    }


    /**
     * Returns the unique identifier for the device
     *
     * @return unique identifier for the device
     */
    private String getDeviceIMEI() {
        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 23) { // Marshmallow

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, Constants.PERMISSION_ALL);
                }
            }
            deviceUniqueIdentifier = tm.getDeviceId();
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceUniqueIdentifier;
    }


}
