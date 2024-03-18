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

    /**
     * Indicates whether the synthesizer is currently producing a tone.
     */
    private volatile boolean isPlaying = false;

    /**
     * The current pitch of the tone to be generated, measured in Hertz. It defaults to 440.0 Hz (A4).
     */
    private volatile double currentPitch = 440.0;

    /**
     * The current volume of the tone being played. The range is 0.0 (silence) to 1.0 (highest volume).
     */
    private volatile float currentVolume = 1.0f;

    /**
     * The sample rate for audio playback is measured in samples per second. Default frequency is 44100 Hz.
     */
    int sampleRate = 44100;

    /**
     * The user-adjusted pitch value in Hertz.
     */
    private double pitch;

    /**
     * The volume level is set by the user. Range: 0.0 to 1.0.
     */
    private float volume;

    /**
     * The path to the temporary file where created tones are stored before being saved.
     */
    String temporaryFilePath;

    /**
     * Output stream for writing generated tones to the temporary file.
     */
    private FileOutputStream temporaryFileOutputStream;


    /**
     * The call is made when the activity begins. Sets up the user interface and synthesizer.
     */
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


    /**
     * Starts the created tone's playback on a new thread.
     */
    private void startPlayback() {
        isPlaying = true;
        final Thread playbackThread = new Thread(this::playGeneratedTone, "Audio Playback Thread");
        playbackThread.start();
    }

    /**
     * Creates and plays a tone using the current pitch and loudness settings.
     * Constantly writes audio data to an AudioTrack for real-time playback.
     */
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

    /**
     * Called when the activity detects that the user has pressed the back key.
     * Stops the tone's playback and releases resources.
     */
    @Override
    protected void onStop() {
        super.onStop();
        isPlaying = false; // Stop the audio playback thread
    }

    /**
     * Sets up the pitch SeekBar and manages user modifications to the tone's pitch.
     */
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

    /**
     * Configures the volume SeekBar and allows user modifications to the tone volume.
     */
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

    /**
     * Initializes the play button and controls tone playback.
     */
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


    /**
     * Sets the add button to save the current tone settings (pitch and loudness) to a temporary file.
     */

    private void setAddButton() {
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> addNoteToTemporaryFile());
    }

    /**
     * Saves a single tone with current pitch and loudness settings to a temporary file.
     */

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

    /**
     * The generated tone data is written to the temporary file using the current settings.
     *
     * @param outputStream The output stream linked to the temporary file.
     * @param pitch        The pitch of the tone, in Hertz.
     * @param volume       The volume of the tone, normalized from 0.0 to 1.0.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
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

    /**
     * Configures the save button to save the composition from the temporary file to a permanent file, and resets the temporary file for future use.
     */

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

    /**
     * Configures the save button to save the composition from the temporary file to a permanent file, and resets the temporary file for future use.
     */
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
        if (!tempFile.delete()) {
            Log.e("SynthesizerActivity", "Error while deleting temporary file!");
        }
        if (!tempFile.createNewFile()) {
            Log.e("SynthesizerActivity", "Error while resetting temporary file!");
        }
    }
}
