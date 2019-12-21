public class Main {
	private static final int MAX_FAM_SIZE = 10;
	private static final int MAX_CHOICE = 10;
	private static final int MIN_PPL = 125;
	private static final int MAX_PPL = 300;

	private final double[][] penalties;
	private final double[][] accounting;

	//private final int[][] choices;


	// all family penalties
	// 5000*100
	final double[][] all_penalties;

	Main(int[][] family_data) {
		this.penalties = new double[MAX_FAM_SIZE + 1][MAX_CHOICE];
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
		// init accounting matrix
		int max_cap = MAX_PPL - MIN_PPL;
		this.accounting = new double[max_cap][max_cap];
		for (int i = 0; i < max_cap; i++) {
			int now = MIN_PPL + i;
			for (int j = 0; j < max_cap; j++) {
				int pre = MIN_PPL + j;
				accounting[i][j] = ((now - 125) / 400.0) * Math.pow(now, 0.5 + (Math.abs(now - pre) / 50.0));
			}
		}
		// init all penalties matrix
		all_penalties = new double[5000][100+1];
		for(int i = 0; i < all_penalties.length; i++) {
			for(int j = 0; j < all_penalties[0].length; j++) {
				all_penalties[i][j] = Double.MAX_VALUE;
			}
		}
		for(int i = 0; i < family_data.length; i++) {
			int famSize = family_data[i][11];
			for(int j = 0; j < 10; j++) {
				int choice_j = family_data[i][j+1];
				all_penalties[i][choice_j] = penalties[famSize][j];
			}
		}
	}

	// [fam_choice, fam_choice] ... 5000

	private double cost(final int[] familyAssignments) {
		double penalty = 0.0;
		double accounting = 0.0;
		for(int i = 0, len = familyAssignments.length; i < len; i++) {
			final int day = familyAssignments[i];
			penalty += all_penalties[i][day];

		}
		return penalty + accounting;
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvReader.read("../../data/family_data.csv");
		int[][] starting_solution = CsvReader.read("../../submission_71647.5625.csv");
		int[] starting_assignments = starting_solution[1];
		Main main = new Main(family_data);
		System.out.println(main.cost(starting_assignments));
	}
}
