package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Class representing a DNS Server.
 *
 * @version 1.0
 */
public class DNSServer {
    
    /**
     * DNS uses port UDP port 53 for the server
     */
    final private int PORT = 53;

    /**
     * set the maximum packet size to be 512 bytes for DNS messages
     */
    final private int MAX_SIZE = 512;

    /**
     * this server will handle requests for a single zone/domain
     */
    private DNSZone zone;

    // TODO: add class variable for the cache
    private DNSCache cache;
    // TODO: add class variable to track pending queries

    private HashMap<Integer, DNSMessage> pendingQueries;
    private DNSMessage query1;
    /**
     * all queries sent from this server will go to a single "upstream" server
     */
    private InetAddress nextServer;
    private int nextServerPort;

    /**
     * Required constructor that simply prints out some messages about the server.
     *
     * @param zone a DNSZone object that has already been constructed
     */
    public DNSServer(DNSZone zone) {
        this.zone = zone;

        /* TODO: add a DNSCache object */

        // set our upstream server to be 127.0.0.53
        // note: we're assuming this is running on a recent Ubuntu system
        //       and we're using the Ubuntu server as our upstream
        cache = new DNSCache();
	try {
            nextServer = InetAddress.getByName("127.0.0.53");
        } catch(UnknownHostException e) {
            System.out.println("Should never get here.");
            System.exit(0);
        }
        nextServerPort = 53;
	pendingQueries = new HashMap<>();
        System.out.printf("Starting server on port %d%n", PORT);
    }

    /**
     * handle one incoming DNS query message
     * TODO: complete me!
     *
     * @param   query   the DNS query message
     * @return          a DatagramPacket object with the response message
     */
    private DatagramPacket handleQuery(DNSMessage query) {
        // print the query message contents
        System.out.println("Query received from " + query.getPacket().getSocketAddress());
        System.out.println(query);

        // look for the record in our zone
        boolean inZone = true;
        var records = zone.getRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());

        // TODO: look for the record in the cache if it's not in our zone

        // send the response back to the client if we found the record either in the zone or the cache
	if(records.isEmpty() == true) {
	    inZone = false;
	    records = cache.findMatchingRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());
	}

       	if(records.size() != 0) {
            // make a response message
            DNSMessage reply = new DNSMessage(query, records, inZone);

            // print the response message contents
            System.out.println("Reply to " + query.getPacket().getSocketAddress());
            System.out.println(reply);

            // make and return a response packet
            return new DatagramPacket(reply.getData(), reply.getDataLength(), query.getPacket().getSocketAddress());
        }

        // if we didn't find the record, send to the next server (see nextServer and nextServerPort variables)

        // TODO: print the response message contents
	System.out.println("Forwarding Query to "+ nextServer + ":"+nextServerPort+"\nQuery: "+query);

        // TODO: store the query so we can respond to it when we get a reply
	pendingQueries.put(query.getID(), query);

        // TODO: make and return a new DatagramPacket query packet to forward
   	return new DatagramPacket(query.getData(), query.getDataLength(), nextServer, nextServerPort);
    }

    /**
     * handle one incoming DNS reply message
     * TODO: complete me!
     *
     * @param   reply   the incoming reply message
     * @return          a DatagramPacket object with the response message
     */
    private DatagramPacket handleReply(DNSMessage reply) {
        // print the reply message contents
        System.out.println("Reply received from " + reply.getPacket().getSocketAddress());
        System.out.println(reply);

        // TODO: match the reply to the original query
        query1 = pendingQueries.get(reply.getID());
        // TODO: add answers to the cache
	var answers = reply.getAnswers();
	for (var answer : answers) {
		cache.addCachedRecord(answer);
	}
        // TODO: remove the original query from the outstanding set
	pendingQueries.remove(reply.getID());
        // TODO: print the reply message again for consistency
	System.out.println("Forwarding reply to  "+query1.getPacket().getSocketAddress());
	System.out.println(reply);
        // TODO: make and return a new response packet to send to the original client
    	return new DatagramPacket(reply.getData(), reply.getDataLength(), query1.getPacket().getSocketAddress());
    }

    /**
     * handle one DNS message
     *
     * @param   incomingPkt the UDP packet containing the incoming DNS message
     * @return              a UDP packet containing the DNS response
     */
    private DatagramPacket handleMessage(DatagramPacket incomingPkt) {
        // TODO: update the cache each time we receive a message, to remove any records with expired TTLs

        // create a DNS Message object that will parse the request packet data
        cache.removeRecords();
	var incomingMessage = new DNSMessage(incomingPkt);

        // handle queries
        if(incomingMessage.isQuery()) {
            return handleQuery(incomingMessage);
        }

        // handle replies
        else {
            return handleReply(incomingMessage);
        }
    }

    /**
     * Open a socket to receive UDP packets and handle those packets
     */
    public void run() {
        // open the socket, ensure it will close when the try block finishes
        try (
            // listen on localhost only
            var sock = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
        ) {
            // keep reading packets one at a time, forever
            while(true) {
                // packet to store the incoming message
                var in_packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);

                // blocking call, read one packet
                sock.receive(in_packet);

                // handle this packet
                var out_packet = handleMessage(in_packet);

                // only send a response if there were no errors
                if (out_packet != null) {
                    sock.send(out_packet);
                }
            }
        } catch(IOException e) {
            // Have to catch IOexceptions for most socket calls
            System.out.println("Network error!");
        }
    }

    /**
     * Server starting point
     *
     * @param args should contain a single value, the filename of the zone file
     */
    public static void main(String[] args) {
        // must have exactly a single command line argument
        if(args.length != 1) {
            System.out.println("Usage: sudo java dns.DNSServer zone_file");
            System.exit(0);
        }

        // make the zone, which will exit() if the file is invalid in any way
        var zone = new DNSZone(args[0]);

        // make the server object then start listening for DNS requests
        var server = new DNSServer(zone);
        server.run();
    }
}
