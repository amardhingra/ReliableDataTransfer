
public class Security {

	public static byte[] calculateChecksum(byte[] data) {

		long sum = calculateSum(data);

		// flip all the bits and keep the last 16
		sum = ~sum;
		sum = sum & 0xFFFF;

		return Converter.convertIntToByteArray((int) sum, 2);
	}

	private static long calculateSum(byte[] data) {

		// sum for total packet and each pair of bytes
		long sum = 0;
		long miniSum = 0;

		// counters for number of bytes
		int bytesLeft = data.length;
		int i = 0;

		while (bytesLeft > 1) {

			// add the sum of every two bytes to the total sum
			miniSum = (((data[i] << 8) & 0xFF00) | ((data[i + 1]) & 0x00FF));
			sum += miniSum;

			// deal with overflow
			if ((sum & 0xFFFFFFFF0000L) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}

			// update counters
			i += 2;
			bytesLeft -= 2;
		}

		// check for an odd number of bytes
		if (bytesLeft > 0) {

			// add 8 zero bits to the end
			miniSum = (data[i] << 8 & 0xFF00);
			sum += miniSum;

			// deal with overflow
			if ((sum & 0xFFFFFFFF0000L) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}
		
		return sum;

	}

	public static boolean verifyChecksum(byte[] packet) {
		
		// get the shecksum
		byte[] checksum = Header.getChecksum(packet);
		
		// overwrite the checksum with zeros
		Header.setTCPChecksum(packet, new byte[2]);
		
		// calculate the sum
		byte[] calculatedChecksum = Converter.convertIntToByteArray((int) calculateSum(packet), 2);

		// check ones complement
		if (calculatedChecksum[0] + checksum[0] == -1
				&& calculatedChecksum[1] + checksum[1] == -1)
			return true;

		return false;

	}

}
