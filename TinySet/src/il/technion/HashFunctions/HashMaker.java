package il.technion.HashFunctions;

public class HashMaker {
	
//currently chain is bounded to be 64. 
	
	 private final int fpSize;
	 private final long fpMask; 
	 private final long minfpMask;
	 private final long chainMask=63l; 
	 private final int bucketRange;
//	 HashFunction f; 
	 FingerPrintAux retVal; 
	 public HashMaker(int fingerprintsize,int bucketrange, int chainrange,int minsize)
	{
		this.fpSize = fingerprintsize;
		this.bucketRange =bucketrange;
		fpMask = (1l<<fpSize)-1;
		minfpMask = (1l<<minsize)-1;
//		f = Hashing.murmur3_128();
		retVal = new FingerPrintAux(0,0,0);
	}
	

	
	public FingerPrintAux createHash(String item)
	{
		return createHash(item.getBytes());
	}
	
	public FingerPrintAux createHash(long item) {
		long hash =  MurmurHashTinyTable.newHash(item);


		retVal.fingerprint = hash&fpMask;

		hash>>>=fpSize;
		retVal.chainId = (int) (hash&chainMask);
		hash>>>=6;
		retVal.bucketId =  (int) ((hash&Long.MAX_VALUE)%bucketRange);

		return retVal;

	}

	public  FingerPrintAux createHash(final byte[] data) {


		long hash =		MurmurHashTinyTable.hash641(data, data.length);
		
		retVal.fingerprint = hash&fpMask;

		if((retVal.fingerprint&minfpMask) ==0l || (retVal.fingerprint&fpMask) ==0)
		{
			retVal.fingerprint++;

		}



		hash>>>=fpSize;
	
		retVal.chainId  = (int) (hash&chainMask);
		hash>>>=6;
		retVal.bucketId = (int) ((hash&Long.MAX_VALUE)%bucketRange);
		 
		
		return retVal;
		
	}
	


}
