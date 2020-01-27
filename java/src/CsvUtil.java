import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * just some utils for reading/writing csv.
 */
class CsvUtil {
	static int[][] read(String src) {
		ArrayList<int[]> a = new ArrayList<>();
		try {
			try (BufferedReader reader = new BufferedReader(new FileReader(src))) {
				for (String row = reader.readLine() /* skip header */; (row = reader.readLine()) != null; ) {
					String[] split_row = row.split(",");
					int[] f_row = new int[split_row.length];
					for (int i = 0; i < split_row.length; i++) {
						f_row[i] = Integer.parseInt(split_row[i]);
					}
					a.add(f_row);
				}
				return a.toArray(new int[][]{});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static void write(int[] assignments, String dst) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(dst));
			writer.write("family_id,assigned_day\n");
			for (int i = 0; i < assignments.length; i++) {
				writer.write(i + "," + assignments[i] + "\n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
