package il.technion.HashFunctions;

public class HashMakerWithIndexBit {
	
//currently chain is bounded to be 64. 
	HashMaker hash;
	FingerPrintAux $;
	public HashMakerWithIndexBit(int fingerprintsize,int bucketrange, int chainrange,int minsize)
	{
		hash = new HashMaker(fingerprintsize, bucketrange, chainrange, minsize);

	}
	

	
	public FingerPrintAux createHash(String item)
	{
		return createHash(item.getBytes());
	}
	public FingerPrintAux createHash(long item)
	{
		 $= hash.createHash(item);
		$.fingerprint<<=1;
		return $;
	}

	public  FingerPrintAux createHash(final byte[] data) {

		 $ = hash.createHash(data);
		$.fingerprint<<=1;
		return $;
		
	}
	


}
