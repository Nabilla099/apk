/*
 * Kh-Loader game log (KEmulator-style dialog & resource log)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.playsoftware.j2meloader.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Lightweight, in-memory + on-disk log of everything the running MIDlet
 * shows on screen (dialog / alert / form text) and every resource file
 * it requests (images, sounds, etc.), similar to the "Log" feature found
 * in KEmulator. Meant to help figure out which file a given piece of text
 * or image came from while reverse engineering / debugging a game.
 */
public final class GameLogger {

	public static final String TAG = "KhLoaderLog";
	private static final int MAX_LINES = 5000;

	public enum Kind {TEXT, IMAGE, RESOURCE, INFO}

	public static final class Entry {
		public final long time;
		public final Kind kind;
		public final String message;

		Entry(long time, Kind kind, String message) {
			this.time = time;
			this.kind = kind;
			this.message = message;
		}
	}

	private static final Deque<Entry> buffer = new ArrayDeque<>();
	private static final SimpleDateFormat TIME_FMT =
			new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
	private static volatile File logFile;
	private static volatile boolean enabled = true;

	private GameLogger() {
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * Must be called once the MIDlet's private data directory is known
	 * (e.g. from AppClassLoader.getDataDir()) so log entries can be
	 * persisted to a file the user can open/share afterwards.
	 */
	public static synchronized void init(String dataDir) {
		if (dataDir == null) {
			return;
		}
		File dir = new File(dataDir);
		if (!dir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dir.mkdirs();
		}
		logFile = new File(dir, "khloader_log.txt");
		buffer.clear();
	}

	public static File getLogFile() {
		return logFile;
	}

	public static synchronized Entry[] getEntries() {
		return buffer.toArray(new Entry[0]);
	}

	public static synchronized void clear() {
		buffer.clear();
		if (logFile != null && logFile.exists()) {
			//noinspection ResultOfMethodCallIgnored
			logFile.delete();
		}
	}

	public static void logDialogText(String source, String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		log(Kind.TEXT, source + ": " + text);
	}

	public static void logImage(String source, String path, int width, int height) {
		String dims = width > 0 && height > 0 ? " (" + width + "x" + height + ")" : "";
		log(Kind.IMAGE, source + ": " + path + dims);
	}

	public static void logResource(String path, boolean found) {
		log(Kind.RESOURCE, (found ? "loaded " : "MISSING ") + path);
	}

	public static void logInfo(String message) {
		log(Kind.INFO, message);
	}

	private static synchronized void log(Kind kind, String message) {
		if (!enabled) {
			return;
		}
		long time = System.currentTimeMillis();
		Entry entry = new Entry(time, kind, message);
		buffer.addLast(entry);
		while (buffer.size() > MAX_LINES) {
			buffer.removeFirst();
		}
		Log.d(TAG, "[" + kind + "] " + message);
		writeToFile(entry);
	}

	private static synchronized void writeToFile(Entry entry) {
		File file = logFile;
		if (file == null) {
			return;
		}
		try (FileOutputStream fos = new FileOutputStream(file, true);
			 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
			String line = "[" + TIME_FMT.format(entry.time) + "] [" + entry.kind + "] "
					+ entry.message + "\n";
			writer.write(line);
		} catch (IOException e) {
			Log.w(TAG, "Failed writing game log", e);
		}
	}

	public static String dumpAsText() {
		StringBuilder sb = new StringBuilder();
		Entry[] entries;
		synchronized (GameLogger.class) {
			entries = buffer.toArray(new Entry[0]);
		}
		for (Entry entry : entries) {
			sb.append('[').append(TIME_FMT.format(entry.time)).append("] [")
					.append(entry.kind).append("] ").append(entry.message).append('\n');
		}
		return sb.toString();
	}
}
