package xyz.n7mn.nico_proxy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Piapro implements ShareService{

    private final Pattern matcher_url1 = Pattern.compile("https://piapro\\.jp/t/(.+)");
    private final Pattern matcher_urlJson = Pattern.compile("\"url\": \"(.+)\",");
    private final Pattern matcher_title = Pattern.compile("<h1 class=\"contents_title\">(.+)</h1>");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        if (!matcher_url1.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://piapro.jp/t/KQn0
        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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

        Matcher matcher = matcher_urlJson.matcher(result);
        if (!matcher.find()){
            throw new Exception("Not Found");
        }

        //System.out.println(matcher.group(1));

        return new ResultVideoData(null, matcher.group(1), false, false, false, null, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        if (!matcher_url1.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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

        Matcher matcher = matcher_title.matcher(result);

        if (matcher.find()){
            return matcher.group(1);
        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "piapro";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
