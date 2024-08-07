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

public class SoundCloud implements ShareService{

    private final Pattern SupportURL_1 = Pattern.compile("https://soundcloud\\.com/");

    //private final Pattern appVersion = Pattern.compile("window\\.__sc_version=\"(\\d+)\"");
    private final Pattern clientId = Pattern.compile("client_application_id:(\\d+),client_id:\"(.+)\",env:\"production\"");
    private final Pattern jsonData = Pattern.compile("window\\.__sc_hydration = \\[(.+)]");
    private final Pattern secretTokenCheck = Pattern.compile("secret_token");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://soundcloud.com/baron1_3/penguin3rd

        if (!SupportURL_1.matcher(data.getURL()).find() && data.getURL().split("/").length != 5){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        final String ClientID; // = "ICQyLasUBASlFk0tLJ8FRmTTFc11FVBZ";

        Request request0 = new Request.Builder()
                .url("https://a-v2.sndcdn.com/assets/49-d6ecabd7.js")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        String result = "";

        try {
            Response response = client.newCall(request0).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        Matcher matcher1 = clientId.matcher(result);
        if (matcher1.find()){
            ClientID = matcher1.group(2);
        } else {
            ClientID = "";
        }


        Request request1 = new Request.Builder()
                .url("https://api-auth.soundcloud.com/oauth/session?client_id="+ClientID)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();


        try {
            Response response = client.newCall(request1).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(result);

        final Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        try {
            Response response = client.newCall(request).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(result);

        final Matcher matcher = jsonData.matcher(result);
        //System.out.println(matcher.find());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }

        //System.out.println("[" + matcher.group(1) + "]");
        final JsonElement json = new Gson().fromJson("[" + matcher.group(1) + "]", JsonElement.class);
        //System.out.println(json);

        int x = 0;
        for (int i = 0; i < json.getAsJsonArray().size(); i++){
            if (json.getAsJsonArray().get(i).getAsJsonObject().has("hydratable") && json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                x = i;
                break;
            }
        }

        final String mediaUrl = json.getAsJsonArray().get(x).getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
        final String track_authorization = json.getAsJsonArray().get(x).getAsJsonObject().get("data").getAsJsonObject().get("track_authorization").getAsString();

        //System.out.println(mediaUrl + "?client_id="+ClientID+"&track_authorization=" + track_authorization);

        // ?secret_token=
        final Request request2;

        if (!secretTokenCheck.matcher(mediaUrl).find()){
            request2 = new Request.Builder()
                    .url(mediaUrl + "?client_id="+ClientID+"&track_authorization=" + track_authorization)
                    .addHeader("x-datadome-clientid", "wlEz39mbH4i1EF83K8NNXGltwpzBmZcgvUwIRgftwHVYUYZHqrMu52LCm3NAh2z0A09o23wktFo32M00R3~G1_58MV~H9d2G1irVg5j7LoiAnAD6EqVnngwcwBNUbzT3")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
        } else {
            request2 = new Request.Builder()
                    .url(mediaUrl + "&client_id="+ClientID+"&track_authorization=" + track_authorization)
                    .addHeader("x-datadome-clientid", "wlEz39mbH4i1EF83K8NNXGltwpzBmZcgvUwIRgftwHVYUYZHqrMu52LCm3NAh2z0A09o23wktFo32M00R3~G1_58MV~H9d2G1irVg5j7LoiAnAD6EqVnngwcwBNUbzT3")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
        }

        result = "";
        try {
            Response response = client.newCall(request2).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(result);
        JsonElement json1 = new Gson().fromJson(result, JsonElement.class);

        return new ResultVideoData(null, json1.getAsJsonObject().get("url").getAsString(), true, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        // https://soundcloud.com/baron1_3/penguin3rd

        if (!SupportURL_1.matcher(data.getURL()).find() && data.getURL().split("/").length != 5){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final Request request = new Request.Builder()
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

        final Matcher matcher = jsonData.matcher(result);
        //System.out.println(matcher.find());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }

        final JsonElement json = new Gson().fromJson("[" + matcher.group(1) + "]", JsonElement.class);
        return json.getAsJsonArray().get(7).getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
    }

    @Override
    public String getServiceName() {
        return "SoundCloud";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
