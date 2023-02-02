package sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

//based on https://github.com/lemire/Code-used-on-Daniel-Lemire-s-blog/blob/master/2012/11/13/src/StaticBitSet.java

public class SmallIntSet implements Set<Integer>, Cloneable {
	long[] data;
	
	private SmallIntSet(long[] d) {
		this.data = d;
	}

	public SmallIntSet(int sizeinbits) {
		this(new long[(sizeinbits + 63) / 64]);
	}

	@Override
	public void clear() {
		Arrays.fill(data, 0);
	}
	@Override
	public int size() {
		int sum = 0;
		for (long l : data)
			sum += Long.bitCount(l);
		return sum;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	public void resize(int sizeinbits) {
		data = Arrays.copyOf(data, (sizeinbits + 63) / 64);
	}

	//The "unsafe" accessors that require that i is in bounds.
	public boolean get(int i) {
		return (data[i / 64] & (1l << (i % 64))) != 0;
	}

	public void set(int i) {
		data[i / 64] |= (1l << (i % 64));
	}

	public void unset(int i) {
		data[i / 64] &= ~(1l << (i % 64));
	}

	public void set(int i, boolean b) {
		if (b)
			set(i);
		else
			unset(i);
	}

	// for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) { // operate on
	// index i here }
	public int nextSetBit(int i) {
		int x = i / 64;
		if (x >= data.length)
			return -1;
		long w = data[x];
		w >>>= (i % 64);
		if (w != 0) {
			return i + Long.numberOfTrailingZeros(w);
		}
		++x;
		for (; x < data.length; ++x) {
			if (data[x] != 0) {
				return x * 64 + Long.numberOfTrailingZeros(data[x]);
			}
		}
		return -1;
	}
	
	//Get the largest integer in the collection (returns -1 if empty)
	public int max() {
		int b = data.length-1;
		while(b >= 0 && data[b] == 0)
			b--;
		if(b == -1)
			return -1;
		return b*64 + (63-Long.numberOfLeadingZeros(data[b]));
	}
	//get the largest occuplied block
	private int maxBlock() {
		int b = data.length-1;
		while(b >= 0 && data[b] == 0)
			b--;
		return b;
	}

	@Override
	public boolean add(Integer e) {
		if(e/64 >= data.length)
			resize(e+1);
		boolean res = !get(e);
		set(e);
		return res;
	}

	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		if(c instanceof SmallIntSet) {
			int pop = size();
			addAll((SmallIntSet)c);
			return pop != size();
		}
		boolean res = false;
		for(Integer o : c) {
			res |= add(o);
		}
		return res;
	}
	
	public void addAll(SmallIntSet c) {
		int cMaxBlock = c.maxBlock();
		if(cMaxBlock > data.length)
			resize(64*cMaxBlock);
		for(int i=0; i<=cMaxBlock; i++) {
			data[i] |= c.data[i];
		}
	}

	@Override
	public boolean contains(Object o) {
		int i = (Integer)o;
		return (i/64 < data.length) && get(i);
	}

	@Override
	public boolean remove(Object o) {
		int i = (Integer)o;
		if(i/64 >= data.length)
			return false;
		boolean res = get(i);
		unset(i);
		return res;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if(c instanceof SmallIntSet) {
			int pop = size();
			removeAll((SmallIntSet)c);
			return pop != size();
		}
		boolean res = false;
		for(Object o : c) {
			res |= remove(o);
		}
		return res;
	}
	
	public void removeAll(SmallIntSet c) {
		int mLen = Math.min(data.length, c.data.length);
		for(int i=0; i<mLen; i++) {
			data[i] &= ~c.data[i];
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean res = false;
		for(int i=nextSetBit(0); i>=0; i=nextSetBit(i+1)) {
			if(!c.contains(i)) {
				res = true;
				unset(i);
			}
		}
		return res;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException("Not impl");
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			int i=nextSetBit(0);

			@Override
			public boolean hasNext() {
				return i >= 0;
			}

			@Override
			public Integer next() {
				int res = i;
				i = nextSetBit(i+1);
				return res;
			}
			
		};
	}

	@Override
	public Object[] toArray() {
		throw new RuntimeException("Not impl");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new RuntimeException("Not impl");
	}
	
	@Override
	public SmallIntSet clone() {
		return new SmallIntSet(this.data.clone());
	}

}