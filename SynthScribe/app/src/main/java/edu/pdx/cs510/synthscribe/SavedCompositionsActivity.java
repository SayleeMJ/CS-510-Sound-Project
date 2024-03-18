package edu.pdx.cs510.synthscribe;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class SavedCompositionsActivity extends Activity {

    private final ArrayList<String> compositionList = new ArrayList<>();
    private ArrayAdapter<String> fileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_compositions);

        ListView listCompositions = findViewById(R.id.listView);
        fileListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, compositionList);
        listCompositions.setAdapter(fileListAdapter);
        listCompositions.setOnItemClickListener((parent, view, position, id) -> playSavedComposition(compositionList.get(position)));

        listSavedCompositions();
    }

    private void listSavedCompositions() {
        File directory = getExternalFilesDir(null);
        assert directory != null;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".pcm"));
        compositionList.clear();
        assert files != null;
        for (File file : files) {
            compositionList.add(file.getName());
        }
        fileListAdapter.notifyDataSetChanged();
    }

    private void playSavedComposition(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        int sampleRate = 44100;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();

            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                audioTrack.write(buffer, 0, read);
            }

            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e) {
            Log.e("SynthesizerActivity", "Error while saving composition file!");
        }
    }
}
