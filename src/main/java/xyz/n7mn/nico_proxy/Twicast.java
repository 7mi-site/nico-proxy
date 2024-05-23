package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Twicast implements ShareService{

    private final String ClientID;
    private final String ClientSecret;

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final Pattern SupportURL_Live1 = Pattern.compile("https://twitcasting\\.tv/(.+)");
    private final Pattern SupportURL_Live2 = Pattern.compile("https://twitcasting\\.tv/(.+)/movie/(\\d+)");

    private final Pattern matcher_playlistJson = Pattern.compile("data-movie-playlist='(.+)'");

    public Twicast(String ClientID, String ClientSecret){
        this.ClientID = ClientID;
        this.ClientSecret = ClientSecret;
    }

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        return getLive(data);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://twitcasting.tv/twitcasting_jp

        String url = "";
        String jsonText = "";

        Matcher matcher_live = SupportURL_Live1.matcher(data.getURL());
        Matcher matcher_live2 = SupportURL_Live2.matcher(data.getURL());


        String id = "";
        boolean checkUsers = true;

        if (matcher_live2.find()){
            id = matcher_live2.group(2);
            checkUsers = false;
        }

        if (matcher_live.find() && checkUsers){
            String[] split = data.getURL().split("/");
            id = split[split.length - 1];
            if (id.isEmpty()){
                id = split[split.length - 2];
            }
        }

        if (checkUsers){
            Request request = new Request.Builder()
                    .url("https://apiv2.twitcasting.tv/users/"+id)
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Api-Version", "2.0")
                    .addHeader("Authorization", "Basic "+new String(Base64.getEncoder().encode((ClientID+":"+ClientSecret).getBytes(StandardCharsets.UTF_8))))
                    .build();
            Response response = client.newCall(request).execute();
            if (response.body() != null){
                jsonText = response.body().string();
            }
            response.close();

            //System.out.println(jsonText);

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            id = String.valueOf(json.getAsJsonObject().getAsJsonObject("user").get("last_movie_id").getAsInt());
            //System.out.println(id);
        }

        Request request = new Request.Builder()
                .url("https://apiv2.twitcasting.tv/movies/"+id)
                .addHeader("Accept", "application/json")
                .addHeader("X-Api-Version", "2.0")
                .addHeader("Authorization", "Basic "+new String(Base64.getEncoder().encode((ClientID+":"+ClientSecret).getBytes(StandardCharsets.UTF_8))))
                .build();
        Response response = client.newCall(request).execute();
        if (response.body() != null){
            jsonText = response.body().string();
        }
        response.close();

        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

        String htmlText = "";
        try {
            if (json.getAsJsonObject().getAsJsonObject("movie").get("hls_url") != JsonNull.INSTANCE){
                url = json.getAsJsonObject().getAsJsonObject("movie").get("hls_url").getAsString();
            } else {
                if (json.getAsJsonObject().getAsJsonObject("movie").get("is_recorded").toString().equals("true")){

                    Request request2 = new Request.Builder()
                            .url(json.getAsJsonObject().getAsJsonObject("movie").get("link").getAsString())
                            .build();
                    Response response2 = client.newCall(request2).execute();
                    if (response2.body() != null){
                        htmlText = response2.body().string();
                    }
                    response2.close();

                } else {
                    throw new Exception("Is Not Recorded");
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new Exception("Not Found");
        }

        if (!htmlText.isEmpty()){
            Matcher matcher = matcher_playlistJson.matcher(htmlText);
            if (matcher.find()){
                JsonElement json1 = new Gson().fromJson(matcher.group(1), JsonElement.class);
                //System.out.println(json1.toString());
                try {
                    url = json1.getAsJsonObject().get("2").getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("source").get("url").getAsString();
                } catch (Exception e){
                    url = null;
                }
            }
        }


        return new ResultVideoData(url, "", true, false, json.getAsJsonObject().getAsJsonObject("movie").get("is_live").toString().equals("true"), "");
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        String title = "";

        Matcher matcher_live = SupportURL_Live1.matcher(data.getURL());
        Matcher matcher_live2 = SupportURL_Live2.matcher(data.getURL());

        boolean isCheckUser = true;

        String id = "";

        if (matcher_live2.find()){
            id = matcher_live2.group(2);
            isCheckUser = false;
        }

        if (matcher_live.find() && isCheckUser){
            String[] split = data.getURL().split("/");
            id = split[split.length - 1];
            if (id.isEmpty()){
                id = split[split.length - 2];
            }
        }

        if (isCheckUser){
            String jsonText = "";
            Request request = new Request.Builder()
                    .url("https://apiv2.twitcasting.tv/users/"+id)
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Api-Version", "2.0")
                    .addHeader("Authorization", "Basic "+new String(Base64.getEncoder().encode((ClientID+":"+ClientSecret).getBytes(StandardCharsets.UTF_8))))
                    .build();
            Response response = client.newCall(request).execute();
            if (response.body() != null){
                jsonText = response.body().string();
            }
            response.close();

            //System.out.println(jsonText);

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            id = String.valueOf(json.getAsJsonObject().getAsJsonObject("user").get("last_movie_id").getAsInt());
            //System.out.println(id);
        }

        String jsonText = "";
        Request request = new Request.Builder()
                .url("https://apiv2.twitcasting.tv/movies/"+id)
                .addHeader("Accept", "application/json")
                .addHeader("X-Api-Version", "2.0")
                .addHeader("Authorization", "Basic "+new String(Base64.getEncoder().encode((ClientID+":"+ClientSecret).getBytes(StandardCharsets.UTF_8))))
                .build();
        Response response = client.newCall(request).execute();
        if (response.body() != null){
            jsonText = response.body().string();
        }
        response.close();

        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        if (json.getAsJsonObject().getAsJsonObject("movie").get("subtitle") != JsonNull.INSTANCE){
            title = json.getAsJsonObject().getAsJsonObject("movie").get("subtitle").getAsString();
        } else {
            title = json.getAsJsonObject().getAsJsonObject("movie").get("title").getAsString();
        }

        return title;
    }

    @Override
    public String getServiceName() {
        return "ツイキャス";
    }

    @Override
    public String getVersion() {
        return "20240502";
    }
}
