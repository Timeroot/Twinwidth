import java.util.Arrays;

public class BruteTW {

	static int[] bestSol;
	static int[] currSol;
	static int depth;
	
	static int twinWidth(Graph g) {
		int N = g.N;
		depth = 0;
		currSol = new int[2*(N-1)];
		
		int res = twinWidth(Trigraph.fromBigraph(g), 0, 1+g.N);
		println(1, "Scored "+res);
		println(1, Arrays.toString(bestSol));
		
		return res;
	}

	private static int twinWidth(Trigraph tg, int lb, int ub) {
		if(tg.E() == 0) {
			//We completed and weren't pruned -- so this must be a new record!
			println(2, "Success here ("+lb+","+ub+")");
			bestSol = currSol.clone();
			return 0;			
		}
		
		if(lb > ub) {
			return ub+1;
		}
		
		int bestScore = ub+1;
		int N = tg.N;
		iLoop: for(int i=0; i<N; i++) {
			//only merge nonempty vertices
			if(tg.degBlk[i] == 0 && tg.degRed[i] == 0)
				continue;
			
			for(int j=i+1; j<N; j++) {
				if(tg.degBlk[j] == 0 && tg.degRed[j] == 0)
					continue;
				
				Trigraph tg_ij = tg.copy();
				int red_ij_0 = tg_ij.mergeRed(i, j);
				if(red_ij_0 > ub)
					continue;
				currSol[2*depth] = i;
				currSol[2*depth+1] = j;
				
				depth++;
				println(4, "Rec");
				int lb_ij = Math.max(lb, red_ij_0);
				println(4, " with "+i+"-"+j+" ==> "+red_ij_0);
				int red_ij_rest = twinWidth(tg_ij, lb_ij, ub);
				//Final width is the max of the width on this merge, and the remaining ones
				int score = Math.max(red_ij_0, red_ij_rest);
				bestScore = Math.min(score, bestScore);
				depth--;
				
				//If we got a new low score, update the ub.
				if(ub != Math.min(ub, bestScore-1)) {
					println(3, "Depth "+ub+": update ub from "+ub+" to "+Math.min(ub, bestScore-1));
				}
				ub = Math.min(ub, bestScore-1);
				
				if(bestScore <= lb) { //we achieved our lower bound, can leve
					println(3, "Met lb="+lb);
					break iLoop;
				}
			}
		}
		
		currSol[2*depth] = -1;
		currSol[2*depth+1] = -1;
		
		println(4, "Ret");
		return bestScore;
	}
	
	private static final int VERBOSE = 0;
	private static final void println(int verblvl, String s) {
		if(VERBOSE >= verblvl)
			System.out.println(s);
	}
}
