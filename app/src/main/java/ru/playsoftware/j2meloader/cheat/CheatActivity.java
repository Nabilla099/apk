/*
 * Kh-Loader cheat menu activity.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.playsoftware.j2meloader.cheat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.shell.MidletThread;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;

/**
 * Reflection-based "cheat engine": scans the currently running MIDlet's
 * object graph for a value, lets the user narrow the results down with
 * repeated scans (like Cheat Engine / GameGuardian's "next scan"), then
 * edit the winning field directly.
 */
public class CheatActivity extends AppCompatActivity {

	private ListView listResults;
	private EditText editSearchValue;
	private TextView textStatus;
	private View btnNewScan;
	private View btnNextScan;
	private List<CheatEngine.Hit> currentHits = new ArrayList<>();
	private HitAdapter adapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cheat);
		setSupportActionBar(findViewById(R.id.toolbar));
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		listResults = findViewById(R.id.list_results);
		editSearchValue = findViewById(R.id.edit_search_value);
		textStatus = findViewById(R.id.text_status);
		btnNewScan = findViewById(R.id.btn_new_scan);
		btnNextScan = findViewById(R.id.btn_next_scan);

		adapter = new HitAdapter();
		listResults.setAdapter(adapter);
		listResults.setOnItemClickListener((parent, view, position, id) ->
				showEditDialog(adapter.getItem(position)));

		btnNewScan.setOnClickListener(v -> doNewScan());
		btnNextScan.setOnClickListener(v -> doNextScan());

		textStatus.setText(getString(R.string.cheat_hint_first_scan));
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	private void doNewScan() {
		String value = editSearchValue.getText().toString();
		if (TextUtils.isEmpty(value)) {
			Toast.makeText(this, R.string.cheat_enter_value, Toast.LENGTH_SHORT).show();
			return;
		}
		var midlet = MidletThread.getMidlet();
		var activity = ContextHolder.getActivity();
		var current = activity != null ? activity.getCurrent() : null;
		if (midlet == null) {
			Toast.makeText(this, R.string.cheat_no_midlet, Toast.LENGTH_SHORT).show();
			return;
		}
		textStatus.setText(getString(R.string.cheat_scanning));
		List<Object> roots = CheatEngine.roots(midlet, current);
		List<CheatEngine.Hit> all = CheatEngine.scanAll(roots);
		currentHits = CheatEngine.narrow(all, value);
		adapter.notifyDataSetChanged();
		btnNextScan.setEnabled(!currentHits.isEmpty());
		textStatus.setText(getString(R.string.cheat_results_count, currentHits.size()));
	}

	private void doNextScan() {
		String value = editSearchValue.getText().toString();
		if (TextUtils.isEmpty(value)) {
			Toast.makeText(this, R.string.cheat_enter_value, Toast.LENGTH_SHORT).show();
			return;
		}
		currentHits = CheatEngine.narrow(currentHits, value);
		adapter.notifyDataSetChanged();
		btnNextScan.setEnabled(!currentHits.isEmpty());
		textStatus.setText(getString(R.string.cheat_results_count, currentHits.size()));
	}

	private void showEditDialog(CheatEngine.Hit hit) {
		if (hit == null) return;
		hit.readCurrent();
		EditText input = new EditText(this);
		input.setText(String.valueOf(hit.lastValue));
		new AlertDialog.Builder(this)
				.setTitle(hit.path)
				.setView(input)
				.setPositiveButton(R.string.cheat_apply, (d, w) -> {
					String text = input.getText().toString().trim();
					boolean ok = writeValue(hit, text);
					Toast.makeText(this, ok ? R.string.cheat_value_applied : R.string.cheat_value_failed,
							Toast.LENGTH_SHORT).show();
					adapter.notifyDataSetChanged();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private boolean writeValue(CheatEngine.Hit hit, String text) {
		try {
			return switch (hit.type) {
				case INT -> hit.write(Integer.parseInt(text));
				case LONG -> hit.write(Long.parseLong(text));
				case FLOAT -> hit.write(Float.parseFloat(text));
				case DOUBLE -> hit.write(Double.parseDouble(text));
				case BOOLEAN -> hit.write(Boolean.parseBoolean(text));
				case STRING -> hit.write(text);
			};
		} catch (Exception e) {
			return false;
		}
	}

	private class HitAdapter extends ArrayAdapter<CheatEngine.Hit> {
		HitAdapter() {
			super(CheatActivity.this, 0);
		}

		@Override
		public int getCount() {
			return currentHits.size();
		}

		@Override
		public CheatEngine.Hit getItem(int position) {
			return currentHits.get(position);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			TextView view;
			if (convertView instanceof TextView) {
				view = (TextView) convertView;
			} else {
				view = new TextView(CheatActivity.this);
				int pad = (int) (12 * getResources().getDisplayMetrics().density);
				view.setPadding(pad, pad / 2, pad, pad / 2);
				view.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
				view.setTextSize(14);
			}
			CheatEngine.Hit hit = currentHits.get(position);
			hit.readCurrent();
			view.setText(hit.getLabel());
			return view;
		}
	}
}
