package wavrecorder.com.fabian.aron.wavrecorder;

import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Created by Aron Fabian on 2018. 04. 11..
 */
public class UploadJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
//        Toast.makeText(this, "UploadJob started", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
