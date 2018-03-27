package wavrecorder.com.fabian.aron.wavrecorder;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
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
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


/**
 * Created by Aron Fabian 2018. 03. 23..
 */

public class RecorderService extends Service {

    private static final String LOG_TAG = "RecorderService";
    private static final int SAMPLERATE = 44100;
    private static int CHANNELCONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static int AUDIOFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int AUDIOSOURCE;
    private int BUFFERSIZE = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT);
    private short[] buffer = new short[BUFFERSIZE];
    private boolean isRunning = false;
    private AudioRecord recorder;
    private DataOutputStream wavOut = null;
    private File wavFile = null;
    private final Object lock = new Object();


    static {
        System.loadLibrary("FilterProcess");
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
                Notification notification;
                Intent notifStopIntent = new Intent(this, RecorderService.class);
                notifStopIntent.setAction(Constants.ACTION.NOTIFSTOPFOREGROUND_ACTION);
                PendingIntent pStopIntent = PendingIntent.getService(this, 0, notifStopIntent, 0);
                NotificationCompat.Builder nBuilder =
                        (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                .setContentTitle("Recording...!")
                                .setOngoing(true)
                                .addAction(R.drawable.stop_icon,"Stop",pStopIntent);
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
                            updateWavHeader(wavFile);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }


                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    Log.i(LOG_TAG, "Received Stop Foreground Intent");
                    stopForeground(true);
                    mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
                    stopSelf();
                }
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Stop Recording", Toast.LENGTH_LONG).show();
    }

    /**
     * Creates an AudioRecorder instace and starts a thread to read and write audiodata.
     */
    private void startRecording() {

        FilterProcess(SAMPLERATE);
        addParametricFilter(1000, 0.5f, -20);

        recorder = new AudioRecord(AUDIOSOURCE, SAMPLERATE, CHANNELCONFIG, AUDIOFORMAT, 2 * BUFFERSIZE);
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/WavRecorder/");
        dir.mkdirs();
        wavFile = new File(dir, "RecordedAudio.wav");
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(wavFile);
            wavOut = new DataOutputStream(os);
            writeWavHeader(wavOut, CHANNELCONFIG, SAMPLERATE, AUDIOFORMAT);
            recorder.startRecording();
            isRunning = true;
            Thread fileWriterThread = new Thread(new Runnable() {
                int read;
                long total = 0;
                byte[] b = new byte[2];
                float[] floats = new float[buffer.length];

                @Override
                public void run() {

                    while (isRunning) {
                        synchronized (lock) {
                            read = recorder.read(buffer, 0, buffer.length);
                            //Log.d(LOG_TAG, "Original " + Arrays.toString(buffer));
                            floats = shortToFloat(buffer);
                            filterProcessing(floats, floats, read);
                            buffer = floatToShort(floats);
                            //Log.d(LOG_TAG, "Filtered " + Arrays.toString(buffer));
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
            });
            if (!fileWriterThread.isAlive()) {
                fileWriterThread.start();
            }
            Toast.makeText(this, "Start Recording", Toast.LENGTH_LONG).show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }


    }

    /**
     * Converts short array to float array.
     * @param pcms input array
     * @return output array
     */
    public static float[] shortToFloat(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    /**
     * Converts float array to short array.
     * @param floats input array
     * @return output array
     */
    public static short[] floatToShort(float[] floats) {
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
     * Sets channels and bitdepth parameters and calls another writeWavHeader() function.
     *
     * @param out
     * @param channelMask
     * @param sampleRate
     * @param encoding
     * @throws IOException
     */
    private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
        short channels;
        switch (channelMask) {
            case AudioFormat.CHANNEL_IN_MONO:
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
                channels = 2;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable channel mask");
        }

        short bitDepth;
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitDepth = 8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitDepth = 16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitDepth = 32;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable encoding");
        }

        writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out        The stream to write the header to
     * @param channels   The number of channels
     * @param sampleRate The sample rate in hertz
     * @param bitDepth   The bit depth
     * @throws IOException
     */
    private void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (must be updated later)
        });
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    private void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }


    /**
     * Creates C++ object in order to do some signal processing using SuperpoweredSDK
     *
     * @param samplerate
     */
    private native void FilterProcess(int samplerate);

    /**
     * Creates a parametric filter and add to the series.
     *
     * @param frequency   frequency in Hertz
     * @param octaveWidth bandwidth in octave
     * @param dbGain      gain in dB
     * @return number of filters in the series
     */
    private native int addParametricFilter(float frequency, float octaveWidth, float dbGain);

    /**
     * Process the input data with the predefined filters
     *
     * @param input           input buffer
     * @param output          output buffer (can be the same as input)
     * @param numberOfSamples number of samples
     */
    private native void filterProcessing(float[] input, float[] output, int numberOfSamples);


}