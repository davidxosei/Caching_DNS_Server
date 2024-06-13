package dns;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class representing a cache of stored DNS records.
 *
 * @version 1.0
 */
public class DNSCache {

    // TODO: fill me in!
	private ArrayList<DNSRecord> cachedRecords;
    private Instant time;

    public DNSCache() {
        newCache();
    }

    private void newCache() {
        cachedRecords = new ArrayList<DNSRecord>();
    }

    public void addCachedRecord(DNSRecord record) {
        if (record.getTypeStr().equals("A")) {
            cachedRecords.add(record);
        }
    }

    public void removeRecords() {
        getTime();
        Iterator<DNSRecord> iterator = cachedRecords.iterator();
        while (iterator.hasNext()) {
            DNSRecord record = iterator.next();
            long totalSeconds = getTotalSeconds(record);
            if (record.getTTL() <= totalSeconds) {
                iterator.remove();
            }
        }
    }

    public ArrayList<DNSRecord> findMatchingRecords(String name, String type, String rClass) {
        ArrayList<DNSRecord> matchingRecords = new ArrayList<>();
        for (DNSRecord record : cachedRecords) {
            if (record.getTypeStr().equals(type) &&
                record.getClassStr().equals(rClass) &&
                record.getName().equals(name)) {
                matchingRecords.add(record);
            }
        }
        return matchingRecords;
    }

    private void getTime() {
        time = Instant.now();
    }
/*
    private long getTotalSeconds(DNSRecord record) {
        Instant recordTime = record.gettimeForConstruction();
        Duration timeElapsed = Duration.between(recordTime, time);
        return timeElapsed.getSeconds();
    }
*/
    private long getTotalSeconds(DNSRecord record) {
    Instant recordTime = record.gettimeForConstruction();
    if (recordTime != null) {
        Duration timeElapsed = Duration.between(recordTime, time);
        return timeElapsed.getSeconds();
    }
    return 0; 
}
}
