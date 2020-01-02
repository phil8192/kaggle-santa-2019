import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

	//static AtomicInteger tripwire = new AtomicInteger(0);

	static class BruteWorker implements Runnable {
		Brute brute;
		BruteWorker(Brute brute) {
			this.brute = brute;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(5*1000);
				brute.optimise();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			//System.out.println(Thread.currentThread().getName() + " brute worker. time to die...");
		}
	}

	static class SAWorker implements Runnable {
		BlockingQueue<Candidate> q;
		final int[][] family_data;
		final int[] assignments;
		final Random prng;
		double temperature;
		double coolingSchedule;
		String id;

		public SAWorker(final int[][] family_data, final int[] assignemnts, final Random prng, BlockingQueue<Candidate> q,
										double temperature, double coolingSchedule, String id) {
			this.q = q;
			this.family_data = family_data;
			this.assignments = Arrays.copyOf(assignemnts, assignemnts.length);
			this.prng = prng;
			this.temperature = temperature;
			this.coolingSchedule = coolingSchedule;
			this.id = id;
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				SA sa = new SA(family_data, Arrays.copyOf(assignments, assignments.length), prng, temperature, coolingSchedule);
				double score = sa.cost(sa.getAssignments());
				double candidateScore = sa.optimise();
				//System.out.println("sa: " + score + " > " + candidateScore);
				if(candidateScore < score) {
					try {
						//tripwire.incrementAndGet();
						Candidate candidate = new Candidate(Arrays.copyOf(sa.getAssignments(), 5000), candidateScore, "sa_"+id);
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						//tripwire.decrementAndGet();
					}
				}
			}
		}
	}

	public static Thread startSAWorker(final int[][] family_data, int[] assignments,
																		 BlockingQueue<Candidate> q, Random prng,
																		 double temperature, double coolingSchedule, String id) {
		SAWorker saWorker = new SAWorker(family_data, assignments, prng, q, temperature, coolingSchedule, id);
		Thread t = Executors.defaultThreadFactory().newThread(saWorker);
		t.start();
		//System.out.println("brute worker alive...");
		return t;
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
			double score = optimiser.cost(optimiser.getAssignments());
			while (!Thread.currentThread().isInterrupted()) {

				long l = System.currentTimeMillis();
				double newScore = score + optimiser.randomBrute(1000, 4, 5, score);
				//System.out.println("rnd brute took " + (System.currentTimeMillis() - l) + "ms.");
				//System.out.println(newScore);
				if (newScore < score) {
					score = newScore;
					try {
						//tripwire.incrementAndGet();
						Candidate candidate = new Candidate(Arrays.copyOf(optimiser.getAssignments(), 5000), newScore, "rnd_brute");
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						//tripwire.decrementAndGet();
					}
				}
			}
			//System.out.println(Thread.currentThread().getName() + " random brute worker. time to die...");
		}
	}

	public static Thread startBruteWorker(final int[][] family_data, int[] assignments, int from, int to,
																				BlockingQueue<Candidate> q, Random prng) {
		BruteWorker bw = new BruteWorker(new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, q, prng));
		Thread t = Executors.defaultThreadFactory().newThread(bw);
		t.start();
		//System.out.println("brute worker alive...");
		return t;
	}

	public static Thread startRandomBruteWorker(final int[][] family_data, int[] assignments,
																							BlockingQueue<Candidate> q, Random prng) {
		Optimiser optimiser = new Optimiser(family_data, Arrays.copyOf(assignments, assignments.length), q, prng);
		RandomBruteWorker randomBruteWorker = new RandomBruteWorker(optimiser, q);
		Thread t = Executors.defaultThreadFactory().newThread(randomBruteWorker);
		t.start();
		//System.out.println("random brute worker alive...");
		return t;
	}

	public static List<Thread> startBruteWorkers(int[][] family_data, int[] initialAsignments,
																										BlockingQueue<Candidate> q, Random prng, boolean brute) {
		List<Thread> l = new ArrayList<>();

		// 2 sa threads (slow, fast)
		//l.add(startSAWorker(family_data, initialAsignments, q, prng, 3, 0.9999999, "slow"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));
		l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt(1337)), 3, 0.999999, "fast"));


		// 1 random brute worker
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, prng));

//		if(brute) {
//			// 10 brute force threads
//			l.add(startBruteWorker(family_data, initialAsignments, 0, 500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 500, 1000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 1000, 1500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 1500, 2000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 2000, 2500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 2500, 3000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 3000, 3500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 3500, 4000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 4000, 4500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 4500, 5000, q, prng));
//		}
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
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");
//		int[][] starting_solution = CsvUtil.read("/tmp/lala.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		final BlockingQueue<Candidate> q = new SynchronousQueue<>();
		List<Thread> bruteWorkers = null;
		boolean useBrute = true;
		if(useBrute) {

			bruteWorkers = startBruteWorkers(family_data, initialAsignments, q, prng, true);

			double best = Double.MAX_VALUE;

			while(true) {
				try {
					//System.out.println("waiting for new score...");
					long l = System.currentTimeMillis();
					Candidate candidate = q.take();
					final double score = candidate.getScore();
					final int[] ass = candidate.getAss();
					final String method = candidate.getMethod();
					//System.out.println("got new score: " + String.format("%.2f", score) + " (" + method + ")");
					if(score < best) {
						System.out.println("score: " + String.format("%.2f", score) + " (" + method + ")");
						CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
						CsvUtil.write(ass, "../../solutions/best.csv");
						//System.out.println("killing workers");

						//if(tripwire.get() == 0) { // only killall if noting waiting to share a result.
							System.out.println("killall");
							killBruteWorkers(bruteWorkers);
							bruteWorkers = startBruteWorkers(family_data, ass, q, prng, true);
						//}
						best = score;
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		} else {
			SA sa = new SA(family_data, initialAsignments, prng, 3,999999);
			double pen = sa.getPenalty(initialAsignments);
			double acc = sa.getAccountingCost();
			double score = pen + acc;
			System.out.println(score + " (" + pen + ") (" + acc + ")");
			sa.optimise();
		}
	}
}
