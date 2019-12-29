import java.util.Arrays;

public class Brute extends Optimiser {
	private final int from;
	private final int to;
	private final int maxChoice;

	public Brute(int[][] familyData, int[] initialAssignments, final int from, final int to, final int maxChoice) {
		super(familyData, initialAssignments);
		this.from = from;
		this.to = to;
		this.maxChoice = maxChoice;
	}

	double optimise() {
		final double current = cost(assignments);
		double score = cost(assignments);
		double currentPenalty = getPenalty(assignments);
		boolean improvement;
		do {
			improvement = false;
			for (int i = from; i < to; i++) {
				for (int j = i + 1; j < 5000; j++) {
					for (int k = j + 1; k < 5000; k++) {
						final Integer[] famIndices = new Integer[]{i, j, k};
						final double delta = scan(famIndices, maxChoice, score, currentPenalty);
						System.out.println(ANSI_GREEN + String.format("%.2f", score) + ANSI_RESET + " " + Arrays.toString(famIndices));
						if (delta < 0) {
							score += delta;
							currentPenalty = getPenalty(assignments);
							System.out.println(String.format("%.2f", score));
							improvement = true;
							CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + "_b3.csv");
							CsvUtil.write(assignments, "../../solutions/best.csv");
						}
					}
				}
			}
		} while (improvement);
		return score - current;
	}
}
