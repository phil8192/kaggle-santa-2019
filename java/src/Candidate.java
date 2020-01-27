final class Candidate {
	private final int[] ass;
	private final double score;
	private final String method;

	/**
	 * candidate solution for queue.
	 * @param ass family assignments
	 * @param score score
	 * @param method algo/method.
	 */
	Candidate(int[] ass, double score, final String method) {
		this.ass = ass;
		this.score = score;
		this.method = method;
	}

	int[] getAss() {
		return ass;
	}

	double getScore() {
		return score;
	}

	public String getMethod() {
		return method;
	}
}