import java.util.Arrays;
import java.util.HashSet;

public class Trigraph {
	int N;
	HashSet<Integer>[] eBlk;
	HashSet<Integer>[] eRed;
	int[] degBlk, degRed;
	int maxRed;

	static final boolean CHECK = false;
	
	public Trigraph(int N_, HashSet<Integer>[] eBlk_, HashSet<Integer>[] eRed_,
			int[] degBlk_, int[] degRed_, int maxRed_) {
		N = N_;
		eBlk = eBlk_;
		eRed = eRed_;
		degBlk = degBlk_;
		degRed = degRed_;
		maxRed = maxRed_;
	}

	public Trigraph(int N_, HashSet<Integer>[] eBlk_, HashSet<Integer>[] eRed_) {
		N = N_;
		eBlk = eBlk_;
		eRed = eRed_;
		degBlk = new int[N];
		degRed = new int[N];
		maxRed = 0;
		
		for(int i=0; i<N; i++) {
			degBlk[i] = eBlk_[i].size();
			degRed[i] = eRed_[i].size();
			maxRed = Math.max(maxRed, degRed[i]);
		}
	}

	@SuppressWarnings("unchecked")
	static Trigraph fromBigraph(Graph g){
		int N = g.N;
		HashSet<Integer>[] eRed = new HashSet[N];
		for(int i=0; i<N; i++) {
			eRed[i] = (HashSet<Integer>) new HashSet<Integer>();
		}
		return new Trigraph(N, cloneList(g.eList), eRed,
				Arrays.copyOf(g.deg,N), new int[N], 0);
	}
	
	void checkConsistency() {
		if(!CHECK)
			return;
		
		int Eb=0, Er=0;
		int checkMaxRed = 0;
		for(int i=0; i<N; i++) {
			Eb+=eBlk[i].size();
			Er+=eRed[i].size();
			if(eBlk[i].size() != degBlk[i])
				throw new RuntimeException("Consistency fail (1) "+i+" ["+eBlk[i].size()+" != "+degBlk[i]+"]");
			if(eRed[i].size() != degRed[i])
				throw new RuntimeException("Consistency fail (2) "+i+" ["+eRed[i].size()+" != "+degRed[i]+"]");

			for(int j : eBlk[i])
				if(!eBlk[j].contains(i))
					throw new RuntimeException("Consistency fail (3) "+i+"-"+j);
			for(int j : eRed[i])
				if(!eRed[j].contains(i))
					throw new RuntimeException("Consistency fail (4) "+i+"-"+j);
			
			checkMaxRed = Math.max(checkMaxRed, degRed[i]);
		}
		if(Eb % 2 != 0)
			throw new RuntimeException("Consistency fail (5)");
		if(Er % 2 != 0)
			throw new RuntimeException("Consistency fail (6)");
//		if(checkMaxRed != maxRed)
//			throw new RuntimeException("Consistency fail (7) "+maxRed+" != "+checkMaxRed);
	}
	
	//Clear all edges from a vertex v.
	//Destructive, obviously. May leave maxRed incorrect.
	void clearVertex(int v) {
		for(int vo : eBlk[v]){
			eBlk[vo].remove(v);
			degBlk[vo]--;
		}
		for(int vo : eRed[v]) {
			eRed[vo].remove(v);
			degRed[vo]--;
		}
		eBlk[v].clear();
		eRed[v].clear();
		degRed[v] = degBlk[v] = 0;
	}
	
	//Deletes the edge (of either color) between v1 and v2. May leave maxRed incorrect.
	void clearEdge(int v1, int v2) {
		if(eBlk[v1].remove(v2)) {
			eBlk[v2].remove(v1);
			degBlk[v1]--;
			degBlk[v2]--;
		} else if(eRed[v1].remove(v2)) {
			eRed[v2].remove(v1);
			degRed[v1]--;
			degRed[v2]--;
		} else {
			throw new RuntimeException("Tried to clear edge "+v1+"-"+v2+" that wasn't in graph.");
		}
	}
	
	void addEdge(int v1, int v2, boolean black) {
		if(black) {
			if(eRed[v1].contains(v2))
				throw new RuntimeException("Tried to add black edge "+v1+"-"+v2+" that was already red.");
			boolean worked = eBlk[v1].add(v2);
			if(!worked)
				throw new RuntimeException("Tried to add black edge "+v1+"-"+v2+" that was already in graph.");
			eBlk[v2].add(v1);
			degBlk[v1]++;
			degBlk[v2]++;
		} else {
			if(eBlk[v1].contains(v2))
				throw new RuntimeException("Tried to add red edge "+v1+"-"+v2+" that was already black.");
			boolean worked = eRed[v1].add(v2);
			if(!worked)
				throw new RuntimeException("Tried to add red edge "+v1+"->"+v2+" that was already in graph.");
			eRed[v2].add(v1);
			degRed[v1]++;
			degRed[v2]++;
			
			if(degRed[v1] > maxRed)
				maxRed = degRed[v1];
			if(degRed[v2] > maxRed)
				maxRed = degRed[v2];
		}
	}

	@SuppressWarnings("unchecked")
	private static HashSet<Integer>[] cloneList(HashSet<Integer>[] arr){
		int N = arr.length;
		HashSet<Integer>[] newArr = new HashSet[N];
		for(int i=0; i<N; i++) {
			newArr[i] = (HashSet<Integer>) arr[i].clone();
		}
		return newArr;
	}
	
	Trigraph copy() {
		HashSet<Integer>[] newBlk = cloneList(eBlk);
		HashSet<Integer>[] newRed = cloneList(eRed);
		return new Trigraph(N, newBlk, newRed, Arrays.copyOf(degBlk,N), Arrays.copyOf(degRed,N), maxRed);
	}
	
	public int E() {
		checkConsistency();
		return EBlk() + ERed();
	}
	
	public int EBlk() {
		int tot = 0;
		for(int i=0; i<N; i++) {
			tot += degBlk[i];
		}
		return tot;
	}
	
	public int ERed() {
		int tot = 0;
		for(int i=0; i<N; i++) {
			tot += degRed[i];
		}
		return tot;
	}
	
	//remove all edges
	void clear() {
		for(int i=0; i<N; i++) {
			eBlk[i].clear();
			eRed[i].clear();
			degBlk[i] = degRed[i] = 0;
		}
		checkConsistency();
	}
	
	//How many vertices have degree > 0, the 'effective' N?
	int nonZeroDegN() {
		int res = 0;
		for(int i=0; i<N; i++)
			if(degBlk[i] > 0 || degRed[i] > 0)
				res++;
		return res;
	}
	
	//adds vNew many new vertices
	void expandBy(int vNew) {
		int Nnew = N + vNew;
		degBlk = Arrays.copyOf(degBlk, Nnew);
		degRed = Arrays.copyOf(degRed, Nnew);
		eBlk = Arrays.copyOf(eBlk, Nnew);
		eRed = Arrays.copyOf(eRed, Nnew);
		for(int i=N; i<Nnew; i++) {
			eBlk[i] = new HashSet<>();
			eRed[i] = new HashSet<>();
		}
		N += vNew;
	}

	//merge two vertices, marking new edges as red as necessary
	//returns new max red degree from the merge
	int mergeRed(int v1, int v2) {
		checkConsistency();
		
		if(v1 == v2)
			throw new RuntimeException("Can't merge "+v1+" to itself");
		
		//Step 1: Remove links between the pair
		if(eBlk[v1].remove(v2)) {
			eBlk[v2].remove(v1);
		}
		if(eRed[v1].remove(v2)) {
			eRed[v2].remove(v1);
		}
		
		//Step 2: Construct v1 neighborhood
		@SuppressWarnings("unchecked")
		HashSet<Integer> N2mN1 = (HashSet<Integer>)eBlk[v2].clone();
		N2mN1.removeAll(eBlk[v1]);

		@SuppressWarnings("unchecked")
		HashSet<Integer> N1mN2 = (HashSet<Integer>)eBlk[v1].clone();
		N1mN2.removeAll(eBlk[v2]);
		
		HashSet<Integer> newRed = eRed[v1];
		newRed.addAll(eRed[v2]);
		newRed.addAll(N2mN1);
		newRed.addAll(N1mN2);

		HashSet<Integer> newBlk = eBlk[v1];
		newBlk.addAll(eBlk[v2]);
		newBlk.removeAll(newRed);
		
		degBlk[v1] = newBlk.size();
		degRed[v1] = newRed.size();
		
		//Step 3: Clear v2
		clearVertex(v2);
		
		//Step 4: Update neighbors (and track max red deg)
		int localMaxRed = degRed[v1];
		
//		for(int nb : eBlk[v1]) {
			//it's black now, so must have had both v1 and v2 as black before.
			//its degree dropped by one from clearVertex(v2), which is the only change it saw.
			//it's already in v1's black neighbors, so ... nothing to do!
//		}
		
		for(int nr : eRed[v1]) {
			if(eBlk[nr].remove(v1)) {
				degBlk[nr]--;
			}
			if(eRed[nr].add(v1)) {
				degRed[nr]++;
				int redNR = degRed[nr];
				localMaxRed = Math.max(localMaxRed, redNR);
			}
		}
		
		if(localMaxRed > maxRed) {
			maxRed = localMaxRed;
		}

		checkConsistency();
		
		return localMaxRed;
	}

	//like mergeRed, but doesn't actually do the merge, just tell you the number it would be
	int wouldMergeRed(int v1, int v2) {
		throw new RuntimeException("Not impl");
	}
	
	String dumpS() {
		String res = "";
		res += "{";
		boolean firstRow = true;
		for(int i=0; i<N; i++) {
			if(degBlk[i] == 0 && degRed[i] == 0)
				continue;
			if(!firstRow) {
				res += ",";
			} else {
				firstRow = false;
			}
			res += "{";
			res += "["+degBlk[i]+"B,"+degRed[i]+"R]";
			boolean first = true;
			for(int vo : eBlk[i]) {
				if(!first)
					res += ", ";
				res += i+"--"+vo;
				first = false;
			}
			for(int vo : eRed[i]) {
				if(!first)
					res += ", ";
				res += i+"-R-"+vo;
				first = false;
			}
			res += "}\n";
		}
		res += "}\n";
		return res;
	}

	void dump() {
		System.out.println(dumpS());
	}
}
