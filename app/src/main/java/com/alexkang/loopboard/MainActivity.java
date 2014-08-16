package com.alexkang.loopboard;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {
	
	private static final String PATH = Environment.getExternalStorageDirectory() + "/LoopBoard";
	private static final File DIR = new File(PATH);

    private SoundListAdapter mAdapter;

    private ArrayList<Integer> mSoundIds;
    protected ArrayList<File> mSounds;
    private SoundPool mSoundPool;
	private MediaRecorder mRecorder;
	private int i = 0; // Keeps track of how many samples were made.

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DIR.mkdirs();
		File noMedia = new File(PATH, ".nomedia");
		saveFile(noMedia);

        mSoundIds = new ArrayList<Integer>();
        mSounds = new ArrayList<File>();
        mSoundPool = new SoundPool(50, AudioManager.STREAM_MUSIC, 0);
        mRecorder = new MediaRecorder();

        ListView mSoundList = (ListView) findViewById(R.id.sound_list);
        mAdapter = new SoundListAdapter(this, mSoundIds, mSoundPool);
        mSoundList.setAdapter(mAdapter);

		Button recButton = (Button) findViewById(R.id.rec_button); // Record button.
		recButton.setOnTouchListener(new OnTouchListener() {
			/*
			 * onTouch buttons are used to record sound by holding down the button to
			 * record, and letting go to save.
			 */
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				int action = motionEvent.getAction();
				
				if (action == MotionEvent.ACTION_DOWN) {
                    try {
                        view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                        startRecording(i);
                    } catch (IllegalStateException e) {
                        return false;
                    }
				}
				else if (action == MotionEvent.ACTION_UP) {
                    try {
                        view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        stopRecording();
                    } catch (IllegalStateException e) {
                        Toast.makeText(getBaseContext(), "Unable to record sound", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    mSoundIds.add(mSoundPool.load(mSounds.get(i).getAbsolutePath(), 1));
                    mAdapter.notifyDataSetChanged();
                    i++;
				}
				
				return true;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_delete: // Deletes all local sounds on external storage.
				deleteAll();
				return true;
			case R.id.action_clear: // Stops all looped play backs AND removes all buttons.
                clear();
				return true;
			case R.id.action_stop: // Stops all looped play backs.
                stopAll();
				return true;
			default:
				return true;
		}
	}

	public void onPause() {
		super.onPause();

        stopAll();
	}
	
	protected void startRecording(int k) throws IllegalStateException {
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
            if (k >= mSounds.size()) {
                mSounds.add(k, new File(PATH, "Sample " + k + ".mp3"));
            } else {
                mSounds.set(k, new File(PATH, "Sample " + k + ".mp3"));
            }
		} catch (Exception e) {
			Log.e("fileError", "Creating file failed");
			return;
		}
		
		mRecorder.setOutputFile(mSounds.get(k).getAbsolutePath());

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e("prepareError", "prepare failed");
		}
		
		mRecorder.start();
	}
	
	protected void stopRecording() throws IllegalStateException {
        mRecorder.stop();
		mRecorder.reset();
	}

    private void saveFile(File f) {
        try {
            FileOutputStream output = new FileOutputStream(f);
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAll() {
        for (int streamId : mAdapter.getStreamIds()) {
            mSoundPool.stop(streamId);
        }
        mAdapter.notifyDataSetChanged();
    }
	
	private void clear() {
        stopAll();

        mSoundIds.clear();
		mSounds.clear();
        i = 0;

        mAdapter.clear();
	}
	
	private void deleteAll() {
		File[] files = DIR.listFiles();
		for (File file: files) {
			file.delete();
		}
        clear();
        Toast.makeText(this, "All local sounds deleted", Toast.LENGTH_SHORT).show();
	}
	
}
