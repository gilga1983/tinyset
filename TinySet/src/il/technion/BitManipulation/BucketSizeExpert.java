package il.technion.BitManipulation;



public class BucketSizeExpert {
	int maxSize;
	int minSize;
	int bucketCapacity; 
	public BucketSizeExpert(int bucketSize,int minSize,int maxSize)
	{
		 bucketCapacity = bucketSize/minSize;
		 this.maxSize =maxSize;
		 this.minSize = minSize; 
	}
	
	
	public int getSize(int nrItems, int actualBucketSize)
	{
		if(nrItems ==0)
			return maxSize;

		int size = actualBucketSize/(nrItems);
		if(size>maxSize)
			size = maxSize;
		if(size<minSize){
			size = minSize;
		}


		return size; 
	}
	
	public int getMod(int nrItems, int actualBucketSize)
	{
		if(nrItems ==0)
			return 0;
		int size = actualBucketSize/(nrItems);

		if(size>=maxSize)
			return 0;
		return actualBucketSize%(nrItems);
	}

}
