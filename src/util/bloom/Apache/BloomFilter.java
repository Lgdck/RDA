/**
 * Copyright (c) 2005, European Commission project OneLab under contract 034819 (http://www.one-lab.org)
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University Catholique de Louvain - UCL
 * nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior
 * written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util.bloom.Apache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import util.bloom.Apache.Hash.Hash;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;

/**
 * Implements a <i>Bloom filter</i>, as defined by Bloom in 1970.
 * <p>
 * The Bloom filter is a data structure that was introduced in 1970 and that has been adopted by 
 * the networking research community in the past decade thanks to the bandwidth efficiencies that it
 * offers for the transmission of set membership information between networked hosts.  A sender encodes 
 * the information into a bit vector, the Bloom filter, that is more compact than a conventional 
 * representation. Computation and space costs for construction are linear in the number of elements.  
 * The receiver uses the filter to test whether various elements are members of the set. Though the 
 * filter will occasionally return a false positive, it will never return a false negative. When creating 
 * the filter, the sender can choose its desired point in a trade-off between the false positive rate and the size. 
 *
 * <p>
 * Originally created by
 * <a href="http://www.one-lab.org">European Commission One-Lab Project 034819</a>.
 *
 * @see Filter The general behavior of a filter
 *
 * @see <a href="http://portal.acm.org/citation.cfm?id=362692&dl=ACM&coll=portal">Space/Time Trade-Offs in Hash Coding with Allowable Errors</a>
 */
public class BloomFilter extends Filter {

    Log log = new Log(BloomFilter.class);

    /**
     *
     */
    private static final long serialVersionUID = 4571691937049250188L;

    private static final byte[] bitvalues = new byte[]{
            (byte) 0x01,
            (byte) 0x02,
            (byte) 0x04,
            (byte) 0x08,
            (byte) 0x10,
            (byte) 0x20,
            (byte) 0x40,
            (byte) 0x80
    };

    /** The bit vector. */
    public BitSet bits;

    //create a lock on the data
    public Semaphore BFOps = null;

    /**
     * number of keys inserted
     */
    public int NumInserted = 0;

    /** Default constructor - use with readFields */
    public BloomFilter() {
        super();
        NumInserted = 0;

        BFOps = new Semaphore(1);
    }


    /**
     * Constructor
     * @param vectorSize The vector size of <i>this</i> filter.
     * @param nbHash The number of hash function to consider.
     * @param hashType type of the hashing function (see
     * {@link org.apache.hadoop.util.hash.Hash}).
     */
    public BloomFilter(int vectorSize, int nbHash, int hashType) {
        super(vectorSize, nbHash, hashType);

        bits = new BitSet(this.vectorSize);
        NumInserted = 0;
        BFOps = new Semaphore(1);
    }

    /**
     * create a Bloom Filter using a BitSet
     * @param mm
     * @return
     */
    public BloomFilter createBloomFilter(BitSet mm) {
        // TODO Auto-generated constructor stub
        BloomFilter f = new BloomFilter(vectorSize, nbHash, hashType);
        if (mm != null) {
            f.NumInserted = -1;
            f.bits.or(mm);
        }
        return f;
    }

    @Override
    public void add(Key key) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }

        int[] h = hash.hash(key);
        //test if we add a new key
/*  if(true){
    boolean isAllTrue=true;
    for(int i = 0; i < nbHash; i++) {  
    	if(!bits.get(h[i])){
    		isAllTrue=false;
    	}
    }
    if(!isAllTrue){
    	NumInserted++;
    }
  }*/
        NumInserted++;
        //NumInserted++;
        //set the key
        hash.clear();
        //System.out.print("\n################\n");
        try {
            BFOps.acquire();
            for (int i = 0; i < nbHash; i++) {
                bits.set(h[i]);
                //  System.out.print(bits.get(h[i])+" ");
            }
            BFOps.release();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //  System.out.print("\n################\n");


    }

    /**
     * set the bits and return the indexes of the bits
     * @param key
     * @return
     */
    public int[] addAndReturnIndex(Key key) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }

        int[] h = hash.hash(key);
	    
/*	    boolean isAllTrue=true;
	    for(int i = 0; i < nbHash; i++) {  
	    	if(!bits.get(h[i])){
	    		isAllTrue=false;
	    	}
	    }
	    if(!isAllTrue){
	    	NumInserted++;
	    }*/
        NumInserted++;


        hash.clear();


        try {
            BFOps.acquire();
            for (int i = 0; i < nbHash; i++) {
                bits.set(h[i]);
                //  System.out.print(bits.get(h[i])+" ");
            }
            BFOps.release();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return h;
        //  System.out.print("\n################\n");
    }

    @Override
    public void and(Filter filter) {
        if (filter == null
                || !(filter instanceof BloomFilter)
                || filter.vectorSize != this.vectorSize
                || filter.nbHash != this.nbHash) {
            throw new IllegalArgumentException("filters cannot be and-ed");
        }

        this.bits.and(((BloomFilter) filter).bits);
    }

    @Override
    public boolean membershipTest(Key key) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        //long T=System.currentTimeMillis();

        int[] h = hash.hash(key);
        hash.clear();

        //log.info(String.format("%d ", System.currentTimeMillis()-T));

        //T=System.currentTimeMillis();

        for (int i = 0; i < nbHash; i++) {
            //System.out.println("$: "+h[i]+", "+bits.get(h[i]));
            if (!bits.get(h[i])) {
                return false;
            }
        }

        //log.info(String.format("%d\n", System.currentTimeMillis()-T));

        return true;
    }

    /**
     * get the hashed Key
     * @param key
     * @return
     */
    public int[] getHash(Key key) {
        return hash.hash(key);
    }

    /**
     * test membership and return the index
     * @param key
     * @param Indexes
     * @return
     */
    public boolean membershipTestAndReturnIndexes(Key key) {

        //intialize as null


        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }

        int[] h = hash.hash(key);
        hash.clear();
        for (int i = 0; i < nbHash; i++) {
            //System.out.println("$: "+h[i]+", "+bits.get(h[i]));
            if (!bits.get(h[i])) {
                return false;
            }
        }
        //System.out.println("$: Yes, found!");
        //yes

        return true;
    }

    /**
     * true bits
     * @return
     */
    public int getNumTrueBits() {
        int sum = 0;
        //return bits.cardinality();

        if (bits.cardinality() > 0) {
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                if (i >= 0) {
                    sum++;
                }
            }
        }
        return sum;
    }

    /**
     * get all true bits
     * @return
     */
    public List<Integer> getAllTrueBits() {
        List<Integer> indexes = new ArrayList<Integer>(2);
	  /*for(int i=0;i<vectorSize;i++){
		  if(bits.get(i)){
			  indexes.add(i);
		  }
	  }*/
        //has true bits
        if (bits.cardinality() > 0) {
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                if (i >= 0) {
                    indexes.add(i);
                }
            }
        }
        return indexes;
    }

    /**
     * simulate the perfect hash
     * @param nKeys
     */
    public void addByPerfectHash(int nKeys) {

        NumInserted = nKeys;
        int pos = -1;

        Random seed = new Random(System.currentTimeMillis());
        //k independent hash function
        for (int j = 0; j < nbHash; j++) {
            Random r = new Random(seed.nextInt());

            for (int i = 0; i < nKeys; i++) {
                pos = r.nextInt(vectorSize);
                setBit(pos);

            }
        }
    }

    /**
     * reset the fit
     * @param pos
     */
    public void flipBit(int pos) {

        try {
            BFOps.acquire();
            bits.flip(pos);
            BFOps.release();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void not() {
        bits.flip(0, vectorSize - 1);
    }

    @Override
    public void or(Filter filter) {
        if (filter == null
                || !(filter instanceof BloomFilter)
                || filter.vectorSize != this.vectorSize
                || filter.nbHash != this.nbHash) {
            throw new IllegalArgumentException("filters cannot be or-ed");
        }
        bits.or(((BloomFilter) filter).bits);
    }

    @Override
    public void xor(Filter filter) {
        if (filter == null
                || !(filter instanceof BloomFilter)
                || filter.vectorSize != this.vectorSize
                || filter.nbHash != this.nbHash) {
            throw new IllegalArgumentException("filters cannot be xor-ed");
        }
        bits.xor(((BloomFilter) filter).bits);
    }

    @Override
    public String toString() {
        return bits.toString();
    }

    /**
     * get a copy of the filter
     * @return
     */
    public BloomFilter getCopy() {
        BloomFilter f = new BloomFilter(vectorSize, nbHash, hashType);
        f.or(this);

        f.NumInserted = this.NumInserted;

        return f;
    }

    public BitSet getBitSet() {
        return bits;

    }

    /**
     * @return size of the the bloomfilter
     */
    public int getVectorSize() {
        return vectorSize;
    }

    // Writable

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        byte[] bytes = new byte[getNBytes()];
        for (int i = 0, byteIndex = 0, bitIndex = 0; i < getVectorSize(); i++, bitIndex++) {
            if (bitIndex == 8) {
                bitIndex = 0;
                byteIndex++;
            }
            if (bitIndex == 0) {
                bytes[byteIndex] = 0;
            }
            if (bits.get(i)) {
                bytes[byteIndex] |= bitvalues[bitIndex];
            }
        }
        out.write(bytes);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);
        bits = new BitSet(this.getVectorSize());
        byte[] bytes = new byte[getNBytes()];
        in.readFully(bytes);
        for (int i = 0, byteIndex = 0, bitIndex = 0; i < getVectorSize(); i++, bitIndex++) {
            if (bitIndex == 8) {
                bitIndex = 0;
                byteIndex++;
            }
            if ((bytes[byteIndex] & bitvalues[bitIndex]) != 0) {
                bits.set(i);
            }
        }
    }

    /* @return number of bytes needed to hold bit vector */
    private int getNBytes() {
        return (getVectorSize() + 7) / 8;
    }

    /**
     * reset to all false bits
     */
    public void clear() {
        if (this.bits != null) {
            this.bits.clear();
        }
        this.NumInserted = 0;
    }

    /**
     * get the size of the bits
     * @return
     */
    public int getSize() {
        return getVectorSize();
    }

    public static void main(String[] args) {


        int m = 57;
        int ks = 20;
        int n = 2;

        BloomFilter test = new BloomFilter(m, ks, Hash.MURMUR_HASH);


        Vector<Key> vec = new Vector<Key>(1);
        Random r = new Random(1);


        int size = 0;


        for (int j = 0; j < n; j++) {

            byte[] key = new byte[6];
            ;

            r.nextBytes(key);

            Key k = new Key(key);

            vec.add(k.makeCopy());
            // if(j%2==0){
            test.add(k.makeCopy());
            size++;
            // }
        }

        System.out.println("FP: " + test.getAveragedObservedFalsePositive() + ", " +
                test.getAveragedPriorFP(2));


        int count = 0;
        for (int j = 0; j < vec.size(); j++) {
            Key k = vec.get(j);
            //System.out.println("$: "+POut.toString(k.getBytes()));

            if (test.membershipTest(k)) {
                count++;
                //  System.out.println("======================");
            }
        }

        System.out.println("$: size: " + vec.size());

        System.out.println("$: Total: " + size);

        System.out.println("$: real: " + count);


    }

    /**
     * set a bit
     * @param indKey
     */
    public void setBit(int indKey) {
        // TODO Auto-generated method stub

        try {
            BFOps.acquire();
            bits.set(indKey);
            BFOps.release();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    /**
     * query the bit
     * @param indKey
     * @return
     */
    public boolean queryBit(int indKey) {
        if (bits.get(indKey)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * filled factor
     * @return
     */
    public double filledFactor() {
        //int ff=getAllTrueBits().size();

        int ff = bits.cardinality();


        // log.info(vectorSize+" "+bits.cardinality()+" "+getAllTrueBits().size());

        return (ff + 0.0) / vectorSize;

    }

    /**
     * prior fp
     * @param n
     * @return
     */
    public double getAveragedPriorFP(int n) {
        // TODO Auto-generated method stub

        //		nbHashLev_2_Bloom);

        return Math.pow((1 - Math.exp(-(n * nbHash + 0.0) / vectorSize)), nbHash);
    }

    /**
     * prior fp
     * @return
     */
    public double getAveragedPriorFP() {
        // TODO Auto-generated method stub

        //		nbHashLev_2_Bloom);
        int n = NumInserted;

        return Math.pow((1 - Math.exp(-(n * nbHash + 0.0) / vectorSize)), nbHash);
    }


    /**
     * posterior
     * @return
     */
    public double getAveragedObservedFalsePositive() {
        // TODO Auto-generated method stub

        return Math.pow(filledFactor(), nbHash);
    }

    /**
     * different bits
     * @param b
     * @return
     */
    public int diff(BloomFilter b) {
        int s = 0;
        for (int i = 0; i < vectorSize; i++) {
            if (queryBit(i) != b.queryBit(i)) {
                s++;
            }
        }
        return s;
    }


    /**
     * query the key
     * @param keyList
     */
    public void query(Set<Key> keyList) {
        // TODO Auto-generated method stub
        Iterator<Key> ier = keyList.iterator();
        while (ier.hasNext()) {
            membershipTest(ier.next());
        }

    }

    public double[] getQueryTimeDetails(Set<Key> keyList) {
        // TODO Auto-generated method stub
        double t1 = System.currentTimeMillis();
        int repts = 1000;

        long TAddress = 0;

        long THash = 0;

        long T;

        for (int i = 0; i < repts; i++) {
            Iterator<Key> ier = (keyList.iterator());
            while (ier.hasNext()) {

                Key key = ier.next();


                T = System.currentTimeMillis();

                int[] h = hash.hash(key);
                hash.clear();

                THash += System.currentTimeMillis() - T;
                //log.info(String.format("%d ", System.currentTimeMillis()-T));

                T = System.currentTimeMillis();

                for (int j = 0; j < nbHash; j++) {
                    //System.out.println("$: "+h[i]+", "+bits.get(h[i]));
                    if (!bits.get(h[j])) {
                    }
                }
                TAddress += System.currentTimeMillis() - T;

            }
        }

        double hashTime = THash / (repts * 1000.0 * keyList.size());
        double address = TAddress / (repts * 1000.0 * keyList.size());

        log.debug(String.format("%d %d %.4e %.4e\n", vectorSize, nbHash, hashTime, address));

        double[] out = {address, hashTime};
        return out;
    }


    /**
     * get query time
     * @param keyList
     * @return
     */
    public double getQueryTime(Set<Key> keyList) {
        // TODO Auto-generated method stub
        //double t1=System.currentTimeMillis();
        int repts = 1;

        //long TAddress=0;

        long THash = 0;

        long T;

        T = System.currentTimeMillis();

        for (int i = 0; i < repts; i++) {
            Iterator<Key> ier = (keyList.iterator());
            while (ier.hasNext()) {

                Key key = ier.next();


                int[] h = hash.hash(key);
                hash.clear();


                //log.info(String.format("%d ", System.currentTimeMillis()-T));

                for (int j = 0; j < nbHash; j++) {
                    //System.out.println("$: "+h[i]+", "+bits.get(h[i]));
                    if (!bits.get(h[j])) {
                    }
                }
                // TAddress+=System.currentTimeMillis()-T;

            }
        }

        THash = System.currentTimeMillis() - T;

        //double hashTime = THash/(repts*1000.0*keyList.size());
        //double address = TAddress/(nbHash*repts*1000.0*keyList.size());

        //log.info(String.format("%d %d %.4e %.4e\n", vectorSize, nbHash,hashTime,address));

        return THash / (0.0 + keyList.size());
        //return nbHash*address + hashTime;
    }


    public void reset() {
        // TODO Auto-generated method stub
        this.bits.clear();
        this.NumInserted = 0;
    }

    /**
     * return string
     * @param key
     * @return
     */
    public String addAndReturnHash(Key key) {
        // TODO Auto-generated method stub
        int[] h = hash.hash(key);

        NumInserted++;
        //NumInserted++;
        //set the key
        hash.clear();
        StringBuffer sb = new StringBuffer();
        //System.out.print("\n################\n");
        for (int i = 0; i < nbHash; i++) {
            bits.set(h[i]);
            sb.append(h[i]);
        }
        //  System.out.print("\n################\n");
        return sb.toString();
    }

}//end class
