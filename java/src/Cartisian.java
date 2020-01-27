import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

public class Cartisian {
	/**
	 * cartesian product of n_fams x max_choice
	 * @param fams number of families
	 * @param maxChoice max choice for each family
	 * @return cartesian products.
	 */
	public static List<List<Integer>> product(final int fams, final int maxChoice) {
		final List<Integer> l = new ArrayList<>();
		for(int i = 0; i < maxChoice; i++) {
			l.add(i);
		}
		final ArrayList<List<Integer>> args = new ArrayList<>();
		for(int i = 0; i < fams; i++) {
			args.add(l);
		}
		return Lists.cartesianProduct(args);
	}
}
