package util.bloom.RDA;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import edu.NUDT.control.PassiveCollectorPeriod;
import edu.harvard.syrah.prp.Log;
import util.async.HashMapCache;
import util.async.java.util.concurrent.ConcurrentHashMap;


public class SimpleRDAMeasurementPoint {


    static Log log = new Log(SimpleRDAMeasurementPoint.class);

    //RDA compute
    //public RDATable sender;
    public RDATable sender;
    //cache for RDA
    public ConcurrentHashMap<Long, Double> SenderTSTable;  //缓存数据包  在packetIncoming改变

    long maximumTotalPackets = 0;

    /**
     * complete the parsing
     */
    public volatile boolean isParsed = false;

    /**
     * @param _row,               # rows,
     * @param _column
     * @param sampleProbByColumns
     */
    public SimpleRDAMeasurementPoint(int _expectedNumEntries) {

        //sender = new RDATable(_expectedNumEntries);
        sender = new RDATable(_expectedNumEntries);
        SenderTSTable = new ConcurrentHashMap<Long, Double>();
    }

    public SimpleRDAMeasurementPoint(int _expectedNumEntries, long totalPackets, int hashNum) {

        sender = new RDATable(_expectedNumEntries);

        sender.setHash(hashNum);

        SenderTSTable = new ConcurrentHashMap<Long, Double>();

        maximumTotalPackets = totalPackets;
        sender.sampleProbability = 1;

    }

    public SimpleRDAMeasurementPoint(int _expectedNumEntries, long totalPackets, double samplingProb) {

        sender = new RDATable(_expectedNumEntries);

        SenderTSTable = new ConcurrentHashMap<Long, Double>();

        maximumTotalPackets = totalPackets;
        sender.sampleProbability = samplingProb;

    }

    public SimpleRDAMeasurementPoint(int _expectedNumEntries, double samplingProb) {

        sender = new RDATable(_expectedNumEntries);
        //receiver = new RDATable(_expectedNumEntries);

        SenderTSTable = new ConcurrentHashMap<Long, Double>();
        //ReceiverTSTable = new Hashtable<Long,Double>();
        //maximumTotalPackets=totalPackets;
        sender.sampleProbability = samplingProb;
        //receiver.sampleProbability=samplingProb;
    }

    /**
     * 存到缓存 TSTable里  该类型的map是在PassiveCollector里定义的SenderTSTable
     * parse the records
     * into the cache and ts
     */
    public void packetIncoming(HashMapCache<Long, Long> cache) {
        // TODO Auto-generated method stub
        log.main("stored: " + cache.size());
        for (Entry<Long, Long> e : cache.entrySet()) {
            Long id = e.getKey();
            //这里随便调用一个读取scaleTS的类就行  精度？
            double ts = (PassiveCollectorPeriod.scaleTS * e.getValue());//(PassiveRDAHost.scaleTS*(e.getValue()-PassiveRDAHost.TSConstant));
            SenderTSTable.put(id, ts);
            sender.insert(id, ts, 1);
        }
    }


    public void packetIncoming(AbstractMap<Long, Long> cache) {
        // TODO Auto-generated method stub
        log.main("stored: " + cache.size());
        for (Entry<Long, Long> e : cache.entrySet()) {
            long id = e.getKey();
            double ts = PassiveCollectorPeriod.scaleTS * e.getValue();//(PassiveRDAHost.scaleTS*(e.getValue()-PassiveRDAHost.TSConstant));
            SenderTSTable.put(id, ts);
            sender.insert(id, ts, 1);
        }
    }

    /**
     * new packet arrives at the sender
     *
     * @param id
     * @param ts
     */
    public void packetIncoming(long id, double ts) {
        if (SenderTSTable.containsKey(id)) {
            //System.err.println("contains key: "+id+", "+ts);
            return;
        } else {
            sender.insert(id, ts, 1);
            SenderTSTable.put(id, (ts));
        }
    }

    public void packetIncomingSample(long id, double ts) {
        if (SenderTSTable.containsKey(id)) {
            System.err.println("contains key: " + id + ", " + ts);
            return;
        } else {
            boolean inserted = sender.InsertSample(id, ts, 1);
            if (inserted) {
                SenderTSTable.put(id, ts);
            }
        }
    }

    /**
     * ignore duplicate packet
     *
     * @param id
     * @param ts
     * @return
     */
    public boolean packetIncomingIgnoreDuplicate(long id,
                                                 double ts) {
        if (SenderTSTable.containsKey(id)) {
            return false;
        } else {
            sender.insert(id, ts, 1);
            SenderTSTable.put(id, ts);
            return true;
        }
    }


    public int decodeSetDiff(RDATable receiver) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver);
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids

        int siz = subtract.decodeIDs();

        SenderItems.clear();
        ReceiverItems.clear();
        subtract.clear();
        return siz;
    }

    public boolean decodeSet(RDATable receiver, HashSet<Long> SenderItems, HashSet<Long> ReceiverItems) {
        /**
         * subtract, a new TBFTable
         */

        RDATable subtract = sender.subtractIBLT(receiver);
        //log.main("subtracted!");
        //decode the ids

        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems); //一个是丢包的id集合  一个是乱序包的id集合
        //log.main("decoded!");
        //	SenderItems.clear();
        //	ReceiverItems.clear();
        //	subtract.clear();
        return decodable;
    }

    /**和RDAMeasurementPoint一样
     * calculate the time
     *
     * @return
     */
    public double[] decodeSetDiffWithTime(RDATable receiver) {
        /**
         * subtract, a new TBFTable
         */
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);

        long time1 = System.nanoTime();

        RDATable subtract = sender.subtractIBLT(receiver);
        long time3 = System.nanoTime();

        long delay1 = time3 - time1;
        //decode the ids

        long[] delay2 = subtract.decodeIDsTime();

        SenderItems.clear();
        ReceiverItems.clear();
        subtract.clear();

        long ts = (delay1 + delay2[1]) / 1000;

        double[] returns = {delay2[0], ts};
        return returns;
    }

    /**和RDAMeasurementPoint一样
     * repair the table of the sender and the receiver
     *
     * @return
     */
    public void repair(RDATable receiver, Hashtable<Long, Double> ReceiverTSTable) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver);
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids
        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems);
        System.out.println("decode: " + decodable + ",s: " + SenderItems.size() + ",r: " + ReceiverItems.size());


        //repair the sender
        if (!SenderItems.isEmpty()) {
            sender.RepairTBF(SenderItems, new Hashtable<Long, Double>(SenderTSTable));
            //repair(sender,SenderItems,SenderTSTable);
        }
        //repair the receiver
        if (!ReceiverItems.isEmpty()) {
            receiver.RepairTBF(ReceiverItems, ReceiverTSTable);
            //repair(receiver,ReceiverItems,ReceiverTSTable);
        }

    }

    /**和RDAMeasurementPoint一样
     * repair directly
     *
     * @param SenderItems
     * @param receiver
     * @param ReceiverItems
     * @param ReceiverMiss
     */
    public void repairDirect(HashSet<Long> SenderItems, RDATable receiver, HashSet<Long> ReceiverItems, Hashtable<Long, Double> ReceiverMiss) {
        /**
         * subtract, a new TBFTable
         */
        //RDATable subtract=sender.subtractIBLT(receiver);
        //HashSet<Long> SenderItems = new HashSet<Long>(2);
        //HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids
        //boolean decodable=subtract.decodeIDs(SenderItems,ReceiverItems);
        //System.out.println("decode: "+decodable+",s: "+SenderItems.size()+",r: "+ReceiverItems.size());

        //repair the sender
        if (!SenderItems.isEmpty()) {
            sender.RepairTBF(SenderItems, new Hashtable<Long, Double>(SenderTSTable));  //为什么传new  而不直接传SenderTSTable?
            //repair(sender,SenderItems,SenderTSTable);
        }
        //repair the receiver
        if (ReceiverItems != null && !ReceiverItems.isEmpty() && ReceiverMiss != null && !ReceiverMiss.isEmpty()) {
            receiver.RepairTBF(ReceiverItems, ReceiverMiss);  //前面的是乱序包集合  后面是发送方缓存
            //repair(receiver,ReceiverItems,ReceiverTSTable);
        }

    }

    /**
     * vary percent of repaired
     *
     * @param percent
     */


    public double[] repairNow(double percent, RDATable receiver, Hashtable<Long, Double> ReceiverTSTable) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver);
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids
        double t1 = System.currentTimeMillis();
        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems);
        double delay = System.currentTimeMillis() - t1;
        System.out.println("decode: " + decodable + ",s: " + SenderItems.size() + ",r: " + ReceiverItems.size());


        //repair the sender
        if (!SenderItems.isEmpty()) {

            Random r = new Random(System.currentTimeMillis());

            Iterator<Long> ier = SenderItems.iterator();
            while (ier.hasNext()) {
                long id = ier.next();
                //sender
                if (SenderTSTable.containsKey(id)) {


                    //remove at sender
                    double ts = SenderTSTable.get(id);
                    sender.erase(id, ts, 1);

                    //receiver, remove directly
                    if (ReceiverTSTable.containsKey(id)) {
                        System.err.println("wrong! " + id);
                        ts = ReceiverTSTable.get(id);
                        receiver.erase(id, ts, 1);
                    }

                }

            }

            ier = ReceiverItems.iterator();
            Long id;
            while (ier.hasNext()) {
                id = ier.next();

                if (ReceiverTSTable.containsKey(id)) {

                    //remove at sender
                    double ts = ReceiverTSTable.get(id);
                    receiver.erase(id, ts, 1);

                    //receiver, remove directly
                    if (SenderTSTable.containsKey(id)) {
                        System.err.println("wrong! " + id);
                        ts = SenderTSTable.get(id);
                        sender.erase(id, ts, 1);
                    }


                }

            }

        }


        double good = 0;
        for (int i = 0; i < sender.hashTable.length; i++) {
            if (//sender.hashTable[i].keyCheck==receiver.hashTable[i].keyCheck&&
                    sender.hashTable[i].Counter > 0 &&
                            sender.hashTable[i].keySum == receiver.hashTable[i].keySum) {
                good += sender.hashTable[i].Counter;
            }
        }


        double[] result = {good / sender.N_HASH, delay};

        return result;
    }

    public double repair(double percent, RDATable receiver, Hashtable<Long, Double> ReceiverTSTable) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver);
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids

        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems);
        System.out.println("decode: " + decodable + ",s: " + SenderItems.size() + ",r: " + ReceiverItems.size());


        //repair the sender
        if (!SenderItems.isEmpty()) {

            Random r = new Random(System.currentTimeMillis());

            Iterator<Long> ier = SenderItems.iterator();
            while (ier.hasNext()) {
                long id = ier.next();
                //sender
                if (SenderTSTable.containsKey(id)) {

                    if (r.nextFloat() > percent) {
                        continue;
                    } else {
                        //remove at sender
                        double ts = SenderTSTable.get(id);
                        sender.erase(id, ts, 1);

                        //receiver, remove directly
							/*if(ReceiverTSTable.containsKey(id)){								
								ts=ReceiverTSTable.get(id);
								receiver.erease(id, ts, 1);
							}*/
                    }
                }

            }

            ier = ReceiverItems.iterator();
            Long id;
            while (ier.hasNext()) {
                id = ier.next();

                if (ReceiverTSTable.containsKey(id)) {
                    if (r.nextFloat() > percent) {
                        continue;
                    } else {
                        //remove at sender
                        double ts = ReceiverTSTable.get(id);
                        receiver.erase(id, ts, 1);

                        //receiver, remove directly
						/*if(SenderTSTable.containsKey(id)){								
							ts=SenderTSTable.get(id);
							sender.erease(id, ts, 1);
						}*/
                    }

                }

            }

        }


        double good = 0;
        for (int i = 0; i < sender.hashTable.length; i++) {
            if (//sender.hashTable[i].keyCheck==receiver.hashTable[i].keyCheck&&
                    sender.hashTable[i].Counter > 0 &&
                            sender.hashTable[i].keySum == receiver.hashTable[i].keySum) {
                good += sender.hashTable[i].Counter;
            }
        }


        return good / sender.N_HASH;

    }

    /**
     * average number
     *
     * @return
     */
    public double getAverage(RDATable receiver) {
        return sender.getAvgTS(receiver);
    }


    /**
     * StandardDeviation
     *
     * @param avg, average number
     * @return
     */
    public double getStandardDeviation(double avg, RDATable receiver, Hashtable<Long, Double> ReceiverTSTable) {
        return sender.getSTD(receiver, avg);

        //return receiver.getVarianceStatistics(sender, avg);
    }

    /**
     * get standard deviation
     *
     * @param avg
     * @param receiver
     * @return
     */
    public double getStandardDeviation(double avg, RDATable receiver) {
        return sender.getSTD(receiver, avg);

        //return receiver.getVarianceStatistics(sender, avg);
    }

    public String getBucketSize(RDATable receiver) {
        // TODO Auto-generated method stub
        return sender.expectedNumEntries + " " + receiver.expectedNumEntries + " ";
    }

    public void clear() {
        // TODO Auto-generated method stub
        isParsed = false;
        SenderTSTable.clear();
        //ReceiverTSTable.clear();
        //SenderTSTable=null;
        //ReceiverTSTable=null;
        sender.clear();
        //receiver.clear();
    }

    public double getGoodPackets(RDATable receiver) {
        // TODO Auto-generated method stub
        return sender.getGoodPackets(receiver);
    }


    /**
     * compute directly
     *
     * @param receiverTSTable
     * @return
     */
    public double[] computeDirect(Hashtable<Long, Double> receiverTSTable) {
        // TODO Auto-generated method stub
        /**
         * read the trace
         * return the target
         */
        double Ak = 0;  //平均时延
        double AkMinusOne = 0;  //存储上一次的平均时延
        double Qk = 0;  //标准差
        double squareRTTSum = 0; //正常数据包 时延的平方和   论文中Algorithm的SqSum
        double counter = 0;  //good packet的计数
        double xk = 0;  //本次正常包的时延
        //
        SetView<Long> keys = Sets.intersection(SenderTSTable.keySet(), receiverTSTable.keySet());  //两个集合的交集  difference 是差集
        int i = 0;
        double[] TS = {0, 0};
        if (!keys.isEmpty()) {  //有交集
            Iterator<Long> ier = keys.iterator();
            while (ier.hasNext()) {
                Long nxt = ier.next();
                TS[0] = SenderTSTable.get(nxt);  //发送方该包的时间戳
                TS[1] = receiverTSTable.get(nxt);  //接收方该包的时间戳
                //System.out.println("line: "+lines + ", "+timestamp);

                if (Double.isInfinite(TS[0]) || Double.isNaN(TS[0]) ||
                        Double.isInfinite(TS[1]) || Double.isNaN(TS[1])) {
                    continue;
                }
                //good
                //average over the delay
                AkMinusOne = Ak;  //存储上一次的平均时延
                xk = Math.abs(TS[0] - TS[1]);  //本次包传送的时延
                //average
                Ak = RDAHostSimpleLossReorder.onePassAvg(i + 1, Ak, xk);
                //standard deviation
                Qk = RDAHostSimpleLossReorder.onePassStandardDeviation(i + 1, AkMinusOne, Ak, Qk, xk);
                i++;
                counter += 1;
                //square
                squareRTTSum += Math.pow(xk, 2);
            }

        }
        //keys.clear();
        //keys=null;
        /**
         * true
         */
        double avg = Ak;
        double standardDeviation = Math.abs(Qk / counter);
        double squareSTD = Math.abs(squareRTTSum / counter - Math.pow(avg, 2));
        //log.main("#Good: "+i+", avg: "+avg+", standardDeviation: "+standardDeviation+"\n"+", squareSTD: "+squareSTD);
        double[] result = {avg, squareSTD};
        return result;
    }

    /**
     * get stored ids
     *
     * @return
     */
    public Set<Long> getDecodeIDs() {
        return this.sender.decodeAllIDs();
    }

    /**
     * contains keys
     *
     * @param key
     * @param ids
     * @return
     */
    public boolean containsKey(Long key, Set<Long> ids) {
        // TODO Auto-generated method stub
        if (ids.contains(key)) {
            return true;
        } else {
            return false;
        }
    }
}
