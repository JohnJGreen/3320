package greenassignment4;

public class FileNameTableEntry {
	
	private char name[];
	private int ptr;
	
	public FileNameTableEntry(char name[], int ptr) {
		this.setName(name);
		this.setPtr(ptr);
	}

	public FileNameTableEntry() {
		// TODO Auto-generated constructor stub
		this.name = new char[56];
		this.ptr = -1;
		for(int i=0; i<56; i++){
			this.name[i] = '+';
		}
	}

	public char[] getName() {
		return name;
	}

	public void setName(char name[]) {
		this.name = name;
	}

	public int getPtr() {
		return ptr;
	}

	public void setPtr(int ptr) {
		this.ptr = ptr;
	}

}
