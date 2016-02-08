package il.technion.BitManipulation;

public class BitwiseArray extends AuxilaryBitSet {

	protected int itemSize;
	protected int capacity;
	protected int bucketBitSize; 
	public short Items[];
	protected int maxitemSize;
	public int BucketCapacity;
	protected int maxAdditionalSize = 15;

	/**
	 *  Represent an array with bit elements. Implemented over the AuxilaryBitSet, its job is to calculate the appropriate bit offsets in order
	 *  to produce the abstraction of an array.
	 * @param capacity
	 * @param itemSize
	 * @param bucketcapacity
	 */
	public BitwiseArray(final int capacity, final int itemSize, int bucketcapacity) {
		super((capacity * itemSize)/64 + 1);
		this.itemSize = itemSize;
		this.capacity = capacity;
		this.BucketCapacity = bucketcapacity;
		this.bucketBitSize = bucketcapacity*itemSize;
		this.maxitemSize = this.itemSize + this.maxAdditionalSize;
	}

	/**
	 * Retrieve the element at index idx. The array calculates the required
	 * offset and length of item.
	 * 
	 * @param idx
	 *            - logical array index.
	 * @return the item (bits of length fingerSize) stored at that index.
	 */
	protected long Get(int bucketId, int idx) {
		return this.Get(bucketId*this.bucketBitSize,bucketId,idx, this.itemSize,0);
	}



	private int getModOffsetFix(int idx, int mod) {
		int effectiveMod = mod;
		if(idx<mod)
			effectiveMod = idx;
		return effectiveMod;
	}
	protected long Get(int bucketStart, int bucketId, int idx,int customSize,int mod) {
		int effectiveMod = getModOffsetFix(idx, mod);;
		int effectiveItemSize = getSizeFix(idx, customSize, mod);
		
			int start = bucketStart + idx*customSize + effectiveMod;
			return super.getBits(start, start + effectiveItemSize);
	}

	protected int getSizeFix(int idx, int customSize, int mod) {
		int effectiveItemSize = customSize;
		if(idx<mod)
			effectiveItemSize = customSize+1;
		return effectiveItemSize;
	}
	protected int findChain(int bucketStart, int bucketId, int idx,int customSize,int mod,int sum) {
		while(sum>0)
		{
			long fp = this.Get(bucketId*this.bucketBitSize,bucketId, idx, customSize, mod);
			sum -= fp&1l;
			idx++;
		}
		return idx;
	}

	protected long Replace(int bucketStart, int bucketId, int idx,int customSize,int mod,long newItem) {
		int effectiveMod = getModOffsetFix(idx, mod);
		int effectiveItemSize = getSizeFix(idx, customSize, mod);
		int start = bucketStart + idx*customSize + effectiveMod;
		return super.replaceBits(start, start + effectiveItemSize,newItem);
	}






	/**
	 * Put an item according to predefined idx, customSize and mod.
	 * @param idx
	 * @param value
	 * @param customSize
	 * @param mod
	 * 
	 * TODO: get and put should consider Anchors (rather than shrink it all). 
	 */

	public void Put(int bucketStart,int bucketId,int idx,  long value,int customSize,int mod) {
		int effectiveMod = getModOffsetFix(idx, mod);
		int effectiveItemSize = getSizeFix(idx, customSize, mod); 

		int start = bucketStart + idx*customSize + effectiveMod;

		// we use this line in order to be able to perform assertions. We take out any un-needed bits in the value. 
		value = value&((1L<<effectiveItemSize)-1);
		super.setBits(start, start + effectiveItemSize, value);
	}
	public void Put(int bucketId,int idx,  long value,int customSize,int mod) {
		int effectiveMod = getModOffsetFix(idx, mod);
		int effectiveItemSize = getSizeFix(idx, customSize, mod); 


		int bucketStart = this.bucketBitSize*bucketId;
		int start = bucketStart + idx*customSize + effectiveMod;

		// we use this line in order to be able to perform assertions. We take out any un needed bits in the value. 
		value = value&((1L<<effectiveItemSize)-1);

		super.setBits(start, start + effectiveItemSize, value);
	}








	/**
	 * puts a value at location idx. The previous item at location idx is
	 * erased.
	 * 
	 * @param idx
	 * @param value
	 */
	protected void Put(int bucketId,int idx, final long value) {
		int bucketStart = this.bucketBitSize*bucketId;
		this.Put(bucketStart,bucketId,idx, value,this.itemSize,0);
		return;
	}

}
