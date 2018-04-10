package javaapi;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

import com.advantest.kei.KSort;
import com.advantest.kei.KTestSystem;
import com.advantest.kei.KDutGroupType;

public class XYSort {
	private static COLUMN[] titles;
	private static int[] posInRecord;	//0:W, 1:X, 2:Y, 3:L, 4:B
	private static Map<List<String>, Integer> records;
	private XYSort() {
		//empty
	}

	final public static void load(String fname) {
		if(records != null) {
			return;	//to ensure one time file loading
		}

		List<String> lines = exec(genCmd(fname));
		if(lines.size() == 0) {
			throw new RuntimeException("Empty file or less than 2 columns");
		}
		titles = toTitles(lines.get(0));
		//System.out.println(Arrays.asList(titles));
		checkTitles(titles);
		posInRecord = savePos(titles);

		records = saveRecord(titles, lines.listIterator(1));
		//print(records);
	}

	final public static void sort() throws NoMoreTargetDutException {
NXTD:
		for(int dut : KTestSystem.getDut(KDutGroupType.CDUT)) {
			for (int channel = DutInterface.setInitialChannel(); channel <= DeviceInfo.CHANNEL_COUNT; channel++, DutInterface.setActiveChannel(channel)) {  
				for (int chip = 1; chip <= DeviceInfo.MULTIDIECHIPS; chip++) {
					String[] wxyl = formatWXYL(dut, channel, chip);
					Integer bin = binOf(wxyl);
					if(bin != null) {
						setDutBin(dut, bin);
						continue NXTD;
					}
				}
			}
		}
	}

	final public static void sort(int dut, String[] wxyl) throws NoMoreTargetDutException {
		if(Arrays.binarySearch(KTestSystem.getDut(KDutGroupType.CDUT), dut) < 0) {
			return;
		}
		Integer bin = binOf(wxyl);
		if(bin != null) {
			setDutBin(dut, bin);
		}
	}

	private static void setDutBin(int dut, Integer bin) throws NoMoreTargetDutException {
		System.out.printf("DUT%02d goes to Bin%d\n", dut, bin);
		SS.dutInfo.addFailedTest(dut, SS.currentTestNum);
		SS.dutInfo.setSortNumber(dut, bin);
		KSort.write(dut, Bin.toSort(bin));
		DutExclusion.setPermanent(dut);
	}

	final private static String[] formatWXYL(int dut, int channel, int chip) {
		String[] wxyl = {
			String.format("%C%C", (char) SS.dutInfo.DUTList[dut - 1].lotInfo.waferNoUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[dut - 1].lotInfo.waferNoLower[channel - 1][chip - 1]),
			String.format("%C%C", (char) SS.dutInfo.DUTList[dut - 1].lotInfo.xLocUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[dut - 1].lotInfo.xLocLower[channel - 1][chip - 1]),
			String.format("%C%C", (char) SS.dutInfo.DUTList[dut - 1].lotInfo.yLocUpper[channel - 1][chip - 1], (char) SS.dutInfo.DUTList[dut - 1].lotInfo.yLocLower[channel - 1][chip - 1]),
			new String(SS.dutInfo.DUTList[dut - 1].lotInfo.lotId[channel - 1][chip - 1], 0, 9)
		};
		//System.out.println(Arrays.asList(wxyl));
		return wxyl;
	}

	private static List<String> key(String[] wxyl) {
		return trans(titles, posInRecord, wxyl);
	}

	private static Integer binOf(String[] wxyl) {
		List<String> _wxyl = key(wxyl);
		return records.get(_wxyl);
	}

	private static void print(Map<List<String>, Integer> map) {
		for(Map.Entry<List<String>, Integer> kv : records.entrySet()) {
			System.out.printf("%s : Bin%d\n", kv.getKey(), kv.getValue());
		}
	}

	private static Map<List<String>, Integer> saveRecord(COLUMN[] titles, ListIterator<String> lineItr) {
		Map<List<String>, Integer> records = new HashMap<List<String>, Integer>();
		while(lineItr.hasNext()) {
			String[] cols = lineItr.next().split("[ \t]+");
			if(titles.length != cols.length) {
				System.out.printf("%s has wrong # of cols\n", Arrays.toString(cols));
				continue;
			}

			int binPos = posInRecord[COLUMN.B.ordinal()];
			int bin = Integer.valueOf(cols[binPos]);
			checkBin(bin);

			cols[binPos] = "";
			records.put(Arrays.asList(cols), bin);
		}
		return records;
	}

	private static void checkBin(int bin) {
		if(2 <= bin && bin <= 7) {
			return;
		}
		throw new RuntimeException("Unsupported Bin" + String.valueOf(bin));
	}

	private static List<String> trans(COLUMN[] titles, int[] posInRecord, String[] wxyl) {
		String[] record = new String[titles.length];
		for(COLUMN fd : titles) {
			int ordinal = fd.ordinal();
			record[posInRecord[ordinal]] = COLUMN.B == fd ? "" : wxyl[ordinal];
		}
		return Arrays.asList(record);
	}

	private static int[] savePos(COLUMN[] titles) {
		int[] posInRecord = new int[COLUMN.values().length];
		Arrays.fill(posInRecord, -1);
		for(int i = 0; i < titles.length; i++) {
			posInRecord[titles[i].ordinal()] = i;
		}
		return posInRecord;
	}

	private static void checkTitles(COLUMN[] titles) {
		Set<COLUMN> sets = new HashSet<COLUMN>();
		for(COLUMN fd : titles) {
			sets.add(fd);
		}
		if(! sets.contains(COLUMN.B)) {
			throw new RuntimeException("Missing column " + COLUMN.B.name());
		}
		if(sets.size() != titles.length) {
			throw new RuntimeException("Detected duplicate columns");
		}
	}

	private static COLUMN[] toTitles(String line) {
		String[] cols = line.split("[ \t]+");
		COLUMN[] titles = new COLUMN[cols.length];
		for(int i = 0; i < cols.length; i++) {
			try {
				titles[i] = Enum.valueOf(COLUMN.class, cols[i]);
			} catch(IllegalArgumentException e) {
				throw new RuntimeException(String.format(
							"'%s' has unknown column '%s'", line, cols[i]));
			}
		}
		return titles;
	}

	private static List<String> exec(String cmd) {
		String[] shellCmd = { "/bin/sh", "-c", cmd };
		try {
			Process p = Runtime.getRuntime().exec(shellCmd);
			print(toLines(p.getErrorStream()));
			return toLines(p.getInputStream());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String genCmd(String fname) {
		String cmdReadFile = String.format("cat %s", fname);
		String cmdFilterLinesWithFieldCount = "awk '2 <= NF && NF <= 5'";
		String cmdRemoveBlankLines = "awk '{$1=$1};1'";

		StringBuilder cmdL = new StringBuilder();
		cmdL.append(cmdReadFile); 
		cmdL.append(" | "); cmdL.append(cmdRemoveBlankLines);
		cmdL.append(" | "); cmdL.append(cmdFilterLinesWithFieldCount);
		return cmdL.toString();
	}

	private static List<String> toLines(InputStream strm) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader( strm));
		List<String> lines = new ArrayList<String>();
		String oneLine = "";
		while ((oneLine = br.readLine()) != null) {
			lines.add(oneLine);
		}
		return lines;
	}

	private static void print(List<String> lst) {
		if(lst.size() == 0) return;
		for(String s : lst) {
			System.out.println(s);
		}
	}

	enum COLUMN {
		W,	//0
		X,	//1
		Y,	//2
		L,	//3
		B,	//4
	};
}


