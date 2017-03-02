package se.sics.mspsim.platform;

public class MemoryContainer {
	private int[] memClone;
	public MemoryContainer(int[] mem) {
		memClone = (int[])mem.clone();
	}
	public int[] getMemory () {
		return memClone;
	}
}
