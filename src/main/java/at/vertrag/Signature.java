package at.vertrag;

import java.util.ArrayList;
import java.util.List;

/**
 * Unterschrift als kompakter String: Striche mit '|' getrennt, Punkte mit ';' getrennt, Koordinaten "x,y".
 * Wird sowohl vom Client (Zeichnen) als auch vom Server (Validierung) benutzt.
 */
public final class Signature {
	public static final int WIDTH = 180;
	public static final int HEIGHT = 50;
	public static final int MAX_POINTS = 600;
	public static final int MIN_POINTS = 8;
	public static final int MAX_LENGTH = 6000;

	private Signature() {
	}

	public static String encode(List<List<int[]>> strokes) {
		StringBuilder sb = new StringBuilder();
		for (List<int[]> stroke : strokes) {
			if (stroke.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('|');
			}
			for (int i = 0; i < stroke.size(); i++) {
				if (i > 0) {
					sb.append(';');
				}
				int[] p = stroke.get(i);
				sb.append(p[0]).append(',').append(p[1]);
			}
		}
		return sb.toString();
	}

	/** Gibt null zurück, wenn der String fehlerhaft ist. Ein leerer String ergibt eine leere Liste. */
	public static List<List<int[]>> parse(String s) {
		List<List<int[]>> result = new ArrayList<>();
		if (s == null || s.isEmpty()) {
			return result;
		}
		if (s.length() > MAX_LENGTH) {
			return null;
		}
		for (String strokePart : s.split("\\|", -1)) {
			List<int[]> stroke = new ArrayList<>();
			for (String pointPart : strokePart.split(";", -1)) {
				String[] xy = pointPart.split(",", -1);
				if (xy.length != 2) {
					return null;
				}
				try {
					int x = Integer.parseInt(xy[0]);
					int y = Integer.parseInt(xy[1]);
					if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
						return null;
					}
					stroke.add(new int[] {x, y});
				} catch (NumberFormatException e) {
					return null;
				}
			}
			result.add(stroke);
		}
		return result;
	}

	public static int countPoints(List<List<int[]>> strokes) {
		int n = 0;
		for (List<int[]> stroke : strokes) {
			n += stroke.size();
		}
		return n;
	}

	/** Server-seitige Prüfung: gültiges Format und genug Punkte. */
	public static boolean isValid(String s) {
		List<List<int[]>> strokes = parse(s);
		if (strokes == null) {
			return false;
		}
		int n = countPoints(strokes);
		return n >= MIN_POINTS && n <= MAX_POINTS;
	}
}
