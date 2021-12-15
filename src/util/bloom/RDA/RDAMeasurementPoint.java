package util.bloom.RDA;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

public class RDAMeasurementPoint {

    RDATable sender;
    Hashtable<Long, Double> SenderTSTable;

    long maximumTotalPackets = 0;

    /**
     * @param _row,               # rows,
     * @param _column
     * @param sampleProbByColumns
     */
    public RDAMeasurementPoint(int _expectedNumEntries) {

        sender = new RDATable(_expectedNumEntries);      //这种构造的RDATable 参数requiredLeadingZeros=0
        SenderTSTable = new Hashtable<Long, Double>();
    }

    public RDAMeasurementPoint(int _expectedNumEntries, long totalPackets, int hashNum) {

        sender = new RDATable(_expectedNumEntries);

        sender.setHash(hashNum);

        SenderTSTable = new Hashtable<Long, Double>();

        maximumTotalPackets = totalPackets;
        sender.sampleProbability = 1;

    }

    public RDAMeasurementPoint(int _expectedNumEntries, long totalPackets, double samplingProb) {

        sender = new RDATable(_expectedNumEntries);

        SenderTSTable = new Hashtable<Long, Double>();

        maximumTotalPackets = totalPackets;
        sender.sampleProbability = samplingProb;

    }

    public RDAMeasurementPoint(int _expectedNumEntries, double samplingProb) {

        sender = new RDATable(_expectedNumEntries);
        //receiver = new RDATable(_expectedNumEntries);

        SenderTSTable = new Hashtable<Long, Double>();
        //ReceiverTSTable = new Hashtable<Long,Double>();
        //maximumTotalPackets=totalPackets;
        sender.sampleProbability = samplingProb;
        //receiver.sampleProbability=samplingProb;
    }

    /**
     * new packet arrives at the sender
     *
     * @param id
     * @param ts
     */
    public void packetIncoming(long id, double ts) {
        if (SenderTSTable.containsKey(id)) {
            System.err.println("contains key: " + id + ", " + ts);
            return;
        } else {
            sender.insert(id, ts, 1);
            SenderTSTable.put(id, ts);
        }
    }

    //没用
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
     * 没用
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


    //没明白
    public int decodeSetDiff(RDATable receiver) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver); //从自己的RDATable 中 与receiver中的RDATable  减去坏的包 （因为这个函数是进行异或 如果两个桶相同  异或就为空 所遗留下的都是坏桶） 即subtract全是坏包
        HashSet<Long> SenderItems = new HashSet<Long>(2);
        HashSet<Long> ReceiverItems = new HashSet<Long>(2);
        //decode the ids

        //没明白
        int siz = subtract.decodeIDs();//这是只含有问题包的桶的 decode？

        SenderItems.clear();
        ReceiverItems.clear();
        subtract.clear();
        return siz;
    }

    //是否成功decode
    public boolean decodeSet(RDATable receiver, HashSet<Long> SenderItems, HashSet<Long> ReceiverItems) {
        /**
         * subtract, a new TBFTable
         */
        RDATable subtract = sender.subtractIBLT(receiver);

        //decode the ids

        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems); //解码成功与否

        //	SenderItems.clear();
        //	ReceiverItems.clear();
        //	subtract.clear();
        return decodable;
    }

    /**
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

        long delay1 = time3 - time1; //求坏的桶时延
        //decode the ids

        long[] delay2 = subtract.decodeIDsTime(); //解码时延  返回类型totalCount(subtract中的“坏纯桶"), delay1

        SenderItems.clear();
        ReceiverItems.clear();
        subtract.clear();

        long ts = (delay1 + delay2[1]) / 1000;

        double[] returns = {delay2[0], ts};
        return returns;
    }

    /**
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
        boolean decodable = subtract.decodeIDs(SenderItems, ReceiverItems); //是否解码成功
        System.out.println("decode: " + decodable + ",s: " + SenderItems.size() + ",r: " + ReceiverItems.size());


        //repair the sender
        if (!SenderItems.isEmpty()) {  //发送方的丢包修复  SendItems里是桶的keysum  而pure桶的keysum 就是报文id
            sender.RepairTBF(SenderItems, SenderTSTable); //从发送方的缓存中修复
            //repair(sender,SenderItems,SenderTSTable);
        }
        //repair the receiver
        if (!ReceiverItems.isEmpty()) { //接收方有乱序包 需修复
            receiver.RepairTBF(ReceiverItems, ReceiverTSTable);
            //repair(receiver,ReceiverItems,ReceiverTSTable);
        }

    }

    /**
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
            sender.RepairTBF(SenderItems, SenderTSTable);
            //repair(sender,SenderItems,SenderTSTable);
        }
        //repair the receiver
        if (ReceiverItems != null && !ReceiverItems.isEmpty() && ReceiverMiss != null && !ReceiverMiss.isEmpty()) {
            receiver.RepairTBF(ReceiverItems, ReceiverMiss);
            //repair(receiver,ReceiverItems,ReceiverTSTable);
        }

    }

    /**
     * vary percent of repaired 不同百分比的修复?
     *
     * @param percent 修复百分比  没有体现
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
                if (SenderTSTable.containsKey(id)) {  //如果发送方的缓存里有这个丢的包

                    //remove at sender   在发送方移除
                    double ts = SenderTSTable.get(id);
                    sender.erase(id, ts, 1);

                    //receiver, remove directly
                    if (ReceiverTSTable.containsKey(id)) {  //接收方缓存里有这个丢的包？  这个判断在下一个方法注释掉了
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
                    if (SenderTSTable.containsKey(id)) {  //发送方缓存里有这个乱序包?   这个判断在下一个方法注释掉了
                        System.err.println("wrong! " + id);
                        ts = SenderTSTable.get(id);
                        sender.erase(id, ts, 1);
                    }

                }

            }

        }

        double good = 0;
        for (int i = 0; i < sender.hashTable.length; i++) {
            if (sender.hashTable[i].keyCheck == receiver.hashTable[i].keyCheck &&
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

                    if (r.nextFloat() > percent) {  //这个percent 是修复的百分比
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
            //修复发送方
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

        //修复后的good packet总数
        double good = 0;
        for (int i = 0; i < sender.hashTable.length; i++) {
            if (sender.hashTable[i].keyCheck == receiver.hashTable[i].keyCheck &&
                    sender.hashTable[i].Counter > 0 &&
                    sender.hashTable[i].keySum == receiver.hashTable[i].keySum) {
                good += sender.hashTable[i].Counter;
            }
        }


        return good / sender.N_HASH;  //平均每个bank的 修复后的好数据包的个数？

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

}
