package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mixcloud implements ShareService{

    private final Pattern matcher_URL = Pattern.compile("https://www\\.mixcloud\\.com/(.+)/(.+)/");


    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Matcher matcher = matcher_URL.matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        final String post = "{\"id\":\"PlayerHeroQuery\",\"query\":\"query PlayerHeroQuery(\\n  $lookup: CloudcastLookup!\\n) {\\n  cloudcast: cloudcastLookup(lookup: $lookup) {\\n    id\\n    name\\n    picture {\\n      isLight\\n      primaryColor\\n      darkPrimaryColor: primaryColor(darken: 60)\\n    }\\n    restrictedReason\\n    seekRestriction\\n    ...HeaderActions_cloudcast\\n    ...PlayButton_cloudcast\\n    ...CloudcastBaseAutoPlayComponent_cloudcast\\n    ...HeroWaveform_cloudcast\\n    ...RepeatPlayUpsellBar_cloudcast\\n    ...HeroAudioMeta_cloudcast\\n    ...HeroChips_cloudcast\\n    ...ImageCloudcast_cloudcast\\n  }\\n  viewer {\\n    restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n    hasRepeatPlayFeature: featureIsActive(switch: \\\"repeat_play\\\")\\n    ...HeroWaveform_viewer\\n    ...HeroAudioMeta_viewer\\n    ...HeaderActions_viewer\\n    id\\n  }\\n}\\n\\nfragment AddToButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n}\\n\\nfragment CloudcastBaseAutoPlayComponent_cloudcast on Cloudcast {\\n  id\\n  streamInfo {\\n    uuid\\n    url\\n    hlsUrl\\n    dashUrl\\n  }\\n  audioLength\\n  seekRestriction\\n  currentPosition\\n}\\n\\nfragment FavoriteButton_cloudcast on Cloudcast {\\n  id\\n  isFavorited\\n  isPublic\\n  slug\\n  owner {\\n    id\\n    isFollowing\\n    username\\n    isSelect\\n    displayName\\n    isViewer\\n  }\\n}\\n\\nfragment FavoriteButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment HeaderActions_cloudcast on Cloudcast {\\n  ...FavoriteButton_cloudcast\\n  ...AddToButton_cloudcast\\n  ...RepostButton_cloudcast\\n  ...MoreMenu_cloudcast\\n  ...ShareButton_cloudcast\\n}\\n\\nfragment HeaderActions_viewer on Viewer {\\n  ...FavoriteButton_viewer\\n  ...RepostButton_viewer\\n  ...MoreMenu_viewer\\n}\\n\\nfragment HeroAudioMeta_cloudcast on Cloudcast {\\n  slug\\n  plays\\n  publishDate\\n  qualityScore\\n  listenerMinutes\\n  owner {\\n    username\\n    id\\n  }\\n  favorites {\\n    totalCount\\n  }\\n  reposts {\\n    totalCount\\n  }\\n  hiddenStats\\n}\\n\\nfragment HeroAudioMeta_viewer on Viewer {\\n  me {\\n    isStaff\\n    id\\n  }\\n}\\n\\nfragment HeroChips_cloudcast on Cloudcast {\\n  isUnlisted\\n  audioType\\n  isExclusive\\n  audioQuality\\n  owner {\\n    isViewer\\n    id\\n  }\\n  restrictedReason\\n  isAwaitingAudio\\n  isDisabledCopyright\\n}\\n\\nfragment HeroWaveform_cloudcast on Cloudcast {\\n  id\\n  audioType\\n  waveformUrl\\n  previewUrl\\n  audioLength\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  restrictedReason\\n  seekRestriction\\n  currentPosition\\n  ...SeekWarning_cloudcast\\n}\\n\\nfragment HeroWaveform_viewer on Viewer {\\n  restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n}\\n\\nfragment ImageCloudcast_cloudcast on Cloudcast {\\n  name\\n  picture {\\n    urlRoot\\n    primaryColor\\n  }\\n}\\n\\nfragment MoreMenu_cloudcast on Cloudcast {\\n  id\\n  isSpam\\n  owner {\\n    isViewer\\n    id\\n  }\\n}\\n\\nfragment MoreMenu_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment PlayButton_cloudcast on Cloudcast {\\n  restrictedReason\\n  owner {\\n    isSubscribedTo\\n    isViewer\\n    id\\n  }\\n  id\\n  isAwaitingAudio\\n  isDraft\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  audioLength\\n  currentPosition\\n  proportionListened\\n  repeatPlayAmount\\n  hasPlayCompleted\\n  seekRestriction\\n  previewUrl\\n  isExclusive\\n  ...StaticPlayButton_cloudcast\\n  ...useAudioPreview_cloudcast\\n  ...useExclusivePreviewModal_cloudcast\\n  ...useExclusiveCloudcastModal_cloudcast\\n}\\n\\nfragment RepeatPlayUpsellBar_cloudcast on Cloudcast {\\n  audioType\\n  owner {\\n    username\\n    displayName\\n    isSelect\\n    id\\n  }\\n}\\n\\nfragment RepostButton_cloudcast on Cloudcast {\\n  id\\n  isReposted\\n  isExclusive\\n  isPublic\\n  reposts {\\n    totalCount\\n  }\\n  owner {\\n    isViewer\\n    isSubscribedTo\\n    id\\n  }\\n}\\n\\nfragment RepostButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment SeekWarning_cloudcast on Cloudcast {\\n  owner {\\n    displayName\\n    isSelect\\n    username\\n    id\\n  }\\n  seekRestriction\\n}\\n\\nfragment ShareButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n  slug\\n  description\\n  audioType\\n  picture {\\n    urlRoot\\n  }\\n  owner {\\n    displayName\\n    isViewer\\n    username\\n    id\\n  }\\n}\\n\\nfragment StaticPlayButton_cloudcast on Cloudcast {\\n  owner {\\n    username\\n    id\\n  }\\n  slug\\n  isAwaitingAudio\\n  restrictedReason\\n}\\n\\nfragment useAudioPreview_cloudcast on Cloudcast {\\n  id\\n  previewUrl\\n}\\n\\nfragment useExclusiveCloudcastModal_cloudcast on Cloudcast {\\n  id\\n  isExclusive\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\\nfragment useExclusivePreviewModal_cloudcast on Cloudcast {\\n  id\\n  isExclusivePreviewOnly\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\",\"variables\":{\"lookup\":{\"username\":\""+matcher.group(1)+"\",\"slug\":\""+matcher.group(2)+"\"}}}";

        final RequestBody body = RequestBody.create(post, JSON);
        Request request = new Request.Builder()
                .url("https://app.mixcloud.com/graphql")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .post(body)
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
        JsonElement json = new Gson().fromJson(result, JsonElement.class);


        StringBuilder result1 = new StringBuilder();
        final String key = "IFYOUWANTTHEARTISTSTOGETPAIDDONOTDOWNLOADFROMMIXCLOUD";
        byte[] ciphertext = Base64.getDecoder().decode(json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("streamInfo").getAsJsonObject().get("dashUrl").getAsString());
        int keyLength = key.length();

        for (int i = 0; i < ciphertext.length; i++) {
            result1.append((char) (ciphertext[i] ^ key.charAt(i % keyLength)));
        }

        return new ResultVideoData(null, result1.toString(), false, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Matcher matcher = matcher_URL.matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        final String post = "{\"id\":\"PlayerHeroQuery\",\"query\":\"query PlayerHeroQuery(\\n  $lookup: CloudcastLookup!\\n) {\\n  cloudcast: cloudcastLookup(lookup: $lookup) {\\n    id\\n    name\\n    picture {\\n      isLight\\n      primaryColor\\n      darkPrimaryColor: primaryColor(darken: 60)\\n    }\\n    restrictedReason\\n    seekRestriction\\n    ...HeaderActions_cloudcast\\n    ...PlayButton_cloudcast\\n    ...CloudcastBaseAutoPlayComponent_cloudcast\\n    ...HeroWaveform_cloudcast\\n    ...RepeatPlayUpsellBar_cloudcast\\n    ...HeroAudioMeta_cloudcast\\n    ...HeroChips_cloudcast\\n    ...ImageCloudcast_cloudcast\\n  }\\n  viewer {\\n    restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n    hasRepeatPlayFeature: featureIsActive(switch: \\\"repeat_play\\\")\\n    ...HeroWaveform_viewer\\n    ...HeroAudioMeta_viewer\\n    ...HeaderActions_viewer\\n    id\\n  }\\n}\\n\\nfragment AddToButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n}\\n\\nfragment CloudcastBaseAutoPlayComponent_cloudcast on Cloudcast {\\n  id\\n  streamInfo {\\n    uuid\\n    url\\n    hlsUrl\\n    dashUrl\\n  }\\n  audioLength\\n  seekRestriction\\n  currentPosition\\n}\\n\\nfragment FavoriteButton_cloudcast on Cloudcast {\\n  id\\n  isFavorited\\n  isPublic\\n  slug\\n  owner {\\n    id\\n    isFollowing\\n    username\\n    isSelect\\n    displayName\\n    isViewer\\n  }\\n}\\n\\nfragment FavoriteButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment HeaderActions_cloudcast on Cloudcast {\\n  ...FavoriteButton_cloudcast\\n  ...AddToButton_cloudcast\\n  ...RepostButton_cloudcast\\n  ...MoreMenu_cloudcast\\n  ...ShareButton_cloudcast\\n}\\n\\nfragment HeaderActions_viewer on Viewer {\\n  ...FavoriteButton_viewer\\n  ...RepostButton_viewer\\n  ...MoreMenu_viewer\\n}\\n\\nfragment HeroAudioMeta_cloudcast on Cloudcast {\\n  slug\\n  plays\\n  publishDate\\n  qualityScore\\n  listenerMinutes\\n  owner {\\n    username\\n    id\\n  }\\n  favorites {\\n    totalCount\\n  }\\n  reposts {\\n    totalCount\\n  }\\n  hiddenStats\\n}\\n\\nfragment HeroAudioMeta_viewer on Viewer {\\n  me {\\n    isStaff\\n    id\\n  }\\n}\\n\\nfragment HeroChips_cloudcast on Cloudcast {\\n  isUnlisted\\n  audioType\\n  isExclusive\\n  audioQuality\\n  owner {\\n    isViewer\\n    id\\n  }\\n  restrictedReason\\n  isAwaitingAudio\\n  isDisabledCopyright\\n}\\n\\nfragment HeroWaveform_cloudcast on Cloudcast {\\n  id\\n  audioType\\n  waveformUrl\\n  previewUrl\\n  audioLength\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  restrictedReason\\n  seekRestriction\\n  currentPosition\\n  ...SeekWarning_cloudcast\\n}\\n\\nfragment HeroWaveform_viewer on Viewer {\\n  restrictedPlayer: featureIsActive(switch: \\\"restricted_player\\\")\\n}\\n\\nfragment ImageCloudcast_cloudcast on Cloudcast {\\n  name\\n  picture {\\n    urlRoot\\n    primaryColor\\n  }\\n}\\n\\nfragment MoreMenu_cloudcast on Cloudcast {\\n  id\\n  isSpam\\n  owner {\\n    isViewer\\n    id\\n  }\\n}\\n\\nfragment MoreMenu_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment PlayButton_cloudcast on Cloudcast {\\n  restrictedReason\\n  owner {\\n    isSubscribedTo\\n    isViewer\\n    id\\n  }\\n  id\\n  isAwaitingAudio\\n  isDraft\\n  isPlayable\\n  streamInfo {\\n    hlsUrl\\n    dashUrl\\n    url\\n    uuid\\n  }\\n  audioLength\\n  currentPosition\\n  proportionListened\\n  repeatPlayAmount\\n  hasPlayCompleted\\n  seekRestriction\\n  previewUrl\\n  isExclusive\\n  ...StaticPlayButton_cloudcast\\n  ...useAudioPreview_cloudcast\\n  ...useExclusivePreviewModal_cloudcast\\n  ...useExclusiveCloudcastModal_cloudcast\\n}\\n\\nfragment RepeatPlayUpsellBar_cloudcast on Cloudcast {\\n  audioType\\n  owner {\\n    username\\n    displayName\\n    isSelect\\n    id\\n  }\\n}\\n\\nfragment RepostButton_cloudcast on Cloudcast {\\n  id\\n  isReposted\\n  isExclusive\\n  isPublic\\n  reposts {\\n    totalCount\\n  }\\n  owner {\\n    isViewer\\n    isSubscribedTo\\n    id\\n  }\\n}\\n\\nfragment RepostButton_viewer on Viewer {\\n  me {\\n    id\\n  }\\n}\\n\\nfragment SeekWarning_cloudcast on Cloudcast {\\n  owner {\\n    displayName\\n    isSelect\\n    username\\n    id\\n  }\\n  seekRestriction\\n}\\n\\nfragment ShareButton_cloudcast on Cloudcast {\\n  id\\n  isUnlisted\\n  isPublic\\n  slug\\n  description\\n  audioType\\n  picture {\\n    urlRoot\\n  }\\n  owner {\\n    displayName\\n    isViewer\\n    username\\n    id\\n  }\\n}\\n\\nfragment StaticPlayButton_cloudcast on Cloudcast {\\n  owner {\\n    username\\n    id\\n  }\\n  slug\\n  isAwaitingAudio\\n  restrictedReason\\n}\\n\\nfragment useAudioPreview_cloudcast on Cloudcast {\\n  id\\n  previewUrl\\n}\\n\\nfragment useExclusiveCloudcastModal_cloudcast on Cloudcast {\\n  id\\n  isExclusive\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\\nfragment useExclusivePreviewModal_cloudcast on Cloudcast {\\n  id\\n  isExclusivePreviewOnly\\n  owner {\\n    username\\n    id\\n  }\\n}\\n\",\"variables\":{\"lookup\":{\"username\":\""+matcher.group(1)+"\",\"slug\":\""+matcher.group(2)+"\"}}}";

        final RequestBody body = RequestBody.create(post, JSON);
        Request request = new Request.Builder()
                .url("https://app.mixcloud.com/graphql")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .post(body)
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
        JsonElement json = new Gson().fromJson(result, JsonElement.class);

        return json.getAsJsonObject().get("data").getAsJsonObject().get("cloudcast").getAsJsonObject().get("name").getAsString();
    }

    @Override
    public String getServiceName() {
        return "Mixcloud";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
