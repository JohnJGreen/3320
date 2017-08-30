package greenassignment4;

import java.util.Date;

public class AttributeBlockEntry {
	
	private int size;
	private String date;
	private int ptr;
	private int ext;
	
	public AttributeBlockEntry(int size, int ptr, int ext) {
		
		this.setSize(size);
		this.setDate();
		this.setPtr(ptr);
		this.setExt(ext);
	}

	public AttributeBlockEntry() {
		this.setSize(0);
		this.setDate();
		this.setPtr(-1);
		this.setExt(-1);
	}

	public AttributeBlockEntry(String string) {
		String str[] = string.split(",");
		if(str.length >= 4){
			this.setSize(Integer.parseInt(str[0]));
			this.date = str[1];
			this.setPtr(Integer.parseInt(str[2]));
			this.setExt(Integer.parseInt(str[3]));
		} else {
			this.setSize(0);
			this.setDate();
			this.setPtr(-1);
			this.setExt(-1);
		}
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		setDate();
		this.size = size;
	}

	public String getDate() {
		return date;
	}

	private void setDate() {
		this.date = (new Date()).toString();
	}

	public int getPtr() {
		return ptr;
	}

	public void setPtr(int ptr) {
		this.ptr = ptr;
	}

	public int getExt() {
		return ext;
	}

	public void setExt(int ext) {
		this.ext = ext;
	}

	public byte[] getBytes() {
		String text;
		
		text = ""+size+","+date+","+ptr+","+ext+',';
		
		return text.getBytes();
	}

}
