package com.alexkang.loopboard;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class Utils {

    static final int MAX_SAMPLES = 24;
    static final String IMPORTED_SAMPLE_PATH =
            Environment.getExternalStorageDirectory() + "/LoopBoard";
    static final int SAMPLE_RATE_HZ = 44100;
    static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    private static final String TAG = "Utils";
    private static final String[] IMPORTED_SAMPLE_TYPES = {"wav", "mp3", "mp4", "m4a"};

    /** Returns whether or not the given audio file can be played by this app. */
    static boolean isSupportedSampleFile(File file) {
        String fileName = file.getName();
        for (String type : IMPORTED_SAMPLE_TYPES) {
            // We use the file extension to determine the audio file type.
            if (fileName.toLowerCase().endsWith(String.format(".%s", type))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Saves an audio recording under the given name.
     *
     * @return whether or not the file was successfully saved
     */
    static boolean saveRecording(Context context, String name, byte[] recordedBytes) {
        try {
            FileOutputStream output = context.openFileOutput(name, Context.MODE_PRIVATE);
            output.write(recordedBytes);
            output.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, String.format("Failed to save recording %s", name));
        }
        return false;
    }
}
