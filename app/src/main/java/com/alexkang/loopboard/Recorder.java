package com.alexkang.loopboard;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Recorder {

    private static final int AUDIO_CUTOFF_LENGTH = 12000;
    private static final int MIN_RECORDING_SIZE = 22000;
    private static final String TAG = "Recorder";

    private final ExecutorService recordExecutor = Executors.newSingleThreadExecutor();

    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;

    interface RecorderCallback {
        void onAudioRecorded(byte[] recordedBytes);
    }

    Recorder() {
        refresh();
    }

    synchronized void startRecording(RecorderCallback recorderCallback) {
        if (isRecording) {
            Log.d(TAG, "startRecording called while another recording is in progress");
            return;
        }

        isRecording = true;
        recordExecutor.execute(() -> {
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                isRecording = false;
                Log.e(TAG, "startRecording failed because the AudioRecord was uninitialized");
                return;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[Utils.MIN_BUFFER_SIZE];

            // Remove a small first chunk of the recording to avoid the sound of the user tapping
            // the button.
            audioRecord.read(
                    new byte[AUDIO_CUTOFF_LENGTH], 0, AUDIO_CUTOFF_LENGTH);

            // Keep recording until stopRecording() is invoked.
            while (isRecording) {
                audioRecord.read(buffer, 0, Utils.MIN_BUFFER_SIZE);
                output.write(buffer, 0, Utils.MIN_BUFFER_SIZE);
            }

            try {
                output.flush();
                byte[] recordedBytes = output.toByteArray();
                output.close();

                // Discard this recording if it was too short.
                if (recordedBytes.length < MIN_RECORDING_SIZE) {
                    return;
                }

                recorderCallback.onAudioRecorded(recordedBytes);
            } catch (IOException e) {
                Log.e(TAG, "Error while ending a recording");
            }
        });
    }

    synchronized void stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "stopRecording called even though no recordings are in progress");
            return;
        }

        // Mark ourselves as not recording so the ongoing recording knows to stop.
        isRecording = false;
    }

    synchronized void refresh() {
        if (audioRecord != null) {
            audioRecord.release();
        }
        audioRecord =
                new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        Utils.SAMPLE_RATE_HZ,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Utils.MIN_BUFFER_SIZE);
    }

    synchronized void shutdown() {
        recordExecutor.shutdown();
        audioRecord.release();
    }
}
