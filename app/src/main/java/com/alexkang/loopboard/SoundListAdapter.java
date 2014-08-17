package com.alexkang.loopboard;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class SoundListAdapter extends ArrayAdapter<byte[]> {

    private static final int SAMPLE_RATE = 44100;

    private Context mContext;

    private AudioTrack[] mTracks = new AudioTrack[50];
    private AudioTrack[] mLoopedTracks = new AudioTrack[50];
    private boolean[] shouldReload = new boolean[50];
    private boolean[] isLooping = new boolean[50];

    public SoundListAdapter(Context context, ArrayList<byte[]> sounds) {
        super(context, R.layout.sound_clip_row, sounds);

        mContext = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final byte[] sound = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.sound_clip_row, parent, false);
        }

        Button record = (Button) convertView.findViewById(R.id.rerecord);
        final Button loopButton = (Button) convertView.findViewById(R.id.loop);
        Button playButton = (Button) convertView.findViewById(R.id.play);

        record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();

                if (action == MotionEvent.ACTION_DOWN) {
                    try {
                        view.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_red_dark));
                        ((MainActivity) mContext).startRecording(position);
                    } catch (IllegalStateException e) {
                        return false;
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    try {
                        view.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_red_light));
                        ((MainActivity) mContext).stopRecording();
                    } catch (IllegalStateException e) {
                        Toast.makeText(mContext, "Unable to record sound", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    loopButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_dark));
                    isLooping[position] = false;
                    shouldReload[position] = true;

                    try {
                        mLoopedTracks[position].stop();
                        mLoopedTracks[position].release();
                    } catch (Exception e) {}

                    notifyDataSetChanged();
                }

                return true;
              }
        });

        playButton.setText("Sample " + position);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                new PlayThread(sound, position).start();
            }
        });

        if (!isLooping[position]) {
            loopButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            loopButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_bright));
        }
        loopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick (View v){
                if (!isLooping[position]) {
                    v.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_bright));
                    new LoopThread(sound, position).start();
                } else {
                    while (true) {
                        if (mLoopedTracks[position].getState() == AudioTrack.STATE_INITIALIZED) {
                            v.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_dark));
                            mLoopedTracks[position].stop();
                            mLoopedTracks[position].release();
                            break;
                        }
                    }
                }
                isLooping[position] = !isLooping[position];
            }

        });

        return convertView;
    }

    protected void stopAll() {
        for (int i=0; i<mTracks.length; i++) {
            try {
                shouldReload[i] = true;
                mTracks[i].stop();
            } catch (Exception e1) {}

            try {
                mLoopedTracks[i].stop();
            } catch (Exception e2) {}

            isLooping[i] = false;
        }

        notifyDataSetChanged();
    }

    private class PlayThread extends Thread {

        private byte[] soundByte;
        private int index;

        public PlayThread(byte[] soundByte, int index) {
            this.soundByte = soundByte;
            this.index = index;
        }

        public void run() {
            if (mTracks[index] == null || shouldReload[index]) {
                mTracks[index] = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        soundByte.length,
                        AudioTrack.MODE_STATIC
                );

                shouldReload[index] = false;

                while (true) {
                    if (mTracks[index].getState() == AudioTrack.STATE_NO_STATIC_DATA) {
                        mTracks[index].write(soundByte, 0, soundByte.length);
                        mTracks[index].play();
                        break;
                    }
                }
            } else {
                mTracks[index].stop();
                mTracks[index].reloadStaticData();
                mTracks[index].play();
            }
        }

    }

    private class LoopThread extends Thread {

        private byte[] soundByte;
        private int index;

        public LoopThread(byte[] soundByte, int index) {
            this.soundByte = soundByte;
            this.index = index;
        }

        public void run() {
            mLoopedTracks[index] = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    soundByte.length,
                    AudioTrack.MODE_STATIC
            );

            while (true) {
                if (mLoopedTracks[index].getState() == AudioTrack.STATE_NO_STATIC_DATA) {
                    mLoopedTracks[index].write(soundByte, 0, soundByte.length);
                    mLoopedTracks[index].setLoopPoints(0, soundByte.length / 2, -1);
                    mLoopedTracks[index].play();
                    break;
                }
            }
        }

    }

}
