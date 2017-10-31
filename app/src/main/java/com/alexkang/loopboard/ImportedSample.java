package com.alexkang.loopboard;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

class ImportedSample extends Sample {

    private final Context context;
    private final File sampleFile;

    private MediaPlayer mediaPlayer;

    ImportedSample(Context context, File sampleFile) {
        this.context = context;
        this.sampleFile = sampleFile;
    }

    @Override
    String getName() {
        return sampleFile.getName();
    }

    @Override
    synchronized void play(boolean isLooped) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, Uri.parse(sampleFile.getAbsolutePath()));
        }
        mediaPlayer.setLooping(isLooped);
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }

    @Override
    synchronized void stop() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    synchronized boolean isLooping() {
        return mediaPlayer != null && mediaPlayer.isLooping();
    }

    @Override
    synchronized void shutdown() {
        stop();
    }
}
