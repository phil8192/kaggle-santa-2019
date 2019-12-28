import java.util.Arrays;

import static java.lang.Math.abs;

public class Optimiser {

	static final String ANSI_RESET = "\u001B[0m";
	static final String ANSI_BLACK = "\u001B[30m";
	static final String ANSI_RED = "\u001B[31m";
	static final String ANSI_GREEN = "\u001B[32m";
	static final String ANSI_YELLOW = "\u001B[33m";
	static final String ANSI_BLUE = "\u001B[34m";
	static final String ANSI_PURPLE = "\u001B[35m";
	static final String ANSI_CYAN = "\u001B[36m";
	static final String ANSI_WHITE = "\u001B[37m";

	static final int MAX_FAM_SIZE = 10;
	static final int MAX_CHOICE = 10;
	static final int MIN_PPL = 125;
	static final int MAX_PPL = 300;

	final double[][] accountingCost;
	final int[] dayCapacities;

	// all family penalties
	// 5000*100
	final double[][] allPenalties;

	final int[][] familyPrefs;
	final int[] familySize;


	Optimiser(int[][] familyData, int[] initialAssignments) {
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
		final double assignedPenalty = allPenalties[fam][assignedDay];
		final double candidatePenalty = allPenalties[fam][candidateDay];
		return candidatePenalty - assignedPenalty;
	}

	double getAccountingDelta(final int assignedDay, final int candidateDay, final int famSize) {
		final double ac1 = getAssignedAccountingDelta(assignedDay, famSize);
		final double ac2 = getCandidateAccountingDelta(assignedDay, candidateDay, famSize);
		return ac1 + ac2;
	}

	boolean checkCapacityConstraints() {
		for (int i = 1; i < dayCapacities.length; i++) {
			final int cap = dayCapacities[i];
			if (cap < MIN_PPL || cap > MAX_PPL) {
				return false; // violation!
			}
		}
		return true;
	}

}
