package wavrecorder.com.fabian.aron.wavrecorder;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Aron Fabian on 2018. 04. 11..
 */
public class UploadJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://ovdafuled.hu/fogadas.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                    }
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Map<String, String> params = new HashMap<>();
                params.put("type", prefs.getString(Constants.FORM_TYPE, ""));
                params.put("location", prefs.getString(Constants.FORM_LOCATION, ""));
                params.put("time", prefs.getString(Constants.FORM_TIME, ""));
                params.put("spl_rms", prefs.getString(Constants.LAEQ_LAST, ""));
                params.put("distance", prefs.getString(Constants.FORM_DISTANCE, ""));
                params.put("phone", Constants.deviceUniqueID);
                params.put("application", getString(R.string.app_name));
                params.put("loudness", prefs.getString(Constants.FORM_LOUDNESS, ""));
                params.put("comment", prefs.getString(Constants.FORM_COMMENT, ""));
                params.put("calibrated", (Constants.calibrationType.ordinal() > 0) ? "1" : "0");
                params.put("request_type", "insert");
                return params;
            }
        };
        queue.add(postRequest);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
