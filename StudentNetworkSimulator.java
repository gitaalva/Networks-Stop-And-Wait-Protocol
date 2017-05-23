import java.util.*;
import java.io.*;
import java.security.MessageDigest;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

	/*
	 * @aalva Cases To be handled;
	 * 		What happens when a timer TimesOut
	 * 			Guess it will be handled: We will check with test cases;
	 */
    public static final int FirstSeqNo = 0;
    int sequenceNumber; 						// variable that stores sequence number for A;
    int checksumvalue;
    private int expectedAckNumber;
    private int expectedSequenceNumber;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    Boolean isFirstPacket; 						// @aalva check if the previous 
    Queue<Message> layer5QueueA; 				// @aalva a queue that stores layer 5 messages from A when send
    							 				// window is full
    int queueMaxSize = 1000; 		 				// @aalva above this size we will exit the window.
    Queue<Packet> sendWindow;
    Queue<Packet> receiverWindow;
    
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize+1;
	RxmtInterval = delay;
    }

   
    int checkShouldBe(Packet packet) {
      char[] data = new char[1000];
      int  checksum;
      data = packet.getPayload().toCharArray();
      checksum = 0;
      int i=0;
    
      while( i < data.length ) {
    	 
        checksum += data[i];
        i++;
    }
      checksum += packet.getAcknum() + packet.getSeqnum();
      checksum = ~checksum;

      return checksum;
  }
    
    
    void sendPacketToB ()
    {
    	if( layer5QueueA.size() == 0 )
    	{
    		isFirstPacket = true; // bad variable name 
    		return ;
    	}
    	else
    	{
    		Message message = layer5QueueA.remove();
    		sequenceNumber = sequenceNumber%LimitSeqNo;
    		expectedAckNumber = sequenceNumber;
    		System.out.println("Sending packet with sequence number " + sequenceNumber);
    		Packet aOutputPacket = new Packet(sequenceNumber,sequenceNumber, FirstSeqNo, message.getData());
    		checksumvalue = checkShouldBe(aOutputPacket);
    		Packet aOutputPacket2 = new Packet(sequenceNumber,sequenceNumber, checksumvalue, message.getData());
    		sendWindow.add(aOutputPacket2);
    		sequenceNumber += 1;
    		toLayer3(A, aOutputPacket2);
    		startTimer (A,RxmtInterval);
    		// How do we deal with expected sequence number;
    		//isAcknowledged = false;
    	}
    }
    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	System.out.println("StudentNetworkSimulator::aOutput begin");
    	if ( layer5QueueA.size() == queueMaxSize )
    	{
    		System.out.println("Sender Buffer Exceeded! Exiting the program");
    		System.exit(1);
    	}
    	else {
    		layer5QueueA.add(message);
    		if (isFirstPacket) {
    			sendPacketToB();
    			isFirstPacket = false;
    		}
    	}
    }
    
    void reTransmitPacket()
    {
    	System.out.println("Time out - Retransmitting packet with sequence number " + sendWindow.peek().getSeqnum());
    	toLayer3(A, sendWindow.peek());
    	startTimer (A,RxmtInterval);
    }
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
    	System.out.println("Received ack for sequence number " + packet.getAcknum());
    	if (packet.getAcknum() == expectedAckNumber)
    	{
    		if (packet.getChecksum() == checkShouldBe(packet) )
    		{
    		 stopTimer(A);
    		 sendWindow.remove();
    		 sendPacketToB();
    		}
    		else 
    		{
    			System.out.println("Wrong checksum! The packet got corrupted! Wait for timeout! ");
    		}
    	}
    	else
    	{
    		// Do nothing
    		System.out.println("Wrong ack! Wait for right ack or timeout! ");
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	reTransmitPacket();
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	isFirstPacket = true;
    	layer5QueueA = new LinkedList<Message>(); 
    	sendWindow = new LinkedList<Packet>();
    	System.out.println("The size of the queue is " + layer5QueueA.size());
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    	
    	if ( packet.getSeqnum() == expectedSequenceNumber)
    	{
    		System.out.println("B - bInput() Received packet with sequence number " + expectedSequenceNumber );
    		if ( packet.getChecksum() == checkShouldBe(packet))
    		{
	    		packet.setAcknum(expectedSequenceNumber);
	    		toLayer5(packet.getPayload());
	    		if (!receiverWindow.isEmpty())
	    		{
	    			receiverWindow.remove();
	    		}
	    		receiverWindow.add(packet);
	    		expectedSequenceNumber = (expectedSequenceNumber+1)%LimitSeqNo;
	    		toLayer3(B,packet);
    	    }
    		else {
	    			System.out.println("B - bInput() Received wrong checksum " );
	        		if (!receiverWindow.isEmpty())
	        		{
	        			toLayer3(B,receiverWindow.peek());
	        		
	        		}
    			
    		     }
    	}
    	else 
    	{
    		// checking non empty for the case where the first packet has wrong sequence number
    		// we do not send any reply in the case of stop and wait
    		// The sender will timeout and resend the packet
    		System.out.println("B - bInput() Received wrong packet with sequence number " + packet.getSeqnum() );
    		if (!receiverWindow.isEmpty())
    		{
    			toLayer3(B,receiverWindow.peek());
    			//
    		}
    	}
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
    	expectedSequenceNumber = 0;
    	receiverWindow = new LinkedList<Packet>();
    }

    // Use to print final statistics
    protected void Simulation_done()
    {

    }	

}
