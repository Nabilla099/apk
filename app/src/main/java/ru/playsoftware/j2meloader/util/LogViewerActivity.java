/*
 * Kh-Loader game log viewer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.playsoftware.j2meloader.util;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import ru.playsoftware.j2meloader.R;

/**
 * Shows the KEmulator-style game log (dialog text + resource/image loads)
 * collected by {@link GameLogger} for the currently running MIDlet.
 */
public class LogViewerActivity extends AppCompatActivity {

	private TextView textLogContent;
	private TextView textLogPath;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_viewer);
		setSupportActionBar(findViewById(R.id.toolbar));
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		textLogContent = findViewById(R.id.text_log_content);
		textLogPath = findViewById(R.id.text_log_path);
		refresh();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		File file = GameLogger.getLogFile();
		textLogPath.setText(file != null
				? getString(R.string.log_saved_at, file.getAbsolutePath())
				: getString(R.string.log_not_saved_yet));
		String text = GameLogger.dumpAsText();
		textLogContent.setText(text.isEmpty() ? getString(R.string.log_empty) : text);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.log_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_log_share) {
			shareLog();
			return true;
		} else if (id == R.id.action_log_clear) {
			GameLogger.clear();
			refresh();
			Toast.makeText(this, R.string.log_cleared, Toast.LENGTH_SHORT).show();
			return true;
		} else if (id == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void shareLog() {
		String text = GameLogger.dumpAsText();
		if (text.isEmpty()) {
			Toast.makeText(this, R.string.log_empty, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, text);
		startActivity(Intent.createChooser(intent, getString(R.string.log_viewer_title)));
	}
}
