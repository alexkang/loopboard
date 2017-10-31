package com.alexkang.loopboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int FOOTER_SIZE_DP = 360;
    private static final String[] PERMISSIONS =
            {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final ArrayList<ImportedSample> importedSamples = new ArrayList<>();
    private final ArrayList<RecordedSample> recordedSamples = new ArrayList<>();
    private final Recorder recorder = new Recorder();
    private final SampleListAdapter sampleListAdapter =
            new SampleListAdapter(this, recorder, importedSamples, recordedSamples);
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();

    // ------- Activity lifecycle methods -------

    @Override
	@SuppressLint("ClickableViewAccessibility")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        checkPermissions();

		// Retrieve UI elements.
        ListView sampleList = findViewById(R.id.sound_list);
        Button recordButton = findViewById(R.id.record_button);

        // Initialize the sample list.
        sampleList.setAdapter(sampleListAdapter);

        // Add some footer space at the bottom of the sample list.
        View footer = new View(this);
        footer.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FOOTER_SIZE_DP));
        footer.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        sampleList.addFooterView(footer, null, false);

        // Define the record button behavior. Tap and hold to record, release to stop and save.
        recordButton.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();

            if (action == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);

                // Make sure we haven't hit our maximum number of recordings before proceeding.
                if (importedSamples.size() + recordedSamples.size() > Utils.MAX_SAMPLES) {
                    Snackbar.make(
                            findViewById(R.id.root_layout),
                            R.string.error_max_samples,
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    recorder.startRecording(recordedBytes -> saveRecording(recordedBytes));
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.setPressed(false);
                recorder.stopRecording();
            }

            return true;
        });
	}

    @Override
    public void onStart() {
        super.onStart();
        refreshRecordings();
    }

    @Override
	public void onPause() {
		super.onPause();
        stopAllSamples();
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdownSamples();

        recorder.shutdown();
        saveExecutor.shutdown();
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
            case R.id.action_delete:
                // Display a deletion confirmation dialog before actually deleting.
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.confirm_delete))
                        .setPositiveButton(R.string.yes, (dialog, which) -> deleteAllRecordings())
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();

                return true;
            case R.id.action_stop:
                // Stop all currently playing samples.
                stopAllSamples();

                return true;
            default:
                return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.length != PERMISSIONS.length) {
            return;
        }

        // Make sure the audio recording permission was granted.
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.RECORD_AUDIO) &&
                    grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_permission, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        // All permissions have been granted. Proceed with the app. Also remember to refresh the
        // recorder in case the record audio permission was recently granted.
        recorder.refresh();
        refreshRecordings();
    }

    // ------- Private methods -------

    private void checkPermissions() {
        // Check all permissions to see if they're granted.
        boolean permissionsGranted = true;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                permissionsGranted = false;
                break;
            }
        }

        // If any permissions aren't granted, make a request.
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void updateTutorialVisibility() {
        if (importedSamples.size() > 0 || recordedSamples.size() > 0) {
            // If any samples already exist, remove the tutorial text.
            findViewById(R.id.tutorial).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tutorial).setVisibility(View.VISIBLE);
        }
    }

    private void refreshRecordings() {
        shutdownSamples();

        // First, add user imported audio files to the top of our sample list. Also, create the
        // LoopBoard directory if it doesn't already exist.
        try {
            File importedDir = new File(Utils.IMPORTED_SAMPLE_PATH);
            importedDir.mkdirs();
            for (File file : importedDir.listFiles()) {
                if (Utils.isSupportedSampleFile(file)) {
                    importedSamples.add(new ImportedSample(this, file));
                }
            }
        } catch (NullPointerException e) {
            // No-op. This means that external storage permission was not granted.
        }

        // Next, add samples recorded from this app.
        for (String fileName : fileList()) {
            RecordedSample recordedSample = RecordedSample.openSavedSample(this, fileName);
            if (recordedSample != null) {
                recordedSamples.add(recordedSample);
            }
        }

        // Tell the ListView to refresh.
        sampleListAdapter.notifyDataSetChanged();
        updateTutorialVisibility();
    }

    private void saveRecording(byte[] recordedBytes) {
	    // Initialize the name input field for the sample.
        @SuppressLint("InflateParams") View saveLayout =
                getLayoutInflater().inflate(R.layout.save_sample_dialog, null);
        EditText sampleNameField = saveLayout.findViewById(R.id.sample_name_field);
        sampleNameField.setText(
                String.format(
                        Locale.ENGLISH,
                        "Sample %d",
                        recordedSamples.size() + 1));
        sampleNameField.selectAll();

        // Create a new dialog for the user to edit the name, and then save the sample.
        runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.name_recording))
                .setView(saveLayout)
                .setPositiveButton(
                        getString(R.string.save), (dialog, which) -> saveExecutor.execute(() -> {
                            String name = sampleNameField.getText().toString();
                            if (Utils.saveRecording(getBaseContext(), name, recordedBytes)) {
                                runOnUiThread(() -> {
                                    recordedSamples
                                            .add(RecordedSample
                                                    .openSavedSample(this, name));
                                    sampleListAdapter.notifyDataSetChanged();
                                    updateTutorialVisibility();
                                });
                            } else {
                                Snackbar.make(
                                        findViewById(R.id.root_layout),
                                        R.string.error_saving,
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        }))
                .setCancelable(false)
                .show());
    }

    private void stopAllSamples() {
        for (Sample sample : importedSamples) {
            sample.stop();
        }
        for (Sample sample : recordedSamples) {
            sample.stop();
        }

        // Refresh the list to update button states.
        sampleListAdapter.notifyDataSetChanged();
    }

    private void shutdownSamples() {
        // First, stop all currently playing samples.
        stopAllSamples();

        // Call shutdown on each sample.
        for (Sample sample : importedSamples) {
            sample.shutdown();
        }
        for (Sample sample : recordedSamples) {
            sample.shutdown();
        }

        // Clear the lists.
        importedSamples.clear();
        recordedSamples.clear();
    }

	private void deleteAllRecordings() {
	    // Stop playing all samples.
        stopAllSamples();

        // Delete all recordings.
        for (String fileName : fileList()) {
            deleteFile(fileName);
        }

        // Update the UI.
        refreshRecordings();
        Snackbar.make(
                findViewById(R.id.root_layout),
                R.string.samples_deleted,
                Snackbar.LENGTH_SHORT).show();
	}
}
