package com.alexkang.dynamicsoundboard;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;

public class MainActivity extends Activity {
	
	MediaRecorder mRecorder;
	File audioFile;
	MediaPlayer mPlayer;
	File[] clips = new File[50];
	int i = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button initButton = (Button) findViewById(R.id.init_button);
		initButton.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				int action = motionEvent.getAction();
				
				if (action == MotionEvent.ACTION_DOWN) {
					startRecording();
				}
				else if (action == MotionEvent.ACTION_UP) {
					stopRecording();
					final int k = i;
					
					ViewGroup layout = (ViewGroup) findViewById(R.id.layout);
					Button soundByte = new Button(getBaseContext());
					soundByte.setText("Sample " + k);
					soundByte.setOnClickListener(new Button.OnClickListener() {
						
						@Override
						public void onClick(View view) {
							playFile(clips[k]);
						}
					});
					layout.addView(soundByte);
					i++;
				}
				
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void playFile(File audio) {
		mPlayer = new MediaPlayer();
		mPlayer.setLooping(false);
		
		try {
			mPlayer.setDataSource(audio.getAbsolutePath());
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e("playbackError", "Media failed to play");
		}
	}
	
	public void stopFile(View view) {
		mPlayer.release();
	}
	
	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		
		File dir = Environment.getExternalStorageDirectory();
		
		try {
			clips[i] = File.createTempFile("ibm", ".3gp", dir);
		} catch (IOException e) {
			Log.e("fileError", "createTempFile failed");
			return;
		}
		
		mRecorder.setOutputFile(clips[i].getAbsolutePath());
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e("prepareError", "prepare failed");
		}
		
		mRecorder.start();
	}
	
	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
	}

}
