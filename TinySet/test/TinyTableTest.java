import org.junit.Test;

import il.technion.TinySet.TinySet;

import org.junit.Assert;

public class TinyTableTest {
	
	// just for fun. 
	@Test
	public void sainityTest()
	{
		TinySet ts = new TinySet(3, 64, 2, 10);
		ts.add("Item");
		Assert.assertTrue(ts.contains("Item"));
		ts.add("AnotherItem");
		Assert.assertTrue(ts.contains("AnotherItem"));
		Assert.assertTrue(ts.contains("Item"));
		Assert.assertTrue(!ts.contains("SomeItem"));


	}
	@Test
	public void extensiveTest()
	{
		// create a TinySet with 256 bits per bucket, 10 buckets and max fingerprint size of 20. 
		TinySet ts = new TinySet(256, 10, 20);
		// add 500 items and verify that all previously added items test positive in every step of the way. 
		for(long i =0; i<500;i++)
		{
			ts.add(i);
			for(long j=0;j<i;j++)
			{
				Assert.assertTrue(ts.contains(j));
			}
		}
	}
	@Test
	public void extensiveTesStringt()
	{
		// create a TinySet with 256 bits per bucket, 10 buckets and max fingerprint size of 20. 
		TinySet ts = new TinySet(256, 10, 20);
		// add 1000 items and verify that all previously added items test positive in every step of the way. 
		for(long i =0; i<1000;i++)
		{
			ts.add(""+ i);
			for(long j=0;j<i;j++)
			{
				Assert.assertTrue(ts.contains(""+j));
			}
		}
	}
}