package com.alexkang.loopboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.List;

public class SampleListAdapter extends BaseAdapter {

    private final Context context;
    private final Recorder recorder;
    private final List<ImportedSample> importedSamples;
    private final List<RecordedSample> recordedSamples;

    SampleListAdapter(
            Context context,
            Recorder recorder,
            List<ImportedSample> importedSamples,
            List<RecordedSample> recordedSamples) {
        this.context = context;
        this.recorder = recorder;
        this.importedSamples = importedSamples;
        this.recordedSamples = recordedSamples;
    }

    @Override
    public int getCount() {
        return importedSamples.size() + recordedSamples.size();
    }

    @Override
    public Sample getItem(int position) {
        if (position < importedSamples.size()) {
            return importedSamples.get(position);
        } else if (position - importedSamples.size() < recordedSamples.size()) {
            return recordedSamples.get(position - importedSamples.size());
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View getView(final int position, View convertView, ViewGroup parent) {
        Sample sample = getItem(position);
        if (sample == null) {
            return convertView;
        }

        if (convertView == null) {
            convertView =
                    LayoutInflater
                            .from(context)
                            .inflate(R.layout.sound_clip_row, parent, false);
        }

        Button stopButton = convertView.findViewById(R.id.stop);
        Button rerecordButton = convertView.findViewById(R.id.rerecord);
        CheckBox loopButton = convertView.findViewById(R.id.loop);
        Button playButton = convertView.findViewById(R.id.play);

        // Update the state of the loop button.
        loopButton.setChecked(sample.isLooping());

        // Choose which buttons to show.
        if (sample instanceof ImportedSample) {
            // Show the stop button and hide the rerecord button.
            stopButton.setVisibility(View.VISIBLE);
            rerecordButton.setVisibility(View.GONE);
        } else {
            // Hide the stop button and show the rerecord button.
            stopButton.setVisibility(View.GONE);
            rerecordButton.setVisibility(View.VISIBLE);
        }

        // Set button listeners.
        playButton.setText(sample.getName());
        playButton.setOnClickListener(v -> {
            if (sample.isLooping()) {
                loopButton.setChecked(false);
            }
            sample.play(false);
        });
        stopButton.setOnClickListener(v -> {
            loopButton.setChecked(false);
            sample.stop();
        });
        rerecordButton.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);
                recorder.startRecording(
                        recordedBytes -> ((RecordedSample) sample).save(context, recordedBytes));
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.setPressed(false);
                recorder.stopRecording();
                loopButton.setChecked(false);
            }
            return true;
        });
        loopButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sample.play(true);
            } else {
                sample.stop();
            }
        });

        return convertView;
    }
}
