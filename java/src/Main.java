import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static java.lang.Math.abs;
import static java.lang.Math.random;

public class Main {

	public static void main(String[] meh) {
		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		boolean useBrute = false;
		if(useBrute) {
			int from = Integer.parseInt(meh[0]);
			int to = Integer.parseInt(meh[1]);
			Brute brute = new Brute(family_data, initialAsignments, from, to, 5);
			double score  = brute.optimise();
		} else {
			SA sa = new SA(family_data, initialAsignments);
			double score = sa.optimise();
		}
	}
}
