package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Youjizz implements ShareService {

    private final Pattern matcher_URL1 = Pattern.compile("https://www\\.youjizz\\.com/videos/(.+)-(\\d+)\\.html");
    private final Pattern matcher_Title = Pattern.compile("<title>(.+)</title>");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        return null;
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Matcher matcher1 = matcher_URL1.matcher(data.getURL());
        boolean b1 = matcher1.find();

        if (b1){

            Request request = new Request.Builder()
                    .url(data.getURL())
                    .addHeader("User-Agent",Constant.nico_proxy_UserAgent)
                    .build();

            String result = "";

            try {
                Response response = client.newCall(request).execute();
                result = response.body().string();
                response.close();
            } catch (Exception e){
                throw e;
            }

            //System.out.println(result);

            Matcher matcher = matcher_Title.matcher(result);
            if (matcher.find()){
                //System.out.println("a");
                return matcher.group(1);

            }

        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "Youjizz";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
