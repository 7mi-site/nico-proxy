package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;
import xyz.n7mn.nico_proxy.twitter.variants;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Pattern;


public class Twitter implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        String url = data.getURL();

        String[] split = url.split("/");
        int i = 0;
        for (String str : split) {
            if (str.startsWith("status")) {
                break;
            }
            i++;
        }

        final String id = split[i + 1].split("\\?")[0];

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (id.length() == 0){
            throw new Exception("Tweet NotFound");
        }

        String HtmlText = "";

        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create("", JSON);
        Request getGuest = new Request.Builder()
                .url("https://api.twitter.com/1.1/guest/activate.json")
                .header("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                .post(body)
                .build();
        Response response1 = client.newCall(getGuest).execute();
        //System.out.println(response.code());
        if (response1.body() != null){
            HtmlText = response1.body().string();
        }

        //System.out.println(HtmlText);
        JsonElement guestTokenJson = new Gson().fromJson(HtmlText, JsonElement.class);
        String token = guestTokenJson.getAsJsonObject().get("guest_token").getAsString();

        try {

            Request build = new Request.Builder()
                    .url("https://twitter.com/i/api/graphql/mbnjGF4gOwo5gyp9pe5s4A/TweetResultByRestId?variables=%7B%22tweetId%22%3A%22"+id+"%22%2C%22withCommunity%22%3Afalse%2C%22includePromotedContent%22%3Afalse%2C%22withVoice%22%3Afalse%7D&features=%7B%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22responsive_web_home_pinned_timelines_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D")
                    .header("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                    .header("X-Client-Transaction-Id", "ozoi3NmSR6Q+mm10a6SD6Ip273gbYKGGhsUVW72QUk6s1chfi1qeS14PIS0fkt/XDlZCcaNY2e8U09ILFtFf++WNxmR3og")
                    .header("x-guest-token", token)
                    .header("x-twitter-active-user", "yes")
                    .header("x-twitter-client-language","ja")
                    .header("cookie", "guest_id_marketing=v1%3A169674475483006773; guest_id_ads=v1%3A169674475483006773; personalization_id=\"v1_IN9WfC2t02KPvj8nTdejeQ==\"; guest_id=v1%3A169674475483006773; gt="+token)
                    .build();

            Response response = client.newCall(build).execute();
            //System.out.println(response.code());
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception("APIError : " + e.getMessage());
        }

        //System.out.println(HtmlText);
        //return null;
///*
        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        try {
            JsonElement element = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("tweetResult").getAsJsonObject("result")
                                      .getAsJsonObject("legacy").getAsJsonObject("entities").getAsJsonArray("media").get(0).getAsJsonObject().getAsJsonObject("video_info").getAsJsonArray("variants");

            String tempJson = element.toString();

            variants[] variants = new Gson().fromJson(tempJson, variants[].class);

            int maxCount = -1;
            int maxBitrate = -1;

            int count = 0;
            for (variants var : variants){
                if (var.getBitrate() >= maxBitrate && var.getBitrate() >= 0){
                    maxCount = count;
                    maxBitrate = var.getBitrate();
                }
                //if (var.getBitrate() == 0 && var.getContent_type().equals("application/x-mpegURL")){
                //    maxCount = count;
                //    break;
                //}
                count++;
            }

            return new ResultVideoData(variants[maxCount].getUrl(),"", false, false, false, "");
        } catch (Exception e){
            throw new Exception("Tweet is Not VideoTweet");
        }
 //*/
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 実装予定はなし
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        return "";
    }

    @Override
    public String getServiceName() {
        return "Twitter";
    }

    @Override
    public String getVersion() {
        return "20231008";
    }
}
