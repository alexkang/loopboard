package com.alexkang.loopboard;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {
	
	private static final String PATH = Environment.getExternalStorageDirectory() + "/LoopBoard";
	private static final File DIR = new File(PATH);
    private static final int SAMPLE_RATE = 44100;
    private static final int MAX_TRACKS = 16;

    private SoundListAdapter mAdapter;

    protected ArrayList<byte[]> mSounds;
	private AudioRecord mRecorder;
    protected int mMinBuffer;

    protected long lastKnownTime = System.nanoTime();

    private boolean isRecording = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DIR.mkdirs();
		File noMedia = new File(PATH, ".nomedia");
        try {
            FileOutputStream output = new FileOutputStream(noMedia);
            output.write(0);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMinBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mSounds = new ArrayList<byte[]>();

        ListView mSoundList = (ListView) findViewById(R.id.sound_list);
        mAdapter = new SoundListAdapter(this, mSounds);
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
                    view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

                    if (((System.nanoTime() - lastKnownTime) / 1e6) < 300) {
                        lastKnownTime = System.nanoTime();
                        return false;
                    }
                    lastKnownTime = System.nanoTime();

                    startRecording(mSounds.size());
				}
				else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    stopRecording();
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
                mAdapter.stopAll();
				return true;
			default:
				return true;
		}
	}

	public void onPause() {
		super.onPause();

        mAdapter.stopAll();
	}
	
	protected void startRecording(int k) {
        if (k >= MAX_TRACKS) {
            Toast.makeText(this, "Cannot create any more sounds", Toast.LENGTH_SHORT).show();
            return;
        } else if (isRecording) {
            return;
        }

        isRecording = true;

        mRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mMinBuffer
        );

        mRecorder.startRecording();
		new RecordingThread(k).start();
	}

	protected void stopRecording() {
        new StopThread().start();
	}
	
	private void clear() {
        mAdapter.stopAll();
		mSounds.clear();
        mAdapter.clear();
	}
	
	private void deleteAll() {
		File[] files = DIR.listFiles();
		for (File file: files) {
            if (!file.getName().equals(".nomedia")) {
                file.delete();
            }
		}
        clear();
        Toast.makeText(this, "All local sounds deleted", Toast.LENGTH_SHORT).show();
	}

    private class RecordingThread extends Thread {

        private int index;

        public RecordingThread(int k) {
            index = k;
        }

        public void run() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[mMinBuffer];
            int b = 0;

            while (b < 14000) {
                try {
                    b += mRecorder.read(buffer, 0, mMinBuffer);
                } catch(NullPointerException e) {
                    return;
                }
            }

            while (isRecording) {
                mRecorder.read(buffer, 0, mMinBuffer);
                output.write(buffer, 0, mMinBuffer);
            }

            try {
                output.flush();
                byte[] byteArray = output.toByteArray();

                if (byteArray.length < 22050) {
                    return;
                }

                if (index >= mSounds.size()) {
                    mSounds.add(byteArray);
                } else {
                    mSounds.set(index, byteArray);
                }

                new SaveThread(byteArray, index).start();

                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class StopThread extends Thread {

        public void run() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            if (mRecorder != null) {
                isRecording = false;

                try {
                    mRecorder.stop();
                    mRecorder.release();
                } catch (IllegalStateException e) {}

                mRecorder = null;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

    }

    private class SaveThread extends Thread {

        byte[] soundByte;
        int index;

        public SaveThread(byte[] soundByte, int index) {
            this.soundByte = soundByte;
            this.index = index;
        }

        public void run() {
            File savedPCM = new File(PATH, "Sample " + index + ".pcm");

            try {
                FileOutputStream output = new FileOutputStream(savedPCM);
                output.write(soundByte);
                output.close();
            } catch (Exception e) {
                System.err.println("Sound unable to be saved.");
            }
        }

    }
	
}
