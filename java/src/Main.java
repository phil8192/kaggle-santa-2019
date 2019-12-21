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

	private Main(int[][] familyData, int[] initialAssignments) {
		this.familyPrefs = new int[5000][10];
		this.familySize = new int[5000];

		this.penalties = new double[MAX_FAM_SIZE + 1][MAX_CHOICE+1];
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
		allPenalties = new double[5000][100+1];
		for(int i = 0; i < allPenalties.length; i++) {
			for(int j = 0; j < allPenalties[0].length; j++) {
				allPenalties[i][j] = Double.MAX_VALUE;
			}
		}
		for(int i = 0; i < familyData.length; i++) {
			int famSize = familyData[i][11];
			familySize[i] = famSize;
			for(int j = 0; j < 10; j++) {
				int choice_j = familyData[i][j+1];
				familyPrefs[i][j] = choice_j;
				allPenalties[i][choice_j] = penalties[famSize][j];
			}
		}
		// init accounting matrix
		int max_cap = MAX_PPL+1; // 1 to 300 ppl
		accountingCost = new double[max_cap][max_cap];
		for (int i = 1; i < max_cap; i++) {
			for (int j = 1; j < max_cap; j++) {
				accountingCost[i][j] = ((i - 125) / 400.0) * Math.pow(i, 0.5 + (abs(i - j) / 50.0));
			}
		}
		// init day capacities
		dayCapacities = new int[100+1];
		for(int i = 0; i < initialAssignments.length; i++) {
			int day = initialAssignments[i];
			int famSize = familyData[i][11];
			dayCapacities[day]+=famSize;
		}
	}

	private double getAccountingCost(final int now, final int pre) {
		//int diff = abs(now - pre);
		//return ((now - 125) / 400.0) * Math.pow(now, 0.5 + (diff / 50.0));
		return accountingCost[now][pre];
	}

	private double cost(final int[] familyAssignments) {
		double penalty = 0.0;
		double accounting = 0.0;
		// day assignment penalty
		for(int i = 0, len = familyAssignments.length; i < len; i++) {
			final int day = familyAssignments[i];
			penalty += allPenalties[i][day];
		}
		// accounting penalty
		// first day is special
		accounting += getAccountingCost(dayCapacities[100], dayCapacities[100]);
		// rest of days starting at day 99
		for(int i = 100; --i > 0; ) {
			accounting += getAccountingCost(dayCapacities[i], dayCapacities[i+1]);
		}
		return penalty + accounting;
	}

	private double localMinima(final int[] assignments) {
		double best = cost(assignments);
		boolean improvement;
		do {
			improvement = false;
			for (int i = 0; i < assignments.length; i++) { // each family i
				//System.out.println("consider family " + i);
				final int[] prefs = familyPrefs[i];
				final int famSize = familySize[i];
				final int assignedDay = assignments[i];
				final int assignedDayCap = dayCapacities[assignedDay];
				if (assignedDayCap - famSize < MIN_PPL) {
					continue; // can not move this family out of current day (constraint violation)
				}
				for (int j = 0; j < 10; j++) {
					final int candidateDay = prefs[j];
					if (candidateDay != assignedDay) {
						final int candidateDayCap = dayCapacities[candidateDay];
						if (candidateDayCap + famSize > MAX_PPL) {
							continue; // can not move this family to candidate day (constraint violation)
						}
						dayCapacities[assignedDay] -= famSize;
						dayCapacities[candidateDay] += famSize;
						assignments[i] = candidateDay;
						final double candidateScore = cost(assignments);
						if (candidateScore < best) {
							best = candidateScore;
							System.out.println("new best = " + best);
							improvement = true;
							break;
						} else {
							dayCapacities[assignedDay] += famSize;
							dayCapacities[candidateDay] -= famSize;
							assignments[i] = assignedDay;
						}
					}
				}
			}
		} while(improvement);
		return best;
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvReader.read("../../../data/family_data.csv");
		int[][] starting_solution = CsvReader.read("../../../submission_71647.5625.csv");
		//int[][] starting_solution = CsvReader.read("/tmp/lala.csv");
		//int[][] starting_solution = CsvReader.read("../../lala.csv");
		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for(int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}
		Main main = new Main(family_data, initialAsignments);
		double score = main.localMinima(initialAsignments);
		System.out.println("minima = " + score);
	}
}
