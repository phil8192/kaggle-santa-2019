import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static java.lang.Math.abs;
import static java.lang.Math.random;

public class Main extends Optimiser {

	private Main(int[][] familyData, int[] initialAssignments) {
		super(familyData, initialAssignments);
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		//int[][] starting_solution = CsvUtil.read("../../../submission_71647.5625.csv");
		//int[][] starting_solution = CsvUtil.read("/tmp/lala.csv"); // 77124.66595889143
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");


		// 71757.52
		//int[][] starting_solution = CsvUtil.read("../../solutions/71535.25513531327.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}
		Main main = new Main(family_data, initialAsignments);


		double score = main.cost(initialAsignments);
		System.out.println("initial = " + score);


//		double diff1 = main.brute(initialAsignments, 1, 4, score);
//		System.out.println("score = " + (score + diff1));
//
//		double diff2 = main.brute(initialAsignments, 2, 4, score);
//		System.out.println("score = " + (score + diff2));

		//double diff3 = main.randomBrute(100000000, initialAsignments, 4 , 4, score);
		//System.out.println("score = " + (score + diff3));


//		int from = Integer.parseInt(meh[0]);
//		int to = Integer.parseInt(meh[1]);
//		double diff = main.brute3(initialAsignments, 5, score, from, to);


//
		double diff = main.optimise(initialAsignments);
		System.out.println("score = " + (score + diff));
	}
}
