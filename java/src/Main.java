import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class Main {

	static class BruteWorker implements Runnable {
		Brute brute;
		BruteWorker(Brute brute) {
			this.brute = brute;
		}

		@Override
		public void run() {
			brute.optimise();
			System.out.println(Thread.currentThread().getName() + " time to die...");
		}
	}

	public static BruteWorker startBruteWorker(final int[][] family_data, int[] assignments, int from, int to,
																						 BlockingQueue<Optimiser.Candidate> q ) {
		BruteWorker bw = new BruteWorker(new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, q));
		Executors.defaultThreadFactory().newThread(bw).start();
		System.out.println("alive...");
		return bw;
	}

	public static List<BruteWorker> startBruteWorkers(int[][] family_data, int[] initialAsignments,
																										BlockingQueue<Optimiser.Candidate> q ) {
		List<BruteWorker> l = new ArrayList<>();
		l.add(startBruteWorker(family_data, initialAsignments, 0, 500, q));
		l.add(startBruteWorker(family_data, initialAsignments, 500, 1000, q));
		l.add(startBruteWorker(family_data, initialAsignments, 1000, 1500, q));
		l.add(startBruteWorker(family_data, initialAsignments, 1500, 2000, q));
		l.add(startBruteWorker(family_data, initialAsignments, 2000, 2500, q));
		l.add(startBruteWorker(family_data, initialAsignments, 2500, 3000, q));
		l.add(startBruteWorker(family_data, initialAsignments, 3000, 3500, q));
		l.add(startBruteWorker(family_data, initialAsignments, 3500, 4000, q));
		l.add(startBruteWorker(family_data, initialAsignments, 4000, 4500, q));
		l.add(startBruteWorker(family_data, initialAsignments, 4500, 5000, q));
		return l;
	}

	public static void killBruteWorkers(List<BruteWorker> bruteWorkerList) {
		for(BruteWorker bruteWorker : bruteWorkerList) {
			bruteWorker.brute.kill();
		}
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");
		//int[][] starting_solution = CsvUtil.read("../../solutions/69629.07_b3.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		final BlockingQueue<Optimiser.Candidate> q = new SynchronousQueue<>();
		List<BruteWorker> bruteWorkers = null;
		boolean useBrute = true;
		if(useBrute) {

			int from = Integer.parseInt(meh[0]);
			int to = Integer.parseInt(meh[1]);
			bruteWorkers = startBruteWorkers(family_data, initialAsignments, q);
		//	double score  = brute.optimise();
		} else {
			SA sa = new SA(family_data, initialAsignments);
			double score = sa.optimise();
		}

		while(true) {
			try {
				System.out.println("waiting for new score...");
				Optimiser.Candidate candidate = q.take();
				final double score = candidate.getScore();
				System.out.println("got new score: " + String.format("%.2f", score));
				final int[] ass = candidate.getAss();
				final String method = candidate.getMethod();
				CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
				CsvUtil.write(ass, "../../solutions/best.csv");
				killBruteWorkers(bruteWorkers);
				bruteWorkers = startBruteWorkers(family_data, ass, q);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
