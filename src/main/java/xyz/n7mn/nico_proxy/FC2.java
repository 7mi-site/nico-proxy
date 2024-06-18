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

public class FC2 implements ShareService {

    private final Pattern matcher_SupportURL1 = Pattern.compile("https://video\\.fc2\\.com/(.+)/content/(.+)");
    private final Pattern matcher_SupportURL2 = Pattern.compile("https://video\\.fc2\\.com/content/(.+)");
    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://video.fc2.com/ja/content/20240504hww75Nv3
        // https://video.fc2.com/content/20240504hww75Nv3
        Matcher matcher1 = matcher_SupportURL1.matcher(data.getURL());
        Matcher matcher2 = matcher_SupportURL2.matcher(data.getURL());

        final String id;
        if (!matcher1.find() && !matcher2.find()){
            throw new Exception("Not Support URL");
        } else if (matcher1.find()){
            id = matcher1.group(2).split("\\?")[0];
        } else{
            id = matcher2.group(1).split("\\?")[0];
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            // https://video.fc2.com/api/v3/videoplaylist/20240504hww75Nv3?sh=1&fs=0
            Request request_html = new Request.Builder()
                    .url("https://video.fc2.com/api/v3/videoplaylist/"+id+"?sh=1&fs=0")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();

        } catch (Exception e) {
            if (data.getProxy() != null) {
                throw new Exception("video.fc2.com " + e.getMessage() + " (Use Proxy : " + data.getProxy().getProxyIP() + ")");
            } else {
                throw new Exception("video.fc2.com " + e.getMessage());
            }
        }

        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        //System.out.println(json);
        JsonElement json1 = json.getAsJsonObject().get("playlist");

        String uri = "";
        if (json1.getAsJsonObject().has("hq")){
            uri = json1.getAsJsonObject().get("hq").getAsString();
        } else if (json1.getAsJsonObject().has("nq")){
            uri = json1.getAsJsonObject().get("nq").getAsString();
        } else if (json1.getAsJsonObject().has("lq")){
            uri = json1.getAsJsonObject().get("lq").getAsString();
        } else {
            throw new Exception("Not Support URL");
        }


        // https://video.fc2.com/api/v3/videoplay/2yISIxC3xx33S2yFCF8FH-2Sa3x-3-y-S74TFjah/2?signature=WTI_11JRLSPRRN2WMPCLMVENL-0CC.AZJIX--6PJM403FQMS5P4F-8T&t=1718705999&0ks34t=0&pf=Win32&lg=ja&referrer=


        return new ResultVideoData("https://video.fc2.com"+uri, null, true,false,false,null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        return "";
    }

    @Override
    public String getServiceName() {
        return "FC2動画";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
