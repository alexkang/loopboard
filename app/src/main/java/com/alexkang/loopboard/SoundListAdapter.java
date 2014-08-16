package com.alexkang.loopboard;

import android.content.Context;
import android.media.SoundPool;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class SoundListAdapter extends ArrayAdapter<Integer> {

    private Context mContext;
    private ArrayList<Integer> mSoundIds;
    private SoundPool mSoundPool;

    private int[] streamIds = new int[50];
    private boolean[] isLooping = new boolean[50];

    public SoundListAdapter(Context context, ArrayList<Integer> soundIds, SoundPool soundPool) {
        super(context, R.layout.sound_clip_row, soundIds);

        mContext = context;
        mSoundIds = soundIds;
        mSoundPool = soundPool;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final int soundId = getItem(position);

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
                    mSoundPool.stop(streamIds[position]);
                    mSoundIds.set(position, mSoundPool.load(((MainActivity) mContext).mSounds.get(position).getAbsolutePath(), 1));
                    notifyDataSetChanged();
                }

                return true;
              }
        });

        playButton.setText("Sample " + position);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                mSoundPool.play(soundId, 1f, 1f, 1, 0, 1f);
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
                    streamIds[position] = mSoundPool.play(soundId, 1f, 1f, 1, -1, 1f);
                } else {
                    v.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_blue_dark));
                    mSoundPool.stop(streamIds[position]);
                }
                isLooping[position] = !isLooping[position];
            }

        });

        return convertView;
    }

    protected int[] getStreamIds() {
        return streamIds;
    }

}
