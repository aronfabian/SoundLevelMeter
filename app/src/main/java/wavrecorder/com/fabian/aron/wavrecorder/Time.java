package wavrecorder.com.fabian.aron.wavrecorder;

import java.util.Calendar;

/**
 * Created by Aron Fabian on 2019. 02. 25..
 */
public class Time {
    private long time;

    public Time() {
        new Time(0);
    }

    public Time(long millis) {
        time = millis;
    }
    public void increment(long millis){
        time += millis;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTimeString(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String msecond = String.valueOf(Math.round((float)calendar.get(Calendar.MILLISECOND)/100));
        String result = String.valueOf(android.text.format.DateFormat.format("kk:mm:ss", new java.util.Date(time-60*60*1000)));
        if(calendar.get(Calendar.MILLISECOND)== 0){
            return result;
        }else {
            result += ","+msecond;
        }
        return result;
    }
}
