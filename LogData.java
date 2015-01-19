import java.util.Date;

public class LogData implements Comparable<LogData> {

	public long time;
	public int source;
	public int destination;
	public int sequenceNumber;
	public int ackNumber;
	public int[] flags;

	public LogData(long time, int source, int destination, int sequenceNumber,
			int ackNumber, int[] flags) {
		this.time = time;
		this.source = source;
		this.destination = destination;
		this.sequenceNumber = sequenceNumber;
		this.ackNumber = ackNumber;
		this.flags = flags;
	}

	@Override
	public int compareTo(LogData other) {

		return (int) (this.time - other.time);

	}

	public String toString() {

		return "TIME: " + new Date(time) + "\nSOURCE PORT NUMBER: " + source
				+ "\nDESTINATION PORT NUMBER: " + destination
				+ "\nSEQUENCE NUMBER: " + sequenceNumber + "\nACK NUMBER: "
				+ ackNumber + "\nURG: " + flags[0] + "\nACK: " + flags[1]
				+ "\nPSH: " + flags[2] + "\nRST: " + flags[3] + "\nSYN: "
				+ flags[4] + "\nFIN: " + flags[5] + "\n\n";

	}

}
