import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
			System.out.println(Thread.currentThread().getName() + " brute worker. time to die...");
		}
	}

	static class RandomBruteWorker implements Runnable {
		Optimiser optimiser;
		BlockingQueue<Candidate> q;
		public RandomBruteWorker(Optimiser optimiser, BlockingQueue<Candidate> q) {
			this.optimiser = optimiser;
			this.q = q;
		}
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				double score = optimiser.cost(optimiser.getAssignments());
				double newScore = score + optimiser.randomBrute(1000000, 3, 3, score);
				//System.out.println(newScore);
				if (newScore < score) {
					try {
						Candidate candidate = new Candidate(optimiser.getAssignments(), newScore, "rnd_brute");
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
			System.out.println(Thread.currentThread().getName() + " random brute worker. time to die...");
		}
	}

	public static Thread startBruteWorker(final int[][] family_data, int[] assignments, int from, int to,
																				BlockingQueue<Candidate> q, Random prng) {
		BruteWorker bw = new BruteWorker(new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, q, prng));
		Thread t = Executors.defaultThreadFactory().newThread(bw);
		t.start();
		System.out.println("brute worker alive...");
		return t;
	}

	public static Thread startRandomBruteWorker(final int[][] family_data, int[] assignments,
																							BlockingQueue<Candidate> q, Random prng) {
		Optimiser optimiser = new Optimiser(family_data, Arrays.copyOf(assignments, assignments.length), q, prng);
		RandomBruteWorker randomBruteWorker = new RandomBruteWorker(optimiser, q);
		Thread t = Executors.defaultThreadFactory().newThread(randomBruteWorker);
		t.start();
		System.out.println("random brute worker alive...");
		return t;
	}

	public static List<Thread> startBruteWorkers(int[][] family_data, int[] initialAsignments,
																										BlockingQueue<Candidate> q, Random prng) {
		List<Thread> l = new ArrayList<>();
		l.add(startBruteWorker(family_data, initialAsignments, 0, 500, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 500, 1000, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 1000, 1500, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 1500, 2000, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 2000, 2500, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 2500, 3000, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 3000, 3500, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 3500, 4000, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 4000, 4500, q, prng));
		l.add(startBruteWorker(family_data, initialAsignments, 4500, 5000, q, prng));
		l.add(startRandomBruteWorker(family_data, initialAsignments, q, prng));
		return l;
	}

	public static void killBruteWorkers(List<Thread> bruteWorkerList) {
		for(Thread bruteWorker : bruteWorkerList) {
			bruteWorker.interrupt();
		}
	}

	public static void main(String[] meh) {
		Random prng = new Random();

		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		//int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");
		int[][] starting_solution = CsvUtil.read("../../../submission_71672.50835891288.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		final BlockingQueue<Candidate> q = new SynchronousQueue<>();
		List<Thread> bruteWorkers = null;
		boolean useBrute = true;
		if(useBrute) {

			int from = Integer.parseInt(meh[0]);
			int to = Integer.parseInt(meh[1]);
			bruteWorkers = startBruteWorkers(family_data, initialAsignments, q, prng);

			double best = Double.MAX_VALUE;

			while(true) {
				try {
					System.out.println("waiting for new score...");
					Candidate candidate = q.take();
					final double score = candidate.getScore();
					final int[] ass = candidate.getAss();
					final String method = candidate.getMethod();
					if(score < best) {
						System.out.println("got new score: " + String.format("%.2f", score) + " (" + method + ")");
						CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
						CsvUtil.write(ass, "../../solutions/best.csv");
						System.out.println("killing workers");
						killBruteWorkers(bruteWorkers);
						bruteWorkers = startBruteWorkers(family_data, ass, q, prng);
						best = score;
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		} else {
			SA sa = new SA(family_data, initialAsignments, prng);
			double pen = sa.getPenalty(initialAsignments);
			double acc = sa.getAccountingCost();
			double score = pen + acc;
			System.out.println(score + " (" + pen + ") (" + acc + ")");
			sa.optimise();
		}
	}
}
