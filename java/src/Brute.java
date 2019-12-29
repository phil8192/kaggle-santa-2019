import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class Brute extends Optimiser {
	private final int from;
	private final int to;
	private final int maxChoice;

	private boolean running = true;

	public Brute(int[][] familyData, int[] initialAssignments, final int from, final int to, final int maxChoice,
							 final BlockingQueue<Candidate> q) {
		super(familyData, initialAssignments, q);
		this.from = from;
		this.to = to;
		this.maxChoice = maxChoice;
	}

	public void kill() {
		running = false;
	}

	double optimise() {
		final double current = cost(assignments);
		double score = cost(assignments);
		double currentPenalty = getPenalty(assignments);
		boolean improvement;
		do {
			improvement = false;
			for (int i = from; i < to; i++) {
				System.out.println(Thread.currentThread().getName() + " " + i);
				for (int j = i + 1; j < 5000; j++) {
					for (int k = j + 1; k < 5000; k++) {
//						if(!running) {
//							return score;
//						}
						if(Thread.currentThread().isInterrupted()) {
							return score;
						}
						final Integer[] famIndices = new Integer[]{i, j, k};
						final double delta = scan(famIndices, maxChoice, score, currentPenalty);
						//System.out.println(ANSI_GREEN + String.format("%.2f", score) + ANSI_RESET + " " + Arrays.toString(famIndices));
						if (delta < 0) {
							score += delta;
							currentPenalty = getPenalty(assignments);
							//System.out.println(String.format("%.2f", score));
							improvement = true;
//							CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + "_b3.csv");
//							CsvUtil.write(assignments, "../../solutions/best.csv");
							try {

									q.put(new Candidate(Arrays.copyOf(assignments, assignments.length), score, "b3"));

							} catch(InterruptedException e) {
								return score;
								//Thread.currentThread().interrupt();
							}
						}
					}
				}
			}
		} while (improvement);
		return score - current;
	}
}
