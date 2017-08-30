package greenassignment4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.StringTokenizer;

public class main {
	
	private static FileNameTableEntry fnt[];
	private static DataBlock data[];
	private static AttributeBlockEntry abpt[];
	private static Boolean blockmap[];
	private static Boolean abptmap[];
	private static int numfnt;
	private static int numabpt;
	private static int numdata;

	public static void main(String[] args) {
	
		StringTokenizer token;
		String cmd, arg = null, arg2 = null;
		Scanner scanner = new Scanner(System.in);
		
		while(true){
			System.out.print("Type Help for a list of avaliable commands or Quit to exit\nCommand: ");
			cmd = null;
			arg = null;
			token = new StringTokenizer(scanner.nextLine());
			cmd = token.nextToken();
			if(token.hasMoreTokens()){
				arg = token.nextToken();
				if(token.hasMoreTokens()){
					arg2 = token.nextToken();
				}
			}
			if(cmd.equalsIgnoreCase("Createfs")){
				createFS(arg);
			}
			else if(cmd.equalsIgnoreCase("Formatfs")){
				formatFS(arg, arg2);
			}
			else if(cmd.equalsIgnoreCase("Savefs")){
				saveFS(arg);
			}
			else if(cmd.equalsIgnoreCase("Openfs")){
				openFS(arg);
			}
			else if(cmd.equalsIgnoreCase("List")){
				list();
			}
			else if(cmd.equalsIgnoreCase("Put")){
				put(arg);
			}
			else if(cmd.equalsIgnoreCase("Get")){
				get(arg);
			}
			else if(cmd.equalsIgnoreCase("Remove")){
				remove(arg);
			}
			else if(cmd.equalsIgnoreCase("Help")){
				helpMenu();
			}
			else if(cmd.equalsIgnoreCase("Quit")){
				break;
			}
			else {
				System.out.println("invalid entry");
			}
		}
		scanner.close();
		System.out.println("Exiting");
	}

	private static void remove(String arg) {
		if(arg == null){System.out.println("invalid argument");return;}
		// check if file exists
		if(exists(arg) != -1){
			System.out.println("removed "+arg);
			// replace with blank entry
			fnt[exists(arg)] = new FileNameTableEntry();
			// rebuild bitmaps
			buildBitmaps();
		} else {
			System.out.println(arg+" not found");
		}
	}

	private static int exists(String arg) {
		String name;
		// check if file is in the name table
		for(int i=0; i<numfnt; i++){
			name = new String(fnt[i].getName()).replaceAll("\0", "");
			if(arg.equals(name)){
				return i;
			}
		}
		return -1;
	}

	private static void get(String arg) {
		if(arg == null){System.out.println("invalid argument");return;}
		// check if file exists
		if(exists(arg) != -1){
			FileOutputStream fos = null;
			AttributeBlockEntry entry;
			try {
				fos = new FileOutputStream(arg);
			} catch (FileNotFoundException e1) {
				System.out.println("file not found");return;
			}
			
			try { // build file from blocks
				entry = abpt[fnt[exists(arg)].getPtr()];
				while(entry.getExt() != -1) {
					fos.write(data[entry.getPtr()].getBlock());
					entry = abpt[entry.getExt()];
				} 
				fos.write(data[entry.getPtr()].getBlock());
				fos.write(structureToBytes());
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("file not found");
		}
	}

	private static void put(String arg) {
		int numblocks=0;
		if(arg == null){System.out.println("invalid argument");return;}
		if(fnt == null){System.out.println("no current file system");return;}
		byte[] array = null, block;
		char name[];
		int i, abptindex, dataindex, fntindex;
		
		try { // read in file
			array = Files.readAllBytes(new File(arg).toPath());
		} catch (IOException e) {
			System.out.println("file not found\n");
			return;
		} 
		// Check if file exists
		if(exists(arg) > -1){
			remove(arg);
		}
		// check for free space
		if(isThereRoom(array.length)){
			name = new char[56];
			// build FNT entry
			for(i=0; i<56; i++){
				if(i < arg.length()){
					name[i] = arg.charAt(i);
				} else{
					break;
				}
			}
			fntindex = getfreeFNT();
			fnt[fntindex].setName(name);
			fnt[fntindex].setPtr(getFreeabpt());
			numblocks = (int) Math.ceil(array.length/256.0);
			// build Blocks
			for(i=0; i<numblocks; i++){
				block = new byte[256];
				for(int x=i*256; x<i*256+256; x++){
					if(x < array.length){
						block[x-(i*256)] = array[x];
					} else {
						break;
					}
				}
				dataindex = getFreedata();
				abptindex = getFreeabpt();
				blockmap[dataindex] = true;
				abptmap[abptindex] = true;
				data[dataindex].setBlock(block);
				abpt[abptindex].setPtr(dataindex);
				abpt[abptindex].setSize(array.length);
				if(i < numblocks-1) {				
					abpt[abptindex].setExt(getFreeabpt());
				} else {
					abpt[abptindex].setExt(-1);
				}
			}
		} else {
			System.out.println("not enough free space\n");
		}
	}

	private static int getfreeFNT() {
		// return index of next free FNT entry
		for(int i=0; i<fnt.length; i++) {
			if(fnt[i].getName()[0] == '+'){
				return i;
			}
		}
		return 0;
	}

	private static int getFreeabpt() {
		// return index of next free ABPT entry
		for(int i=0; i<abptmap.length; i++) {
			if(abptmap[i] == false){
				return i;
			}
		}
		return 0;
	}

	private static int getFreedata() {
		// return index of next free data block
		for(int i=(int) Math.ceil(numabpt/2.0); i<blockmap.length; i++) {
			if(blockmap[i] == false){
				return i;
			}
		}
		return 0;
	}

	private static boolean isThereRoom(int length) {
		int numblocks;
		// check for room in fnt abpt and data
		if(freeFNT() == false){
			return false;
		}
		numblocks = (int) Math.ceil(length/256.0);
		if(freeABPT() < numblocks) {
			return false;
		}
		else if(freedata() < numblocks){
			return false;
		}
		else{
			return true;
		}
	}

	private static boolean freeFNT() {
		// check for a free fnt space
		for(int i=0; i<fnt.length; i++) {
			if(fnt[i].getName()[0] == '+'){
				return true;
			}
		}
		return false;
	}

	private static int freedata() {
		// return # free data blocks
		int count = 0;
		for(int i=(int) Math.ceil(numabpt/2.0); i<data.length; i++) {
			if(blockmap[i] == false){
				count++;
			}
		}
		return count;
	}

	private static int freeABPT() {
		// return # free abpt entries
		int count = 0;
		for(int i=0; i<abpt.length; i++) {
			if(abptmap[i] == false){
				count++;
			}
		}
		return count;
	}

	private static void list() {
		String name;
		if(fnt == null){System.out.println("no current file system");return;}
		for(int i=0; i<numfnt; i++) {
			// check if a real entry
			if(fnt[i].getName()[0] != '+'){
				name = "";
				for(int x=0; x<56; x++) {
					if(fnt[i].getName()[x] != 0){
						name += fnt[i].getName()[x];
					}
				}
				// print formated string
				name =  String.format("%-10s %5d Bytes Created on %s", 
						name,
						abpt[fnt[i].getPtr()].getSize(),
						abpt[fnt[i].getPtr()].getDate());
				System.out.println(name);
			}
		}
	}

	private static void openFS(String arg) {

		if(arg == null){System.out.println("invalid argument");return;}
		int count = 0;
		FileInputStream fstream = null;
		String strLine, str = "";
		try {
			fstream = new FileInputStream(arg);
		} catch (FileNotFoundException e) {
			System.out.println("file not found");return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		try {
			while ((strLine = br.readLine()) != null){
				// file system structure
				if (count == 0) {
					buildStructure(strLine);
				}
				// FNT
				else if (count == 1) {
					buildFNT(strLine);
				}
				// Data
				else {
					str += (strLine+"\n");
				}
				count++;
			}
			buildData(str);
			buildBitmaps();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Close the input stream
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void buildBitmaps() {
		AttributeBlockEntry abptentry;
		// initialize bitmaps
		for(int i=0; i<numabpt; i++) {
			abptmap[i] = false;
		}
		for(int i=0; i<numdata; i++) {
			blockmap[i] = false;
		}
		// Traverse fnt entries and follow links
		for(int i=0; i<fnt.length; i++) {
			if(fnt[i].getName()[0] != '+'){
				abptmap[fnt[i].getPtr()] = true;
				abptentry = abpt[fnt[i].getPtr()];
				if(abptentry.getPtr() != -1){
					blockmap[abptentry.getPtr()] = true;
				}
				while(abptentry.getExt() != -1){
					abptmap[abptentry.getExt()] = true;
					abptentry = abpt[abptentry.getExt()];					
				}		
			}
		}
	}

	private static void buildData(String str) {
		String text;
		for(int i=0; i<numabpt; i++){
			text = "";
			for(int x=0; x<128; x++){
				text += str.charAt(i*128+x);
			}
			abpt[i] = new AttributeBlockEntry(text);
		}
		for(int i=0; i<numdata; i++){
			text = "";
			for(int x=0; x<256; x++){
				text += str.charAt(i*256+x);
			}
			data[i] = new DataBlock(text);
		}
	}

	private static void buildFNT(String str) {
		char name[];
		String entries[] = str.split("_");
		String entry[] = null;
		String entname;
		for(int i=0; i<numfnt; i++){
			name = new char[56];
			entry = entries[i].split(",");
			entname = entry[0];
			for(int j=0; j<entname.length(); j++){
				name[j] = entname.charAt(j);
			}
			fnt[i].setName(name);
			fnt[i].setPtr(Integer.valueOf(entry[1]));
		}
	}

	private static void buildStructure(String str){
		String[] tokens;
		tokens = str.split(",");
		createFS(tokens[2]);
		formatFS(tokens[0], tokens[1]);
	}

	private static void saveFS(String arg) {
		int i;
		if(arg == null){
			System.out.println("invalid argument");
			return;
		}
		if(fnt == null){System.out.println("no current file system");return;}
		saveabptEntries();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(arg);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try { // write data to file
			fos.write(structureToBytes());
			fos.write(fntToBytes());
			fos.write("\n".getBytes());
			for(i=0; i<data.length-1; i++){
				fos.write(data[i].getBlock());
			}
			fos.write(data[i].getBlock());
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void saveabptEntries() {
		// save abpt in mem to data blocks
		byte bytes[] = null, databytes[];
		for(int i=0; i<numabpt; i++){
			bytes = abpt[i].getBytes();
			databytes = data[i/2].getBlock();
			if(i%2 == 0){
				for(int x=0; x<128; x++){
					if(x < bytes.length){
						databytes[x] = bytes[x];
					} else {
						databytes[x] = (byte) 0;
					}
				}
			} else {
				for(int x=0; x<128; x++){
					if(x < bytes.length){
						databytes[x+128] = bytes[x];
					} else {
						databytes[x+128] = (byte) 0;
					}
				}
			}
			data[i/2].setBlock(databytes);
		}
	}

	private static byte[] structureToBytes() {
		String text;
		text = numfnt+","+numabpt+","+numdata+"\n";
		return text.getBytes();
	}

	private static byte[] fntToBytes() {
		String text = "";
		for(int i=0; i<numfnt; i++) {
			text += (new String(fnt[i].getName()).replaceAll("\0", ""))+
					","+fnt[i].getPtr()+"_";
		}
		return text.getBytes();
	}

	private static void formatFS(String arg, String arg2) {
		if(arg == null){System.out.println("invalid argument 1");return;}
		if(arg2 == null){System.out.println("invalid argument 2");return;}
		if(data == null){System.out.println("no file system to format");return;}
		numfnt = Integer.parseInt(arg);
		numabpt = Integer.parseInt(arg2);
		if (data == null || numabpt >= numdata || numabpt < 1) {
			System.out.println("invalid number of abpt entries.");
		}
		else {
			blockmap = new Boolean[data.length];
			abptmap = new Boolean[numabpt];
			fnt = new FileNameTableEntry[numfnt];
			abpt = new AttributeBlockEntry[numabpt];
			for(int i=0; i<numfnt; i++) {
				fnt[i] = new FileNameTableEntry();
			}
			for(int i=0; i<data.length; i++) {
				blockmap[i] = new Boolean(false);
			}
			for(int i=0; i<numabpt; i++) {
				abpt[i] = new AttributeBlockEntry();
				abptmap[i] = new Boolean(false);
				blockmap[(int) Math.floor(i/2.0)] = true;
			}
		}
	}

	private static void createFS(String arg) {
		if(arg == null){System.out.println("invalid argument");return;}
		int size = Integer.parseInt(arg);
		data = new DataBlock[size];
		for(int i=0; i<size; i++) {
			data[i] = new DataBlock();
		}
		numdata = size;
	}

	private static void helpMenu() {
		// TODO Auto-generated method stub
		String text = "Welcome to the 3320 file system\n\n"
				+ "Avaliable commands:\n"
				+ "Createfs #ofblocks  	- creates a file system\n"
				+ "Formatfs #filenames #ABPTenties"
				+ "Savefs name      	- saves the current file system under specified name, takes file system name as an argument\n"
				+ "Openfs name      	- opens the specified file system, takes file system name as an argument\n"
				+ "List             	- list contents of current file system, takes no arguments\n"
				+ "Remove Name      	- removes specified file from the current file system, takes file name\n"
				+ "Put Externalfile 	- adds a file to the file system, takes a file name\n"
				+ "Get Externalfile 	- adds a file to the operating system, takes afile name\n"
				+ "Quit             	- exits the file system\n\n";
		System.out.println(text);
	}
	
}
