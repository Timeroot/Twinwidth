import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;

public class Graph {
	int N;
	HashSet<Integer>[] eList;
	int[] deg;
	
	static final boolean CHECK = false;
	
	public Graph(int N_, HashSet<Integer>[] eList_,
			int[] deg_) {
		N = N_;
		eList = eList_;
		deg = deg_;
	}

	public Graph(int N_, HashSet<Integer>[] eList_) {
		N = N_;
		eList = eList_;
		deg = new int[N];
		for(int i=0; i<N; i++) {
			deg[i] = eList_[i].size();
		}
	}
	
	@SuppressWarnings("unchecked")
	public Graph() {
		this(0, new HashSet[0], new int[0]);
	}
	
	void checkConsistency() {
		if(!CHECK)
			return;
		
		int n_edge=0;
		for(int i=0; i<N; i++) {
			n_edge+=eList[i].size();
			if(eList[i].size() != deg[i])
				throw new RuntimeException("Consistency fail (1)"+i);
			for(int j : eList[i])
				if(!eList[j].contains(i))
					throw new RuntimeException("Consistency fail (1)"+i+"-"+j);
		}
		if(n_edge % 2 != 0)
			throw new RuntimeException("Consistency fail (2)");
	}
	
	//Clear all edges from a vertex v.
	//Destructive, obviously.
	void clearVertex(int v) {
		for(int vo : eList[v]){
			eList[vo].remove(v);
			deg[vo]--;
		}
		eList[v].clear();
		deg[v] = 0;
	}
	
	void clearEdge(int v1, int v2) {
		if(v1 == v2)
			throw new RuntimeException("Tried to remove self-loop "+v1);
		boolean worked = eList[v1].remove(v2) && eList[v2].remove(v1);
		if(!worked)
			throw new RuntimeException("Tried to clear edge "+v1+"-"+v2+" that wasn't in graph.");
		deg[v1]--;
		deg[v2]--;
	}
	
	void addEdge(int v1, int v2) {
		if(v1 == v2)
			throw new RuntimeException("Tried to add self-loop "+v1);
		boolean worked = eList[v1].add(v2) && eList[v2].add(v1);
		if(!worked)
			throw new RuntimeException("Tried to add edge "+v1+"-"+v2+" that was already in graph.");
		deg[v1]++;
		deg[v2]++;
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
	
	Graph copy() {
		HashSet<Integer>[] newEList = cloneList(eList);
		return new Graph(N, newEList, Arrays.copyOf(deg,N));
	}
	
	public int E() {
		checkConsistency();
		int tot = 0;
		for(int i=0; i<N; i++)
			tot += deg[i];
		return tot/2;
	}
	
	//remove all edges
	void clear() {
		for(int i=0; i<N; i++) {
			eList[i].clear();
			deg[i] = 0;
		}
		checkConsistency();
	}
	
	//How many vertices have degree > 0, the 'effective' N?
	int nonZeroDegN() {
		int res = 0;
		for(int i=0; i<N; i++)
			if(deg[i] > 0)
				res++;
		return res;
	}
	
	//adds vNew many new vertices
	void expandBy(int vNew) {
		int Nnew = N + vNew;
		deg = Arrays.copyOf(deg, Nnew);
		eList = Arrays.copyOf(eList, Nnew);
		for(int i=N; i<Nnew; i++) {
			eList[i] = new HashSet<>();
		}
		N += vNew;
	}
	
	String dumpS() {
		String res = "";
		res += "{";
		boolean firstRow = true;
		for(int i=0; i<N; i++) {

			String rowStr = "";
			boolean first = true;
			for(int vo : eList[i]) {
				if(!first)
					rowStr += ", ";
				rowStr += i+"--"+vo;
				first = false;
			}
			
			if(rowStr.length() == 0)
				continue;
			
			if(!firstRow) {
				res += ",";
			} else {
				firstRow = false;
			}
			res += "{"+rowStr+"}\n";
		}
		res += "}\n";
		return res;
	}

	void dump() {
		System.out.println(dumpS());
	}
	
	void dumpMMA() {
		System.out.println("Graph[Union[Sort /@ Flatten@");
		String res = "";
		res += "{";
		boolean firstRow = true;
		for(int i=0; i<N; i++) {

			String rowStr = "";
			boolean first = true;
			for(int vo : eList[i]) {
				if(!first)
					rowStr += ", ";
				rowStr += "UndirectedEdge["+(1+i)+","+(1+vo)+"]";
				first = false;
			}
			
			if(rowStr.length() == 0)
				continue;
			
			if(!firstRow) {
				res += ",";
			} else {
				firstRow = false;
			}
			res += "{"+rowStr+"}\n";
		}
		res += "}\n";
		System.out.println(res);
		System.out.println("], VertexLabels -> Automatic, GraphLayout -> Automatic]");
	}

	//Changes the graph to its complement
	public void complement() {
		for(int i=0; i<N; i++) {
			for(int j=i+1; j<N; j++) {
				if(eList[i].contains(j)) {
					//blk -> nothing
					eList[i].remove(j);
					eList[j].remove(i);
				} else {
					//nothing -> blk
					eList[i].add(j);
					eList[j].add(i);
				}
			}
		}
		//fix degree info
		for(int i=0; i<N; i++) {
			deg[i] = N-1 - deg[i];
		}
	}
	
	//Check for connected components.
	public ArrayList<ArrayList<Integer>> connComps(){
		ArrayList<ArrayList<Integer>> res = new ArrayList<>();
		boolean[] visited = new boolean[N];
		Queue<Integer> toVisit = new ArrayDeque<Integer>();
		for(int i=0; i<N; i++) {
			if(visited[i])
				continue;
			toVisit.add(i);
			ArrayList<Integer> comp = new ArrayList<>();
			res.add(comp);
			
			do {
				int v = toVisit.poll();
				if(visited[v])
					continue;
				
				visited[v] = true;
				comp.add(v);
				
				for(int v2 : eList[v]) {
					if(!visited[v2])
						toVisit.add(v2);
				}
				
			} while(!toVisit.isEmpty());
		}
		return res;
	}
}
