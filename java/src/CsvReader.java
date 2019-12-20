import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class CsvReader {
	public static int[][] read(String src) {
		ArrayList<int[]> a = new ArrayList<>();
		try {
			try (BufferedReader reader = new BufferedReader(new FileReader(src))) {
				for (String row = reader.readLine() /* skip header */; (row = reader.readLine()) != null; ) {
					String[] split_row = row.split(",");
					int[] f_row = new int[]{
							Integer.parseInt(split_row[0]),
							Integer.parseInt(split_row[1])
					};
					a.add(f_row);
				}
				return a.toArray(new int[][]{});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
