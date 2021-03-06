package wavrecorder.com.fabian.aron.wavrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;


/**
 * Created by Aron Fabian on 2018. 09. 15..
 */

@RuntimePermissions
public class CalibrationActivity extends AppCompatActivity implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        Button startButton = findViewById(R.id.btn_start_calib);
        Button stopButton = findViewById(R.id.btn_stop_calib);
        Button sendButton = findViewById(R.id.btn_send_calib);
        Button downloadButton = findViewById(R.id.btn_download_calib);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CalibrationActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    @Override
    public void onClick(View v) {
        CalibrationActivityPermissionsDispatcher.buttonHandlingWithPermissionCheck(this, v);
    }

    @NeedsPermission({Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void buttonHandling(View v) {

        switch (v.getId()) {
            case R.id.btn_start_calib:
                if (!RecorderService.isAlive) {
                    Intent startIntent = new Intent(CalibrationActivity.this, RecorderService.class);
                    startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startIntent.putExtra("CalibrationMode",true);
                    CheckBox filterCheckBox = findViewById(R.id.check_filters);
                    startIntent.putExtra("useFilters",filterCheckBox.isChecked());
                    startService(startIntent);

                } else {
                    Toast.makeText(this, R.string.rec_started, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_stop_calib:
                if (RecorderService.isAlive) {
                    Intent stopIntent = new Intent(CalibrationActivity.this, RecorderService.class);
                    stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    startService(stopIntent);
                } else {
                    Toast.makeText(this, R.string.rec_stopped, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_send_calib:
                if (!RecorderService.isAlive) {
                    sendWavFile();
                } else {
                    //TODO: ha még megy a mérés valamit ki kell írni
                    //Toast.makeText(this, R.string.rec_stopped, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_download_calib:
                if(!RecorderService.isAlive){
                    downloadCalibrationFile();
                } else {
                    //TODO: ha még megy a mérés valamit ki kell írni
                }


            default:
                break;
        }
    }
    JSONObject bestMatchPhone = null;
    private void downloadCalibrationFile() {
        String url = "https://last.hit.bme.hu/ovdafuled/PhoneDatabase.json";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,url,null, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                //Log.d("JSON: ",response.toString());
                try{
                    // Get the JSON array
                    JSONArray array = response.getJSONArray("Phones");

                    // Loop through the array elements

                    for(int i=0;i<array.length();i++){
                        JSONObject phone = array.getJSONObject(i);
                        String marketName = phone.getString("market_name");
                        String modelName =  phone.getString("model");
                        String imei =  phone.getString("imei");
                        String phoneId =  phone.getString("phone_id");

                       if(marketName.equals(Constants.deviceMarketName)){
                           Constants.calibrationType = CalibrationType.MODELLY_CALIBRATED;
                           bestMatchPhone = new JSONObject(phone.toString());
                       }
                       if(modelName.equals(Constants.deviceModel)){
                           Constants.calibrationType = CalibrationType.MODELLY_CALIBRATED;
                           bestMatchPhone = new JSONObject(phone.toString());
                       }
                       if(phoneId.equals(Constants.deviceUniqueID) || imei.equals(Constants.deviceUniqueID)){
                           Constants.calibrationType = CalibrationType.UNIQUELY_CALIBRATED;
                           bestMatchPhone = new JSONObject(phone.toString());
                       }

                    }
                    if(Constants.calibrationType != CalibrationType.NOT_CALIBRATED){
                        saveCalibrationData();
                        Toast.makeText(getApplicationContext(),R.string.calib_data_downloaded,Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(),R.string.no_calib_data,Toast.LENGTH_LONG).show();

                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error [" + error + "]");
            }
        });
        RequestQueue requestQueue  = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }

    private void saveCalibrationData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(Constants.CALIBTYPE,Constants.calibrationType.toString());
        try {
            if(bestMatchPhone != null){
                if (bestMatchPhone.has("full_scale_spl")){
                    prefEditor.putFloat("fullScaleSPL",Float.valueOf(bestMatchPhone.getString("full_scale_spl")));
                } else  {
                    prefEditor.putFloat("fullScaleSPL",0);
                }

                prefEditor.putString("classOneCalibData",bestMatchPhone.getString("Class1"));
                prefEditor.putString("classTwoCalibData",bestMatchPhone.getString("Class2"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefEditor.apply();

    }

    private void sendWavFile() {
        String url = "https://last.hit.bme.hu/ovdafuled/ovd_a_fuled_calibration_upload.php";
        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        //Toast.makeText(getApplicationContext(), R.string.alert_comment_sukses, Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error Response", error.getMessage());
                //Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();

            }
        }
        );
        //smr.addStringParam("USER","123");

        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/Zajszintmero/");
        //File wavFile  = lastFileModified(dir.getAbsolutePath());
        File wavFile  = new File(dir, Constants.fileName + ".wav");
        smr.addFile("wavFile", wavFile.getAbsolutePath());

        File infoFile = new File(dir, Constants.fileName + ".txt");
        smr.addFile("infoFile", infoFile.getAbsolutePath());
        Log.d("MULTIPART: ",smr.toString());

        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mRequestQueue.add(smr);

    }

}
