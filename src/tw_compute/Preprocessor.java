package tw_compute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Preprocessor {
	private static final int VERBOSE = !MainP23.VERB?0: 1;
	
	//if true, just return "2" at the end when you would need to search.
	//obviously produces incorrect results -- just for testing the preprocessor.
	private static final boolean SKIP_HARD = false;
	
	private static final boolean USE_BRIDGE = false;
	
	//Solve, by preprocessing + reducing + splitting.
	static int[] sol;
	public static int solve(Graph g, int[] sol) {
		int N = g.N;
		int depth = 0;
		if(sol.length != 2*(N-1))
			throw new RuntimeException("Bad length");
		
		//Twin reductions
		ArrayList<Integer> twinDelVerts = checkTwinsBigraph(g, sol, depth);
		depth += twinDelVerts.size();
		
		int tww;
		
		if(twinDelVerts.size() > 0){
			//move to subgraph
			//get the remaining vertices
			ArrayList<Integer> remVerts = new ArrayList<Integer>(N-twinDelVerts.size());
			twinDelVerts.sort(Integer::compare);
			for(int v=0; v<N; v++) {
				if(Collections.binarySearch(twinDelVerts, v) < 0) {
					//not in delVerts
					remVerts.add(v);
				}
			}
			
			Graph remG = g.subgraph(remVerts);
			tww = compSolve(remG, sol, depth, true);
			
			//Fix up mapping, updating depth as we go
			for(int i=0; i<(remG.N-1); i++) {
				sol[2*depth] = remVerts.get(sol[2*depth]);
				sol[2*depth+1] = remVerts.get(sol[2*depth+1]);
				depth++;
			}
		} else {
			//Solve by components
			tww = compSolve(g, sol, depth, true);
		}

		println(1, "Final tww (pp) = "+tww);
		return tww;
	}
	
	//Reduce by taking complements and connected components.
	//Writes to sol at offset 2depth. Adds to depth by g.N-1
	//If "tryCompl" is true, it will try it with the complement of the graph as well.
	//(Passing it with true will complement the graph, and call this function recursively
	// with it set to false; unless it split it, in which case it will be set to true again.)
	//Returns tww of g.
	public static int compSolve(Graph g, int[] sol, int depth, boolean tryCompl) {
		//Break into connected components, including the degzeros
		ArrayList<ArrayList<Integer>> comps = g.connComps(true);
		if(comps.size() == 1) {
			//no splitting.
			if(tryCompl) {
				//It didn't split; so now we try complementing and see if it splits.
				g.complement();
				int tww = compSolve(g, sol, depth, false);
				return tww;
			}
				
			//No more progress being made. Switch to the hard solver.
			int solSize = g.N-1;
			int[] hardSol = new int[2*solSize];
			
			//TODO pass in lb,ub
			int tww = USE_BRIDGE?
					articulationSolve(g, 0, g.N, hardSol)
					:hardSolve(g, 0, g.N, hardSol);
			
			System.arraycopy(hardSol, 0, sol, 2*depth, 2*solSize);
			depth += solSize;
			return tww;
			
		} else {
			//TODO order components from largest expected tww to smallest, for better lb/ub
			int maxTww = 0;
			for(ArrayList<Integer> comp1 : comps) {
				if(comp1.size() == 1) {
					//skip; it's a single degree zero vertex.
					continue;
				}
				
				Graph g1Comp;
				g1Comp = g.subgraph(comp1);
				int g1N = g1Comp.N;
				
				//Recurse on the component, using its complement (since this component def can't split)
				g1Comp.complement();
				int tww = compSolve(g1Comp, sol, depth, false);
				
				//Update running tww max
				maxTww = Math.max(maxTww, tww);
				
				//Fix up mapping, updating depth as we go
				for(int i=0; i<(g1N-1); i++) {
					sol[2*depth] = comp1.get(sol[2*depth]);
					sol[2*depth+1] = comp1.get(sol[2*depth+1]);
					depth++;
				}
			}
			
			//Merge components together
			//Join first element of each component (from 2nd onwards)
			//with the first element of the first component.
			int v00 = comps.get(0).get(0);
			for(int compI=1; compI<comps.size(); compI++) {
				int vI0 = comps.get(compI).get(0);
				sol[2*depth] = v00;
				sol[2*depth+1] = vI0;
				depth++;
			}

			return maxTww;
		}
	}

	public static int articulationSolve(Graph g, int lb, int ub, int[] sol) {
		if(g.N == 0) {
			throw new RuntimeException("Graph of size 0?!");
		} else if(g.N == 1) {
			return 0; //single degree zero-vertex
		} else if(g.N <= 3) {
			throw new RuntimeException("Graph of size 2 or 3 should be reduced");
		} else if(g.N <= 4) {//TODO
			//Special cases for small ones
			// only size 4 that isn't reduced: P4
			// only size 5 that aren't reduced: P5, bull, C5 -- https://www.graphclasses.org/smallgraphs.html#nodes5
			return hardSolve(g, lb, ub, sol);
		}

		
		//Special case for paths
		{
			//Normalize complement
			int E = g.E();
			int eMax = g.N*(g.N-1)/2;
			if(g.E() > eMax - E) {
				//Take the complement
				g.complement();
				E  = eMax - E;
			}
			println(1, "articuSolv, N="+g.N+", E="+E);
			
			if(E == g.N-1) {
				//tree
				int maxDeg = -1;
				for(int i=0; i<g.N; i++) {
					maxDeg = Math.max(maxDeg, g.deg[i]);
				}
				if(maxDeg == 2) {
					//path
					//TODO
					return hardSolve(g, 1, 1, sol);
				} else {
					//non-path tree
					//TODO
					return hardSolve(g, 1, 2, sol);
				}
			}
		}
		
		
		//First try splitting on a bridge
		{
			//Track the best one we find (if any).
			//We define the "best" as the one that minimizes (maximum size of any resulting components)
			int bestBridgeI = -1;
			int bestBridgeJ = -1;
			boolean bestBridgeNeedsComplement = false;
			int bestBridgeCompSize = g.N*2; //metric for "best"
			
			for(int tries = 0; tries < 2; tries++) {
				ArrayList<Integer> bridgeList = ArticulationPoint.bridge(g);
				int nBrg = bridgeList.size()/2;
				for(int i=0; i<nBrg; i++) {
					int u = bridgeList.get(2*i);
					int v = bridgeList.get(2*i+1);
					if(g.deg[u] == 1 || g.deg[v] == 1) {
						println(2,"SIMPLE BRIDGE "+u+" "+v);
						continue; //skip bridges of size 1
					} //else...
					println(2,"FANCY BRIDGE "+u+" "+v);
					
					//What components would we get if we cut this out
					g.clearEdge(u, v);
					ArrayList<ArrayList<Integer>> splitComps = g.connComps(false);
					g.addEdge(u, v);
					
					if(splitComps.size() != 2)
						throw new RuntimeException("Bridge removal shouldn't make more than 2...");
					//see below, some more assumptions that this is only 2 (Right??)
					
					int biggestCompSize =
							splitComps.stream().mapToInt(x -> x.size()).max().orElseThrow();
					
					if(biggestCompSize < bestBridgeCompSize) {
						bestBridgeI = u;
						bestBridgeJ = v;
						bestBridgeCompSize = biggestCompSize;
					}
				}
				
				//first tried failed. complement and try again
				if(tries == 0) {
					g.complement();
					println(3, "complement...");
					bestBridgeNeedsComplement ^= true;
				}
			}
			
			//Did we find a good bridge? If so, split and solve based on that
			if(bestBridgeI >= 0) {
				
				if(bestBridgeNeedsComplement)
					g.complement();
				
				int u = bestBridgeI;
				int v = bestBridgeJ;
				
				g.clearEdge(u, v);
				ArrayList<ArrayList<Integer>> comps = g.connComps(true);
				if(comps.size() == 1) {
					throw new RuntimeException("Split at "+u+", "+v+" and got comps "+comps.size());
				}
				g.addEdge(u, v);
				
				println(1, "Split at bridge "+u+"-"+v+" and got comps of sizes "+
						comps.stream().map(x -> ""+x.size()).reduce("", (x,y)->x+"|"+y)+"|");

				//TODO order components from largest expected tww to smallest, for better lb/ub
				
				int N_comp = comps.size();
				int[][] compSols = new int[N_comp][];
				int[] comp_twss = new int[N_comp];
				
				if(N_comp != 2)
					throw new RuntimeException("Number of components should be two after removing bridge");
				
				for(int i_comp = 0; i_comp < N_comp; i_comp++) {
					ArrayList<Integer> comp1 = comps.get(i_comp);
					
					if(comp1.size() == 1) {
						throw new RuntimeException("Split at "+u+", "+v+" and got a size-1 component "+comp1);
					}
					
					//Add in the other side of the edge
					if(comp1.contains(u)) {
						if(comp1.contains(v))
							throw new RuntimeException("u + v");
						comp1.add(v);
					} else {
						comp1.add(u);
						if(!comp1.contains(v))
							throw new RuntimeException("!u + !v");
					}
					
					Graph g1Comp;
					g1Comp = g.subgraph(comp1);
					int g1N = g1Comp.N;
					
					int[] comp_sol = new int[2*(g1N-1)];
					int comp_tww = Preprocessor.solve(g1Comp, comp_sol);
					
					compSols[i_comp] = comp_sol;
					comp_twss[i_comp] = comp_tww;
				}

				if(comp_twss[0] != comp_twss[1]) {
					println(1, "BRIDGE SUCCESS: "+comp_twss[0]+", "+comp_twss[1]);
					//TODO write sol
					return Math.max(comp_twss[0], comp_twss[1]);
				}
				
				//TODO other ways they can be safely combined??
				println(1, "BRIDGE FAILURE: "+comp_twss[0]+", "+comp_twss[1]);
				
//				//Update running tww max
//				maxTww = Math.max(maxTww, tww);
//				
//				//Fix up mapping, updating depth as we go
//				for(int i=0; i<(g1N-1); i++) {
//					sol[2*depth] = comp1.get(sol[2*depth]);
//					sol[2*depth+1] = comp1.get(sol[2*depth+1]);
//					depth++;
//				}
//				
//				//Merge components together
//				//Join first element of each component (from 2nd onwards)
//				//with the first element of the first component.
//				int v00 = comps.get(0).get(0);
//				for(int compI=1; compI<comps.size(); compI++) {
//					int vI0 = comps.get(compI).get(0);
//					sol[2*depth] = v00;
//					sol[2*depth+1] = vI0;
//					depth++;
//				}
//				
//				return maxTww;
				
			}
		}
		
		//articulation points
//		for(int tries = 0; tries < 2; tries++) {
//			//need to try once on g, and once on its complement.
//			ArrayList<Integer> aplist = ArticulationPoint.AP(g);
//			for(Integer ap_v : aplist) {
////				System.out.println(" v="+ap_v);
//				for(int v_n : g.eList[ap_v]) {
////					System.out.println("  vn="+v_n+", deg="+g.deg[v_n]);
//				}
//			}
//			if(aplist.size() > 0) {
//				g.dumpMMA();
////				g.dumpJava();
//			}
//			
//			//first tried failed. complement and try again
//			if(tries == 0) {
//				g.complement();
//				println(3, "complement...");
//			}
//		}

		return hardSolve(g, lb, ub, sol);
	}
	
	//pass it on to the searching solver.
	//saying: we don't care if the result is in the range [0,lb], or if it's [ub,inf).
	//we only differentiate between values in the range [lb,ub].
	//Returns tww.
	public static int hardSolve(Graph g, int lb, int ub, int[] sol) {
		if(SKIP_HARD)return 2;
		
		{//for debugging
			int E = g.E();
			int eMax = g.N*(g.N-1)/2;
			if(g.E() > eMax - E) {
				//Take the complement
				g.complement();
				E  = eMax - E;
			}
//			g.dumpMMA();
//			g.dumpJava();
		}
		
		//TODO use the lb, ub.
		int tww = BruteTW.twinWidth(g);
		if(sol.length != BruteTW.bestSol.length)
			throw new RuntimeException("Bad copy dest");
		System.arraycopy(BruteTW.bestSol, 0, sol, 0, BruteTW.bestSol.length);
		return tww;
	}

	//Returns a list of deleted vertices
	private static ArrayList<Integer> checkTwinsBigraph(Graph g, int[] currSol, int depth) {
		int N = g.N;
		ArrayList<Integer> delVerts = new ArrayList<Integer>();
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
					delVerts.add(j);
					
					//mark progress and keep going
					progress = true;
					currSol[2*depth] = i;
					currSol[2*depth+1] = j;
					depth++;
					
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
//		int eBlk = g.E();
//		int eMax = N*(N-1)/2;
//		if(eBlk > eMax) {
//			//Take the complement
//			g.complement();
//			println(2, "Complementation after initial twinprune");
//		}
		
		return delVerts;
	}
	
	private static final void println(int verblvl, String s) {
		if(VERBOSE >= verblvl)
			System.out.println(s);
	}
}
