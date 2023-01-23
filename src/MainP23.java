import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;

public class MainP23 {
	
	static final String TO_RUN = 
			"tiny-test/"
//			"stdin"
//			"unit"
			;
	
	static final boolean VERB = true;
	
	public static void main(String[] args) throws IOException {
		if(TO_RUN.equals("stdin")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			PrintStream fileout = new PrintStream(System.out);
			Graph g = parse(reader);
			int res = solve(g, fileout);
			if(VERB) System.out.println("TW = "+res);
			
		} else if(TO_RUN.endsWith("/")) {
			int[] answers = new int[] {1, 2, 0, 0, 2, 3, 3, 0, 2, 2, 4, 1, 2};
			int ansI = 0;
			
			File dir = new File(TO_RUN);
			PrintStream fileout = new PrintStream(System.out);
			File[] fileList = dir.listFiles();
			Arrays.sort(fileList);
			for(File f : fileList) {
				if(f.getName().endsWith(".gr")) {
					System.out.println("Reading "+f);
					BufferedReader reader = new BufferedReader(new FileReader(f));
					Graph g = parse(reader);
					int res = solve(g, fileout);
					if(VERB) System.out.println("TW = "+res);
					if(res != answers[ansI]) {
						throw new RuntimeException("Expected "+answers[ansI]);
					}
					ansI++;
				}
			}
		} else if(TO_RUN.equals("unit")) {
			unitTests();
		} else {
			System.out.println("Unknown problem source "+TO_RUN);
		}
	}
	
	public static int solve(Graph g, PrintStream fileout) {
		long startT;
		if(VERB) startT = System.currentTimeMillis();
		
		int res = BruteTW.twinWidth(g);
		int[] sol = BruteTW.bestSol;
		int N = g.N;
		
		int steps = 0;
		boolean[] gone = new boolean[N];
		for(int i=0; i<N-1; i++) {
			if(sol[2*i] == sol[2*i+1]) {
				break;
			}
			steps++;
			gone[sol[2*i+1]] = true;
			sol[2*i]++;
			sol[2*i+1]++;
		}
		for(int i=0; i<steps; i++) {
			fileout.println(sol[2*i]+" "+sol[2*i+1]);
		}
		//Need to end the printing with deg-zero contractions
		for(int i=1; i<N; i++) {
			if(!gone[i])
				fileout.println(1+" "+(i+1));
		}
		
		if(VERB) {
			float t = (System.currentTimeMillis()-startT) * 0.001f;
			System.out.println("Took "+t+"s");
		}
		return res;
	}
	
	public static void unitTests() {
		Testing.testGraphOps();
		System.out.println("Passed GraphOps");
		Testing.testPath();
		System.out.println("Passed testPath");
		Testing.testCograph();
		System.out.println("Passed testCograph");
		Testing.testStar();
		System.out.println("Passed testStar");
	}
	
	public static Graph parse(BufferedReader reader) throws IOException {
		int M = -1;
		Graph g = new Graph();
		String line;
		int ni = -1;
		while((line = reader.readLine()) != null) {
			if(line.startsWith("c") || line.length() == 0)
				continue;
			if(ni == -1) {
				int N = Integer.parseInt(line.split(" ")[2]);
				M = Integer.parseInt(line.split(" ")[3]);
				ni++;
				g.expandBy(N);
				
			} else {
				String[] pts = line.split(" ");
				int v1 = Integer.parseInt(pts[0])-1;
				int v2 = Integer.parseInt(pts[1])-1;
				g.addEdge(v1, v2);
				ni++;
			}
			if(ni == M)
				break;//done
		}
		return g;
	}
}
