package tw_compute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class MainP23 {
	
	static final String TO_RUN = 
//			"tiny-test/"
//			"exact-public/"
			"stdin"
//			"unit"
			;
	
	
	//If nonnull, and TO_RUN is set to run on a directory, these will mark a "start" and "end" filename
	//to process. Files are processed in directory in lexiographic order, and these will be substrings of
	//file names to start and stop processing. For instance, setting "_100" and "150.gr" will process only
	//files exact_100.gr through exact_150.gr, inclusive.
	static final String START_AT = null; //null, "100"
	static final String STOP_AT = "050"; //null, "104"
	
	static final boolean VERB = false;
	static final boolean PRINT_SOL_TESTING = false;
	
	public static void main(String[] args) throws IOException {
		if(TO_RUN.equals("stdin")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			PrintStream fileout = new PrintStream(System.out);
			Graph g = parse(reader);
			int res = solve(g, fileout);
			if(VERB) System.out.println("TW = "+res);
			
		} else if(TO_RUN.endsWith("/")) {
			int[] answers_tiny = new int[] {1, 2, 0, 0, 2, 3, 3, 0, 2, 2, 4, 1, 2};
			int ansI = 0;
			
			File dir = new File(TO_RUN);
			PrintStream fileout = new PrintStream(System.out);
			if(!PRINT_SOL_TESTING) {
				fileout = new PrintStream(OutputStream.nullOutputStream());
			}
			File[] fileList = dir.listFiles();
			Arrays.sort(fileList);
			boolean in_active_set = (START_AT == null); //if null, we start active
			for(File f : fileList) {
				if(START_AT != null && f.getName().contains(START_AT)) {
					in_active_set = true;
				}
				if(!in_active_set)
					continue;
				
				if(f.getName().endsWith(".gr")) {
					System.out.println("Reading "+f);
					BufferedReader reader = new BufferedReader(new FileReader(f));
					Graph g = parse(reader);
					int res = solve(g, fileout);
					if(VERB) System.out.println("TW = "+res);
					if(TO_RUN.equals("tiny-test/") && res != answers_tiny[ansI]) {
						throw new RuntimeException("Expected "+answers_tiny[ansI]);
					}
					ansI++;
					System.out.println();
				}
				
				if(STOP_AT != null && f.getName().contains(STOP_AT)) {
					break;
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
		
		//Can replace Preprocessor with BruteTW or SimpleTW, etc.

//		int[] sol = new int[2*(g.N-1)];
//		int res = Preprocessor.solve(g, sol);
		
		int res = BruteTW.twinWidth(g);
		int[] sol = BruteTW.bestSol;
		
		//validate
		int verifiedRes = Verifier.validate(g, sol);
		if(res != verifiedRes)
			throw new RuntimeException("Bad reported tww");
		
		int N = g.N;
		
		for(int i=0; i<N-1; i++) {
			fileout.println((1+sol[2*i])+" "+(1+sol[2*i+1]));
		}
		
		if(VERB) {
			float t = (System.currentTimeMillis()-startT) * 0.001f;
			System.out.println("Took "+t+"s");
		}
		return res;
	}
	
	public static void unitTests() {
		Testing.testGraphOps();
		System.out.println("Passed testGraphOps");
		Testing.testPath();
		System.out.println("Passed testPath");
		Testing.testCograph();
		System.out.println("Passed testCograph");
		Testing.testStar();
		System.out.println("Passed testStar");
		Testing.testBridges();
		System.out.println("Passed testBridges");
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
