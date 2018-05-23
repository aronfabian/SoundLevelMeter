package wavrecorder.com.fabian.aron.wavrecorder;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Created by Aron Fabian 2018. 03. 23..
 */

public class RecorderService extends Service {

    private static final String LOG_TAG = "RecorderService";
    private static final int SAMPLERATE = 44100;
    private static final int CHANNELCONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIOFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int AUDIOSOURCE;
    public static String classType = Constants.MEASUREMENT_CLASS.CLASS_ONE;
    private final int BUFFERSIZE = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT);
    private short[] buffer = new short[BUFFERSIZE];
    private short[] bufferC = new short[BUFFERSIZE];
    private boolean isRunning = false;
    private AudioRecord recorder;
    private DataOutputStream wavOut = null;
    private FileOutputStream os;
    private File wavFile = null;
    public static boolean saveFile = false;
    private final Object lock = new Object();
    public static boolean isAlive = false;
    private final Intent instIntent = new Intent(Constants.ACTION.DBA_BROADCAST_ACTION);
    private final Intent meanIntent = new Intent(Constants.ACTION.LAEQ_BROADCAST_ACTION);
    private int secCount = 0;
    private long sampleCount = 0;
    private static int rmsUpdateTime = SAMPLERATE;
    public static int filterNumA = 0;
    public static int filterNumC = 0;
    private double dBA;
    private double dBC = 0;
    private double lAeq = 0;
    private long sumSquaresA = 0;
    private long sumSquaresC;
    private long rmsSquareA = 0;
    public long rmsSquareC;
    private long sumRmsSquareA = 0;
    private int measLength = 0;

    static {
        if (MediaRecorder.getAudioSourceMax() >= 9) {
            AUDIOSOURCE = MediaRecorder.AudioSource.UNPROCESSED;
        } else if (MediaRecorder.getAudioSourceMax() >= 6) {
            AUDIOSOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
        } else {
            AUDIOSOURCE = MediaRecorder.AudioSource.MIC;
        }
    }

    public RecorderService() {
        super();
        isAlive = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
                RecorderService.isAlive = true;
                Notification notification;
                Intent notifStopIntent = new Intent(this, RecorderService.class);
                notifStopIntent.setAction(Constants.ACTION.NOTIFSTOPFOREGROUND_ACTION);
                PendingIntent pStopIntent = PendingIntent.getService(this, 0, notifStopIntent, 0);
                NotificationCompat.Builder nBuilder =
                        (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                .setContentTitle("Recording...!")
                                .setOngoing(true)
                                .addAction(R.drawable.stop_icon, "Stop", pStopIntent);
                notification = nBuilder.build();
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
                Log.d(LOG_TAG, "Received Start Foreground Intent");

                startRecording();

            } else if ((intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) ||
                    (intent.getAction().equals(Constants.ACTION.NOTIFSTOPFOREGROUND_ACTION))) {

                isRunning = false;
                synchronized (lock) {
                    if (recorder != null) {
                        try {
                            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                recorder.stop();
                                FilterPlugin.filterProcessDelete();
                            }
                        } catch (IllegalStateException ex) {
                            ex.printStackTrace();
                        }
                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                            recorder.release();
                        }
                    }
                    if (wavOut != null) {
                        try {
                            wavOut.close();
                            WavHelper.updateWavHeader(wavFile);
                            Log.i(LOG_TAG, "Update Wav Header");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }


                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    Log.i(LOG_TAG, "Received Stop Foreground Intent");
                    stopForeground(true);
                    mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
                    JobScheduler jobScheduler =
                            (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.schedule(new JobInfo.Builder(4,
                            new ComponentName(this, UploadJobService.class))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
                            .setRequiresCharging(false)
                            .build());
                    stopSelf();
                }
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, R.string.stop_meas, Toast.LENGTH_LONG).show();
        isAlive = false;
    }

    /**
     * Creates an AudioRecorder instace and starts a thread to read and write audiodata.
     */
    private void startRecording() {
        FilterPlugin.filterProcessCreate(SAMPLERATE);
        FilterPlugin.setFilters(this, classType);

        recorder = new AudioRecord(AUDIOSOURCE, SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT, 2 * BUFFERSIZE);
        if (saveFile) {
            File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/WavRecorder/");
            dir.mkdirs();
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String year = String.valueOf(calendar.get(java.util.Calendar.YEAR));
            String month = String.valueOf(calendar.get(java.util.Calendar.MONTH) + 1);
            String day = String.valueOf(calendar.get(java.util.Calendar.DAY_OF_MONTH));
            String hour = String.valueOf(calendar.get(java.util.Calendar.HOUR_OF_DAY));
            String minute = String.valueOf(calendar.get(java.util.Calendar.MINUTE));
            String second = String.valueOf(calendar.get(java.util.Calendar.SECOND));
            wavFile = new File(dir, Constants.deviceUniqueID + "_" + year + "_" + month + "_" + day + "_" + hour + "_" + minute + "_" + second + ".wav");
            os = null;
        }
        try {
            if (saveFile) {
                os = new FileOutputStream(wavFile);
                wavOut = new DataOutputStream(os);
                WavHelper.writeWavHeader(wavOut, CHANNELCONFIG, SAMPLERATE, AUDIOFORMAT);
            }
            recorder.startRecording();
            isRunning = true;
            Thread processingThread = new Thread(new Runnable() {

                int read;
                long total = 0;
                final byte[] b = new byte[2];
                float[] floats = new float[buffer.length];
                float[] floatsC = new float[buffer.length];

                @Override
                public void run() {

                    while (isRunning) {
                        synchronized (lock) {
                            read = recorder.read(buffer, 0, buffer.length);
                            //Log.d(LOG_TAG, "Original " + Arrays.toString(buffer));
                            floats = shortToFloat(buffer);
                            if (filterNumC != 0) {
                                FilterPlugin.filterProcessingC(floats, floatsC, read);
                            } else {
                                floatsC = floats;
                            }
                            if (filterNumA != 0) {
                                FilterPlugin.filterProcessingA(floats, floats, read);
                            }
                            buffer = floatToShort(floats);
                            bufferC = floatToShort(floatsC);
                            //Log.d(LOG_TAG, "Filtered " + Arrays.toString(buffer));

                            calcRMS(read);

                            if (saveFile) {
                                // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                                if (total + read > 4294967295L) {
                                    // Write as many bytes as we can before hitting the max size
                                    for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
                                        try {
                                            b[0] = (byte) (buffer[i] & 0x00FF);
                                            b[1] = (byte) ((buffer[i] >> 8) & 0x00FF);
                                            wavOut.write(b, 0, 2);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    // Write out the entire read buffer
                                    try {
                                        for (int i = 0; i < read; i++) {
                                            b[0] = (byte) (buffer[i] & 0x00FF);
                                            b[1] = (byte) ((buffer[i] >> 8) & 0x00FF);
                                            wavOut.write(b, 0, 2);
                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    total += read;
                                }
                            }
                        }
                    }
                }

                private void calcRMS(int length) {

//                  UNPROCESSED:
//                      Audio input sensitivity MUST be set such that a 1000 Hz sinusoidal tone source
//                      played at 94 dB Sound Pressure Level (SPL) yields a response with RMS of 520
//                      for 16 bit-samples (or -36 dB Full Scale for floating point/double precision samples

//                  VOICE_RECOGNITION AND MIC:
//                      Audio input sensitivity SHOULD be set such that a 90 dB sound power level
//                      (SPL) source at 1000 Hz yields RMS of 2500 for 16-bit samples.
                    double offset;
                    if (AUDIOSOURCE == MediaRecorder.AudioSource.UNPROCESSED) {
                        offset = 129.98;
                    } else {
                        //                       offset = 112.35;
                        offset = 120.00;
                    }
                    long dBBase = 32768 * 32768;

                    int i = 0;
                    for (short value : Arrays.copyOfRange(buffer, 0, length - 1)) {
                        sumSquaresA += (long) value * (long) value;
                        sumSquaresC += (long) bufferC[i] * (long) bufferC[i];
                        i++;
                        sampleCount++;
                        if (sampleCount >= rmsUpdateTime) {
                            if (sumSquaresA == 0) {
                                dBA = 0;
                            } else {
                                rmsSquareA = sumSquaresA / sampleCount;
                                sumRmsSquareA += rmsSquareA;
                                dBA = (10 * Math.log10((double) rmsSquareA / dBBase)) + offset;
                            }
                            if (sumSquaresC == 0) {
                            } else {
                                double temp = (10 * Math.log10((double) rmsSquareC / dBBase)) + offset;
                                rmsSquareC = sumSquaresC / sampleCount;
                                if (temp > dBC) {
                                    dBC = temp;
                                    instIntent.putExtra("dBC_max", dBC);
                                }
                            }
                            sampleCount = 0;
                            sumSquaresA = 0;
                            sumSquaresC = 0;
                            instIntent.removeExtra("dBA");
                            instIntent.putExtra("dBA", dBA);
                            sendBroadcast(instIntent);
                        }
                        secCount++;
                        if (secCount == SAMPLERATE) {
                            measLength++;
                            secCount = 0;
                            lAeq = (10 * Math.log10((double) sumRmsSquareA / measLength / dBBase)) + offset;
                            meanIntent.putExtra("LAeq", lAeq);
                            sendBroadcast(meanIntent);
                        }

                    }


                }
            });
            if (!processingThread.isAlive()) {
                processingThread.start();
            }
            Toast.makeText(this, R.string.start_meas, Toast.LENGTH_LONG).show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }


    }

    /**
     * Converts short array to float array.
     *
     * @param pcms input array
     * @return output array
     */
    private static float[] shortToFloat(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    /**
     * Converts float array to short array.
     *
     * @param floats input array
     * @return output array
     */
    private static short[] floatToShort(float[] floats) {
        short[] shorts = new short[floats.length];
        for (int i = 0; i < floats.length; i++) {
            shorts[i] = (short) Math.round(floats[i]);
            if (floats[i] < Short.MIN_VALUE) {
                shorts[i] = Short.MIN_VALUE;
            }
            if (floats[i] > Short.MAX_VALUE) {
                shorts[i] = Short.MAX_VALUE;
            }
        }
        return shorts;
    }

    /**
     * Sets RMS calculation window width
     *
     * @param s "sec" = 1s or "milli" = 100ms
     */
    public static void setRmsUpdateTime(String s) {
        if (s.equals("sec")) {
            rmsUpdateTime = SAMPLERATE;
        } else if (s.equals("milli")) {
            rmsUpdateTime = SAMPLERATE / 10;
        }
    }


}