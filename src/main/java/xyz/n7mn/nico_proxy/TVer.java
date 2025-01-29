package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer implements ShareService{

    private final Pattern Support_URLVideo1 = Pattern.compile("https://tver\\.jp/episodes/(.+)");
    private final Pattern Support_URLLive1 = Pattern.compile("https://tver\\.jp/live/(.+)");
    private final Pattern Support_URLLive2 = Pattern.compile("https://tver\\.jp/live/simul/(.+)");
    private final Pattern Support_URLLive3 = Pattern.compile("https://tver.jp/live/special/(.+)");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://tver.jp/episodes/epq882oemn
        Matcher matcher = Support_URLVideo1.matcher(data.getURL().split("\\?")[0]);

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String id = matcher.group(1);

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Request request1 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/public/current_time")
                .build();

        Response response1 = client.newCall(request1).execute();
        response1.close();

        String jsonText = "";

        Request request2 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/platform_users/info?platform_uid=b7cd9e1b8ae94ed3865233c74ccfcf5d47f5&platform_token=xxyjo2stlpqv5a93plut3u4bcr08fwcv395t454c")
                .build();

        Response response2 = client.newCall(request2).execute();
        if (response2.body() != null){
            jsonText = response2.body().string();
        }
        response2.close();

        //System.out.println(jsonText);

        String videoRefID = "";
        String accountID = "";
        Request request3 = new Request.Builder()
                .url("https://statics.tver.jp/content/episode/"+id+".json?v=13")
                .build();
        Response response3 = client.newCall(request3).execute();
        if (response3.body() != null){
            jsonText = response3.body().string();
        }
        response3.close();
        //System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

        videoRefID = json.getAsJsonObject().getAsJsonObject("video").get("videoRefID") != null ? json.getAsJsonObject().getAsJsonObject("video").get("videoRefID").getAsString() : "";
        accountID = json.getAsJsonObject().getAsJsonObject("video").get("accountID").getAsString();
        String videoID = json.getAsJsonObject().getAsJsonObject("video").get("videoID") != null ? json.getAsJsonObject().getAsJsonObject("video").get("videoID").getAsString() : "";


        Request request4 = new Request.Builder()
                .url("http://players.brightcove.net/"+accountID+"/default_default/config.json")
                .build();

        Response response4 = client.newCall(request4).execute();
        if (response4.body() != null){
            jsonText = response4.body().string();
        }
        response4.close();
        //System.out.println(jsonText);

        json = new Gson().fromJson(jsonText, JsonElement.class);
        String policy_key = json.getAsJsonObject().getAsJsonObject("video_cloud").get("policy_key").getAsString();


//
        Request request5;
        if (!videoRefID.isEmpty()){
            request5 = new Request.Builder()
                    .url("https://edge.api.brightcove.com/playback/v1/accounts/"+accountID+"/videos/ref%3A"+videoRefID)
                    .addHeader("Accept", "application/json;pk="+policy_key)
                    .build();
        } else {
            request5 = new Request.Builder()
                    .url("https://edge.api.brightcove.com/playback/v1/accounts/"+accountID+"/videos/"+videoID+"?config_id=f0876aa7-0bab-4049-ab23-1b2001ff7c79")
                    .addHeader("Accept", "application/json;pk="+policy_key)
                    .build();
        }

        Response response5 = client.newCall(request5).execute();
        if (response5.body() != null){
            jsonText = response5.body().string();
        }
        response5.close();
        //System.out.println(jsonText);
        json = new Gson().fromJson(jsonText, JsonElement.class);

        String url = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("src").getAsString();
        return new ResultVideoData(url, null, true, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        Matcher matcher = Support_URLLive1.matcher(data.getURL());
        Matcher matcher2 = Support_URLLive2.matcher(data.getURL());
        Matcher matcher3 = Support_URLLive3.matcher(data.getURL());

        boolean a = matcher.find();
        boolean b = matcher2.find();
        boolean c = matcher3.find();
        if (!a && !b && !c){
            throw new Exception("Not Support URL");
        }

        String id = a && !b ? matcher.group(1) : matcher2.group(1);
        if (c){
            id = matcher3.group(1);
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        long currentTime = 0;
        Request request1 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/public/current_time")
                .build();

        Response response1 = client.newCall(request1).execute();
        if (response1.body() != null){
            JsonElement json = new Gson().fromJson(response1.body().string(), JsonElement.class);
            currentTime = json.getAsJsonObject().getAsJsonObject("result").get("epoch").getAsLong();
        }
        response1.close();

        Request request2 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/platform_users/info?platform_uid=b7cd9e1b8ae94ed3865233c74ccfcf5d47f5&platform_token=xxyjo2stlpqv5a93plut3u4bcr08fwcv395t454c")
                .build();

        Response response2 = client.newCall(request2).execute();
        response2.close();

        String jsonText = "";

        String url = "";
        String sessionId = "";
        String jsonSendID = "";
        String liveID = "";

        if (a && !b && !c){

            Request request3 = new Request.Builder()
                    .url("https://service-api.tver.jp/api/v1/callLiveTimeline/"+id)
                    .addHeader("x-tver-platform-type","web")
                    .build();

            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();
            //System.out.println(jsonText);

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            for (JsonElement element : json.getAsJsonObject().getAsJsonObject("result").getAsJsonArray("contents")) {
                long start = element.getAsJsonObject().getAsJsonObject("content").get("startAt").getAsLong();
                long end = element.getAsJsonObject().getAsJsonObject("content").get("endAt").getAsLong();
                if (start <= currentTime && end >= currentTime){
                    liveID = element.getAsJsonObject().getAsJsonObject("content").get("id").getAsString();
                    break;
                }
            }


            Request request4 = new Request.Builder()
                    .url("https://playback.api.streaks.jp/v1/projects/tver-simul-"+id+"/medias/ref:simul-"+id)
                    .addHeader("X-Streaks-Api-Key", id)
                    .build();

            Response response4 = client.newCall(request4).execute();
            if (response4.body() != null){
                jsonText = response4.body().string();
            }
            response4.close();

            //System.out.println(jsonText);
            json = new Gson().fromJson(jsonText, JsonElement.class);

            sessionId = json.getAsJsonObject().get("id").getAsString();
            jsonSendID = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("id").getAsString();
            // このままでは見れないが仮置き
            url = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("src").getAsString();

        }

        if (b && !c){
            liveID = id;
            Request request5 = new Request.Builder()
                    .url("https://statics.tver.jp/content/live/"+id+".json?v=7")
                    .build();

            Response response5 = client.newCall(request5).execute();
            if (response5.body() != null){
                jsonText = response5.body().string();
            }
            response5.close();
            //System.out.println(jsonText);
            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

            Request request5_2 = new Request.Builder()
                    .url("https://playback.api.streaks.jp/v1/projects/"+json.getAsJsonObject().getAsJsonObject("liveVideo").get("projectID").getAsString()+"/medias/"+json.getAsJsonObject().getAsJsonObject("liveVideo").get("mediaID").getAsString())
                    .addHeader("X-Streaks-Api-Key", json.getAsJsonObject().getAsJsonObject("liveVideo").get("apiKey").getAsString())
                    .build();

            Response response5_2 = client.newCall(request5_2).execute();
            if (response5_2.body() != null){
                jsonText = response5_2.body().string();
            }
            response5_2.close();
            //System.out.println(jsonText);
            json = new Gson().fromJson(jsonText, JsonElement.class);

            sessionId = json.getAsJsonObject().get("id").getAsString();
            jsonSendID = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("id").getAsString();
            // このままでは見れないが仮置き
            url = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("src").getAsString();
        }

        if (c){
            liveID = id;
            Request request6 = new Request.Builder()
                    .url("https://statics.tver.jp/content/live/"+id+".json?v=8")
                    .build();

            Response response6 = client.newCall(request6).execute();
            if (response6.body() != null){
                jsonText = response6.body().string();
            }

            response6.close();
            //System.out.println(jsonText);

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

            Request request6_2 = new Request.Builder()
                    .url("https://playback.api.streaks.jp/v1/projects/tver-splive/medias/ref:"+id)
                    .addHeader("X-Streaks-Api-Key", json.getAsJsonObject().getAsJsonObject("liveVideo").get("apiKey").getAsString())
                    .build();

            Response response6_2 = client.newCall(request6_2).execute();
            if (response6_2.body() != null){
                jsonText = response6_2.body().string();
            }
            response6_2.close();
            //System.out.println(jsonText);

            json = new Gson().fromJson(jsonText, JsonElement.class);
            if (!json.getAsJsonObject().has("sources") && json.getAsJsonObject().get("status").getAsInt() == 404){
                throw new Exception("Not Found");
            }

            sessionId = json.getAsJsonObject().get("id").getAsString();
            jsonSendID = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("id").getAsString();
            url = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("src").getAsString();

        }

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");

        String text = "{\n" +
                "    \"ads_params\": {\n" +
                "        \"tvcu_pcode\": \"09\",\n" +
                "        \"tvcu_ccode\": \"09201\",\n" +
                "        \"tvcu_zcode\": \"3208501\",\n" +
                "        \"tvcu_gender\": \"m\",\n" +
                "        \"tvcu_gender_code\": \"1\",\n" +
                "        \"tvcu_age\": 30,\n" +
                "        \"tvcu_agegrp\": 3,\n" +
                "        \"delivery_type\": \"simul\",\n" +
                "        \"is_dvr\": 0,\n" +
                "        \"rdid\": \"\",\n" +
                "        \"idtype\": \"\",\n" +
                "        \"is_lat\": \"\",\n" +
                "        \"bundle\": \"\",\n" +
                "        \"iuid\": \"my5821gsnc0114482616\",\n" +
                "        \"interest\": \"\",\n" +
                "        \"video_id\": \""+liveID+"\",\n" +
                "        \"device\": \"pc\",\n" +
                "        \"device_code\": \"0001\",\n" +
                "        \"tag_type\": \"browser\",\n" +
                "        \"item_eventid\": \"62033\",\n" +
                "        \"item_programkey\": \"00005\",\n" +
                "        \"item_category\": \"99\",\n" +
                "        \"item_episodecode\": \"d5bb7aba-4602-4707-8ccf-8638ecdd36ce\",\n" +
                "        \"item_originalmeta1\": \"\",\n" +
                "        \"item_originalmeta2\": \"\",\n" +
                "        \"ntv_ppid\": \"z75i3v2wb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"tbs_ppid\": \"f87wu4inb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"tx_ppid\": \"t87wrus6b7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"ex_ppid\": \"n6dsf79vb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"cx_ppid_gam\": \"b8a35iwjb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"mbs_ppid_gam\": \"x32ck84sb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"abc_ppid\": \"c2fq84emb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"tvo_ppid\": \"i3wtqjeyb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"ktv_ppid\": \"g9byn7reb7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"ytv_ppid\": \"g8kusm76b7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"vr_uuid\": \"AB4AC57F-4DA9-420F-855E-2FC497141B59\",\n" +
                "        \"personalIsLat\": \"0\",\n" +
                "        \"platformAdUid\": \"6bfe71c2-0b12-4529-b5dc-207fb644031c\",\n" +
                "        \"platformUid\": \"b7cd9e1b8ae94ed3865233c74ccfcf5d47f5\",\n" +
                "        \"memberId\": \"\",\n" +
                "        \"c\": \"simul\",\n" +
                "        \"luid\": \"AB4AC57F-4DA9-420F-855E-2FC497141B59\"\n" +
                "    },\n" +
                "    \"id\": \""+jsonSendID+"\"\n" +
                "}";

        Request request6 = new Request.Builder()
                .url("https://ssai.api.streaks.jp/v1/projects/tver-simul-ntv/medias/"+sessionId+"/ssai/session")
                .post(RequestBody.create(text, mediaType))
                .build();

        Response response6 = client.newCall(request6).execute();
        if (response6.body() != null){
            jsonText = response6.body().string();
        }
        response6.close();
        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        if (!json.isJsonArray() && json.getAsJsonObject().has("message")){
            if (!json.getAsJsonObject().get("message").getAsString().equals("Config Not Found")){
                url = url + "&" + json.getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString();
            }
        } else {
            url = url + "&" + json.getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString();
        }



        return new ResultVideoData(url, null, true, false, true, null);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        Matcher matcher_video = Support_URLVideo1.matcher(data.getURL());
        Matcher matcher_live1 = Support_URLLive1.matcher(data.getURL());
        Matcher matcher_live2 = Support_URLLive2.matcher(data.getURL());
        Matcher matcher_live3 = Support_URLLive3.matcher(data.getURL());

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        boolean isVideo = matcher_video.find();
        boolean isLive1 = matcher_live1.find();
        boolean isLive2 = matcher_live2.find();
        boolean isLive3 = matcher_live3.find();

        String id = "";
        if (isVideo){
            id = matcher_video.group(1);
        }

        if (isLive1 && !isLive2 && !isLive3){
            id = matcher_live1.group(1);
        }

        if (isLive2 && !isLive3){
            id = matcher_live2.group(1);
        }

        if (isLive3){
            id = matcher_live2.group(1);
        }

        long currentTime = 0L;
        Request request1 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/public/current_time")
                .build();

        Response response1 = client.newCall(request1).execute();
        if (response1.body() != null){
            JsonElement json = new Gson().fromJson(response1.body().string(), JsonElement.class);
            currentTime = json.getAsJsonObject().getAsJsonObject("result").get("epoch").getAsLong();
        }
        response1.close();

        if (isVideo){

            Request request2 = new Request.Builder()
                    .url("https://platform-api.tver.jp/v2/api/platform_users/info?platform_uid=b7cd9e1b8ae94ed3865233c74ccfcf5d47f5&platform_token=xxyjo2stlpqv5a93plut3u4bcr08fwcv395t454c")
                    .build();

            String jsonText = "";
            Response response2 = client.newCall(request2).execute();
            response2.close();

            Request request3 = new Request.Builder()
                    .url("https://statics.tver.jp/content/episode/"+id+".json?v=13")
                    .build();
            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            //System.out.println(json);
            return json.getAsJsonObject().get("title").getAsString();
        }

        if (isLive1 && !isLive2){
            String jsonText = "";
            String title = "";
            Request request3 = new Request.Builder()
                    .url("https://service-api.tver.jp/api/v1/callLiveTimeline/"+id)
                    .addHeader("x-tver-platform-type","web")
                    .build();

            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();
            //System.out.println(jsonText);

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            for (JsonElement element : json.getAsJsonObject().getAsJsonObject("result").getAsJsonArray("contents")) {
                long start = element.getAsJsonObject().getAsJsonObject("content").get("startAt").getAsLong();
                long end = element.getAsJsonObject().getAsJsonObject("content").get("endAt").getAsLong();
                if (start <= currentTime && end >= currentTime){
                    if (element.getAsJsonObject().getAsJsonObject("content").get("title") == null || element.getAsJsonObject().getAsJsonObject("content").get("title").getAsString().equals(" ")){
                        title = element.getAsJsonObject().getAsJsonObject("content").get("seriesTitle").getAsString();
                    } else {
                        title = element.getAsJsonObject().getAsJsonObject("content").get("title").getAsString();
                    }
                    break;
                }
            }

            //System.out.println(title);
            return title;
        }

        if (isLive2){
            String jsonText = "";
            Request request4 = new Request.Builder()
                    .url("https://statics.tver.jp/content/live/"+id+".json?v=7")
                    .build();

            Response response4 = client.newCall(request4).execute();
            if (response4.body() != null){
                jsonText = response4.body().string();
            }
            response4.close();
            //System.out.println(jsonText);
            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

            return json.getAsJsonObject().get("title").getAsString();
        }

        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getVersion() {
        return "20240502";
    }
}
