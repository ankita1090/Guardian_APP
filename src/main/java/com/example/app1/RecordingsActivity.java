package com.example.app1;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class RecordingsActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> fileNames;
    private ArrayList<File> fileList;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        // --- System Bars Black Color ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        listView = findViewById(R.id.recordingsListView);
        loadRecordings();

        // 1. Play on Click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playAudio(fileList.get(position));
            }
        });

        // 2. Delete on Long Click
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(position);
                return true;
            }
        });

        // Back Button Logic
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadRecordings() {
        fileNames = new ArrayList<>();
        fileList = new ArrayList<>();

        File directory = getExternalFilesDir(null);
        if (directory != null) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".mp3")) {
                        fileNames.add(file.getName());
                        fileList.add(file);
                    }
                }
            }
        }

        // --- Custom Adapter for Dark Text ---
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, fileNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);

                // Text ko Dark Navy aur Bold banaya
                text.setTextColor(Color.parseColor("#1A1A2E"));
                text.setTypeface(null, Typeface.BOLD);
                text.setTextSize(14);

                return view;
            }
        };

        listView.setAdapter(adapter);

        // Empty state check
        View emptyView = findViewById(R.id.emptyView);
        if (emptyView != null) {
            emptyView.setVisibility(fileNames.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void playAudio(File file) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error playing file", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setMessage("Kya aap is file ko delete karna chahte hain?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (fileList.get(position).delete()) {
                            Toast.makeText(RecordingsActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                            loadRecordings();
                        }
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}