package smallgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import tw_compute.Graph;

public class G6Code {

	//just for debugging -- what graph we're currently processing
	static String currGraphG6;
	
	static long processed = 0;
	static final int PRINT_PROGRESS_EVERY = 1000000;
	
	@SuppressWarnings("resource")
	static public Iterator<Graph> read_graphs(String filename) throws FileNotFoundException{
		final File f = new File(filename);
		final BufferedReader br = new BufferedReader(new FileReader(f));
		final Iterator<String> graphlines = br.lines().iterator();
		return new Iterator<Graph>() {
			boolean done = false;
			@Override
			public boolean hasNext() {
				return !done;
			}

			@Override
			public Graph next() {
				String line = graphlines.next();
				
				if(!graphlines.hasNext()) {
					done=true;
					try {
						br.close();
					} catch (IOException e) {}
				}
				return decodeG6(line);
			}
			
		};
	}
	
	//https://users.cecs.anu.edu.au/~bdm/data/formats.txt
	static public Graph decodeG6(String line) {
		if(PRINT_PROGRESS_EVERY > 0) {
			processed++;
			if(processed % PRINT_PROGRESS_EVERY == 0) {
				System.out.println("Processed "+processed+", at "+line);
			}
		}
		currGraphG6 = line;
		//Read the small number
		long N;
		int off = 0;//current index into bytestream
		if(line.charAt(0) != 126) {
			N = line.charAt(0)-63;
			off=1;
		} else if(line.charAt(1) != 126) {
			N = ((line.charAt(1)-63)<<12)|((line.charAt(2)-63)<<6)|(line.charAt(3)-63);
			off=2;
		} else {
			N = ((line.charAt(2)-63)<<30)|((line.charAt(3)-63)<<24)|((line.charAt(4)-63)<<18)
					|((line.charAt(5)-63)<<12)|((line.charAt(6)-63)<<6)|(line.charAt(7)-63);
			off=3;
		}
		if(N > Integer.MAX_VALUE) {
			throw new RuntimeException("Graphs of size "+N+" greater than MAXINT not supported");
		}
		int n = (int)N;
		
		Graph g = new Graph(n);
		int sextet = 0;
		int bits_in_sextet = 0;
		//iterate in order (1,0), (2,0), (2,1), (3,0), etc.
		for(int i=1; i<n; i++) {
			for(int j=0; j<i; j++) {
				if(bits_in_sextet == 0) {
					//get the next six bits
					sextet = line.charAt(off)-63;
					if(sextet < 0 || sextet > 63)
						throw new RuntimeException("Bad character '"+line.charAt(off)+"' in G6 graph");
					off++;
					bits_in_sextet = 6;
				}
				
				if(((sextet>>5)&1) == 1) {
					g.addEdge(i, j);
				}
				sextet <<= 1;
				bits_in_sextet--;
			}
		}
		if(off != line.length())
			throw new RuntimeException("Extra characters at end of G6 code "+line);
		
		return g;
	}

	static public String encodeG6(Graph g) {
		StringBuilder sb = new StringBuilder();
		int N = g.N();
		if(N <= 62) {
			sb.append((char)(63+N));
		} else if(N <= 258047) {
			sb.append((char)126);
			sb.append((char)(63+((N>>12)&0x3F)));
			sb.append((char)(63+((N>>6)&0x3F)));
			sb.append((char)(63+(N&0x3F)));
		} else {
			sb.append((char)126);
			sb.append((char)126);
			sb.append((char)(63+((N>>30)&0x1)));
			sb.append((char)(63+((N>>24)&0x3F)));
			sb.append((char)(63+((N>>18)&0x3F)));
			sb.append((char)(63+((N>>12)&0x3F)));
			sb.append((char)(63+((N>>6)&0x3F)));
			sb.append((char)(63+(N&0x3F)));
		}
		int sextet = 0;
		int bits_in_sextet = 0;
		//iterate in order (1,0), (2,0), (2,1), (3,0), etc.
		for(int i=1; i<N; i++) {
			for(int j=0; j<i; j++) {
				sextet <<= 1;
				if(g.eList[i].contains(j)) {
					sextet |= 1;
				}
				bits_in_sextet++;
				if(bits_in_sextet == 6) {
					sb.append((char)(63+sextet));
					bits_in_sextet = 0;
					sextet = 0;
				}
			}
		}
		if(bits_in_sextet > 0) {
			sextet <<= (6-bits_in_sextet);
			sb.append((char)(63+sextet));
		}
		return sb.toString();
	}
}
