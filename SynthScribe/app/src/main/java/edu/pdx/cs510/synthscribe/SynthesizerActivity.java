package edu.pdx.cs510.synthscribe;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.SeekBar;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;



public class SynthesizerActivity extends Activity {

    private volatile boolean isPlaying = false;
    private volatile double currentPitch = 440.0;
    private volatile float currentVolume = 1.0f;
    int sampleRate = 44100;
    private double pitch;

    private float volume;
    String temporaryFilePath;
    private FileOutputStream temporaryFileOutputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synthesizer);

        temporaryFilePath = getExternalFilesDir(null) + "/temporaryComposition.pcm";
        setupPitchSeekBar();
        setupVolumeSeekBar();
        setupPlayButton();

        setAddButton();
        setupSaveButton();
    }

    private void startPlayback() {
        isPlaying = true;
        final Thread playbackThread = new Thread(this::playGeneratedTone, "Audio Playback Thread");
        playbackThread.start();
    }

    private void playGeneratedTone() {

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        short[] buffer = new short[bufferSize];
        int samplesPerWaveLength;
        int angle = 0;
        audioTrack.play();

        while (isPlaying) {
            currentPitch = pitch;
            currentVolume = volume;
            samplesPerWaveLength = (int) ((sampleRate / currentPitch));
            audioTrack.setVolume(currentVolume);
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
                addNoteToTemporaryFile();
            }
        });
    }

    private void setupVolumeSeekBar() {
        SeekBar volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress / 100.0f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                addNoteToTemporaryFile();
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
    private void setAddButton() {
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> addNoteToTemporaryFile());
    }

    private void addNoteToTemporaryFile() {
        try {
            if (temporaryFileOutputStream == null) {
                temporaryFileOutputStream = new FileOutputStream(temporaryFilePath, true);
            }
            writeNoteToTemporaryFile(temporaryFileOutputStream, pitch, volume);
        } catch (IOException e) {
            Log.e("SynthesizerActivity", "Error while accessing file!");
        }
    }

    private void writeNoteToTemporaryFile(FileOutputStream outputStream, double pitch, float volume) throws IOException {
        int duration = 1; // seconds for the note's duration
        int numSamples = duration * sampleRate;
        byte[] noteData = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; ++i) {
            double angle = 2 * Math.PI * i / (sampleRate / pitch);
            short val = (short) (Math.sin(angle) * Short.MAX_VALUE * volume);
            noteData[i * 2] = (byte) (val & 0xFF);
            noteData[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }

        outputStream.write(noteData);
    }

    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            try {
                saveCompositionAndResetTemporaryFile();
            } catch (IOException e) {
                Log.e("SynthesizerActivity", "Error while accessing file!");
            }
        });
    }

    private void saveCompositionAndResetTemporaryFile() throws IOException {
        File tempFile = new File(temporaryFilePath);
        File savedFile = new File(getExternalFilesDir(null), "Composition_" + System.currentTimeMillis() + SystemClock.currentGnssTimeClock() + ".pcm");

        try (FileInputStream fileInputStream = new FileInputStream(tempFile);
             FileOutputStream fileOutputStream = new FileOutputStream(savedFile);
             FileChannel src = fileInputStream.getChannel();
             FileChannel dest = fileOutputStream.getChannel()) {
            dest.transferFrom(src, 0, src.size());
        }

        if (temporaryFileOutputStream != null) {
            temporaryFileOutputStream.close();
            temporaryFileOutputStream = null;
        }
        if(!tempFile.delete()){
            Log.e("SynthesizerActivity", "Error while deleting temporary file!");
        }
        if(!tempFile.createNewFile()){
            Log.e("SynthesizerActivity", "Error while resetting temporary file!");
        }
    }
}
