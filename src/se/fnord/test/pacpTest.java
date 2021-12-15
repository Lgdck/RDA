package se.fnord.test;

import se.fnord.PcapReader;
import se.fnord.PcapRecord;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * @author lgd
 * @date 2021/12/13 16:48
 */
public class pacpTest {
    public static void main(String[] args) throws IOException {
        PcapReader pcapReader = PcapReader.create(Paths.get("E:\\work\\rda\\univ1_trace\\ftp3.pcap"));
        Iterator<PcapRecord> iterator = pcapReader.iterator();

        while (iterator.hasNext()){
            PcapRecord next = iterator.next();
            System.out.println(next.index() +  " ---- " + next.timestamp() + " --- " +next.capturedLength());

        }
    }
}
