package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

public class PeerTube implements ShareService {

    private final Pattern matcher_URL = Pattern.compile("https://(.+)/w/(.+)");
    private final Pattern matcher_URL2 = Pattern.compile("https://(.+)/videos/watch/(.+)");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://yajuvideo.org/w/9MBdbufRqShj1JNwgVZdhK
        Matcher matcher1 = matcher_URL.matcher(data.getURL());
        Matcher matcher2 = matcher_URL2.matcher(data.getURL());

        boolean match1 = matcher1.find();
        boolean match2 = matcher2.find();

        if (!match1 && !match2){
            throw new Exception("Not Support URL");
        }

        final String host = match1 ? matcher1.group(1) : matcher2.group(1);
        final String id = match1 ? matcher1.group(2).split("\\?")[0] : matcher2.group(2).split("\\?")[0];
        // https://yajuvideo.org/api/v1/videos/9MBdbufRqShj1JNwgVZdhK

        Request request = new Request.Builder()
                .url("https://" + host + "/api/v1/videos/" + id)
                .header("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request).execute();

        String jsontext = "";
        if (response.body() != null){
            jsontext = response.body().string();
            //System.out.println(jsontext);
        }
        response.close();

        JsonElement json = new Gson().fromJson(jsontext, JsonElement.class);
        JsonArray playlists = json.getAsJsonObject().get("streamingPlaylists").getAsJsonArray();

        long size = -1;
        String VideoURL = "";

        for (JsonElement playlist : playlists) {
            for (JsonElement file : playlist.getAsJsonObject().get("files").getAsJsonArray()) {
                if (size <= file.getAsJsonObject().get("size").getAsLong()){
                    VideoURL = file.getAsJsonObject().get("fileUrl").getAsString();
                    size = file.getAsJsonObject().get("size").getAsLong();
                }
            }
        }

        if (VideoURL.isEmpty()){
            VideoURL = playlists.get(0).getAsJsonObject().get("playlistUrl").getAsString();
        }

        return new ResultVideoData(VideoURL, null, true, false, size == -1, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return getVideo(data);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://yajuvideo.org/w/9MBdbufRqShj1JNwgVZdhK
        Matcher matcher1 = matcher_URL.matcher(data.getURL());
        Matcher matcher2 = matcher_URL2.matcher(data.getURL());

        boolean match1 = matcher1.find();
        boolean match2 = matcher2.find();

        if (!match1 && !match2){
            throw new Exception("Not Support URL");
        }

        final String host = match1 ? matcher1.group(1) : matcher2.group(1);
        final String id = match1 ? matcher1.group(2).split("\\?")[0] : matcher2.group(2).split("\\?")[0];
        // https://yajuvideo.org/api/v1/videos/9MBdbufRqShj1JNwgVZdhK

        Request request = new Request.Builder()
                .url("https://" + host + "/api/v1/videos/" + id)
                .header("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request).execute();

        String jsontext = "";
        if (response.body() != null){
            jsontext = response.body().string();
            //System.out.println(jsontext);
        }
        response.close();
        JsonElement json = new Gson().fromJson(jsontext, JsonElement.class);

        return json.getAsJsonObject().get("name").getAsString();
    }

    @Override
    public String getServiceName() {
        return "PeerTube";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
