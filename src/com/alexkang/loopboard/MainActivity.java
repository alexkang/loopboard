package com.alexkang.loopboard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	private static final String PATH = Environment.getExternalStorageDirectory() + "/LoopBoard";
	private static final File DIR = new File(PATH);
	private MediaRecorder mRecorder;
	private ArrayList<File> clips = new ArrayList<File>();
	private int i = 0; // Keeps track of how many samples were made.

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DIR.mkdirs();
		File noMedia = new File(PATH, ".nomedia");
		saveFile(noMedia);
		
		Button recButton = (Button) findViewById(R.id.rec_button); // Record button.
		
		recButton.setOnTouchListener(new OnTouchListener() {
			
			/*
			 * onTouch buttons are used to record sound by holding down the button to
			 * record, and letting go to save.
			 */
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				int action = motionEvent.getAction();
				final int k = i;
				final MediaPlayer mPlayer = new MediaPlayer();
				
				if (action == MotionEvent.ACTION_DOWN) {
					startRecording(k);
				}
				else if (action == MotionEvent.ACTION_UP) {					
					stopRecording();
					
					// Button row is made by putting Button objects in a LinearLayout.
					final ViewGroup row = (ViewGroup) new LinearLayout(getBaseContext());
					final ViewGroup layout = (ViewGroup) findViewById(R.id.layout);
					final Button soundByte = new Button(getBaseContext());
					Button reRecSound = new Button(getBaseContext());
					ToggleButton loop = new ToggleButton(getBaseContext());
					
					// Setting aesthetics.
					LayoutParams layoutParams = new LayoutParams(
							LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
					LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					rowParams.setMargins(0, 0, 0, 5);
					
					Typeface slabLight = Typeface.createFromAsset(getAssets(), "RobotoSlab-Light.ttf");
					Typeface slabRegular = Typeface.createFromAsset(getAssets(), "RobotoSlab-Regular.ttf");
					Typeface slabBold = Typeface.createFromAsset(getAssets(), "RobotoSlab-Bold.ttf");
					
					row.setLayoutParams(rowParams);
					soundByte.setTextColor(Color.WHITE);
					soundByte.setBackgroundColor(Color.parseColor("#38b2ce"));
					soundByte.setTypeface(slabLight);
					soundByte.setLayoutParams(new LayoutParams(
							300, LayoutParams.WRAP_CONTENT));
					reRecSound.setBackgroundColor(Color.parseColor("#04819e"));
					reRecSound.setTypeface(slabBold);
					reRecSound.setLayoutParams(layoutParams);
					reRecSound.setText("rec");
					loop.setBackgroundColor(Color.parseColor("#60b9ce"));
					loop.setTypeface(slabRegular);
					loop.setLayoutParams(layoutParams);
					loop.setText("loop!");
					loop.setTextOn("don't loop!");
					loop.setTextOff("loop!");
					
					// Labeling the samples.
					if (k < 9) {
						soundByte.setText("Sample 0" + (k+1));
					}
					else {
						soundByte.setText("Sample " + (k+1));
					}
					
					// Normal play back.
					soundByte.setOnClickListener(new Button.OnClickListener() {
						
						/*
						 * A new MediaPlayer is constructed to simultaneously play loops
						 * along with regular play back.
						 */
						@Override
						public void onClick(View view) {
							MediaPlayer mPlayerReg = new MediaPlayer();
							playFile(clips.get(k), mPlayerReg, false);
						}
					});
				
					// Re-Record audio on a certain button.
					reRecSound.setOnTouchListener(new OnTouchListener() {
						
						@Override
						public boolean onTouch(View view, MotionEvent motionEvent) {
							int action = motionEvent.getAction();
							if (action == MotionEvent.ACTION_DOWN) {
								startRecording(k);
							}
							else if (action == MotionEvent.ACTION_UP) {
								stopRecording();
							}
							return false;
						}
					});
					
					// Looped play back.
					loop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (isChecked) {
								playFile(clips.get(k), mPlayer, true);
							}
							else if (!isChecked) {
								stopFile(mPlayer);
							}
						}
					});
					
					row.addView(reRecSound);
					row.addView(soundByte);
					row.addView(loop);
					layout.addView(row);
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		switch (item.getItemId()) {
			case R.id.action_delete: // Deletes all local sounds on external storage.
				clear(layout);
				deleteAll();
				return true;
			case R.id.action_clear: // Stops all looped play backs AND removes all buttons.
				clear(layout);
				return true;
			case R.id.action_stop: // Stops all looped play backs.
				switchAll(layout, false);
				return true;
			default:
				return true;
		}
	}

	public void onPause() {
		super.onPause();
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		switchAll(layout, false);
	}
	
	public void playFile(File audio, MediaPlayer mPlayer, boolean loop) {
		mPlayer.setLooping(loop);
		try {
			mPlayer.setDataSource(audio.getAbsolutePath());
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e("playbackError", "Media failed to play");
		}
	}
	
	/*
	 * The MediaPlayer is not stopped or released in order to replay loops later on.
	 */
	public void stopFile(MediaPlayer mPlayer) {
		mPlayer.reset();
	}
	
	public void switchAll(LinearLayout layout, boolean status) {
		for (int i=0; i<layout.getChildCount(); i++) {
			LinearLayout row = (LinearLayout) layout.getChildAt(i);
			ToggleButton button = (ToggleButton) row.getChildAt(2);
			button.setChecked(status);
		}
	}
	
	private void startRecording(int k) {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		
		try {
			clips.add(k, new File(PATH, "Sample " + k + ".mp3"));
		} catch (Exception e) {
			Log.e("fileError", "Creating file failed");
			return;
		}
		
		mRecorder.setOutputFile(clips.get(k).getAbsolutePath());
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
	
	private void clear(LinearLayout layout) {
		for (int i=0; i<layout.getChildCount(); i++) {
			LinearLayout row = (LinearLayout) layout.getChildAt(i);
			ToggleButton button = (ToggleButton) row.getChildAt(2);
			button.setChecked(false);
		}
		layout.removeAllViews();
		i = 0;
	}
	
	private void saveFile(File f) {
		try {
			FileOutputStream output = new FileOutputStream(f);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void deleteAll() {
		File[] files = DIR.listFiles();
		for (File file: files) {
			file.delete();
		}
	}
	
}
