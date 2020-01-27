import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class Brute extends Optimiser {
	private final int from;
	private final int to;
	private final int maxChoice;

	public Brute(int[][] familyData, int[] initialAssignments, final int from, final int to, final int maxChoice,
							 final BlockingQueue<Candidate> q, Random prng) {
		super(familyData, initialAssignments, q, prng);
		this.from = from;
		this.to = to;
		this.maxChoice = maxChoice;
	}

	/**
	 * brute force all 3 family moves.
	 * @return delta from starting score.
	 */
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
						if(Thread.currentThread().isInterrupted()) {
							return score;
						}
						final Integer[] famIndices = new Integer[]{i, j, k};
						final double delta = scan(famIndices, maxChoice, score, currentPenalty);
						if (delta < 0) {
							score += delta;
							currentPenalty = getPenalty(assignments);
							improvement = true;
							try {
									q.put(new Candidate(Arrays.copyOf(assignments, assignments.length), score, "b3"));
							} catch(InterruptedException e) {
								return score;
							}
						}
					}
				}
			}
		} while (improvement);
		return score - current;
	}
}
