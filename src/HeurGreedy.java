//Computes a heuristic greedy solution
public class HeurGreedy {
	private static final int VERBOSE = !MainP23.VERB?0: 0;

	static int[] currSol;
	static int depth;
	
	static int twinWidth(Graph g) {
		int N = g.N;
		depth = 0;
		currSol = new int[2*(N-1)];
		
		//do initial twin checks
		checkTwinsBigraph(g);
		
		int res;
		if(g.E() == 0) {
			res = 0;
			currSol.clone();
		} else {
			//TODO: connected components
			res = twinWidth(Trigraph.fromBigraph(g));
		}
		
		println(1, "Heuristic scored "+res);
		return res;
	}

	static int nodes;
	
	//lastI and lastJ are the numbers for the last merger, to break some ordering symmetry.
	private static int twinWidth(Trigraph tg) {
		int highestRedDeg = 0;
		
		while(tg.E() > 0) {
			int N = tg.N;
			
			//reductions
			int eBlk = tg.EBlk();
			int eRed = tg.ERed();
			int eMax = N*(N-1)/2;
			if(eBlk > eMax - eRed) {
				//Take the complement
				tg.complement();
			}
			
			if(checkTwinsTrigraph(tg) > 0) {
				if(tg.E() == 0)
					break;//done early
			}
			
			IJChoice heurSel = highRedHeur(tg);
			
			int i = heurSel.i, j = heurSel.j;
			
			currSol[2*depth] = i;
			currSol[2*depth+1] = j;
			depth++;
			
			tg = tg.copy();
			int ij_dRed = tg.mergeRed(i, j);
			highestRedDeg = Math.max(highestRedDeg, ij_dRed);
		}
		
		return highestRedDeg;
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
						if(!g.eList[j].contains(in) && in != j) {
							continue jLoop;
						}
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
					
					//update i degree
					id = g.deg[i];
					if(id == 0)
						continue iLoop;
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
	
	private static final void println(int verblvl, String s) {
		if(VERBOSE >= verblvl)
			System.out.println(s);
	}
	
	//represents on selection of which (i,j) to merge according to a heuristic
	static final class IJChoice {
		final int i, j;
		IJChoice(int i_, int j_){
			i = i_; j = j_;
		}
	}

	//picks two vertices that are close and small degree if possible
	public static IJChoice fastHeur(Trigraph tg) {
		int N = tg.N;
		int bestI=-1, bestJ=-1;
		int minDegI = 100*N;
		
		iLoop: for(int i=0; i<N; i++) {
			//only merge nonempty vertices
			if(tg.degBlk[i] == 0 && tg.degRed[i] == 0)
				continue iLoop;
			
			int effDegI = tg.degBlk[i] + 2*tg.degRed[i];
			if(effDegI >= minDegI) {
				continue iLoop;
			} else {
				minDegI = effDegI;
				bestI = i;
				bestJ = -1;
			}
			
			if(tg.degRed[i] > 0) {
				int minDegJ = 100*N;
				jLoop: for(int j : tg.eRed[i]) {
					int effDegJ = tg.degBlk[j] + 2*tg.degRed[j];
					if(effDegJ >= minDegJ) {
						continue jLoop;
					} else {
						minDegJ = effDegJ;
						bestJ = j;
					}
				}
			} else {
				int minDegJ = 100*N;
				jLoop: for(int j : tg.eBlk[i]) {
					int effDegJ = tg.degBlk[j] + 2*tg.degRed[j];
					if(effDegJ >= minDegJ) {
						continue jLoop;
					} else {
						minDegJ = effDegJ;
						bestJ = j;
					}
				}
			}
		}
		
		return new IJChoice(bestI, bestJ);
	}
	
	public static IJChoice highRedHeur(Trigraph tg) {
		int N = tg.N;
		int bestI=-1, bestJ=-1;
		int bestDRed = N+1;
		int bestERed = N*N;
		int bestEBlk = N*N;
		
		iLoop: for(int i=0; i<N; i++) {
			//only merge nonempty vertices
			if(tg.degBlk[i] == 0 && tg.degRed[i] == 0)
				continue iLoop;
			
			jLoop: for(int j=i+1; j<N; j++) {
				if(tg.degBlk[j] == 0 && tg.degRed[j] == 0)
					continue jLoop;
				
				Trigraph tg_ij = tg.copy();
				int ij_dRed = tg_ij.mergeRed(i, j);
				int ij_eRed = tg_ij.ERed();
				int ij_eBlk = tg_ij.EBlk();
				
				if(ij_dRed > bestDRed)
					continue jLoop;
				if(ij_dRed == bestDRed) {
					if(ij_eRed > bestERed)
						continue jLoop;
					if(ij_eRed == bestERed) {
						if(ij_eBlk >= bestEBlk)
							continue jLoop;
					}
				}
				//new best
				bestI = i; bestJ = j;
				bestDRed = ij_dRed;
				bestERed = ij_eRed;
				bestEBlk = ij_eBlk;
			}
		}
		
		return new IJChoice(bestI, bestJ);
	}
}
