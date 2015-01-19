public class Header {

	public static int getSourcePort(byte[] header) {
		byte[] sourcePort = new byte[2];
		System.arraycopy(header, 0, sourcePort, 0, sourcePort.length);
		return Converter.convertByteArrayToInt(sourcePort);
	}

	public static int getDestinationPort(byte[] header) {
		byte[] destPort = new byte[2];
		System.arraycopy(header, 2, destPort, 0, destPort.length);
		return Converter.convertByteArrayToInt(destPort);
	}

	public static int getSequenceNumber(byte[] header) {
		byte[] seqNumb = new byte[4];
		System.arraycopy(header, 4, seqNumb, 0, seqNumb.length);
		return Converter.convertByteArrayToInt(seqNumb);
	}

	public static int getAckNumber(byte[] header) {
		byte[] destPort = new byte[4];
		System.arraycopy(header, 8, destPort, 0, destPort.length);
		return Converter.convertByteArrayToInt(destPort);
	}

	public static int[] getFlags(byte[] header) {
		byte[] flags = new byte[1];
		System.arraycopy(header, 13, flags, 0, flags.length);
		
		int[] flagsAsInts = new int[6];
		
		int flagInt = Converter.convertByteArrayToInt(flags);
		
		flagsAsInts[0] = flagInt/32;
		flagInt %= 32;
		flagsAsInts[1] = flagInt/16;
		flagInt %= 16;
		flagsAsInts[2] = flagInt/8;
		flagInt %= 8;
		flagsAsInts[3] = flagInt/4;
		flagInt %= 4;
		flagsAsInts[4] = flagInt/2;
		flagInt %= 2;
		flagsAsInts[5] = flagInt;
		
		return flagsAsInts;
	}

	public static byte[] getChecksum(byte[] header) {
		byte[] checkSum = new byte[2];
		System.arraycopy(header, 16, checkSum, 0, checkSum.length);
		return checkSum;
	}

	public static void setSourcePort(byte[] header, int srcPort) {

		byte[] sourcePort = Converter.convertIntToByteArray(srcPort, 2);
		System.arraycopy(sourcePort, 0, header, 0, sourcePort.length);

	}

	public static void setDestinationPort(byte[] header, int destPort) {

		byte[] destinationPort = Converter.convertIntToByteArray(destPort, 2);
		System.arraycopy(destinationPort, 0, header, 2, destinationPort.length);

	}

	public static void setSequenceNumber(byte[] header, int seqNum) {

		byte[] sequenceNumber = Converter.convertIntToByteArray(seqNum, 4);
		System.arraycopy(sequenceNumber, 0, header, 4, sequenceNumber.length);

	}

	public static void setAckNumber(byte[] header, int ackNum) {

		byte[] ackNumber = Converter.convertIntToByteArray(ackNum, 4);
		System.arraycopy(ackNumber, 0, header, 8, ackNumber.length);

	}

	public static void setOpts(byte[] header) {

		byte[] opts = new byte[1];
		System.arraycopy(opts, 0, header, 12, opts.length);

	}

	public static void setFlags(byte[] header, int[] flags) {

		int flagsAsInt = flags[5] + flags[4]*2 + flags[3]*4 + flags[2]*8 + flags[1]*16 + flags[0]*32;
		byte[] flagByte = Converter.convertIntToByteArray(flagsAsInt, 1);
		System.arraycopy(flagByte, 0, header, 13, flagByte.length);

	}

	public static void setWindowSize(byte[] header) {

		byte[] windowSize = new byte[2];
		System.arraycopy(windowSize, 0, header, 14, windowSize.length);

	}

	public static void setTCPChecksum(byte[] header, byte[] checksum) {

		System.arraycopy(checksum, 0, header, 16, checksum.length);

	}

	public static void setUrgPointer(byte[] header) {

		byte[] urgPointer = new byte[2];
		System.arraycopy(urgPointer, 0, header, 18, urgPointer.length);

	}
	
	public static byte[] createClosingHeader(boolean ack){
		
		byte[] header = new byte[20];
		int[] close = {0,0,0,0,0,1};
		
		if(ack)
			close[1] = 1;
		
		setFlags(header, close);
		
		return header;
	}

}
