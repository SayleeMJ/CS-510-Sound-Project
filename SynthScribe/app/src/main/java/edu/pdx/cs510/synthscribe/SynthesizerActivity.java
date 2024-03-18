package edu.pdx.cs510.synthscribe;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;



public class SynthesizerActivity extends Activity {

    private volatile boolean isPlaying = false;
    private volatile double currentPitch = 440.0;
    int sampleRate = 44100;
    private double pitch;
    String tempFilePath;
    private FileOutputStream tempFileOutputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synthesizer);

        tempFilePath = getExternalFilesDir(null) + "/tempComposition.pcm";
        setupWaveformSpinner();
        setupPitchSeekBar();
        setupPlayButton();

        setupAddButton();
        setupSaveButton();
    }

    private void setupWaveformSpinner() {
        Spinner waveformSpinner = findViewById(R.id.waveformSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.waveform_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        waveformSpinner.setAdapter(adapter);

        waveformSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void startPlayback() {
        isPlaying = true;
        final Thread playbackThread = new Thread(this::playTone, "Audio Playback Thread");
        playbackThread.start();
    }

    private void playTone() {

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        short[] buffer = new short[bufferSize];
        int samplesPerWaveLength;
        int angle = 0;
        audioTrack.play();

        while (isPlaying) {
            currentPitch = pitch;
            samplesPerWaveLength = (int) (sampleRate / currentPitch);
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (short) (Math.sin(2.0 * Math.PI * angle / samplesPerWaveLength) * Short.MAX_VALUE);
                angle = (angle + 1) % samplesPerWaveLength;
            }
            audioTrack.write(buffer, 0, buffer.length);
        }
        audioTrack.stop();
        audioTrack.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isPlaying = false; // Stop the audio playback thread
    }

    private void setupPitchSeekBar() {
        SeekBar pitchSeekBar = findViewById(R.id.pitchSeekBar);
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                pitch = progress * 10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                addNoteToFile();
            }
        });
    }

    private void setupPlayButton() {
        Button playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> {
            if (!isPlaying) {
                startPlayback(); // Start or resume playback
                playButton.setText(R.string.stop_sound);
            } else {
                isPlaying = false; // Stop playback
                playButton.setText(R.string.start_sound);
            }
        });
    }


    // Add button
    private void setupAddButton() {
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            addNoteToFile();
        });
    }

    private void addNoteToFile() {
        try {
            if (tempFileOutputStream == null) {
                tempFileOutputStream = new FileOutputStream(tempFilePath, true);
            }
            writeNoteToFile(tempFileOutputStream, pitch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeNoteToFile(FileOutputStream outputStream, double pitch) throws IOException {
        int duration = 1; // seconds for the note's duration
        int numSamples = duration * sampleRate;
        byte[] noteData = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; ++i) {
            double angle = 2 * Math.PI * i / (sampleRate / pitch);
            short val = (short) (Math.sin(angle) * Short.MAX_VALUE);
            noteData[i * 2] = (byte) (val & 0xFF);
            noteData[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }

        outputStream.write(noteData);
    }

    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            try {
                saveCompositionAndReset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveCompositionAndReset() throws IOException {
        File tempFile = new File(tempFilePath);
        File savedFile = new File(getExternalFilesDir(null), "Composition_" + System.currentTimeMillis() + ".pcm");

        try (FileChannel src = new FileInputStream(tempFile).getChannel();
             FileChannel dest = new FileOutputStream(savedFile).getChannel()) {
            dest.transferFrom(src, 0, src.size());
        }

        if (tempFileOutputStream != null) {
            tempFileOutputStream.close();
            tempFileOutputStream = null;
        }
        tempFile.delete();
        tempFile.createNewFile();
    }
}