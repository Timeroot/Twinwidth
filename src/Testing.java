import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class Testing {
	static Random rand = new Random(1234);
	
	public static void testGraphOps() {
		for(int test=0; test<1000; test++) {
			Testing.testGraphOps_1();
		}
	}
	
	public static void testGraphOps_1() {
		Graph g = new Graph();
		g.expandBy(20);
		
		for(int ne = 0; ne < 50; ne++) {
			int v1 = rand.nextInt(20);
			int v2 = rand.nextInt(20);
			if(v1 == v2 || g.eList[v1].contains(v2))
				continue;
			g.addEdge(v1, v2);
		}
		
		Trigraph tg = Trigraph.fromBigraph(g);
		
		while(tg.E() > 0) {
			int v1;
			do
				v1 = rand.nextInt(20);
			while(tg.degBlk[v1] + tg.degRed[v1] == 0);

			int v2;
			do
				v2 = rand.nextInt(20);
			while(v1 == v2 || tg.degBlk[v2] + tg.degRed[v2] == 0);
			
//			System.out.println("Merge "+v1+"&"+v2);
			tg.mergeRed(v1, v2);
		}
		
		tg.checkConsistency();
	}
	
	public static void testPath() {
		Graph g = new Graph();
		g.expandBy(7);
		
		g.addEdge(5, 1);
		g.addEdge(1, 4);
		g.addEdge(4, 0);
		g.addEdge(0, 3);
		g.addEdge(3, 2);
		g.addEdge(6, 2);
		
		int res = BruteTW.twinWidth(g);
		System.out.println("Brute TW on P5: "+res);
		if(res != 1)
			throw new RuntimeException("Should be 1");
	}

	public static void testCograph() {
		Graph g = new Graph();
		g.expandBy(10);
		
		g.addEdge(3, 1);
		g.addEdge(0, 4);
		g.addEdge(0, 5);
		g.addEdge(4, 5);
		g.addEdge(2, 7);
		g.addEdge(7, 9);
		g.addEdge(8, 2);
		g.addEdge(8, 9);

		int res = BruteTW.twinWidth(g);
		System.out.println("Brute TW on cograph: "+res);
		if(res != 0)
			throw new RuntimeException("Should be 0");
	}
	
	public static void testStar() {
		Graph g = new Graph();
		g.expandBy(9);
		
		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(0, 3);
		g.addEdge(0, 4);
		g.addEdge(1, 5);
		g.addEdge(2, 6);
		g.addEdge(3, 7);
		g.addEdge(4, 8);
		
		Trigraph tg = Trigraph.fromBigraph(g);
		
		int[] reds = new int[8];
		reds[0] = tg.mergeRed(1, 5);
		reds[1] = tg.mergeRed(2, 6);
		reds[2] = tg.mergeRed(1, 2);
		reds[3] = tg.mergeRed(3, 7);
		reds[4] = tg.mergeRed(1, 3);
		reds[5] = tg.mergeRed(0, 1);
		reds[6] = tg.mergeRed(0, 4);
		reds[7] = tg.mergeRed(0, 8);
		
		int[] correct = new int[] {1, 2, 1, 2, 1, 1, 1, 0};
		for(int i=0; i<7; i++) {
			if(reds[i] != correct[i])
				throw new RuntimeException("Star test step "+i);
		}
	}
}
