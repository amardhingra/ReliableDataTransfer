public class Converter {

	public static byte[] convertIntToByteArray(int number, int numberOfBytes) {

		byte[] value = new byte[numberOfBytes];

		for (int i = 0; i < numberOfBytes; i++)
		 value[i] = (byte) (number >> ((numberOfBytes - 1 - i) * 8));

		return value;
	}

	public static int convertByteArrayToInt(byte[] array) {

		int value = 0;
	    for (int i = 0; i < array.length; i++) {
	        value += (array[i] & 255) << (array.length - 1 - i) * 8;
	    }
	    return value;

	}
}
