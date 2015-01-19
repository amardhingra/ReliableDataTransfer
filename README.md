######################################################################
Name : Amar Dhingra
README
######################################################################

Compile Instructions: 
% make

Run instructions: 

First start Receiver
% receiver <filename> <listening_port> <sender_IP> 
		   <sender_port> <log_filename>

Then start Sender
% sender <filename> <remote_IP> <remote_port> 
		 <ack_port_num> <log_filename> <window_size>
		 
Sender: 
The sender breaks the file given by filename, breaks it into packets of
size 576 bytes (including a 20 byte header) and uses a stop and go process
to send the file to the reciever. It sets the sequence number to the number
of the first byte of the packet and the ack number to the last number of the
packet. After sending every packet and receiving acks for every packet it sends
a packet with the FIN bit set to 1, waits for an ACK with FIN set to 1 and
terminates.

Receiver:
The receiver waits for packets, checks that the packet was uncorrupted and 
newer than the last received packet and sends an ack containing the number of
the next expected packet. When it receives a packet with the FIN bit set to 1
it sends an ack with FIN and ACK set to 1 and terminates.

TIMEOUT:
The timeout is initially set at 1s and doubles every time a packet times out.
A timer is started every time a packet is transmitted and when the ack is 
received the estimated RTT and deviation in RTT are calculated. The next
timeout is then set at ESTIMATED_RTT + 4 * DEV_RTT

Sercurity:
Security provides methods for calculating and verifying checksums of a packet.
It calculates the checksum over a pseudo packet containing all the data
and header with the checksum field set to 0. The checksum sums every pair of 
bytes with wrap around and takes the ones complement of the total sum. To 
verify the packet is uncorrupted it sums the entire packet without the checksum
and adds the checksum. If the packet is uncorrupted the value is 16 1's

Header:
Header provides methods for setting different parts of the header

LogData:
LogData provides a class to hold the log data for every packet that is received
Both sender and receiver maintain an arraylist of LogData which they sort by time
and print to <log_filename> at the end. Each packet in the <log_filename> is 
separated by a new line character.

FileIO and Converter:
These are helper classes which provide methods for reading and writing to files
and converting ints to byte arrays and byte arrays to ints
