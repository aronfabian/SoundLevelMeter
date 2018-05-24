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
    private final int REC_BUFFERSIZE = 2 * AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT);
    private short[] buffer = new short[REC_BUFFERSIZE / 2]; //REC_BUFFERSIZE / 4
    private short[] bufferC = new short[REC_BUFFERSIZE / 2]; //REC_BUFFERSIZE / 4
    private boolean isRunning = false;
    private AudioRecord recorder;
    private DataOutputStream wavOut = null;
    private FileOutputStream infoOut = null;
    private File wavFile = null;
    private File infoFile = null;
    public static boolean saveFile = false;
    private final Object lock = new Object();
    public static boolean isAlive = false;
    private final Intent instIntent = new Intent(Constants.ACTION.DBA_DBC_BROADCAST_ACTION);
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
    private long rmsSquareC;
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
        Log.d(LOG_TAG, "Recording buffersize in Bytes:  " + Integer.toString(REC_BUFFERSIZE));
        Log.d(LOG_TAG, "Temporary buffersize in Samples:  " + Integer.toString(buffer.length));
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

        recorder = new AudioRecord(AUDIOSOURCE, SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT, REC_BUFFERSIZE);
        FileOutputStream os;
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
            infoFile = new File(dir, Constants.deviceUniqueID + "_info.txt");
            os = null;
        }
        try {
            if (saveFile) {
                infoOut = new FileOutputStream(infoFile);
                String infos = "Device Unique ID (IMEI): " + Constants.deviceUniqueID + "\nDevice Model: " + Constants.deviceModel + "\nMarket Name: " + Constants.deviceMarketName;
                infoOut.write(infos.getBytes());
                infoOut.close();
                os = new FileOutputStream(wavFile);
                wavOut = new DataOutputStream(os);
                WavHelper.writeWavHeader(wavOut, CHANNELCONFIG, SAMPLERATE, AUDIOFORMAT);
            }
            recorder.startRecording();
            isRunning = true;
            Thread processingThread = new Thread(new Runnable() {

                int nSamplesRead, maxSamplesToWrite;
                long total = 0;
                final byte[] b = new byte[2];
                final byte[] bBuffer = new byte[buffer.length * 2];
                float[] floats = new float[buffer.length];
                float[] floatsC = new float[buffer.length];
                Long tsLong1, tsLong2, dtsLong;
                String ts;

                @Override
                public void run() {

                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                    while (isRunning) {
                        synchronized (lock) {
                            tsLong1 = android.os.SystemClock.elapsedRealtimeNanos(); // System.currentTimeMillis();
                            nSamplesRead = recorder.read(buffer, 0, buffer.length);
                            tsLong2 = android.os.SystemClock.elapsedRealtimeNanos(); // System.currentTimeMillis();
                            dtsLong = (tsLong2 - tsLong1) / 1000000;
                            Log.d(LOG_TAG, Integer.toString(nSamplesRead) + " samples read.");
                            Log.d(LOG_TAG, "Wait time for read: " + dtsLong.toString() + " milli seconds");
//                            floats = shortToFloat(buffer);
//                            if (filterNumC != 0) {
//                                FilterPlugin.filterProcessingC(floats, floatsC, nSamplesRead);
//                            } else {
//                                floatsC = floats;
//                            }
//                            if (filterNumA != 0) {
//                                FilterPlugin.filterProcessingA(floats, floats, nSamplesRead);
//                            }
//                            buffer = floatToShort(floats);
//                            bufferC = floatToShort(floatsC);
//                            //Log.d(LOG_TAG, "Filtered " + Arrays.toString(buffer));

//                            calcRMS(nSamplesRead);

                            if (saveFile) {
                                // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                                if (total + nSamplesRead > 4294967295L) {
                                    // Write as many bytes as we can before hitting the max size
                                    maxSamplesToWrite = (int) (4294967295L - total);
                                    for (int i = 0; i < maxSamplesToWrite; i++) {
                                        try {
                                            b[0] = (byte) (buffer[i] & 0x00FF);
                                            b[1] = (byte) ((buffer[i] >> 8) & 0x00FF);
                                            wavOut.write(b, 0, 2);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        total += maxSamplesToWrite;
                                    }
                                } else {
                                    // Write out the entire read buffer
                                    try {
//                                        for (int i = 0; i < nSamplesRead; i++) {
//                                            b[0] = (byte) (buffer[i] & 0x00FF);
//                                            b[1] = (byte) ((buffer[i] >> 8) & 0x00FF);
//                                            wavOut.write(b, 0, 2);
//                                        }
                                        for (int i = 0; i < nSamplesRead; i++) {
                                            bBuffer[i << 1] = (byte) (buffer[i] & 0x00FF);
                                            bBuffer[(i << 1) + 1] = (byte) ((buffer[i] >> 8) & 0x00FF);
                                        }
                                        wavOut.write(bBuffer, 0, nSamplesRead << 1);

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    total += nSamplesRead;
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
                        offset = 112.35;
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
        } finally {
            if (infoOut != null) {
                try {
                    infoOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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