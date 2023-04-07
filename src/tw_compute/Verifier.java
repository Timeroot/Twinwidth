package tw_compute;
import java.util.Arrays;

//Simple class for checking the tww of a given contraction sequence, and that it's valid.
//Also provides useful info such as, does a given vertex saturate the contraction sequence?
public class Verifier {
	public static int validate(Graph g, int[] sol) {
//		System.out.println(Arrays.toString(sol));
		Trigraph tg = Trigraph.fromBigraph(g);
		int N = g.N;
		if(sol.length != 2*(N-1))
			throw new RuntimeException("Wrong size array");
		
		boolean[] gone = new boolean[N];
		int maxRed = 0;
		for(int i=0; i<N-1; i++) {
			int u = sol[2*i];
			int v = sol[2*i + 1];
			if(gone[u])
				throw new RuntimeException("Vertex "+u+" is gone");
			if(gone[v])
				throw new RuntimeException("Vertex "+v+" is gone");
			if(u == v)
				throw new RuntimeException("Merging "+u+" with itself");
			if(v == 0)
				throw new RuntimeException("Merging 0 away");
			
			int thisRed = tg.mergeRed(u, v);
			maxRed = Math.max(maxRed, thisRed);
			
			gone[v] = true;
		}
		if(gone[0])
			throw new RuntimeException("Vertex 0 is gone");
		
		return maxRed;
	}
}
