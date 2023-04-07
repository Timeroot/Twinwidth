package smallgraph;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import tw_compute.BruteTW;
import tw_compute.Graph;

public class SmallGraph_Analysis {

	//Computing TWW of many small graphs.
	//Graph databases downloaded from https://users.cecs.anu.edu.au/~bdm/data/graphs.html
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println(G6Code.encodeG6(G6Code.decodeG6("J?`DFVfzv~?").bar()));
		
		Iterator<Graph> graphlist = G6Code.read_graphs("./smallgraphs/perfect11.g6");
		
		//Known obstructions:
		//N=4, TWW=1: only P4.
		
		//Minimal obstructions that imply TWW>=2
		String[][] tww2witStrings = new String[][] {
			//pairs of (G6 code, name).
			//N=5, TWW=2
			{"DUW","C5"},
			//N=6, TWW=2
			{"EhEG","C6"},{"Ey[g","S3"},{"EjCg","Antenna"},
			//N=7, TWW=2
			{"FhC_G","T2"},{"FhCKG","C7"},{"FhDOG","X2"},{"FrGX?","X3"},{"FhOgW","X30"},
			{"FUwK?","X175"},{"FxE\\g","X42"},{"FhFh?","X32"},{"FhEj?","X33"},
			{"F~p`_","RisingSun"},{"FhCJo","X176"},{"FhhWW","X36"},{"FhFj?","X35"},
			{"Fy\\HG","X34"},{"FhFx?","X31"},{"F?qcw","XF_2^2"},
		};
		int n2Witness =
				2*tww2witStrings.length-1 +
				2*Data.minimal_tww2n8.length-1 +
				1*Data.minimal_tww2n9.length; //graphs + their duals, except for self-duals
		Graph[] tww2witnesses = new Graph[n2Witness];
		HashMap<Graph, String> nameMap = new HashMap<>();
		
		{int destI = 0;
			for(int i=0; i<tww2witStrings.length; i++) {
				String name = tww2witStrings[i][1];
				Graph g = G6Code.decodeG6(tww2witStrings[i][0]);
				tww2witnesses[destI++] = g;
				nameMap.put(g, name);
				
				if(!name.equals("C5") && !name.equals("unk8SD")) {
					Graph co_g = g.bar();
					tww2witnesses[destI++] = co_g;
					nameMap.put(co_g, "co"+name);
				}
			}
			for(int i=0; i<Data.minimal_tww2n8.length; i++) {
				String name = Data.minimal_tww2n8[i][1];
				Graph g = G6Code.decodeG6(Data.minimal_tww2n8[i][0]);
				tww2witnesses[destI++] = g;
				nameMap.put(g, name);
				
				if(!name.equals("C5") && !name.equals("unk8SD")) {
					Graph co_g = g.bar();
					tww2witnesses[destI++] = co_g;
					nameMap.put(co_g, "co"+name);
				}
			}
			for(int i=0; i<Data.minimal_tww2n9.length; i++) {
				String name = Data.minimal_tww2n9[i];//use g6 as name
				Graph g = G6Code.decodeG6(Data.minimal_tww2n9[i]);
				tww2witnesses[destI++] = g;
				nameMap.put(g, name);
				//no complementation
			}
		}

//		for(String g6 : Data.minimal_tww2n10) {
//			Graph g = G6Code.decodeG6(g6);
//			Graph gb = g.bar();
//			boolean selfdual = containsInduced(g, gb, false);
//			if(selfdual) {
//				System.out.println("Graph "+g6+" self dual");
//			}
//		}
		
//		System.out.println(2*Data.minimal_tww2n8.length-1);
//		System.out.println(Data.minimal_tww2n9.length);
//		System.out.println(Data.minimal_tww2n10.length);
		
		
		String[][] tww3witStrings = new String[][] {
			//pairs of (G6 code, name).
			//N=8, TWW=3
			{"GEhbtg","HoG-35543"},//Gyrobifastigium, self-dual
			{"GCR`rk","unk"},//dual to "GCpulw"
			{"GCpelW","unk"},//dual to "GEhbtk"
		};
		int n3Witness = 2*(tww3witStrings.length)-1; //graphs + their duals, except for self-duals
		Graph[] tww3witnesses = new Graph[n3Witness];
		
		{int destI = 0;
		for(int i=0; i<tww3witStrings.length; i++) {
			String name = tww3witStrings[i][1];
			Graph g = G6Code.decodeG6(tww3witStrings[i][0]);
			tww3witnesses[destI++] = g;
			nameMap.put(g, name);
			
			if(!name.equals("HoG-35543")) {
				Graph co_g = g.bar();
				tww3witnesses[destI++] = co_g;
				nameMap.put(co_g, "co"+name);
			}
		}}
		
		//Checking complements and self-comps in the witness list
//		for(int i=0; i<tww2witStrings.length; i++) {
//			Graph gI = G6Code.decodeG6(tww2witStrings[i][0]);
//			if(containsInduced(gI, gI.bar(), false)) {
//				System.out.println("Self-complementary: "+G6Code.encodeG6(gI));
//			}
//			for(int j=i+1; j<tww2witStrings.length; j++) {
//				Graph gJ = G6Code.decodeG6(tww2witStrings[j][0]);
//				if(containsInduced(gI, gJ.bar(), false)) {
//					System.out.println(G6Code.encodeG6(gI)+" == co-"+G6Code.encodeG6(gJ));
//				}	
//			}
//		}
		
		//verify witness validity
		for(Graph wit : tww2witnesses) {
			int tww = BruteTW.twinWidth(wit);
			if(tww != 2)
				throw new RuntimeException("Graph witness should have right tww, has "+tww);
			//doesn't contain anything else
			for(Graph subwit : tww2witnesses) {
				if(subwit.N() < wit.N() && containsInduced(wit, subwit, true)) {
					throw new RuntimeException("Graph witness has subwitness: "+nameMap.get(wit)+" > "+nameMap.get(subwit));
				}
			}
		}
		
		gLoop: for( ; graphlist.hasNext(); ) {
			Graph g = graphlist.next();
			
			//skip boring graphs
			{
				if(g.connComps(true).size() > 1) {
//					System.out.println("Discon");
					continue gLoop;
				}
				g.complement();
				if(g.connComps(true).size() > 1) {
//					System.out.println("Disconnected complement");
					continue gLoop;
				}
				g.complement();//restore original
			}
			if(hasTwins(g)) {
//				System.out.println("Twin vertices");
				continue gLoop;
			}
			
			int tww = BruteTW.twinWidth(g);
			if(tww == 0) {
//				System.out.println("Cograph");
			} else if(tww == 1) {
				boolean p4 = containsP4(g, false);
				if(!p4)
					throw new RuntimeException("no P4 in tww==1!?");
				
//				System.out.println("TWW=1, induced P4");
			} else {
				if(tww > 2) {
//					System.out.println("skip tww of "+tww);
					continue;
				}
				
				if(!containsP4(g, false))
					throw new RuntimeException("no P4 in tww==2 !?");
				
				boolean witnessed = false;
//				for(Graph wit : tww2witnesses) {
//					if(containsInduced(g, wit, false)) {
////						System.out.println("Contains induced "+nameMap.get(wit));
//						witnessed = true;
//						continue;
//					}
//				}
				for(int i=0; i<g.N(); i++) {
					Graph g2 = g.copy();
					g2.clearVertex(i);
					int tww_del = BruteTW.twinWidth(g2);
					if(tww_del == tww) {
						witnessed=true;
						break;
					}
				}
				
				if(!witnessed) {
					System.out.println("TWW="+tww);
					System.out.println("No witness!");
					System.out.println(G6Code.currGraphG6);
					g.dump();
				}
			}
		}
	}
	
	private static boolean hasTwins(Graph g) {
		int N = g.N();
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
				
				return true;
			}
		}
		return false;
	}

	private static boolean containsP4(Graph g, boolean printWitness) {
		int N = g.N();
		//one end is i, other end is j 
		for(int i=0; i<N; i++) {
			jL: for(int j=i+1; j<N; j++) {
				
				//i not a neighbor j
				if(g.eList[i].contains(j))
					continue jL;
				
				//k is neighbor of i, not of j 
				kL: for(int k : g.eList[i]) {
					if(g.eList[k].contains(j))
						continue kL;
					//l is neighbor of k and j, but not i
					@SuppressWarnings("unchecked")
					HashSet<Integer> ls = (HashSet<Integer>)g.eList[k].clone();
					ls.removeAll(g.eList[i]);
					ls.retainAll(g.eList[j]);//ensures it isn't i or j
					if(ls.size() > 0) {
						if(printWitness)
							System.out.println("P4 witness: "+i+"-"+k+"-"+ls.iterator().next()+"-"+j);
						
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean containsC5(Graph g, boolean printWitness) {
		int N = g.N();
		// i-j-k-l-m-i cycle
		//by symmetry, i is the least of the 5, and j<m
		
//		iL:
			for(int i=0; i<N; i++) {
			
			jL: for(int j : g.eList[i]) {
				if(j < i)
					continue jL;

				kL: for(int k : g.eList[j]) {
					if(k < i)
						continue kL;
					if(g.eList[i].contains(k))
						continue kL;
					
					lL: for(int l : g.eList[k]) {
						if(l < i)
							continue lL;
						if(g.eList[i].contains(l) || g.eList[j].contains(l))
							continue lL;
						
						mL: for(int m : g.eList[l]) {
							if(m < i || m < j)
								continue mL;
							if(!g.eList[i].contains(m))
								continue mL;
							if(g.eList[j].contains(m) || g.eList[k].contains(m))
								continue mL;
							
							if(printWitness)
								System.out.println("C5 witness: "+i+"-"+j+"-"+k+"-"+l+"-"+m+"-"+i);
							
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private static boolean containsInduced(Graph g, Graph h, boolean printWitness) {
		//pretty dumb but it works. Tries assigning each vertex each place
		//sizes of graphs
		int Ng = g.N(), Nh = h.N();
		if(Ng < Nh)
			return false;//can't embed into smaller
		if(Ng == Nh) {
			//isomorphism, can do some quick throwouts
			if(g.E() != h.E())
				return false;//number of edges must match
		}
		
		//current embedding destinations
		int[] hDest = new int[Nh];
		
		//vertex we're currently trying to embed
		int currDest = 0;
		
		configLoop: while(true) {
			//check if we reached end for the currently enumerated vertex 
			if(hDest[currDest] == Ng) {
				hDest[currDest]=0;
				currDest--;
				if(currDest == -1)
					break configLoop; //yes, and that was v=0, so we done
				hDest[currDest]++;
				continue configLoop;
			}
			
			//check if we reused a previously allocated vertex
			for(int prev=0; prev<currDest; prev++) {
				if(hDest[prev] == hDest[currDest]) {
					hDest[currDest]++;
					continue configLoop;
				}
			}
			
			//check that constraints (present/nonpresent edges) are obeyed
			for(int prev=0; prev<currDest; prev++) {
				boolean hEdge = h.eList[prev].contains(currDest);
				boolean gEdge = g.eList[hDest[prev]].contains(hDest[currDest]);
				if(hEdge != gEdge) {
					hDest[currDest]++;
					continue configLoop;
				}
			}
			
			//all good, this vertex was successfully embedded.
			if(currDest == Nh-1) {
				//actually, that was the last one, we got a hit!
				if(printWitness) {
					System.out.println("H embedding: "+Arrays.toString(hDest));
				}
				return true;
			} else {
				//continue to next vertex
				currDest++;
				hDest[currDest] = 0;
			}
		}
		
		return false;
	}
}