import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.abs;

public class Main {
	private static final int MAX_FAM_SIZE = 10;
	private static final int MAX_CHOICE = 10;
	private static final int MIN_PPL = 125;
	private static final int MAX_PPL = 300;

	private final double[][] penalties;
	private final double[][] accountingCost;

	private final int[] dayCapacities;

	// all family penalties
	// 5000*100
	final double[][] allPenalties;

	final int[][] familyPrefs;
	final int[] familySize;

	private final Random prng = new Random();

	private Main(int[][] familyData, int[] initialAssignments) {
		this.familyPrefs = new int[5000][10];
		this.familySize = new int[5000];
		this.penalties = new double[MAX_FAM_SIZE + 1][MAX_CHOICE + 1];
		for (int i = 1; i < MAX_FAM_SIZE; i++) {
			penalties[i][0] = 0;
			penalties[i][1] = 50;
			penalties[i][2] = 50 + 9 * i;
			penalties[i][3] = 100 + 9 * i;
			penalties[i][4] = 200 + 9 * i;
			penalties[i][5] = 200 + 18 * i;
			penalties[i][6] = 300 + 18 * i;
			penalties[i][7] = 300 + 36 * i;
			penalties[i][8] = 400 + 36 * i;
			penalties[i][9] = 500 + 36 * i + 199 * i;
			penalties[i][10] = 500 + 36 * i + 398 * i;
		}
		// init all penalties matrix
		allPenalties = new double[5000][100 + 1];
		for (int i = 0; i < allPenalties.length; i++) {
			for (int j = 0; j < allPenalties[0].length; j++) {
				allPenalties[i][j] = Double.MAX_VALUE;
			}
		}
		for (int i = 0; i < familyData.length; i++) {
			int famSize = familyData[i][11];
			familySize[i] = famSize;
			for (int j = 0; j < 10; j++) {
				int choice_j = familyData[i][j + 1];
				familyPrefs[i][j] = choice_j;
				allPenalties[i][choice_j] = penalties[famSize][j];
			}
		}
		// init accounting matrix
		int max_cap = MAX_PPL + 1; // 1 to 300 ppl
		accountingCost = new double[max_cap][max_cap];
		for (int i = 1; i < max_cap; i++) {
			for (int j = 1; j < max_cap; j++) {
				accountingCost[i][j] = ((i - 125) / 400.0) * Math.pow(i, 0.5 + (abs(i - j) / 50.0));
			}
		}
		// init day capacities
		dayCapacities = new int[100 + 1];
		for (int i = 0; i < initialAssignments.length; i++) {
			int day = initialAssignments[i];
			int famSize = familySize[i];
			dayCapacities[day] += famSize;
		}
	}

	private double getAccountingCost(final int now, final int pre) {
		return accountingCost[now][pre];
	}

	private double cost(final int[] familyAssignments) {
		double penalty = 0.0;
		double accounting = 0.0;
		// day assignment penalty
		for (int i = 0, len = familyAssignments.length; i < len; i++) {
			final int day = familyAssignments[i];
			penalty += allPenalties[i][day];
		}
		// accounting penalty
		// first day is special
		accounting += getAccountingCost(dayCapacities[100], dayCapacities[100]);
		// rest of days starting at day 99
		for (int i = 100; --i > 0; ) {
			accounting += getAccountingCost(dayCapacities[i], dayCapacities[i + 1]);
		}
		return penalty + accounting;
	}

	private double acceptanceProbability(final double oldScore, final double newScore, final double temperature) {
		final double d = newScore - oldScore;
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	private double fastCost(final int assignedDay, final int candidateDay, final int famSize, final int fam,
													final int[] assignments, final double best) {
		// todo: cost based on delta
		return cost(assignments);
	}

	private double tryMove(final int assignedDay, final int candidateDay, final int famSize, final int fam,
												 final int[] assignments, final double best, final double temperature) {
		// break and eval
		dayCapacities[assignedDay] -= famSize;
		dayCapacities[candidateDay] += famSize;
		assignments[fam] = candidateDay;
		final double candidateScore = fastCost(assignedDay, candidateDay, famSize, fam, assignments, best);

		if (candidateScore < best || acceptanceProbability(best, candidateScore, temperature) >= prng.nextDouble()) {
			return candidateScore - best;
		} else { // fix
			dayCapacities[assignedDay] += famSize;
			dayCapacities[candidateDay] -= famSize;
			assignments[fam] = assignedDay;
			return Double.MAX_VALUE;
		}
	}

	private double localMinima(final int[] assignments, final double temperature) {
		double best = cost(assignments);
		System.out.println(best);
		boolean improvement;
		do {
			improvement = false;
			for (int i = 0; i < assignments.length; i++) { // each family i
				final int[] prefs = familyPrefs[i];
				final int famSize = familySize[i];
				final int assignedDay = assignments[i];
				final int assignedDayCap = dayCapacities[assignedDay];
				if (assignedDayCap - famSize >= MIN_PPL) {
					for (int j = 0; j < 10; j++) {
						final int candidateDay = prefs[j];
						if (candidateDay != assignedDay) {
							final int candidateDayCap = dayCapacities[candidateDay];
							if (candidateDayCap + famSize <= MAX_PPL) {
								final double delta = tryMove(assignedDay, candidateDay, famSize, i, assignments, best, temperature);
								if (delta < 0.0) {
									best += delta;
									System.out.println(best);
									improvement = true;
									i = 0; // todo: don't look bits
									break; // start from family 0 again
								}
								if (delta >= 0.0 && delta < Double.MAX_VALUE) {
									break; // family was moved, move on to next family
								}
							}
						}
					}
				}
			}
		} while (improvement);
		return best;
	}

	private void sanity(int[] assignments) {
		int[] dayCaps = new int[100 + 1];
		for (int i = 0; i < assignments.length; i++) {
			int assignment = assignments[i];
			dayCaps[assignment] += familySize[i];
		}
		for (int i = 1; i < dayCaps.length; i++) {
			int cap = dayCaps[i];
			if (cap != dayCapacities[i]) {
				System.err.println("day " + i + " cap inconsistency:\n" +
						Arrays.toString(dayCapacities) + " !=\n" + Arrays.toString(dayCaps));
			}
			if (cap < MIN_PPL) {
				System.err.println("cap " + cap + " < " + MIN_PPL);
			}
			if (cap > MAX_PPL) {
				System.err.println("cap " + cap + " > " + MAX_PPL);
			}
		}
	}

	private double optimise(final int[] assignments) {
		double temperature = 10;
		double coolingSchedule = 0.99;
		double best = localMinima(assignments, 0);
		System.out.println("best = " + best);
		for (int i = 0; i < 10000; i++) {
			localMinima(assignments, temperature);
			double score = localMinima(assignments, 0);
			if (score < best) {
				best = score;
				sanity(assignments);
				System.out.println("**** new best = " + best + " ****");
				CsvUtil.write(assignments, "../../solutions/" + score + ".csv");
				CsvUtil.write(assignments, "../../solutions/best.csv");
			}
			temperature *= coolingSchedule;
		}
		return best;
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		//int[][] starting_solution = CsvUtil.read("../../../submission_71647.5625.csv");
		//int[][] starting_solution = CsvUtil.read("/tmp/lala.csv"); // 77124.66595889143
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");
		//int[][] starting_solution = CsvUtil.read("../../solutions/bad.csv"); //  1725999432440170

		// 71757.52
		//int[][] starting_solution = CsvUtil.read("../../solutions/71535.25513531327.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}
		Main main = new Main(family_data, initialAsignments);
		//System.out.println(main.cost(initialAsignments));

		//double score = main.localMinima(initialAsignments, 0);
		//System.out.println(score);
		//CsvUtil.write(initialAsignments, "/tmp/x.csv");
		double score = main.optimise(initialAsignments);
		System.out.println("minima = " + score);
	}
}
