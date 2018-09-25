package wavrecorder.com.fabian.aron.wavrecorder;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Created by Aron Fabian on 2018. 04. 11..
 */
public class UploadJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Toast.makeText(this, prefs.getString("LAeqHistory",""), Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
