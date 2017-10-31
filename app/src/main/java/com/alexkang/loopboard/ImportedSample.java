package com.alexkang.loopboard;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

class ImportedSample extends Sample {

    private final String name;
    private final MediaPlayer mediaPlayer;

    ImportedSample(Context context, File file) {
        name = file.getName();
        mediaPlayer = MediaPlayer.create(context, Uri.parse(file.getAbsolutePath()));
    }

    @Override
    String getName() {
        return name;
    }

    @Override
    synchronized void play(boolean isLooped) {
        mediaPlayer.setLooping(isLooped);
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }

    @Override
    synchronized void stop() {
        mediaPlayer.pause();
        mediaPlayer.setLooping(false);
    }

    @Override
    synchronized boolean isLooping() {
        return mediaPlayer.isLooping();
    }

    @Override
    synchronized void shutdown() {
        mediaPlayer.release();
    }
}
