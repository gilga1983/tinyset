package il.technion.TinySet;

import il.technion.BitManipulation.BitHelper;
import il.technion.BitManipulation.BitwiseArray;
import il.technion.BitManipulation.BucketSizeExpert;
import il.technion.HashFunctions.FingerPrintAux;
import il.technion.HashFunctions.HashMakerWithIndexBit;
/**
 * TinySet - an access efficient Bloom filter construction. 
 * This code implements the TinySet algorithm for space efficient fingerprint hashtable. 
 * TinySet is often more space efficient than Bloom filter and since it is based on a table it can be extended to support
 * functionalities that Bloom filters cannot such as values, and removals. 
 * (currently just Bloom filter functionality is implemented.) 
 * 
 * @author Gil Einziger, gilga1983@gmail.com
 *
 */

public class TinySet extends BitwiseArray
{

	protected int nrItems;
	boolean bloomFilter = false;
	long chainIndex[];
	HashMakerWithIndexBit hashFunc; 
	BucketSizeExpert bucketMaster;
	
	/**
	 *  A simple constructor - an hash table that can downsize fingerprints up to a single index bit. 
	 *  Note that we need a single bit per fingerprint for the indexing method. 
	 * @param bucketBitSize     - each bucket is independent, size of a cache line seems like a good call. 
	 * @param nrBuckets         - To work fast TinySet requires buckets to be relatively small and contain 
	 *                             say 30-60 fingerprints on average. 
	 * @param maxFingerprintSize - This implementation requires that maxFingerprintSize + 6 bits for chain (there are 64 chains per bucket) 
	 *                             + log(number of buckets) is less than 64 bits. It is easy to change but require a better hash function.
	 *                             Similarly this implementation does not allow fingerprint size to be larger than 64 bits.  
	 */
	public TinySet(int bucketBitSize, int nrBuckets,int maxFingerprintSize)
	{
		this(0,bucketBitSize,nrBuckets,maxFingerprintSize);

	}
	/**
	 *  Constructor for more advanced users, the number of resize operations can be influenced with a combination of minimal size
	 *  and max additional bits. 
	 * @param minimalFingerprintSize - minimal size of fingerprint (without index bit) 
	 * @param bucketFingerprintCapacity -  maximal number of fingerprints per bucket (at your own risk not to overflow)
	 * @param nrBuckets - number of buckets - 
	 * @param maxAdditionalBits - maximal fingerprint size is minimalFingerprintSize + maxAdditional bits the number of buckrts, + 6 bits for chain 
	 * + fingerprint should be less than 64 bits due implementation limitations. 
	 */
	public TinySet(int minimalFingerprintSize, int bucketFingerprintCapacity,int nrBuckets,int maxAdditionalBits)
	{
		super(bucketFingerprintCapacity*nrBuckets, minimalFingerprintSize+1,bucketFingerprintCapacity);
		this.maxAdditionalSize = maxAdditionalBits;
		this.nrItems = 0;
		chainIndex = new long[nrBuckets];

		Items = new short[nrBuckets];
		hashFunc = new HashMakerWithIndexBit(minimalFingerprintSize+maxAdditionalSize, nrBuckets, 64,minimalFingerprintSize);
		this.BucketCapacity = bucketFingerprintCapacity;
		this.bucketMaster = new BucketSizeExpert((minimalFingerprintSize+1)*bucketFingerprintCapacity,minimalFingerprintSize+1,minimalFingerprintSize+maxAdditionalSize+1);
	}
	
	/**
	 * Add an item to TinySet. 
	 * @param item
	 */
	public void add(String item)
	{
		this.addItem(hashFunc.createHash(item));	
	}
	/**
	 * Adding a long is faster than adding a string due to hash calculation overheads. 
	 * @param item
	 */
	public void add(long item)
	{
		this.addItem(hashFunc.createHash(item));	
	}
	/**
	 * Test an item for being a member of the set - if the item was added before contains returns true, 
	 * otherwise it returns false usually - or true if there is a false positive. Note that items are not stored in TinySet 
	 * just their fingerprints. 
	 * @param item
	 * @return
	 */
	public boolean contains(String item)
	{

		return this.contains(hashFunc.createHash(item));
	}
	/**
	 * Working with longs is faster than working with Strings. 
	 * @param item
	 * @return
	 */
	public boolean contains(long item)
	{

		return this.contains(hashFunc.createHash(item));
	}


	private int baseRank(int bucketNumber,int chainNumber)
	{
		long mask = ~((-1l)<<chainNumber);
		return Long.bitCount(chainIndex[bucketNumber]&mask); 
	}

	private int getChainStart(int bucketStart,int bucketNumber,int chainNumber, int size, int mod)
	{
		return this.findChain(bucketStart, bucketNumber, 0, size, mod,baseRank(bucketNumber,chainNumber));

	}


	/**
	 * Implementation of add method. 
	 * 
	 * @param bucketNumber
	 * @param chainNumber
	 * @param fingerPrint
	 */
	private void addItem(FingerPrintAux item)
	{
		 this.nrItems++ ;
		// first, let's calculate the bucket size and mod, we do it now so we only do it once. 
		int size = this.getBucketItemSize(item.bucketId,this.bucketBitSize);
		int mod = this.getBucketMod(item.bucketId, this.bucketBitSize);
		int newSize = this.getNextItemSize(item.bucketId,this.bucketBitSize);
		int newMod = this.getNextBucketMod(item.bucketId, this.bucketBitSize);
		// second, lets calculate the next item bucket size and mod. 
		int bucketStart = this.bucketBitSize*item.bucketId ;
		int idxToAdd = getChainStart(bucketStart,item.bucketId,item.chainId ,size,mod);
		// we may also need to make room for current buckets, and shrink the finger prints. 
		item.fingerprint = makeRoomForBucketItem(item.bucketId, item.fingerprint, idxToAdd,size,mod,newSize,newMod);
		//indexing operation - if last mark last. 
		if(!this.MarkChain(item.bucketId, item.chainId))
			item.fingerprint = FingerPrintAux.setLast(item.fingerprint);
		//finally we put the item in the bucket, and perform a local bucket shifting as needed. 
		this.PutAndPush(item.bucketId, idxToAdd, item.fingerprint,newSize,newMod);
	}
	
	
	/**
	 *  Resize the fingerprints to make room for one more. 
	 * @param bucketNumber
	 * @param fingerPrint
	 * @param idx
	 * @param oldItemSize
	 * @param oldMod
	 * @param newItemSize
	 * @param newMod
	 * @return
	 */
	private long makeRoomForBucketItem(int bucketNumber, long fingerPrint, int idx, int oldItemSize, int oldMod,int newItemSize,int newMod) {

		//first we check if a downsize of the items is required. 
		if(oldItemSize!=newItemSize || newMod!=oldMod)
		{			
//			System.out.println("Items: "+this.Items[bucketNumber] +"  Bucket: "+bucketNumber+ " Resizing items: "+ oldItemSize + " " + newItemSize + " "+ oldMod + " "+ newMod);
			resizeItems(bucketNumber,oldItemSize,newItemSize,oldMod,newMod);
		}

		//we calculate the length of the new item size, and adjust the fingerprint to that size.  
		
		int nsize = super.getSizeFix(idx, newItemSize, newMod);
		return BitHelper.adjustFingerPrint(nsize, fingerPrint);
	}
	/** 
	 *  Actually down sizing fingerprints. 
	 * @param bucketId
	 * @param oldSize
	 * @param newSize
	 * @param oldMod
	 * @param newMod
	 */
	private void resizeItems(int bucketId,int oldSize, int newSize,int oldMod, int newMod) {


		int bucketStart = this.bucketBitSize*bucketId ;
		int nsize = newSize;
		
		int startIdx = 0;
		
		//calculate resize start. 
		while(this.getSizeFix(startIdx, oldSize, oldMod)==this.getSizeFix(startIdx, newSize, newMod))
				startIdx++;
		
		
		for( int i=startIdx; i<this.Items[bucketId];i++)
		{
			nsize = super.getSizeFix(i, newSize, newMod);
			long oldFp = this.Replace(bucketStart,bucketId,i,oldSize,oldMod,0l);
			oldFp = BitHelper.adjustFingerPrint(nsize, oldFp);
			this.Put(bucketStart,bucketId,i, oldFp,newSize,newMod);
		}

	}



	/**
	 *  Methods inherited from bitwise array - 
	 *  may be needed to extend functionality but of no interest to TinySet user.
	 *  Allow direct access to the bitwise array that implements TinySet. 
	 */
	protected void Put(int bucketId,int idx, final long value) {
		int size = this.getBucketItemSize(bucketId,bucketBitSize);
		int mod = this.getBucketMod(bucketId,bucketBitSize);
		this.Put(bucketId,idx, value,size,mod);
		return;
	}
	/**
	 *  Methods inherited from bitwise array - 
	 *  may be needed to extend functionality but of no interest to TinySet user.
	 *  Allow direct access to the bitwise array that implements TinySet. 
	 */
	protected long Get(int bucketID, int idx) {
		int size = this.getBucketItemSize(bucketID,bucketBitSize);
		int mod =getBucketMod(bucketID,bucketBitSize);
		int bucketStart = bucketID*bucketBitSize;
		return this.Get(bucketStart,bucketID,idx, size,mod);
	}

	/**
	 * calculate the size of fingerprint for a specific bucket. 
	 * @param bucketNumber
	 * @param bucketSize
	 * @return
	 */
	private int getBucketItemSize(int bucketNumber,int bucketSize)
	{

		return this.bucketMaster.getSize(Items[bucketNumber],bucketSize);
	}
	/**
	 * calculate the size of fingerprint for a specific bucket after adding another item. 
	 * @param bucketNumber
	 * @param bucketSize
	 * @return
	 */
	private int getNextItemSize(int bucketNumber,int bucketSize)
	{

		return this.bucketMaster.getSize(Items[bucketNumber]+1,bucketSize);
	}
	/**
	 * calculate the mod of fingerprint for a specific bucket after adding another item. 
	 * fingerprints with index less than mod in the bucket are 1 bit longer than the rest. 
	 * @param bucketNumber
	 * @param bucketSize
	 * @return
	 */
	private int getNextBucketMod(int bucketNumber,int actualBucketSize)
	{
		return this.bucketMaster.getMod(Items[bucketNumber]+1,actualBucketSize);
	}
	/**
	 * calculate the mod of fingerprint for a specific bucket 
	 * fingerprints with index less than mod in the bucket are 1 bit longer than the rest. 
	 * @param bucketNumber
	 * @param bucketSize
	 * @return
	 */
	private int getBucketMod(int bucketNumber,int actualBucketSize)
	{
		return this.bucketMaster.getMod(Items[bucketNumber],actualBucketSize);

	}
	/**
	 *  Added for clearity - checks if a specific chain is not empty.  
	 * @param bucketNumber
	 * @param chainNumber
	 * @return
	 */
	private boolean containChain(int bucketNumber, int chainNumber)
	{
		return (chainIndex[bucketNumber]&((1l)<<chainNumber)) != 0; 
	}
	/**
	 *  Marks a chain as not empty and return if it was empty before marking it.  
	 * @param bucketNumber
	 * @param chainNumber
	 * @return
	 */
	private boolean MarkChain(int bucketNumber, int chainNumber)
	{
		long mask = ((1l)<<chainNumber);
		boolean result = (chainIndex[bucketNumber]&mask) == mask; 
		chainIndex[bucketNumber]|=mask;
		return result; 
	}
	/**
	 *  Prints the hash table in a manner that abstracts the indexing technique. Note that the least significant bit indicates 
	 *  if an item is last in chain. Used for Debug or if you want to learn how the table works.  
	 * @param bucketNumber
	 * @param chainNumber
	 * @return
	 */
	public void printTable()
	{
		
		for(int i =0; i<chainIndex.length;i++)
		{
			int k =0;
			for(int j =0; j<64; j++)
			{
				if(( (1l<<j) & chainIndex[i])!=0)
				{long item =0;
					System.out.println("Chain: "  + j);
					do
					{
						 item = this.Get(i, k);
						k++;
						System.out.println(item + "	"+ (k-1));
					}
					while((item&1l) != 1l);
				}
				
			}
		

		}

	}
	/**
	 * Implementation of the contain method. 
	 * @param item
	 * @return
	 */
	private boolean contains(FingerPrintAux item)
	{	
		int bucketSize = this.bucketBitSize ;
		// if the chain is empty the item is not there. 
		if(!containChain(item.bucketId,item.chainId))
			return false;
		
		int bucketStart = this.bucketBitSize*item.bucketId ;

		int itemSize = this.getBucketItemSize(item.bucketId,bucketSize);
		int mod  = this.getBucketMod(item.bucketId, bucketSize);
		int idx = getChainStart(bucketStart,item.bucketId,item.chainId,itemSize,mod);
		//fingers can be two sizes, instead of constantly adjusting create two sizes and check them. 
		long sfingerPrint = item.fingerprint&((1<<itemSize)-1); 
		long lfingerPrint = item.fingerprint&((1<<(itemSize+1))-1);
		long otherFingerprint = this.Get(bucketStart,item.bucketId,idx,itemSize,mod);

		long fpTocomper = sfingerPrint;
		if(idx <mod)
			fpTocomper = lfingerPrint;

		while((fpTocomper^otherFingerprint)>1l) 
		{
			if((otherFingerprint&1l) ==1l)
				return false;
			idx++;
			fpTocomper = sfingerPrint;
			if(idx <mod)
				fpTocomper = lfingerPrint;
			otherFingerprint = this.Get(bucketStart,item.bucketId,idx,itemSize,mod);
		}
		return true;



	}




	/**
	 * Put a value at location idx, if the location is taken shift the items to
	 * be left until an open space is discovered.
	 * 
	 * @param idx
	 *            - index to put in
	 * @param value
	 *            - value to put in
	 * @param mod 
	 * 				- bucket mod, (in order to decode bucket)
	 * @param size 
	 * 				- bucket item size. (in order to decode bucket)
	 * @param chainNumber 
	 */
	private void PutAndPush(int bucketId, int idx,  long value, int size, int mod) {
		this.Items[bucketId]++;
		int bucketStart = this.bucketBitSize*bucketId;
		long itemToShift = value; 
		for(int i =idx; i<Items[bucketId];i++)
		{
			itemToShift = this.Replace(bucketStart,bucketId,idx,size,mod,itemToShift);
			idx++;
			// items may be shorten by a single bit while shifted to the right - so we need to adjust their size. 
			itemToShift = BitHelper.adjustFingerPrint(this.getSizeFix(idx, size, mod), itemToShift);

		}

	}
	/**
	 * returns the number of items stored in TinySet. 
	 * @return
	 */
	public int getNrItems() {

		return this.nrItems;
	}

}
