/* h0 h0 h0 */

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.random;

public class Main {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";


	private static final int MAX_FAM_SIZE = 10;
	private static final int MAX_CHOICE = 10;
	private static final int MIN_PPL = 125;
	private static final int MAX_PPL = 300;

	private final double[][] accountingCost;

	private final int[] dayCapacities;

	// all family penalties
	// 5000*100
	private final double[][] allPenalties;

	private final int[][] familyPrefs;
	private final int[] familySize;

	private final Random prng = new Random();

	private Main(int[][] familyData, int[] initialAssignments) {
		this.familyPrefs = new int[5000][10];
		this.familySize = new int[5000];
		double[][] penalties = new double[MAX_FAM_SIZE + 1][MAX_CHOICE + 1];
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

	private double getAccountingCost(final int now, final int pre) {
		return accountingCost[now][pre];
	}

	private double getPenalty(final int[] familyAssignments) {
		double penalty = 0.0;
		for (int i = 0, len = familyAssignments.length; i < len; i++) {
			final int day = familyAssignments[i];
			penalty += allPenalties[i][day];
		}
		return penalty;
	}

	private double getAccountingCost() {
		double accounting = 0.0;
		// first day is special
		accounting += getAccountingCost(dayCapacities[100], dayCapacities[100]);
		for (int i = 100; --i > 0; ) {
			accounting += getAccountingCost(dayCapacities[i], dayCapacities[i + 1]);
		}
		return accounting;
	}

	private double cost(final int[] familyAssignments) {
		final double penalty = getPenalty(familyAssignments);
		final double accounting = getAccountingCost();
		return penalty + accounting;
	}

	private double acceptanceProbability(final double oldScore, final double newScore, final double temperature) {
		final double d = newScore - oldScore;
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	// the amount that assignedDay contributes to the accounting cost.
	private double getAssignedAccountingDelta(final int assignedDay, final int famSize) {
		final double ac1; // how it is.
		final double ac2; // how it is *without* family.
		if (assignedDay == 1) {
			ac1 = getAccountingCost(dayCapacities[1], dayCapacities[2]);
			ac2 = getAccountingCost(dayCapacities[1] - famSize, dayCapacities[2]);
		} else if (assignedDay == 100) {
			ac1 = getAccountingCost(dayCapacities[99], dayCapacities[100]) + getAccountingCost(dayCapacities[100], dayCapacities[100]);
			ac2 = getAccountingCost(dayCapacities[99], dayCapacities[100] - famSize) + getAccountingCost(dayCapacities[100] - famSize, dayCapacities[100] - famSize);
		} else {
			final int nextDayCap = dayCapacities[assignedDay - 1];
			final int currDayCap = dayCapacities[assignedDay];
			final int prevDayCap = dayCapacities[assignedDay + 1];
			final int propDayCap = currDayCap - famSize;
			ac1 = getAccountingCost(nextDayCap, currDayCap) + getAccountingCost(currDayCap, prevDayCap);
			ac2 = getAccountingCost(nextDayCap, propDayCap) + getAccountingCost(propDayCap, prevDayCap);
		}
		return ac2 - ac1;
	}

	// the amount that candidateDay will contribute to the accounting cost.
	private double getCandidateAccountingDelta(final int assignedDay, final int candidateDay, final int famSize) {
		final double ac1; // how it is
		final double ac2; // how it is *with* family.

		if (candidateDay == 1) {
			ac1 = getAccountingCost(dayCapacities[1], dayCapacities[2] - (2 == assignedDay ? famSize : 0));
			ac2 = getAccountingCost(dayCapacities[1] + famSize, dayCapacities[2] - (2 == assignedDay ? famSize : 0));
		} else if (candidateDay == 100) {
			ac1 = getAccountingCost(dayCapacities[99] - (99 == assignedDay ? famSize : 0), dayCapacities[100]) + getAccountingCost(dayCapacities[100], dayCapacities[100]);
			ac2 = getAccountingCost(dayCapacities[99] - (99 == assignedDay ? famSize : 0), dayCapacities[100] + famSize) + getAccountingCost(dayCapacities[100] + famSize, dayCapacities[100] + famSize);
		} else {
			final int nextDayCap = dayCapacities[candidateDay - 1] - (candidateDay - 1 == assignedDay ? famSize : 0);
			final int currDayCap = dayCapacities[candidateDay];
			final int prevDayCap = dayCapacities[candidateDay + 1] - (candidateDay + 1 == assignedDay ? famSize : 0);
			final int propDayCap = currDayCap + famSize;
			ac1 = getAccountingCost(nextDayCap, currDayCap) + getAccountingCost(currDayCap, prevDayCap);
			ac2 = getAccountingCost(nextDayCap, propDayCap) + getAccountingCost(propDayCap, prevDayCap);
		}
		return ac2 - ac1;
	}

	private double getPenaltyDelta(final int fam, final int assignedDay, final int candidateDay) {
		final double assignedPenalty = allPenalties[fam][assignedDay];
		final double candidatePenalty = allPenalties[fam][candidateDay];
		return candidatePenalty - assignedPenalty;
	}

	private double getAccountingDelta(final int assignedDay, final int candidateDay, final int famSize) {
		final double ac1 = getAssignedAccountingDelta(assignedDay, famSize);
		final double ac2 = getCandidateAccountingDelta(assignedDay, candidateDay, famSize);
		return ac1 + ac2;
	}

	private double localMinima(final int[] assignments, final double temperature, final int maxLoops) {
		double currentPenalty = getPenalty(assignments);
		double currentAccount = getAccountingCost();
		double current = currentPenalty + currentAccount;
		boolean improvement;
		int max = maxLoops;
		do {
			improvement = false;
			fams:
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

								final double penaltyDelta = getPenaltyDelta(i, assignedDay, candidateDay);
								final double accountDelta = getAccountingDelta(assignedDay, candidateDay, famSize);
								final double delta = penaltyDelta + accountDelta;

								final double candidateScore = current + delta;
								if (candidateScore < current || (maxLoops > 0 && acceptanceProbability(current, candidateScore, temperature) >= prng.nextDouble())) {
									dayCapacities[assignedDay] -= famSize;
									dayCapacities[candidateDay] += famSize;
									assignments[i] = candidateDay;

									current += delta;
									currentPenalty += penaltyDelta;
									currentAccount += accountDelta;

//									final double sanity = cost(assignments) - current;
//									if(Math.abs(sanity) >= 0.00001) {
//										System.out.println(current + " != " + cost(assignments));
//										System.exit(1);
//									}

									if (delta < 0.0) {
										//System.out.println(current);
										improvement = true;
										if (temperature == 0) break fams; // start from family 0 again
										else break;
										//break fams;
									} else {
										break; // family was moved, move on to next family
									}
								}
							}
						}
					}
				}
			}
		} while (improvement && (maxLoops == 0 || --max > 0));
		return current;
	}

	private double optimise(final int[] assignments) {
		//double temperature = 5;
		//double coolingSchedule = 0.9999999;
		double temperature = 5;
		double coolingSchedule = 0.99999;
		double best = localMinima(assignments, 0, 0);
		System.out.println("best = " + String.format("%.2f", best));
		while (temperature > 0.001) {
			localMinima(assignments, temperature, 1);
			double score = localMinima(assignments, 0, 0);
			System.out.println(String.format("%.2f", score) + " T = " + String.format("%.6f", temperature));
			if (score < best) {
				best = score;
				sanity(assignments);
				System.out.println("**** new best = " + String.format("%.2f", best) + " **** T = " + temperature);
				CsvUtil.write(assignments, "../../solutions/" + score + ".csv");
				CsvUtil.write(assignments, "../../solutions/best.csv");
			}
			temperature *= coolingSchedule;
		}
		return best;
	}

	private boolean checkCapacityConstraints() {
		for (int i = 1; i < dayCapacities.length; i++) {
			final int cap = dayCapacities[i];
			if (cap < MIN_PPL || cap > MAX_PPL) {
				return false; // violation!
			}
		}
		return true;
	}

	private double scan(final Integer[] fams, final int[] assignments, final int maxChoice, final double current) {
		final double origPenalty = getPenalty(assignments);

		// todo: use getPenaltyDelta(final int fam, final int assignedDay, final int candidateDay)
		double penaltyDelta = 0.0;

		// stash the original assignments
		final int[] original = new int[fams.length];
		for (int i = 0; i < fams.length; i++) {
			original[i] = assignments[fams[i]];
		}

		// try all possible configurations
		final List<List<Integer>> prods = Cartisian.product(fams.length, maxChoice);
		for (final List<Integer> prod : prods) {
			//System.out.println(prod);
			for (int i = 0; i < fams.length; i++) {
				final int fam = fams[i];
				final int famSize = familySize[fam];
				final int choice = prod.get(i);
				final int assignedDay = assignments[fam];
				final int candidateDay = familyPrefs[fam][choice];

				// assign this family
				dayCapacities[assignedDay] -= famSize;
				dayCapacities[candidateDay] += famSize;
				assignments[fam] = candidateDay;

				penaltyDelta += getPenaltyDelta(fam, assignedDay, candidateDay);

			}
			// if no constraint violation and improvement, return improvement delta
			if (checkCapacityConstraints()) {
				// expensive -> final double candidateCost = cost(assignments);
				final double accountingCost = getAccountingCost();
				final double candidateCost = (origPenalty+penaltyDelta) + accountingCost;
				final double delta = candidateCost - current;
				if (delta < 0) {
					return delta;
				}
			}
		}
		// no improvement found. reset back to original
		for (int i = 0; i < original.length; i++) {
			final int fam = fams[i];
			final int famSize = familySize[fam];
			final int assignedDay = assignments[fam];
			final int originalDay = original[i];
			dayCapacities[assignedDay] -= famSize;
			dayCapacities[originalDay] += famSize;
			assignments[fam] = originalDay;
		}
		return 0;
	}

	private double brute(final int[] assignments, final int fams, final int maxChoice, final double current) {
		double score = current;
		boolean improvement;
		List<List<Integer>> famCombos = Cartisian.product(fams, 5000);
		do {
			improvement = false;
			for (final List<Integer> combo : famCombos) {
				final Integer[] famIndices = combo.toArray(new Integer[]{});
				final double delta = scan(famIndices, assignments, maxChoice, current);
				System.out.println(Arrays.toString(famIndices));
				if (delta < 0) {
					score += delta;
					improvement = true;
					CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + ".csv");
				}
			}
		} while(improvement);
		return score - current;
	}

	private double randomBrute(final int[] assignments, final int fams, final int maxChoice, final double current) {
		// get list of random family indices
		final Integer[] randomFams = prng.ints(0, 5000)
				.boxed()
				.distinct()
				.limit(fams)
				.toArray(Integer[]::new);
		return scan(randomFams, assignments, maxChoice, current);
	}

	private double randomBrute(final int rounds, final int[] assignments, final int fams, final int maxChoice, final double score) {
		double current = score;
		for (int i = 0; i < rounds; i++) {
			final double delta = randomBrute(assignments, 1 + prng.nextInt(fams), maxChoice, current);
			System.out.println("probe = " + ANSI_GREEN + i + ANSI_RESET + " (" + String.format("%.2f", current) + ")");
			if (delta < 0) {
				current += delta;
				//System.out.println("**** new brute score = " + current + "****");
				CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", current) + ".csv");
				CsvUtil.write(assignments, "../../solutions/best.csv");
			}
		}
		return current - score;
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
		//System.out.println(main.cost(initialAsignments));

		//final long l = System.currentTimeMillis();
		//double score = main.localMinima(initialAsignments, 0, 0);
		//System.out.println(System.currentTimeMillis() - l + "ms.");
		//System.out.println(score);
		//CsvUtil.write(initialAsignments, "/tmp/x.csv");
		//main.optimise(initialAsignments);

		double score = main.cost(initialAsignments);
		// 10 million rounds of random brute force.
		double bruteDiff = main.brute(initialAsignments, 2, 4, score);
		//if(bruteDiff < 0) {
		//	CsvUtil.write(initialAsignments, "/tmp/x.csv");
		//}
		System.out.println("score = " + (score + bruteDiff));

	}
}
