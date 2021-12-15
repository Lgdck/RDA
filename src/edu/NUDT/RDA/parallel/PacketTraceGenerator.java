package edu.NUDT.RDA.parallel;

import util.async.Weibull;
import edu.harvard.syrah.prp.Log;


/**
 * generate packet trace   产生韦伯分布的数据包
 * @author ericfu
 *
 */
public class PacketTraceGenerator {

	long seed=21213233;
	double NextTime=0;
	long NextID=0;
	long weibullMultiplier=10;
	Weibull weibuller=null;
	eduni.simjava.distributions.Sim_uniform_obj uniformDrop=null;
	eduni.simjava.distributions.Sim_uniform_obj uniformReorder=null;
	//loss
	public double dropProbability=0;//0-1
	public double reorderProbability=0;//0-1
	/**
	 * input
	 * @param scale, alpha
	 * @param shape, beta [0.6,0.8]
	 * @param initialTS
	 * @param _startID
	 */
	public PacketTraceGenerator(double scale, double shape,double _dropProbability,double _reorderProbability,long initialTS,long _startID){
		seed=System.currentTimeMillis();
		weibuller = new Weibull("Delay",scale,shape,System.currentTimeMillis());
		uniformDrop = new eduni.simjava.distributions.Sim_uniform_obj("drop",0,1,System.currentTimeMillis());
		uniformReorder = new eduni.simjava.distributions.Sim_uniform_obj("reorder",0,1,System.currentTimeMillis());
		dropProbability = _dropProbability;
		reorderProbability = _reorderProbability;
		NextTime =initialTS;
		NextID = _startID;
	}
	
	/**
	 * generate the packet trace by the weibull distribution
	 * @return, id, sendTimeStamp,receiveTimeStamp
	 */
	public double[] NextWeibullPacket(){
		if(weibuller==null){System.err.println("Weibull empty");return null;}
		double delay=weibuller.sample();//0-1之间
		//System.out.println("weibull: "+delay);
		double [] value=new double[3];
		//id
		value[0]=NextID++;
		//send time
		value[1]=NextTime;
		double networkDelay=Math.round(delay*weibullMultiplier);
		//delivery time
		value[2]=NextTime+networkDelay;
		NextTime+=NextInterPacketArrivalDelay(networkDelay);  //networkDelay * 1.5
		return value;  //id  发送时间戳  接收时间戳
	}

	/**
	 * 传来的参数是发送时延
	 * @param timeT
	 * @return
	 */
	public double[] NextWeibullPacket(double timeT){
		if(weibuller==null){System.err.println("Weibull empty");return null;}
		double delay=weibuller.sample(); //产生一个韦伯分布的数
		//System.out.println("weibull: "+delay);
		double [] value=new double[3];
		//id
		value[0]=NextID++%Long.MAX_VALUE;
		//send time
		value[1]=Math.abs(timeT);
		double networkDelay=Math.abs(Math.round(delay*weibullMultiplier));
//		while(networkDelay<=1){
//			networkDelay=Math.round(weibuller.sample()*weibullMultiplier);
//		}
		//delivery time
		value[2]=value[1]+networkDelay;
		NextTime=value[1];
		return value;
	}
	
	public double NextWeibullPacketDelay(){
		if(weibuller==null){System.err.println("Weibull empty");return -1;}
		double delay=weibuller.sample();
		double vv=(delay*weibullMultiplier);
		//System.out.println("weibull: "+vv);
		return vv;
	}
	
	/**
	 * 2, good, 0, drop, 1 reorder
	 * @return
	 */
	public int dropOrReorder(){
		
		double p = Math.abs(uniformDrop.sample());
		//System.out.println("p: "+p+"drop: "+dropProbability+", reorder: "+reorderProbability);
		if(p<=dropProbability+reorderProbability){  //<= 0.1(丢) + 0.1(乱)
			if(uniformReorder.sample()<=dropProbability/(dropProbability+reorderProbability)){  //前面判断了已经是故障包（20%概率）  这里是再分为丢包 和 乱序包  一半一半的概率
				return 0; //丢
			}else{
				return 1; //乱
			}
		}else{
			return 2;
		}
	}
	
	/**
	 * inter-Arrival Packet Delay
	 * @return
	 */
	public double NextInterPacketArrivalDelay(double networkDelay){
		return (networkDelay*1.5);
	}
	
	/**
	 * drop packet, uniform distribution
	 * @return
	 */
	public boolean dropPacketOld(){
		if(uniformDrop.sample()<=dropProbability){
			//drop
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * the packet is received by the receiver, but not the sender, at the beginning, ending
	 * @return
	 */
	public boolean reorderPacketOld(){
		if(uniformReorder.sample()<=reorderProbability){
			//reorder
			return true;
		}else{
			return false;
		}
	}
}
