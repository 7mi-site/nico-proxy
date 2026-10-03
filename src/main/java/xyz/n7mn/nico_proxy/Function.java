package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class Function {

    public static Gson gson = new Gson();

    public static String Version = "2.0.2";

    public static String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:157.0) Gecko/20100101 Firefox/157.0 nico-proxy/"+Version;


    public static byte[] decompressByte(byte[] content, String compressType) throws Exception {
        byte[] body = content;

        if (compressType == null || compressType.isEmpty()){
            return body;
        }

        if (compressType.toLowerCase(Locale.ROOT).equals("gzip")){

            ByteArrayInputStream stream = new ByteArrayInputStream(content);
            GZIPInputStream gis = new GZIPInputStream(stream);
            body = gis.readAllBytes();
            gis.close();
            stream.close();

/*        } else if (compressType.toLowerCase(Locale.ROOT).equals("br")){

            String brotliPath = Function.getBrotliPath();
            String d_file = "./text_d_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt";
            String o_file = "./text_d_"+ UUID.randomUUID().toString()+"_"+new Date().getTime()+".txt.br";

            Runtime runtime = Runtime.getRuntime();
            if (!brotliPath.isEmpty()){

                if (isFoundFile(o_file)){
                    deleteFile(o_file);
                }
                writeFile(o_file, body);

                //final Process exec0 = runtime.exec(new String[]{brotliPath, "-9", "-o", "text.br2", "text.txt"});
                final Process exec0 = runtime.exec(new String[]{brotliPath, "-o" , d_file, "-d" , o_file});
                Thread.ofVirtual().start(() -> {
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    if (exec0.isAlive()) {
                        exec0.destroy();
                    }
                });
                exec0.waitFor();

                if (isFoundFile(d_file)){
                    body = getFileByBinary(d_file);
                }

                deleteFile(o_file);
                deleteFile(d_file);

                //System.out.println(body.length);

            }
*/
        }
        return body;
    }
}
