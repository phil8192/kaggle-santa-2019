/* h0 h0 h0 */

import java.awt.desktop.SystemSleepEvent;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static java.lang.Math.abs;

class Optimiser {

	static final String ANSI_RESET = "\u001B[0m";
	static final String ANSI_BLACK = "\u001B[30m";
	static final String ANSI_RED = "\u001B[31m";
	static final String ANSI_GREEN = "\u001B[32m";
	static final String ANSI_YELLOW = "\u001B[33m";
	static final String ANSI_BLUE = "\u001B[34m";
	static final String ANSI_PURPLE = "\u001B[35m";
	static final String ANSI_CYAN = "\u001B[36m";
	static final String ANSI_WHITE = "\u001B[37m";

	private static final int MAX_FAM_SIZE = 10;
	private static final int MAX_CHOICE = 10;
	static final int MIN_PPL = 125;
	static final int MAX_PPL = 300;

	private final double[][] accountingCost;
	final int[] dayCapacities;
	final int[] assignments;

	// all family penalties
	// 5000*100
	private final double[][] allPenalties;

	final int[][] familyPrefs;
	final int[] familySize;

	final Random prng;

	final BlockingQueue<Candidate> q;

	private final Set<Integer> bad_mondays = new HashSet<>();

	public int[] getAssignments() {
		return assignments;
	}

	Optimiser(int[][] familyData, int[] initialAssignments, final BlockingQueue<Candidate> q, final Random prng) {
		this.prng = prng;
		this.q = q;
		this.assignments = initialAssignments;
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
		//System.out.println(Thread.currentThread().getName() + " initialised with " + cost(assignments));

		bad_mondays.add(100);
		bad_mondays.add(93);
		bad_mondays.add(86);
		bad_mondays.add(79);
		bad_mondays.add(72);
		bad_mondays.add(65);
		bad_mondays.add(51);
		bad_mondays.add(44);

	}

	void sanity(int[] assignments) {
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

	double getPenalty(final int[] familyAssignments) {
		double penalty = 0.0;
		for (int i = 0, len = familyAssignments.length; i < len; i++) {
			final int day = familyAssignments[i];
			penalty += allPenalties[i][day];
//			if(bad_mondays.contains(day) && dayCapacities[day] > 125) {
//				penalty += 10000;
//			}
		}
		return penalty;
	}

	double getAccountingCost() {
		double accounting = 0.0;
		// first day is special
		accounting += getAccountingCost(dayCapacities[100], dayCapacities[100]);
		for (int i = 100; --i > 0; ) {
			accounting += getAccountingCost(dayCapacities[i], dayCapacities[i + 1]);
		}
		return accounting;
	}

	double cost(final int[] familyAssignments) {
		final double penalty = getPenalty(familyAssignments);
		final double accounting = getAccountingCost();
		return penalty + accounting;
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

	double getPenaltyDelta(final int fam, final int assignedDay, final int candidateDay) {
		double assignedPenalty = allPenalties[fam][assignedDay];
		double candidatePenalty = allPenalties[fam][candidateDay];

//		if(bad_mondays.contains(assignedDay) && dayCapacities[assignedDay] > 125) {
//			assignedPenalty += 10000;
//		}
//
//		if(bad_mondays.contains(candidateDay) && dayCapacities[candidateDay] >= 125) {
//			candidatePenalty += 10000;
//		}

		return candidatePenalty - assignedPenalty;
	}

	double getAccountingDelta(final int assignedDay, final int candidateDay, final int famSize) {
		final double ac1 = getAssignedAccountingDelta(assignedDay, famSize);
		final double ac2 = getCandidateAccountingDelta(assignedDay, candidateDay, famSize);
		return ac1 + ac2;
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

	double scan(final Integer[] fams, final int maxChoice, final double current, final double currentPenalty) {

		// todo: use getPenaltyDelta(final int fam, final int assignedDay, final int candidateDay)
		//double penaltyDelta = 0.0;

		// stash the original assignments
		final int[] original = new int[fams.length];
		for (int i = 0; i < fams.length; i++) {
			original[i] = assignments[fams[i]];
		}

		// try all possible configurations
		final List<List<Integer>> prods = Cartisian.product(fams.length, maxChoice);


		for (final List<Integer> prod : prods) { // for each set of choices
		//	System.out.println(prod);

			double penaltyDelta = 0.0;

			// set all the fams
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

//			final double __penalty = currentPenalty + penaltyDelta;
//			final double xpenalty = getPenalty(assignments);
//			if(Math.abs(__penalty - xpenalty) > 0.00001) {
//				System.out.println("X _pen = " + __penalty + " X pen = " + xpenalty);
//			}

			// if no constraint violation and improvement, return improvement delta
			if (checkCapacityConstraints()) {
				// expensive -> final double candidateCost = cost(assignments);


				final double _penalty = currentPenalty + penaltyDelta;
				//final double _penalty = getPenalty(assignments);
				//if(Math.abs(_penalty - penalty) > 0.00001) {
				//	System.out.println("X _pen = " + _penalty + " X pen = " + penalty);
				//}

				//System.out.println("pen = " + penalty + " _pen = " + _penalty);
				final double accountingCost = getAccountingCost();

				final double candidateCost = _penalty + accountingCost;
				final double delta = candidateCost - current;
				if (delta < 0) {
					//final double penalty = getPenalty(assignments);
					//System.out.println("pen = " + penalty + " _pen = " + _penalty);
					return delta;
				}
			}
			// put the back so can do penalty delta in next set of choices
			for (int i = 0; i < original.length; i++) {
				final int fam = fams[i];
				final int famSize = familySize[fam];
				final int assignedDay = assignments[fam];
				final int originalDay = original[i];
				dayCapacities[assignedDay] -= famSize;
				dayCapacities[originalDay] += famSize;
				assignments[fam] = originalDay;
			}
		}
		return 0;
	}

	private double randomBrute(final int fams, final int maxChoice, final double current, final double currentPenalty) {
		// get list of random family indices
		final Integer[] randomFams = prng.ints(0, 5000)
				.boxed()
				.distinct()
				.limit(fams)
				.toArray(Integer[]::new);
		return scan(randomFams, maxChoice, current, currentPenalty);
	}

	double randomBrute(final int rounds, final int fams, final int maxChoice, final double score) {
		double currentPenalty = getPenalty(assignments);
		double current = score;
		for (int i = 0; i < rounds; i++) {
			if(Thread.currentThread().isInterrupted()) {
				return current - score;
			}
			final double delta = randomBrute(fams, maxChoice, current, currentPenalty);
		//	System.out.println("probe = " + ANSI_GREEN + i + ANSI_RESET + " (" + String.format("%.2f", current) + ")");
			if (delta < 0) {
				current += delta;
				currentPenalty = getPenalty(assignments);
			}
		}
		return current - score;
	}

	double brute(final int fams, final int maxChoice, final double current) {
		double score = current;
		double currentPenalty = getPenalty(assignments);

		boolean improvement;
		List<List<Integer>> famCombos = Cartisian.product(fams, 5000);
		int i = 1;
		do {
			improvement = false;
			for (final List<Integer> combo : famCombos) {
				final Integer[] famIndices = combo.toArray(new Integer[]{});
				System.out.println(i + ": " + Arrays.toString(famIndices));
				final double delta = scan(famIndices, maxChoice, score, currentPenalty);

				if (delta < 0) {
					score += delta;

					//double p = getPenalty(assignments);
					//double a = getAccountingCost();
					//double c = p + a;

					currentPenalty = getPenalty(assignments);
//					System.out.println(String.format("%.2f", score) + " (" + c + ")");
//					if(Math.abs(c - score) > 0.00001) {
//						System.exit(0);
//					} else {


					//CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + "_b.csv");
					//CsvUtil.write(assignments, "../../solutions/best.csv");


					//}



i++;
					improvement = true;



				}
			}
		} while (improvement);
		return score - current;
	}

	double optimise() {
		final double score = cost(assignments);
		return randomBrute(1000000, 3, 5, score);
	}

}