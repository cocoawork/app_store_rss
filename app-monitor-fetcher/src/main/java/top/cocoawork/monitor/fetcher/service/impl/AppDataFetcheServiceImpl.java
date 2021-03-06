package top.cocoawork.monitor.fetcher.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.cocoawork.monitor.common.constant.ApplicationConstant;
import top.cocoawork.monitor.common.enums.AppType;
import top.cocoawork.monitor.fetcher.service.AppDataFetchService;
import top.cocoawork.monitor.service.api.AppInfoService;
import top.cocoawork.monitor.service.api.AppOutlineService;
import top.cocoawork.monitor.service.api.UserFavourService;
import top.cocoawork.monitor.service.api.UserService;
import top.cocoawork.monitor.service.api.dto.*;


import java.io.IOException;
import java.util.*;

@Service
public class AppDataFetcheServiceImpl implements AppDataFetchService {

    private Logger logger = LoggerFactory.getLogger(AppDataFetcheServiceImpl.class);

    public static final String APP_STORE_RSS_BASE_URL = "https://rss.itunes.apple.com/api/v1/";
    public static final String APP_STORE_BASE_LOOKUP_URL = "https://itunes.apple.com/lookup";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
//
//    private AppOutlineService appOutlineService;
//
    @Reference
    private AppInfoService appInfoService;

    @Reference
    private UserFavourService userAppService;
//
//    private UserService userService;




    @Override
    public void fetchAppOutline(String countryCode, AppType.MediaType mediaType, AppType.FeedType feedType){

        logger.info("开始抓取app简介信息==>>国家{}|类型{}", countryCode, feedType);

        String url = APP_STORE_RSS_BASE_URL + countryCode + "/" + mediaType.getRawValue() + "/" + feedType.getRawValue() + "/all/100/explicit.json";
        String resultString = restTemplate.getForObject(url, String.class);


        logger.debug("抓取app简介信息结果：{}", resultString);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(resultString);
        } catch (IOException e) {
            logger.error("app简介信息json解析错误",e);
            return;
        }
        JsonNode feedNode = rootNode.get("feed");
        JsonNode resultNode = feedNode.get("results");
        if (resultNode.isArray()) {
            Iterator<JsonNode> iterator = resultNode.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                String appId = node.get("id").textValue();
                AppOutlineDto appOutline = null;
                try {
                    appOutline = objectMapper.treeToValue(node, AppOutlineDto.class);
                } catch (JsonProcessingException e) {
                    logger.error("获取app详细信息json转AppOutline错误",e);
                    return;
                }


                JsonNode genres = node.get("genres");
                if (genres.isArray()) {
                    HashSet<GenreDto> genreDtos = new HashSet<>();
                    for (JsonNode genre : genres) {
                        try {
                            GenreDto genreDto = objectMapper.treeToValue(genre, GenreDto.class);
                            genreDtos.add(genreDto);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    }
                    appOutline.setGenre(genreDtos);
                }

                appOutline.setId(appId);
                appOutline.setFeedType(feedType.getRawValue());
                appOutline.setCountryCode(countryCode);
                appOutline.setMediaType(mediaType.getRawValue());


                rocketMQTemplate.sendOneWay(ApplicationConstant.MQ_TOPIC+":"+ApplicationConstant.MQ_TOPIC_TAG_APPOUTLINE, appOutline);

//                AppOutlineDto existAppOutline = appOutlineService.selectById(appId);
//                if (null != existAppOutline) {
//                    appOutlineService.update(appOutline);
//                } else {
//                    appOutlineService.insert(appOutline);
//                }
            }
        }
    }

    @Override
    public void fetchAppInfo(String appId) {

        String url = APP_STORE_BASE_LOOKUP_URL + "?id=" + appId;
        String result = restTemplate.getForObject(url, String.class);
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode jsonNode = null;
        try {
            jsonNode = om.readTree(result);
        } catch (IOException e) {
            logger.error("app详细信息json解析错误", e);
            return;
        }
        JsonNode results = jsonNode.get("results");
        JsonNode element = results.get(0);
        if (null != element) {
            AppInfoDto appinfo = null;
            try {
                appinfo = om.treeToValue(element, AppInfoDto.class);
            } catch (JsonProcessingException e) {
                logger.error("获取app详细信息json转AppInfo错误", e);
                return;
            }
            appinfo.setAppId(appId);

//            //根据appid查询appinfo
            AppInfoDto existAppInfo = appInfoService.selectById(appId);

            rocketMQTemplate.sendOneWay(ApplicationConstant.MQ_TOPIC+":"+ApplicationConstant.MQ_TOPIC_TAG_APPINFO, appinfo);
//            if (null != existAppInfo) {
//                appInfoService.update(appinfo);
//            } else {
//                appInfoService.insert(appinfo);
//            }

            int ret = -1;
            if (existAppInfo != null) {
                //比较两个对象的日期，判断是否发生版本更新
                String oldVersion = existAppInfo.getVersion();
                String newVersion = appinfo.getVersion();
                ret = oldVersion.compareTo(newVersion);
            }
            if (ret < 0) {
                //发生版本更新
                //1.根据appid查找关注的用户
                //2.根据userid查找用户email
                //3.根据email想用户发送邮件提醒更新
                List<UserFavourDto> userApps = userAppService.selectUserAppsByAppId(appId);
                for (UserFavourDto userApp : userApps) {


                }
            }
        }
    }


}
