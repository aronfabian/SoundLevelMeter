package wavrecorder.com.fabian.aron.wavrecorder;

import android.Manifest;

/**
 * Created by Aron Fabian on 2018. 03. 23..
 */

public class Constants {
    public interface ACTION {

        String STARTFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.startforeground";
        String STOPFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.stopforeground";
        String NOTIFSTOPFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.notifstopforeground";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 0;
    }

    public static int PERMISSION_ALL = 1;
    public static String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

}
