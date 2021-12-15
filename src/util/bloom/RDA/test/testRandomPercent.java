package util.bloom.RDA.test;

import java.util.Random;

/**
 * @author lgd
 * @date 2021/12/11 15:26
 */
public class testRandomPercent {
    public static void main(String[] args){
        Random r = new Random(System.currentTimeMillis());
        for (int i=0;i<10;i++){
            System.out.println(r.nextDouble());
        }
    }
}
