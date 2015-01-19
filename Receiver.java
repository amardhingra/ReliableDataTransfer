import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

public class Receiver {

	// class variables
	String filename;
	int listeningPort;
	String senderIP;
	int senderPort;
	String logFilename;
	int localPort;

	public Receiver(String filename, int listeningPort, String senderIP,
			int senderPort, String logFilename) {

		this.filename = filename;
		this.listeningPort = listeningPort;
		this.senderIP = senderIP;
		this.senderPort = senderPort;
		this.logFilename = logFilename;

	}

	public void startListening() throws IOException {

		// create sockets for input and output
		DatagramSocket incomingSocket = new DatagramSocket(listeningPort);
		DatagramSocket outgoingSocket = new DatagramSocket();
		localPort = outgoingSocket.getLocalPort();

		// create a printwriter for writing the file being received
		PrintWriter writer = new PrintWriter(filename);

		int lastSequenceNumber = -1;

		ArrayList<LogData> logData = new ArrayList<LogData>();

		for (;;) {

			// create a datagram packet to accept incoming packets
			byte[] buff = new byte[Sender.PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buff, buff.length);

			// receive incoming packets
			incomingSocket.receive(packet);
			// get the data from the packet and take the header out
			byte[] packetData = packet.getData();
			byte[] header = getHeader(packetData);

			// add a new data log
			logData.add(new LogData(System.currentTimeMillis(), Header
					.getSourcePort(header), Header
					.getDestinationPort(header), Header
					.getSequenceNumber(header),
					Header.getAckNumber(header), Header.getFlags(header)));
			
			// check if the packet is valid
			if (!isClosingConnection(header)) {

				if (Security.verifyChecksum(packetData)) {
				
					// verify that the data is in order
					byte[] data = getData(packetData);
					if (Header.getSequenceNumber(header) > lastSequenceNumber) {
						
						writer.print(new String(removeZeros(data)));
						writer.flush();
						lastSequenceNumber = Header.getSequenceNumber(header);
						
						// create and send an ack to the sender
						DatagramPacket ack = createAck(header, false);
						outgoingSocket.send(ack);
						
						logData.add(new LogData(System.currentTimeMillis(), Header
								.getSourcePort(ack.getData()), Header
								.getDestinationPort(ack.getData()), Header
								.getSequenceNumber(ack.getData()),
								Header.getAckNumber(ack.getData()), Header.getFlags(ack.getData())));
						
					} else {
						
						// change the ack number being sent, create and send an ack to the sender
						Header.setSequenceNumber(header, lastSequenceNumber);
						DatagramPacket ack = createAck(header, false);
						outgoingSocket.send(ack);
						
						logData.add(new LogData(System.currentTimeMillis(), Header
								.getSourcePort(ack.getData()), Header
								.getDestinationPort(ack.getData()), Header
								.getSequenceNumber(ack.getData()),
								Header.getAckNumber(ack.getData()), Header.getFlags(ack.getData())));
						
					}

				} 


			} else {
				// if the sender wanted to close the connection
				// create a closing header and send the packet
				DatagramPacket ack = createAck(header, true);
				outgoingSocket.send(ack);


				logData.add(new LogData(System.currentTimeMillis(), Header
						.getSourcePort(ack.getData()), Header
						.getDestinationPort(ack.getData()), Header
						.getSequenceNumber(ack.getData()),
						Header.getAckNumber(ack.getData()), Header.getFlags(ack.getData())));
				
				
				break;
			}

		}

		// close the resources
		writer.close();
		incomingSocket.close();
		outgoingSocket.close();

		// write the log output to an output file or the stdout
		String data = "";
		Collections.sort(logData);
		for (LogData ld : logData)
			data += ld.toString();
		FileIO.writeToFile(logFilename, data);

		System.out.println("Delivery completed successfully");

	}

	public DatagramPacket createAck(byte[] header, boolean closeConnection)
			throws UnknownHostException {

		// create a byte array for the ack
		byte[] ackBytes = new byte[20];

		// set the fields for the ack
		Header.setSourcePort(ackBytes, localPort);
		Header.setDestinationPort(ackBytes, senderPort);

		// set the fin bit if required
		if (closeConnection){
			Header.setAckNumber(ackBytes, 0);
			Header.setFlags(ackBytes, new int[] { 0, 1, 0, 0, 0, 1 });
		}
		else{
			Header.setAckNumber(ackBytes, Header.getAckNumber(header) + 1);
			Header.setFlags(ackBytes, new int[] { 0, 1, 0, 0, 0, 0 });
		}

		return new DatagramPacket(ackBytes, ackBytes.length,
				InetAddress.getByName(senderIP), senderPort);
	}

	public byte[] getHeader(byte[] packetData) {

		// returns the first 20 bytes of the
		byte[] header = new byte[20];
		System.arraycopy(packetData, 0, header, 0, header.length);
		return header;
	}

	public byte[] getData(byte[] packetData) {

		// returns the file data from the packet
		byte[] data = new byte[556];
		System.arraycopy(packetData, 20, data, 0, data.length);
		return data;
	}

	public boolean isClosingConnection(byte[] header) {

		// checks if the fin bit is set
		int[] flags = Header.getFlags(header);
		if (flags[5] == 1)
			return true;

		return false;
	}
	
	private byte[] removeZeros(byte[] data){
		
		int count = 0;
		for (int i = data.length - 1; i >= 0; i--)
			if (data[i] != 0)
				break;
			else
				count++;
		
		byte[] dataNoZeros = new byte[data.length - count];
		
		System.arraycopy(data, 0, dataNoZeros, 0, dataNoZeros.length);
		
		return dataNoZeros;
	}

	public static void main(String[] args) {

		if (args.length < 5){
			System.out.println("Incorrect usage");
			System.out.println("Usage: receiver <filename> <listening_port> " + 
			"<sender_IP> <sender_port> <log_filename>");
			System.exit(1);
		}
		
		// parsing the command line arguments
		String filename = args[0];
		int listeningPort = Integer.parseInt(args[1]);
		String senderIP = args[2];
		int senderPort = Integer.parseInt(args[3]);
		String logFilename = args[4];

		// create a receiver and start listening for the file
		Receiver r = new Receiver(filename, listeningPort, senderIP,
				senderPort, logFilename);
		try {
			r.startListening();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
