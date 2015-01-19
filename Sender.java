import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

public class Sender {

	// variables for packet size
	static final int PACKET_SIZE = 576;
	static final int HEADER_SIZE = 20;
	static final int DATA_SIZE = PACKET_SIZE - HEADER_SIZE;

	// timeout variables
	int INITIAL_TIME_OUT = 1000;
	int TIME_OUT = INITIAL_TIME_OUT;
	long ESTIMATED_RTT = INITIAL_TIME_OUT;

	int DEV_RTT = 0;

	// class variables
	String filename;
	String remoteIP;
	int remotePort;
	int ackPortNum;
	String logFilename;
	int windowSize;
	int localPort;

	public Sender(String filename, String remoteIP, int remotePort,
			int ackPortNum, String logFilename, int windowSize) {

		this.filename = filename;
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.ackPortNum = ackPortNum;
		this.logFilename = logFilename;
		this.windowSize = windowSize;

	}

	public void sendFile() throws IOException {

		// setup counters for reporting at end
		int numberOfBytes = 0;
		int numberOfSegments = 0;
		int numberOfTransmissions = 0;

		// create a socket for sending datagrams and get the port number
		DatagramSocket socket = new DatagramSocket();
		localPort = socket.getLocalPort();

		// create a socket for receiving acks and bing to the given port number
		DatagramSocket ackSocket = new DatagramSocket(ackPortNum);

		// read all the data from the file into one string
		// and exit if the file isn't found
		byte[] fileBytes = null;
		try {
			fileBytes = FileIO.readFromFile(filename);
		} catch (FileNotFoundException e) {
			System.out.println("File not found. Exiting now");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// convert the file into a byte array and save the length of the array
		numberOfBytes = fileBytes.length;

		// split the file into individual packets
		DatagramPacket[] packets = splitFileIntoPackets(fileBytes);
		numberOfSegments = packets.length;

		// create an arraylist for log data
		ArrayList<LogData> logData = new ArrayList<LogData>();

		// send the packets using a stop and go protocol
		int currentPacketNumber = 0;
		while (currentPacketNumber < packets.length) {

			// keep resending the same packet until an ACK is received
			boolean sentPacket = false;
			while (!sentPacket) {
				try {

					long startTime = System.currentTimeMillis();

					// send the packet and increment number of transmissions
					socket.send(packets[currentPacketNumber]);
					numberOfTransmissions++;

					logData.add(new LogData(System.currentTimeMillis(), Header
							.getSourcePort(packets[currentPacketNumber]
									.getData()), Header
							.getDestinationPort(packets[currentPacketNumber]
									.getData()), Header
							.getSequenceNumber(packets[currentPacketNumber]
									.getData()), Header
							.getAckNumber(packets[currentPacketNumber]
									.getData()), Header
							.getFlags(packets[currentPacketNumber].getData())));

					// create a datagram for the ack
					byte[] ackBuf = new byte[HEADER_SIZE];
					DatagramPacket ack = new DatagramPacket(ackBuf,
							ackBuf.length);

					// set the timeout based on past packets and wait for ack
					ackSocket.setSoTimeout(TIME_OUT);
					ackSocket.receive(ack);

					// add a new data log
					byte[] ackData = ack.getData();
					logData.add(new LogData(System.currentTimeMillis(), Header
							.getSourcePort(ackData), Header
							.getDestinationPort(ackData), Header
							.getSequenceNumber(ackData), Header
							.getAckNumber(ackData), Header.getFlags(ackData)));

					// check that the packet received was the correct ack
					sentPacket = verifyAck(ack, currentPacketNumber, false);
					recalculateRTT(System.currentTimeMillis() - startTime);
					TIME_OUT = (int) (ESTIMATED_RTT + 4 * DEV_RTT);
				} catch (SocketTimeoutException e) {
					// on timeout double the timeout
					TIME_OUT *= 2;
				}
			}

			currentPacketNumber++;
		}

		// create a closing header
		byte[] closingHeader = Header.createClosingHeader(false);
		DatagramPacket closingPacket = new DatagramPacket(closingHeader,
				closingHeader.length, InetAddress.getByName(remoteIP),
				remotePort);

		// keep sending the closing packet until an ack is received
		boolean sentClosingPacket = false;
		while (!sentClosingPacket) {
			try {

				// send the packet
				socket.send(closingPacket);

				logData.add(new LogData(System.currentTimeMillis(), Header
						.getSourcePort(closingPacket.getData()), Header
						.getDestinationPort(closingPacket.getData()), Header
						.getSequenceNumber(closingPacket.getData()), Header
						.getAckNumber(closingPacket.getData()), Header
						.getFlags(closingPacket.getData())));

				// create a datagram for the ack
				byte[] ackBuf = new byte[HEADER_SIZE];
				DatagramPacket ack = new DatagramPacket(ackBuf, ackBuf.length);

				// set timeout and wait for for a packet
				ackSocket.setSoTimeout(TIME_OUT);
				ackSocket.receive(ack);

				// add a new data log
				byte[] ackData = ack.getData();
				int[] flags = Header.getFlags(ackData);

				logData.add(new LogData(System.currentTimeMillis(), Header
						.getSourcePort(ackData), Header
						.getDestinationPort(ackData), Header
						.getSequenceNumber(ackData), Header
						.getAckNumber(ackData), Header.getFlags(ackData)));

				// check ack was received for closing packet
				sentClosingPacket = flags[1] == 1 && flags[5] == 1;
			} catch (SocketTimeoutException e) {
				// if timeout event occurs double timeout
				TIME_OUT *= 2;
			}
		}

		// close resources
		ackSocket.close();
		socket.close();

		// write the log output to an output file or the stdout
		String data = "";
		Collections.sort(logData);
		for (LogData ld : logData)
			data += ld.toString();
		data += "ESTIMATED RTT: " + ESTIMATED_RTT / 1000.00 + "s";
		FileIO.writeToFile(logFilename, data);

		// print statistics to console
		System.out.println("Delivery completed successfully");
		System.out.println("Total bytes sent = " + numberOfBytes);
		System.out.println("Segments sent = " + numberOfSegments);
		System.out.println("Segments retransmitted = "
				+ (numberOfTransmissions - numberOfSegments));
	}

	public void recalculateRTT(long newRTT) {
		ESTIMATED_RTT = (long) (0.875 * ESTIMATED_RTT + 0.125 * newRTT);
		DEV_RTT = (int) (0.75 * DEV_RTT + 0.25 * (Math.abs(ESTIMATED_RTT
				- newRTT)));

	}

	public boolean verifyAck(DatagramPacket ack, int currentPacketNumber,
			boolean verifyClosing) {

		// get the data from the ack and copy the ack number to a new array
		byte[] ackData = ack.getData();
		byte[] ackBytes = new byte[4];
		System.arraycopy(ackData, 8, ackBytes, 0, ackBytes.length);

		// get the flags from the header
		int[] flags = Header.getFlags(ackData);

		if (verifyClosing && flags[1] == 0 && flags[5] == 0)
			return true;

		// check if the packet was an ack and if it is the right in order ack
		if (flags[1] == 1
				&& Converter.convertByteArrayToInt(ackBytes) > currentPacketNumber
						* DATA_SIZE) {
			// check if the ack was a closing ack if required
			return true;
		}

		return false;
	}

	public DatagramPacket[] splitFileIntoPackets(byte[] fileBytes)
			throws UnknownHostException {

		// calculates the number of packets required to transmit the file
		// and create an array of that size
		DatagramPacket[] packets = new DatagramPacket[fileBytes.length
				/ DATA_SIZE + 1];

		// create an array of datagram packets to be sent
		for (int i = 0; i < packets.length; i++) {
			packets[i] = createDatagramPacket(fileBytes, i);
		}

		return packets;

	}

	public DatagramPacket createDatagramPacket(byte[] fileBytes, int packetNum)
			throws UnknownHostException {

		// create a byte array large enough for the packet
		byte[] packetData = new byte[Math.min(DATA_SIZE, fileBytes.length
				- packetNum * DATA_SIZE)];

		// copy the correct section of the file into the byte array
		System.arraycopy(fileBytes, packetNum * DATA_SIZE, packetData, 0,
				Math.min(DATA_SIZE, fileBytes.length - packetNum * DATA_SIZE));

		// create a byte array for the header
		byte[] header = new byte[HEADER_SIZE];

		// set the fields in the header
		Header.setSourcePort(header, localPort);
		Header.setDestinationPort(header, remotePort);
		Header.setSequenceNumber(header, packetNum * DATA_SIZE);
		Header.setAckNumber(header, packetNum * DATA_SIZE + packetData.length);
		Header.setOpts(header);
		Header.setFlags(header, new int[] { 0, 0, 0, 0, 0, 0 });
		Header.setWindowSize(header);
		Header.setTCPChecksum(header, new byte[2]);
		Header.setUrgPointer(header);

		// create a byte array for the whole packet
		byte[] packet = new byte[Math.min(PACKET_SIZE, HEADER_SIZE
				+ packetData.length)];

		// copy the header and packet data into the packet
		System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
		System.arraycopy(packetData, 0, packet, HEADER_SIZE, packetData.length);

		Header.setTCPChecksum(header, Security.calculateChecksum(packet));

		// copy the header and packet data into the packet
		System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
		System.arraycopy(packetData, 0, packet, HEADER_SIZE, packetData.length);

		return new DatagramPacket(packet, packet.length,
				InetAddress.getByName(remoteIP), remotePort);
	}

	public static void main(String[] args) {

		if (args.length < 5) {
			System.out.println("Incorrect usage");
			System.out
					.println("Usage: sender <filename> <remote_IP> <remote_port> "
							+ "<ack_port_num> <log_filename> <window_size>");
			System.exit(1);
		}

		// parse the command line arguments
		String filename = args[0];
		String remoteIP = args[1];
		int remotePort = Integer.parseInt(args[2]);
		int ackPortNum = Integer.parseInt(args[3]);
		String logFilename = args[4];
		int windowSize = 1;
		if (args.length == 6)
			windowSize = Integer.parseInt(args[5]);

		// create a new sender and send the file
		Sender s = new Sender(filename, remoteIP, remotePort, ackPortNum,
				logFilename, windowSize);
		try {
			s.sendFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
