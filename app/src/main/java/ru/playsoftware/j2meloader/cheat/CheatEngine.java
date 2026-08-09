/*
 * Kh-Loader cheat engine.
 *
 * Since the MIDlet runs as regular Java objects inside this same process
 * (there's no native game process to attach a raw /proc/pid/mem scanner
 * to, and doing that would require root + native code), values are found
 * and edited by walking the live object graph of the running MIDlet with
 * reflection instead. This gives Cheat-Engine-like "search a value, narrow
 * down, edit it" behaviour without needing root.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.playsoftware.j2meloader.cheat;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

/**
 * Value types that can be scanned/edited.
 */
public final class CheatEngine {

	public enum ValueType {INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING}

	private static final int MAX_DEPTH = 6;
	private static final int MAX_VISITED = 20000;
	private static final Set<String> SKIP_PREFIXES = Set.of(
			"java.", "javax.microedition.lcdui.", "android.", "androidx.",
			"dalvik.", "kotlin.", "kotlinx.");

	/** A single found field, with the object instance that owns it. */
	public static final class Hit {
		public final Object owner;
		public final Field field;
		public final String path;
		public final ValueType type;
		public Object lastValue;

		Hit(Object owner, Field field, String path, ValueType type, Object lastValue) {
			this.owner = owner;
			this.field = field;
			this.path = path;
			this.type = type;
			this.lastValue = lastValue;
		}

		public String getLabel() {
			return path + " = " + lastValue;
		}

		/** Re-reads the current live value from the owning object. */
		public Object readCurrent() {
			try {
				field.setAccessible(true);
				Object v = field.get(owner);
				lastValue = v;
				return v;
			} catch (Exception e) {
				return lastValue;
			}
		}

		public boolean write(Object newValue) {
			try {
				field.setAccessible(true);
				switch (type) {
					case INT -> field.setInt(owner, ((Number) newValue).intValue());
					case LONG -> field.setLong(owner, ((Number) newValue).longValue());
					case FLOAT -> field.setFloat(owner, ((Number) newValue).floatValue());
					case DOUBLE -> field.setDouble(owner, ((Number) newValue).doubleValue());
					case BOOLEAN -> field.setBoolean(owner, (Boolean) newValue);
					case STRING -> field.set(owner, String.valueOf(newValue));
				}
				lastValue = newValue;
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	private CheatEngine() {
	}

	/**
	 * Roots to start walking from: the MIDlet instance itself and whatever
	 * Displayable is currently on screen (covers most game/player state that
	 * hangs off the active Canvas).
	 */
	public static List<Object> roots(MIDlet midlet, Displayable current) {
		List<Object> roots = new ArrayList<>();
		if (midlet != null) roots.add(midlet);
		if (current != null) roots.add(current);
		return roots;
	}

	/**
	 * Walks the object graph from the given roots and returns every
	 * numeric/boolean/String field found, tagged with its current value.
	 * Only descends into classes that look like they belong to the game
	 * itself (not framework/library internals) to keep this fast and to
	 * avoid noise.
	 */
	public static List<Hit> scanAll(List<Object> roots) {
		List<Hit> hits = new ArrayList<>();
		Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		ArrayDeque<Object> queue = new ArrayDeque<>();
		ArrayDeque<Integer> depths = new ArrayDeque<>();
		ArrayDeque<String> paths = new ArrayDeque<>();
		for (Object root : roots) {
			if (root == null || !visited.add(root)) continue;
			queue.add(root);
			depths.add(0);
			paths.add(root.getClass().getSimpleName());
		}
		while (!queue.isEmpty() && visited.size() < MAX_VISITED) {
			Object obj = queue.poll();
			int depth = depths.poll();
			String path = paths.poll();
			Class<?> cls = obj.getClass();
			if (isSkippable(cls) || depth > MAX_DEPTH) continue;
			for (Class<?> c = cls; c != null && !isSkippable(c); c = c.getSuperclass()) {
				for (Field f : c.getDeclaredFields()) {
					if (Modifier.isStatic(f.getModifiers())) continue;
					f.setAccessible(true);
					Object value;
					try {
						value = f.get(obj);
					} catch (Exception e) {
						continue;
					}
					if (value == null) continue;
					Class<?> type = f.getType();
					ValueType vt = toValueType(type, value);
					String fieldPath = path + "." + f.getName();
					if (vt != null) {
						hits.add(new Hit(obj, f, fieldPath, vt, value));
					} else if (!type.isPrimitive() && !isSkippable(type)
							&& !type.isArray() && visited.add(value)) {
						queue.add(value);
						depths.add(depth + 1);
						paths.add(fieldPath);
					} else if (type.isArray() && !type.getComponentType().isPrimitive()
							&& visited.add(value)) {
						int len = Array.getLength(value);
						for (int i = 0; i < Math.min(len, 64); i++) {
							Object el = Array.get(value, i);
							if (el != null && visited.add(el)) {
								queue.add(el);
								depths.add(depth + 1);
								paths.add(fieldPath + "[" + i + "]");
							}
						}
					}
				}
			}
		}
		return hits;
	}

	/** Re-filters an existing hit list down to entries whose live value now equals target. */
	public static List<Hit> narrow(List<Hit> hits, String targetText) {
		List<Hit> result = new ArrayList<>();
		for (Hit hit : hits) {
			Object current = hit.readCurrent();
			if (matches(current, hit.type, targetText)) {
				result.add(hit);
			}
		}
		return result;
	}

	private static boolean matches(Object current, ValueType type, String targetText) {
		try {
			return switch (type) {
				case INT -> ((Number) current).intValue() == Integer.parseInt(targetText.trim());
				case LONG -> ((Number) current).longValue() == Long.parseLong(targetText.trim());
				case FLOAT -> ((Number) current).floatValue() == Float.parseFloat(targetText.trim());
				case DOUBLE -> ((Number) current).doubleValue() == Double.parseDouble(targetText.trim());
				case BOOLEAN -> ((Boolean) current) == Boolean.parseBoolean(targetText.trim());
				case STRING -> String.valueOf(current).equals(targetText);
			};
		} catch (Exception e) {
			return false;
		}
	}

	private static ValueType toValueType(Class<?> type, Object value) {
		if (type == int.class || type == Integer.class) return ValueType.INT;
		if (type == long.class || type == Long.class) return ValueType.LONG;
		if (type == float.class || type == Float.class) return ValueType.FLOAT;
		if (type == double.class || type == Double.class) return ValueType.DOUBLE;
		if (type == boolean.class || type == Boolean.class) return ValueType.BOOLEAN;
		if (type == String.class) return ValueType.STRING;
		return null;
	}

	private static boolean isSkippable(Class<?> cls) {
		String name = cls.getName();
		for (String prefix : SKIP_PREFIXES) {
			if (name.startsWith(prefix)) return true;
		}
		return false;
	}
}
