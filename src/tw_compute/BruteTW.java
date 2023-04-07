package tw_compute;
import java.util.*;

public class BruteTW {
	private static final int VERBOSE = !MainP23.VERB?0: 3;

	static int[] bestSol;
	static int[] currSol;
	static int depth;
	
	public static int twinWidth(Graph g) {
		
		int N = g.N;
		depth = 0;
		currSol = new int[2*(N-1)];
		
		//do initial twin checks
		int reduce_depth = checkTwinsBigraph(g);
		println(1, "After detwinning, N="+g.nonZeroDegN()+", E="+g.E());
		println(1, "Reduced "+reduce_depth+" by twins");
		int res;
		
		if(g.E() == 0) {
			res = 0;
			bestSol = currSol.clone();
			println(1, "Twin reduction solved it, the end");
			
		} else {
			
			int heurUB = HeurGreedy.twinWidth(g);
			int[] heurSol = HeurGreedy.currSol;
			int heurDepth = HeurGreedy.depth;
			println(2, "Heur: TW<="+heurUB);
			println(3, Arrays.toString(heurSol));
			
			nodes=0;
			res = twinWidth(Trigraph.fromBigraph(g), 0, heurUB-1, -1, -1);
			println(1, nodes+" nodes visited");
			if(res >= heurUB) {
				println(2, "Couldn't beat heuristic");
				println(3, "My sol: "+Arrays.toString(currSol));
				println(3, "My d: "+depth);
				println(3, "Heur sol: "+Arrays.toString(heurSol));
				println(3, "Heur d: "+heurDepth);
				res = heurUB;
				
				//Pad out the heuristic solution with our de-twinning 
				for(int i=heurDepth; i-->0; ) {
					heurSol[2*(i+depth)] = heurSol[2*i];
					heurSol[2*(i+depth)+1] = heurSol[2*i+1];
				}
				for(int i=0; i<depth; i++) {
					heurSol[2*i] = currSol[2*i];
					heurSol[2*i+1] = currSol[2*i+1];
				}
				bestSol = heurSol;
				println(3, "Assembled sol: "+Arrays.toString(heurSol));
			}
		}
		
		println(1, "Scored "+res);
		println(2, Arrays.toString(bestSol));

		depth -= reduce_depth;
		if(depth != 0)
			throw new RuntimeException("Depth tracking failed, "+depth);
		
		degZeroFixup(N);
		return res;
	}

	static int nodes;
	
	//lastI and lastJ are the numbers for the last merger, to break some ordering symmetry.
	private static int twinWidth(Trigraph tg, int lb, int ub, int lastI, int lastJ) {
		nodes++;
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
		
		//If we did twin merging, skip symmetry breaking
		if(reduce_depth > 0) {
			lastI = -1;
			lastJ = -1;
		}
		
		//TODO turn this into generating a whole bunch of options and ranking them. performs better + cleaner
		int bestScore = ub+1;
		iLoop: for(int i0=0; i0<N; i0++) {
			//only merge nonempty vertices
			if(tg.degBlk[i0] == 0 && tg.degRed[i0] == 0)
				continue;
			
			jLoop: for(int j0=i0+1; j0<N; j0++) {
				if(tg.degBlk[j0] == 0 && tg.degRed[j0] == 0)
					continue jLoop;

				int i, j;
				i = i0;
				j = j0;

				
				Trigraph tg_ij = tg.copy();
				int red_ij_0 = tg_ij.mergeRed(i, j);
				if(red_ij_0 > ub)
					continue jLoop;

				int lb_ij = Math.max(lb, red_ij_0);
				
				//Check if we can skip due to symm breaking
				if(i < lastI && j != lastI && j != lastJ) {
					//double check they're all different
					if(i==lastI || i==lastJ || j==lastI || j==lastJ) {
						throw new RuntimeException("Bad assert in symm break");
					}
					//check if CD's red degree couldn't have been reduced by AB to improve
					if(tg_ij.degRed[i] >= lb_ij && tg_ij.eRed[i].contains(lastI)) {
						//can't skip symmetry bc CD improved by AB
					} else {
						//okay, now check no neighboring vertex was a problem
						boolean noBadNeighbor = true;
						for(int zNeigh : tg.eRed[lastI]) {
							if(tg.degRed[zNeigh] >= lb_ij && tg.eRed[i].contains(zNeigh) && tg.eRed[j].contains(zNeigh)) {
								noBadNeighbor = false;
								break;
							}
						}
						if(noBadNeighbor) {
							//all good! move on 
							continue jLoop;
						} else {
							//can't skip symmetry bc some neighbor improved by AB
						}
					}
				} else {
					//can't skip symmetry bc sorted
				}
				//symmetry for the case of exchanging
				// merging AB then ABC, vs AC then ABC.
				if((i == lastI && j < lastJ) || (j == lastI)) {
					//which are which? A is doubled, C is other new
					int a, c;
					if(i == lastI && j < lastJ) {
						a = i;
						c = j;
					} else if(j == lastI) {
						a = lastI;
						c = i;
					} else {
						throw new RuntimeException("Bad assert in symm break[2]");
					}
					//neither
					// -the AC vertex has a critically high red-degree (higher than ABC) -- which is at
					//  most 1 less than ABC
					if(tg_ij.degRed[i] >= lb_ij) {
						//can't skip symmetry
					} else {
						//nor
					    // -C shares a high-red-degree vertices with AB
						boolean noBadNeighbor = true;
						for(int zNeigh : tg.eRed[c]) {
							if(tg.degRed[zNeigh] >= lb_ij && tg.eRed[a].contains(zNeigh)) {
								noBadNeighbor = false;
								break;
							}
						}
						if(noBadNeighbor) {
							//all good! move on 
							continue jLoop;
						}
					}
				}
				
				currSol[2*depth] = i;
				currSol[2*depth+1] = j;

				depth++;
				println(4, "Rec");
				println(4, " with "+i+"-"+j+" ==> "+red_ij_0);
				int red_ij_rest = twinWidth(tg_ij, lb_ij, ub, i, j);
				//Final width is the max of the width on this merge, and the remaining ones
				int score = Math.max(red_ij_0, red_ij_rest);
				bestScore = Math.min(score, bestScore);
				depth--;
				
				//If we got a new low score, update the ub.
				if(ub != Math.min(ub, bestScore-1)) {
					println(3, "Depth "+ub+": update ub from "+ub+" to "+Math.min(ub, bestScore-1));
				}
				ub = Math.min(ub, bestScore-1);
				
				if(lb > ub) { //we achieved our lower bound, can leve
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
					
					//mark progress and keep going
					progress = true;
					currSol[2*depth] = i;
					currSol[2*depth+1] = j;
					depth++;
					reduce_depth++;
					
					//update i degree
					id = g.deg[i];
					if(id == 0)
						continue iLoop;
				}
			}
			if(!progress)
				break passLoop;
		}
		
		//Normalize to sparsity
		int eBlk = g.E();
		int eMax = N*(N-1)/2;
		if(eBlk > eMax) {
			//Take the complement
			g.complement();
			println(2, "Complementation after initial twinprune");
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
					if((idB != jdB) || (idR != jdR)) //degrees don't match
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
					
					//mark progress and keep going
					progress = true;
					currSol[2*depth] = i;
					currSol[2*depth+1] = j;
					depth++;
					reduce_depth++;

					//update i degree
					idB = tg.degBlk[i];
					idR = tg.degRed[i];
					if(idB + idR == 0) //empty vertex
						continue iLoop;
				}
			}
			if(!progress)
				break passLoop;
		}
		return reduce_depth;
	}
	
	private static void cleanDepth(int reduce_depth) {
		for( ; reduce_depth > 0; reduce_depth--) {
			depth--;
			currSol[2*depth] = -1;
			currSol[2*depth+1] = -1;
		}
	}

	private static final void println(int verblvl, String s) {
		if(VERBOSE >= verblvl)
			System.out.println(s);
	}
	
	private static void degZeroFixup(int N) {
		int steps = 0;
		boolean[] gone = new boolean[N];
		for(int i=0; i<N-1; i++) {
			if(bestSol[2*i] == bestSol[2*i+1]) {
				break;
			}
			steps++;
			gone[bestSol[2*i+1]] = true;
		}
		for(int i=1; i<N; i++) {
			if(!gone[i]) {
				bestSol[2*steps] = 0;
				bestSol[2*steps+1] = i;
				steps++;
			}
		}
	}
}
