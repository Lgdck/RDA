package util.bloom.RDA.test;

import edu.harvard.syrah.sbon.async.Config;

import java.io.*;
import java.util.*;

/**
 * @author lgd
 * @date 2021/12/9 16:58
 */
public class filetest {
    static {

        // All config properties in the file must start with 'HSHer.'

        Config.read("RDA", System
                .getProperty("RDA.config", "config/RDA.cfg"));
    }

    //session
    public static final String SessionFileName=Config
            .getConfigProps().getProperty("SessionFileName", "oU20");

    public static void main(String[] args) throws IOException {

        Scanner sc=new Scanner(new File(SessionFileName));
        FileOutputStream outputStream=new FileOutputStream(new File("convert.txt"));
        List<Map<String,Object>>list=new ArrayList<>();
        while (sc.hasNext()){
            String str=sc.nextLine();
            StringTokenizer st=new StringTokenizer(str);
            int count=0;
            Map<String,Object>map=new HashMap<>();
            while (st.hasMoreElements()){
                String rec = st.nextToken();
                if (count==0){
                    map.put("ts",rec);
                }else if (count==1){
                    map.put("src",rec);
                }else if (count==2){
                    map.put("des",rec);
                }else if (rec.contains("Seq=")){
                    String seq = rec.substring(4);
                    map.put("seq",Long.parseLong(seq));
                }else if (rec.contains("Ack=")){
                    String ack=rec.substring(4);
                    map.put("ack",Long.parseLong(ack));
                }else if (rec.contains("Win=")){
                    String win=rec.substring(4);
                    map.put("win",Long.parseLong(win)); //随便取个当check
                }else if (map.get("direction")==null &&(map.get("src")!=null && map.get("des")!=null)){
                    map.put("direction",map.get("src")+"->"+map.get("des"));
                }
                count++;
            }
            list.add(map);
            System.out.println(list.size());
        }
        outputStream.write("ts            ".getBytes());
        outputStream.write("src                ".getBytes());
        outputStream.write("des             ".getBytes());
        outputStream.write("seq      ".getBytes());
        outputStream.write("ack      ".getBytes());
        outputStream.write("check(win)      ".getBytes());
        outputStream.write("direction         \r\n".getBytes());
        for (Map<String, Object> map : list) {
            StringBuilder sb=new StringBuilder();
            for (int i=0;i<map.keySet().size();i++){
                Object value=null;
                if (i==0)   value=map.get("ts");
                else if (i==1)   value=map.get("src");
                else if (i==2)   value=map.get("des");
                else if (i==3)   value=map.get("seq");
                else if (i==4)   value=map.get("ack");
                else if (i==5)   value=map.get("win");
                else if (i==6)   value=map.get("direction");
                sb.append(value).append("      ");
            }

            byte[] bytes = sb.toString().getBytes();
            outputStream.write(bytes);
            outputStream.write("\r\n".getBytes());
        }
        outputStream.close();;
    }
}
