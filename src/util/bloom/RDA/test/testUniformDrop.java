package util.bloom.RDA.test;

import edu.NUDT.RDA.parallel.PacketTraceGenerator;

import java.util.Random;

/**
 * @author lgd
 * @date 2021/12/10 14:43
 */
public class testUniformDrop {
    public static void main(String[] args){
        eduni.simjava.distributions.Sim_uniform_obj drop=new eduni.simjava.distributions.Sim_uniform_obj("drop",0,1,System.currentTimeMillis());
//        for (int i=0;i<10;i++)  System.out.println(drop.sample());


        Random r = new Random(System.currentTimeMillis());
        long initialTS = r.nextLong() % 100000;
        long _startID = r.nextLong() % 100000;
        PacketTraceGenerator pg =new PacketTraceGenerator(6.647,0.7,0.1,0.1,initialTS,_startID);
        for (int i=0;i<10;i++){
            System.out.println(pg.dropOrReorder());  // 2->正常包  1-> 乱序包  0 ->丢包
        }

    }
}
