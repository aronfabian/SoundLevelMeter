package wavrecorder.com.fabian.aron.wavrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startButton = (Button) findViewById(R.id.btn_start);
        Button stopButton = (Button) findViewById(R.id.btn_stop);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        if(!hasPermissions(this, Constants.PERMISSIONS)){
            ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_ALL);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                Intent startIntent = new Intent(MainActivity.this, RecorderService.class);
                startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                startService(startIntent);
                break;
            case R.id.btn_stop:
                Intent stopIntent = new Intent(MainActivity.this, RecorderService.class);
                stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                startService(stopIntent);
                break;

            default:
                break;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
