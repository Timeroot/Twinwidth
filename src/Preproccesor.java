import java.util.ArrayList;
import java.util.Collections;

public class Preproccesor {
	private static final int VERBOSE = !MainP23.VERB?0: 2;
	
	//Solve, by preprocessing + reducing + splitting.
	static int[] sol;
	public static int solve(Graph g) {
		int N = g.N;
		int depth = 0;
		sol = new int[2*(N-1)];
		
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
			int tww = hardSolve(g, 0, g.N, hardSol);
			System.arraycopy(hardSol, 0, sol, depth, 2*solSize);
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
	
	//pass it on to the searching solver.
	//saying: we don't care if the result is in the range [0,lb], or if it's [ub,inf).
	//we only differentiate between values in the range [lb,ub].
	//Returns tww.
	public static int hardSolve(Graph g, int lb, int ub, int[] solDest) {
		//TODO use the lb, ub.
		int tww = BruteTW.twinWidth(g);
		if(solDest.length != BruteTW.bestSol.length)
			throw new RuntimeException("Bad copy dest");
		System.arraycopy(BruteTW.bestSol, 0, solDest, 0, BruteTW.bestSol.length);
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
