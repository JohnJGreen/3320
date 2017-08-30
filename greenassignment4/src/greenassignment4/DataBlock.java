package greenassignment4;

public class DataBlock {
	
	private byte block[];
	
	public DataBlock() {
		
		this.setBlock(new byte[256]);
	}

	public DataBlock(String text) {
		byte x[] = new byte[256];
		for(int i=0; i<256; i++){
			x[i] = (byte) text.charAt(i);
		}
		this.setBlock(x);
	}

	public byte[] getBlock() {
		return block;
	}

	public void setBlock(byte block[]) {
		this.block = block;
	}

}
