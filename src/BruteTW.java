import java.util.*;

public class BruteTW {
	private static final int VERBOSE = 1;

	static int[] bestSol;
	static int[] currSol;
	static int depth;
	
	static int twinWidth(Graph g) {
		int N = g.N;
		depth = 0;
		currSol = new int[2*(N-1)];
		
		//do initial twin checks
		int reduce_depth = checkTwinsBigraph(g);
		int res;
		
		if(g.E() == 0) {
			res = 0;
			bestSol = currSol.clone();
			println(1, "Twin reduction solved it, the end");
		} else {
			res = twinWidth(Trigraph.fromBigraph(g), 0, 1+g.N);
		}
		
		println(1, "Scored "+res);
		println(2, Arrays.toString(bestSol));

		depth -= reduce_depth;
		if(depth != 0)
			throw new RuntimeException("Depth tracking failed, "+depth);
		
		return res;
	}

	private static int twinWidth(Trigraph tg, int lb, int ub) {
		//Look for reductions
		
		//Edge density very high? Take the complement
		int N = tg.N;
		int eBlk = tg.EBlk();
		int eRed = tg.ERed();
		int eMax = N*(N-1)/2;
		if(eBlk > eMax - eRed) {
			//Take the complement
			tg.complement();
		}

		//Take a note of how much we increase 'depth' by here, so we can undo later
		int reduce_depth = checkTwinsTrigraph(tg);
		
		if(tg.E() == 0) {
			//We completed and weren't pruned -- so this must be a new record!
			println(2, "Success here ("+lb+","+ub+")");
			bestSol = currSol.clone();
			cleanDepth(reduce_depth);
			return 0;			
		}
		
		if(lb > ub) {
			cleanDepth(reduce_depth);
			return ub+1;
		}
		
		int bestScore = ub+1;
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
		
		cleanDepth(reduce_depth);
		
		println(4, "Ret");
		return bestScore;
	}
	
	private static int checkTwinsBigraph(Graph g) {
		int N = g.N;
		int reduce_depth = 0;
		//Look for twin vertices
		passLoop: while(true) {//loop as long as we merged something this pass
			boolean progress = false;
			iLoop: for(int i=0; i<N; i++) {
				int id = g.deg[i];
				if(id == 0) //empty vertex
					continue iLoop;
				
				jLoop: for(int j=i+1; j<N; j++) {
					int jd = g.deg[j];
					if(id != jd) //degrees don't match
						continue jLoop;
					
					for(Integer in : g.eList[i]) {
						if(!g.eList[j].contains(in) && in != j)
							continue jLoop;
					}
					
					//they're twins! merge
					//tg.mergeRed(i, j);
					//can bypass that and just delete j.
					g.clearVertex(j);
					progress = true;
					currSol[2*depth] = i;
					currSol[2*depth+1] = j;
					depth++;
					reduce_depth++;
				}
			}
			if(!progress)
				break passLoop;
		}
		return reduce_depth;
	}
	
	private static int checkTwinsTrigraph(Trigraph tg) {
		int N = tg.N;
		int reduce_depth = 0;
		//Look for twin vertices
		passLoop: while(true) {//loop as long as we merged something this pass
			boolean progress = false;
			iLoop: for(int i=0; i<N; i++) {
				int idB = tg.degBlk[i];
				int idR = tg.degRed[i];
				if(idB + idR == 0) //empty vertex
					continue iLoop;
				
				jLoop: for(int j=i+1; j<N; j++) {
					int jdB = tg.degBlk[j];
					int jdR = tg.degRed[j];
					if((jdB + jdR == 0) || (idB != jdB) || (idR != jdR)) //degrees don't match
						continue jLoop;
					
					for(Integer inB : tg.eBlk[i]) {
						if(!tg.eBlk[j].contains(inB) && inB != j)
							continue jLoop;
					}
					for(Integer inR : tg.eRed[i]) {
						if(!tg.eRed[j].contains(inR) && inR != j)
							continue jLoop;
					}
					
					//they're twins! merge
					//tg.mergeRed(i, j);
					//can bypass that and just delete j.
					tg.clearVertex(j);
					progress = true;
					currSol[2*depth] = i;
					currSol[2*depth+1] = j;
					depth++;
					reduce_depth++;
				}
			}
			if(!progress)
				break passLoop;
		}
		return reduce_depth;
	}
	
	private static void cleanDepth(int reduce_depth) {
		for( ; reduce_depth > 0; reduce_depth--) {
			System.out.println("E");
			depth--;
			currSol[2*depth] = -1;
			currSol[2*depth+1] = -1;
		}
	}

	private static final void println(int verblvl, String s) {
		if(VERBOSE >= verblvl)
			System.out.println(s);
	}
}
