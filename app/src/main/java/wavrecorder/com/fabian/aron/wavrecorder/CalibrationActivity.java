package wavrecorder.com.fabian.aron.wavrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

/**
 * Created by Aron Fabian on 2018. 09. 15..
 */

@RuntimePermissions
public class CalibrationActivity extends Activity implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        Button startButton = (Button) findViewById(R.id.btn_start_calib);
        Button stopButton = (Button) findViewById(R.id.btn_stop_calib);
        Button sendButton = (Button) findViewById(R.id.btn_send_calib);
        Button downloadButton = (Button) findViewById(R.id.btn_download_calib);
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

    private void downloadCalibrationFile() {
        
    }

    private void sendWavFile() {
        String url = "";
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
        });
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/WavRecorder/");
        File wavFile  = lastFileModified(dir.getAbsolutePath());
        smr.addFile("param file", wavFile.getAbsolutePath());


        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mRequestQueue.add(smr);
    }

    public static File lastFileModified(String dir) {
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }
}
