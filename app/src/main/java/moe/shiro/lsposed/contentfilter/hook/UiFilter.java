package moe.shiro.lsposed.contentfilter.hook;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import moe.shiro.lsposed.contentfilter.config.RuleKeys;
import moe.shiro.lsposed.contentfilter.config.RulesSnapshot;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XposedBridge;

final class UiFilter {
    private static final int TAG_COLLAPSED_REASON = 0x7f0f0421;
    private static final int TAG_PENDING_CHECK = 0x7f0f0422;
    private static final int TAG_ORIGINAL_HEIGHT = 0x7f0f0423;
    private static final int MAX_TEXT_LENGTH = 260;
    private static final int MAX_USER_TEXT_LENGTH = 80;
    private static final int MAX_PARENT_DEPTH = 14;
    private static final int MAX_SCAN_TEXT_VIEWS = 80;
    private static final int MAX_MODEL_DEPTH = 3;
    private static final int MAX_MODEL_FIELDS = 96;
    private static final int MAX_MODEL_METHODS = 24;
    private static final int MAX_MODEL_COLLECTION_ITEMS = 8;
    private static final int MAX_DEBUG_FEED_SAMPLES_PER_KEY = 3;
    private static final int MAX_DEBUG_FEED_SAMPLE_KEYS = 64;
    private static final int MAX_DEBUG_SAMPLE_DEPTH = 5;
    private static final int MAX_DEBUG_SAMPLE_VIEWS = 48;
    private static final int MAX_DEBUG_SAMPLE_TEXT_CHARS = 48;
    private static final Map<Object, Boolean> PAGER_RECYCLER_ADAPTERS = new WeakHashMap<>();
    private static final Map<Object, Boolean> NON_FEED_RECYCLER_ADAPTERS = new WeakHashMap<>();
    private static final Map<String, Integer> DEBUG_FEED_SAMPLE_COUNTS = new HashMap<>();
    private static final String[] NON_FEED_SURFACE_SIGNATURES = {
            "drawer",
            "navigationview",
            "bottomnavigation",
            "bottom_navigation",
            "navigationbar",
            "navigation_bar",
            "tablayout",
            "tab_layout",
            "tabbar",
            "tab_bar",
            "bottombar",
            "bottom_bar",
            "maintab",
            "main_tab",
            "hometab",
            "home_tab",
            "toolbar",
            "topbar",
            "titlebar",
            "actionbar",
            "searchbar",
            "popup",
            "dialog",
            "menu",
            "setting",
            "preference",
            "comment",
            "comments",
            "reply",
            "discussion",
            "danmaku",
            "danmu",
            "message",
            "messages",
            "notice",
            "notification",
            "conversation",
            "session",
            "privateletter",
            "private_letter",
            "directmessage",
            "direct_message",
            "imsession",
            "im_session",
            "imcontact",
            "im_contact",
            "immessage",
            "im_message",
            "chat",
            "contact",
            "friend",
            "friends",
            "familiar",
            "familiar_feed",
            "评论",
            "回复",
            "弹幕",
            "消息",
            "通知",
            "会话",
            "私信",
            "聊天",
            "联系人",
            "好友",
            "朋友",
            "通讯录",
            "互关"
    };
    private static final String[] NON_FEED_VISIBLE_TEXT_SIGNATURES = {
            "好友私信",
            "私信",
            "消息",
            "聊天",
            "会话",
            "联系人",
            "通讯录",
            "互关",
            "好友",
            "朋友",
            "好友列表",
            "朋友列表",
            "我的好友",
            "我的朋友",
            "小伙伴",
            "陌生人消息",
            "未关注人消息",
            "群消息",
            "客服消息",
            "应援团消息"
    };
    private static final String[] NON_FEED_BINDING_SIGNATURES = {
            "comment2",
            "comment3",
            "commentv3",
            "commentlist",
            "commentitem",
            "commentcard",
            "commenttree",
            "commentarea",
            "commentdetail",
            "commentfeedlist",
            "commentfragment",
            "commenthalfweb",
            "commentpage",
            "commentsearch",
            "commentadapter",
            "commentsview",
            "replylist",
            "replyitem",
            "mainreply",
            "subreply",
            "replydetail",
            "replyfragment",
            "replypage",
            "replyadapter",
            "discussion",
            "danmaku",
            "danmu",
            "biliresourcecomment",
            "appcommcomment",
            "appcomment",
            "biliresourceim",
            "resourceim",
            "srcsappim",
            "bplusim",
            "awemeim",
            "awemeimsdk",
            "imsdk",
            "imsession",
            "imsessionlist",
            "imsessionitem",
            "imsessionadapter",
            "imconversation",
            "imconversationlist",
            "imconversationitem",
            "imcontact",
            "imcontactlist",
            "imcontactitem",
            "immessage",
            "immessagelist",
            "immessageitem",
            "imchat",
            "privateletter",
            "privateletterlist",
            "privatemessage",
            "directmessage",
            "noticesactivity",
            "noticeactivity",
            "notificationactivity",
            "notificationlist",
            "notificationitem",
            "noticeadapter",
            "conversationactivity",
            "conversationlist",
            "conversationitem",
            "conversationadapter",
            "homecommunication",
            "customercommunication",
            "customerconversation",
            "strangerconversation",
            "unfollowconversation",
            "garbageconversation",
            "groupconversation",
            "aiconversation",
            "chatgptconversation",
            "singlechat",
            "chatgroup",
            "chatsetting",
            "imaccount",
            "imshare",
            "imsetting",
            "imfragmentcontainer",
            "privatesession",
            "sessionmanage",
            "messagesession",
            "ecommessagelist",
            "messagelist",
            "messageitem",
            "messagecenter",
            "messagetip",
            "messagesetting",
            "contactactivity",
            "contactshare",
            "contactselect",
            "contactpicker",
            "contactlist",
            "contactitem",
            "relationselect",
            "relationlist",
            "relationitem",
            "familiar",
            "familiarfeed",
            "familiarpage",
            "homepagefamiliar",
            "friendfeed",
            "friendsfeed",
            "friendpage",
            "friendstab",
            "friendgroup",
            "friendlist",
            "frienditem",
            "评论",
            "回复",
            "弹幕",
            "消息",
            "通知",
            "会话",
            "私信",
            "聊天",
            "联系人",
            "好友",
            "朋友",
            "通讯录",
            "互关"
    };
    private static final String[] NON_FEED_MODEL_FIELD_SIGNATURES = {
            "commentlist",
            "commentitem",
            "commentcard",
            "commenttree",
            "commentarea",
            "replylist",
            "replyitem",
            "mainreply",
            "subreply",
            "danmakulist",
            "danmulist",
            "conversationlist",
            "conversationitem",
            "imconversation",
            "imconversationlist",
            "imconversationitem",
            "imsession",
            "imsessionlist",
            "imsessionitem",
            "immessage",
            "immessagelist",
            "immessageitem",
            "imcontact",
            "imcontactlist",
            "imcontactitem",
            "chatlist",
            "chatitem",
            "chatmessage",
            "messagelist",
            "messageitem",
            "sessionlist",
            "sessionitem",
            "contactlist",
            "contactitem",
            "relationlist",
            "relationitem",
            "familiar",
            "familiarfeed",
            "friendfeed",
            "friendsfeed",
            "friendlist",
            "frienditem",
            "privateletter"
    };
    private static final String[] FEED_BINDING_SIGNATURES = {
            "feed",
            "feeditem",
            "feedcard",
            "videofeed",
            "videocard",
            "shortvideo",
            "homepagefeed",
            "homepagehot",
            "homefeed",
            "recommendfeed",
            "recommend",
            "recommendation",
            "rcmd",
            "recfeed",
            "pegasus",
            "theseus",
            "storyfeed",
            "feedplayer",
            "playfeed"
    };
    private static final String[] LIVE_VIEW_SIGNATURES = {
            "live",
            "living",
            "liveroom",
            "live_room",
            "livestream",
            "live_stream",
            "broadcast",
            "anchor",
            "liveicon",
            "live_icon",
            "livecover",
            "live_cover",
            "liverooms",
            "live_rooms",
            "liveplayer",
            "live_player",
            "livewindow",
            "live_window",
            "roominfo",
            "room_info",
            "avatarliving",
            "avatar_living"
    };
    private static final String[] AD_VIEW_SIGNATURES = {
            "feedad",
            "feed_ad",
            "adfeed",
            "ad_feed",
            "adcard",
            "ad_card",
            "nativead",
            "native_ad",
            "commercialize",
            "commercial",
            "advertise",
            "advertisement",
            "sponsor",
            "sponsored",
            "promotion",
            "biliad",
            "bili_ad",
            "advideo",
            "ad_video",
            "adupper",
            "ad_upper",
            "adpanel",
            "ad_panel",
            "adanchor",
            "ad_anchor",
            "adplantseeds",
            "ad_plant_seeds",
            "adnativecard",
            "ad_native_card",
            "adviewpegasus",
            "ad_view_pegasus",
            "plantseeds",
            "plant_seeds",
            "commandmarkgoods",
            "command_mark_goods",
            "markgoods",
            "mark_goods",
            "mallfeed",
            "mall_feed",
            "insertedbanner",
            "inserted_banner",
            "douyinmall",
            "douyin_mall",
            "ecom",
            "ecom_",
            "ecom_live",
            "ecom_cart",
            "ecom_sku",
            "ec_feed",
            "ec_live",
            "ec_card",
            "shop_window",
            "mall_card",
            "ecommerce",
            "commerce",
            "shopping",
            "shop"
    };
    private static final String[] AD_RESOURCE_SIGNATURES = {
            "ad_selector",
            "ad_story",
            "ad_feed_",
            "ad_generalize",
            "ad_player_recommend_goods",
            "ad_search_inline",
            "ad_download",
            "ad_file",
            "ad_sound",
            "ad_more",
            "ad_return",
            "adreward",
            "ad_task",
            "ad_coins",
            "ad_feedback",
            "ad_live_badge",
            "ad_native_card_",
            "feed_ad",
            "ad_inline",
            "ad_vip",
            "ad_game",
            "ad_av",
            "ad_web",
            "banner_ad",
            "story_ad",
            "ad_badge",
            "ad_tag",
            "cm_mark",
            "creative_ad",
            "pegasus_ad",
            "commercial",
            "promotion",
            "sponsor",
            "ecom_",
            "ecom_live",
            "ecom_cart",
            "ecom_sku",
            "ec_feed",
            "ec_live",
            "ec_card",
            "shop_window",
            "mall_card",
            "mall_icon",
            "mall_module",
            "mall_window",
            "mall_video",
            "mall_station_line",
            "mallwidget",
            "ecom_cart",
            "ecom_search_shop",
            "ecom_screen_short_icon",
            "ec_commerce_anchor",
            "ec_commerce_recommend",
            "ec_mall_load_more",
            "commerce_anchor_v3",
            "commerce_draw_ad",
            "commerce_video_product_inner_feed",
            "commerce_playback_explaining",
            "commerce_playback_gesture",
            "commodity_main_card",
            "commodity_redblueballs",
            "product_card_",
            "product_group_",
            "shop_header_",
            "follow_ecom_guide",
            "shop_card",
            "shop_info",
            "shop_store_line",
            "shopping_cart_line",
            "bili_ad_",
            "bilibili_ad_feed",
            "ad_anchor_download",
            "bg_ad_anchor_download",
            "ad_video_upper",
            "ad_story_twist",
            "ad_feed_gif_twist",
            "ad_pegasus",
            "ad_pegasus_149_carousel",
            "ad_plant_seeds",
            "ad_player_live_reservation",
            "bg_ad_live_reservation",
            "ad_video_detail",
            "ad_new_panel",
            "ad_component",
            "ad_cover_badge",
            "ad_space_shop",
            "ad_shape_following_goods",
            "ad_fragment_video_page",
            "ad_live_ad_big_card",
            "ad_close_image",
            "ad_xmark_close",
            "ad_play_icon",
            "icon_ad_close",
            "biligame_ad",
            "bili_ad_goods_price_discount",
            "bili_ad_new_panel_goods_price_discount",
            "vector_ad_upper",
            "ic_ad_upper",
            "ic_ad_progress",
            "ic_ad_panel",
            "ad_cm_mark",
            "ad_banner_inline",
            "ad_banner_top",
            "command_mark_goods",
            "following_card_goods",
            "mall_home_feed",
            "mall_home_feeds",
            "mall_feed_blast",
            "mall_theseus_inserted_banner",
            "mall_shop_home_feed_tag",
            "goods_price_discount"
    };
    private static final String[] LIVE_RESOURCE_SIGNATURES = {
            "ic_live",
            "live_badge",
            "live_card",
            "live_icon",
            "live_preview",
            "live_room",
            "room_live",
            "inline_live",
            "right_top_live_badge",
            "bg_live_card",
            "story_enter_live_room",
            "story_live_no_circle",
            "splash_new_story_enter_live_room_live",
            "story_live",
            "splash_story_enter_live_room",
            "live_home_feed",
            "live_video_feed",
            "live_video_feed_entrance",
            "live_ic_live",
            "player_live_avatar",
            "avatar_living",
            "living_animation",
            "live_cover",
            "live_rooms",
            "live_player",
            "live_window",
            "room_info",
            "live_state",
            "liveanimation",
            "feed_c2_live_tag",
            "feed_c2_dynamic_live_tag",
            "feed_live_",
            "tp_inline_live_widget",
            "story_list_item_live",
            "item_live_banner",
            "live_entrance_bg",
            "douyin_live_icon",
            "douyin_live_rank",
            "ec_commerce_recommend_live",
            "ec_live_list_card",
            "ec_live_card_buy",
            "ec_live_explain",
            "ec_shop_live_indicator",
            "ec_living_lottie",
            "ec_store_lottie_store_logo_is_living",
            "commerce_anchor_v3_live",
            "following_live_uplist",
            "live_feed_rank",
            "live_feed_guide",
            "liveroom_top3_rank",
            "search_result_live",
            "inner_push_live",
            "plantseeds_inline_live",
            "uplist_live_card",
            "dy_uplist_live_card",
            "live_card_mark",
            "mall_home_feeds_live_card",
            "bili_shape_inner_push_live",
            "bili_ic_inline_live",
            "pegasus_card_badge_live_icon",
            "goods_feed_mall_live"
    };
    private static final String[] MODEL_LIVE_SIGNATURES = {
            "is_live",
            "islive",
            "live_status",
            "livestatus",
            "live_state",
            "livestate",
            "livebadge",
            "live_badge",
            "livecard",
            "live_card",
            "liveroom",
            "live_room",
            "liveroomid",
            "live_room_id",
            "liveid",
            "live_id",
            "roomid",
            "room_id",
            "roominfo",
            "room_info",
            "livestream",
            "live_stream",
            "livepreview",
            "live_preview",
            "liveicon",
            "live_icon",
            "liveiconurl",
            "live_icon_url",
            "livecover",
            "live_cover",
            "liverooms",
            "live_rooms",
            "liveplayer",
            "live_player",
            "livewindow",
            "live_window",
            "prelive",
            "pre_live",
            "inlinelive",
            "inline_live",
            "righttoplivebadge",
            "right_top_live_badge",
            "avatarliving",
            "avatar_living",
            "living",
            "feedlive",
            "feed_live",
            "feedlivecard",
            "feed_live_card",
            "feedliveadcard",
            "feed_live_ad_card",
            "feedc2live",
            "feed_c2_live",
            "feedc2dynamiclive",
            "feed_c2_dynamic_live",
            "eclivelistcard",
            "ec_live_list_card",
            "eclivecard",
            "ec_live_card",
            "ecshoplive",
            "ec_shop_live",
            "ecliving",
            "ec_living",
            "livereservation",
            "live_reservation",
            "livefeed",
            "live_feed",
            "livefeedrank",
            "live_feed_rank",
            "livebanner",
            "live_banner",
            "liveuplist",
            "live_uplist",
            "liverank",
            "live_rank",
            "liveentrance",
            "live_entrance",
            "liveguide",
            "live_guide",
            "livepush",
            "live_push",
            "livingtag",
            "living_tag",
            "anchorinfo",
            "anchor_info"
    };
    private static final String[] MODEL_AD_SIGNATURES = {
            "adcommon",
            "adinfo",
            "ad_info",
            "aditem",
            "ad_item",
            "adtype",
            "ad_type",
            "adsource",
            "ad_source",
            "addesc",
            "ad_desc",
            "admark",
            "ad_mark",
            "adroot",
            "ad_root",
            "adtitle",
            "ad_title",
            "adbadge",
            "ad_badge",
            "adlabel",
            "ad_label",
            "adicon",
            "ad_icon",
            "adbanner",
            "ad_banner",
            "adlive",
            "ad_live",
            "adwrapper",
            "ad_wrapper",
            "adreport",
            "ad_report",
            "adcard",
            "ad_card",
            "adfeed",
            "ad_feed",
            "feedad",
            "feed_ad",
            "bannerad",
            "banner_ad",
            "storyad",
            "story_ad",
            "rawad",
            "raw_ad",
            "rawadinfo",
            "raw_ad_info",
            "rawaddata",
            "raw_ad_data",
            "awemerawad",
            "aweme_raw_ad",
            "nativead",
            "native_ad",
            "is_ad",
            "isad",
            "cminfo",
            "cm_info",
            "cmmark",
            "cm_mark",
            "creative_id",
            "creativeid",
            "commercialize",
            "commercial",
            "commerce",
            "ecommerce",
            "ecom",
            "ecomlive",
            "ecom_live",
            "ecomliveparams",
            "ecom_live_params",
            "ecomdata",
            "ecom_data",
            "ecfeed",
            "ec_feed",
            "eclive",
            "ec_live",
            "eccard",
            "ec_card",
            "ecshop",
            "ec_shop",
            "ecmall",
            "ec_mall",
            "douyinmall",
            "douyin_mall",
            "mallcard",
            "mall_card",
            "mallcarditem",
            "mall_card_item",
            "mallinfo",
            "mall_info",
            "mallindex",
            "mall_index",
            "mallmodule",
            "mall_module",
            "mallvideo",
            "mall_video",
            "mallwindow",
            "mall_window",
            "mallwidget",
            "mall_widget",
            "shopwindow",
            "shop_window",
            "shopinfo",
            "shop_info",
            "shopcard",
            "shop_card",
            "shopheader",
            "shop_header",
            "shopid",
            "shop_id",
            "shopfatherid",
            "shop_father_id",
            "shopname",
            "shop_name",
            "shoplogo",
            "shop_logo",
            "shopurl",
            "shop_url",
            "shophomepage",
            "shop_home_page",
            "productcard",
            "product_card",
            "productgroup",
            "product_group",
            "productid",
            "product_id",
            "productchannel",
            "product_channel",
            "commodity",
            "commodityid",
            "commodity_id",
            "commoditytype",
            "commodity_type",
            "promotionid",
            "promotion_id",
            "skuid",
            "sku_id",
            "ecomentranceform",
            "ecom_entrance_form",
            "ecomsceneid",
            "ecom_scene_id",
            "shopuserid",
            "shop_user_id",
            "commerceanchor",
            "commerce_anchor",
            "commerceanchorv3",
            "commerce_anchor_v3",
            "eccommerceanchor",
            "ec_commerce_anchor",
            "eccommercerecommend",
            "ec_commerce_recommend",
            "adreservation",
            "ad_reservation",
            "adanchor",
            "ad_anchor",
            "adplantseeds",
            "ad_plant_seeds",
            "adpegasus",
            "ad_pegasus",
            "adcomponent",
            "ad_component",
            "adcoverbadge",
            "ad_cover_badge",
            "plantseeds",
            "plant_seeds",
            "commandmarkgoods",
            "command_mark_goods",
            "markgoods",
            "mark_goods",
            "goodsinfo",
            "goods_info",
            "productinfo",
            "product_info",
            "skuinfo",
            "sku_info",
            "couponinfo",
            "coupon_info",
            "goodslist",
            "goods_list",
            "productlist",
            "product_list",
            "mallfeed",
            "mall_feed",
            "mallhomefeed",
            "mall_home_feed",
            "mallfeeds",
            "mall_feeds",
            "insertedbanner",
            "inserted_banner",
            "incomecenter",
            "income_center",
            "shopping",
            "promotion",
            "sponsor",
            "downloadcompliance"
    };
    private static final String[] MODEL_USER_TEXT_SIGNATURES = {
            "authorname",
            "authornickname",
            "authornick",
            "authorusername",
            "authoruname",
            "ownername",
            "ownernickname",
            "ownerusername",
            "uploadername",
            "uploadernickname",
            "uploaderusername",
            "creatorname",
            "creatornickname",
            "publishername",
            "publishernickname",
            "accountname",
            "accountnickname",
            "profilename",
            "profilenickname",
            "membername",
            "membernickname",
            "username",
            "usernickname",
            "usernick",
            "displayname",
            "screenname",
            "upname",
            "upnickname",
            "uppername",
            "uppernickname",
            "anchorname",
            "anchornickname",
            "nickname"
    };
    private static final String[] MODEL_USER_TEXT_EXACT_NAMES = {
            "author",
            "getauthor",
            "owner",
            "getowner",
            "uploader",
            "getuploader",
            "creator",
            "getcreator",
            "publisher",
            "getpublisher",
            "account",
            "getaccount",
            "profile",
            "getprofile",
            "member",
            "getmember",
            "user",
            "getuser",
            "up",
            "getup",
            "upper",
            "getupper",
            "uname",
            "getuname",
            "nick",
            "getnick",
            "nickname",
            "getnickname",
            "displayname",
            "getdisplayname",
            "screenname",
            "getscreenname"
    };
    private static final String[] MODEL_USER_CONTAINER_SIGNATURES = {
            "author",
            "owner",
            "uploader",
            "creator",
            "publisher",
            "account",
            "profile",
            "member",
            "user",
            "upper",
            "anchor"
    };
    private static final String[] MODEL_USER_NAME_SUFFIXES = {
            "name",
            "nickname",
            "nick",
            "uname"
    };

    private UiFilter() {
    }

    static void rememberRecyclerAdapter(Object adapter, View recyclerView) {
        if (adapter == null || recyclerView == null) {
            return;
        }
        markNonFeedRecyclerAdapter(adapter, recyclerView);
        if (!markPagerRecyclerAdapter(adapter, recyclerView)) {
            recyclerView.post(() -> markPagerRecyclerAdapter(adapter, recyclerView));
        }
    }

    static void handleTextChanged(
            Context context,
            TextView textView,
            CharSequence text,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (textView instanceof EditText
                || text == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, textView)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        Match match = matchText(rules, packageName, text);
        if (!match.blocked) {
            return;
        }
        scheduleCollapse(context, textView, match, rules, packageName, processName, currentActivity);
    }

    static void handleResourceBadgeSet(
            Context context,
            View view,
            int resId,
            String source,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (view == null
                || resId == 0
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, view)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        Match match = matchResourceBadge(context, rules, packageName, resId);
        if (!match.blocked) {
            return;
        }
        scheduleResourceCollapse(context, view, match, rules, packageName, processName, currentActivity, source);
    }

    static void handleViewTextSignal(
            Context context,
            View view,
            CharSequence text,
            String source,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (view == null
                || view instanceof EditText
                || text == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, view)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        Match match = matchText(rules, packageName, text);
        if (!match.blocked) {
            return;
        }
        scheduleSignalCollapse(context, view, match, rules, packageName, processName, currentActivity, source, text);
    }

    static void handleViewTagSignal(
            Context context,
            View view,
            int key,
            Object value,
            String source,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (view == null
                || value == null
                || isInternalTagKey(key)
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, view)
                || containsNonFeedIdentity(value)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        if (!rules.hasActiveRules(packageName)) {
            return;
        }
        Match match;
        if (value instanceof CharSequence) {
            match = matchText(rules, packageName, (CharSequence) value);
        } else {
            IdentityHashMap<Object, Boolean> nonFeedVisited = new IdentityHashMap<>();
            if (containsNonFeedModelIdentity(value, 0, nonFeedVisited, new int[]{0})) {
                return;
            }
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            String keyName = key == 0 ? "" : normalizeIdentityToken(resourceName(context, key));
            match = scanModelObject(rules, packageName, value, 0, visited, new int[]{0}, keyName);
        }
        if (!match.blocked) {
            return;
        }
        scheduleSignalCollapse(
                context,
                view,
                match,
                rules,
                packageName,
                processName,
                currentActivity,
                tagSource(context, source, key),
                describeTagValue(value)
        );
    }

    static void handleDrawableSignal(
            Context context,
            View view,
            Drawable drawable,
            String source,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (view == null
                || drawable == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)
                || isInsideNonFeedSurface(context, view)
                || containsNonFeedIdentity(drawable)) {
            return;
        }
        RulesSnapshot rules = RulesCache.get(context);
        if (!rules.hasActiveRules(packageName)) {
            return;
        }
        IdentityHashMap<Object, Boolean> nonFeedVisited = new IdentityHashMap<>();
        if (containsNonFeedModelIdentity(drawable, 0, nonFeedVisited, new int[]{0})) {
            return;
        }
        Match match = scanModelObject(rules, packageName, drawable, 0, new IdentityHashMap<>(), new int[]{0}, "");
        if (!match.blocked) {
            return;
        }
        scheduleSignalCollapse(
                context,
                view,
                match,
                rules,
                packageName,
                processName,
                currentActivity,
                source,
                describeTagValue(drawable)
        );
    }

    static void handleBoundItem(
            Context context,
            View itemView,
            Object adapter,
            Object holder,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (itemView == null
                || !ActivityClassifier.isContentActivity(currentActivity)
                || isNonFeedActivity(currentActivity)) {
            return;
        }
        if (isNonFeedBinding(context, itemView, adapter, holder, packageName, currentActivity)) {
            restoreView(itemView);
            return;
        }
        boolean pagerPageItem = isPagerPageItem(itemView, adapter, holder);
        RulesSnapshot rules = RulesCache.get(context);
        if (!rules.hasActiveRules(packageName)) {
            restoreView(itemView);
            return;
        }
        Match match = scanViewTree(context, rules, packageName, itemView, 0, new int[]{0});
        if (!match.blocked) {
            match = scanBoundModel(rules, packageName, itemView, holder);
        }
        if (match.blocked) {
            if (requiresFeedLikeTarget(match)
                    && !isFeedLikeBinding(context, itemView, adapter, holder, currentActivity)) {
                restoreView(itemView);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip user match outside feed-like binding: " + match
                            + " package=" + packageName
                            + " process=" + processName
                            + " activity=" + currentActivity
                            + " container=" + itemView.getClass().getName());
                }
                return;
            }
            if (pagerPageItem) {
                concealPagerPage(itemView, match);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager page concealed package=" + packageName
                            + " process=" + processName
                            + " activity=" + currentActivity
                            + " reason=" + match
                            + " container=" + itemView.getClass().getName());
                }
                return;
            }
            collapseView(itemView, match);
            if (rules.debugLog) {
                XposedBridge.log("[LCF] bind collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " container=" + itemView.getClass().getName());
            }
        } else {
            restoreView(itemView);
            maybeLogUnmatchedFeedItem(
                    context,
                    rules,
                    packageName,
                    processName,
                    currentActivity,
                    itemView,
                    adapter,
                    holder
            );
        }
    }

    private static Match scanViewTree(
            Context context,
            RulesSnapshot rules,
            String packageName,
            View view,
            int depth,
            int[] textViews
    ) {
        if (depth > MAX_PARENT_DEPTH || textViews[0] > MAX_SCAN_TEXT_VIEWS) {
            return Match.none();
        }
        if (isNonFeedSurfaceView(context, view)) {
            return Match.none();
        }
        if (view instanceof TextView && !(view instanceof EditText)) {
            textViews[0]++;
            Match match = matchText(rules, packageName, ((TextView) view).getText());
            if (match.blocked) {
                return match;
            }
        }
        Object tag = view.getTag();
        if (tag instanceof CharSequence) {
            Match match = matchText(rules, packageName, (CharSequence) tag);
            if (match.blocked) {
                return match;
            }
        }
        CharSequence description = view.getContentDescription();
        if (description != null) {
            Match match = matchText(rules, packageName, description);
            if (match.blocked) {
                return match;
            }
        }
        Match identifierMatch = matchViewIdentity(context, rules, packageName, view);
        if (identifierMatch.blocked) {
            return identifierMatch;
        }
        if (!(view instanceof ViewGroup)) {
            return Match.none();
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            Match child = scanViewTree(context, rules, packageName, group.getChildAt(i), depth + 1, textViews);
            if (child.blocked) {
                return child;
            }
        }
        return Match.none();
    }

    private static Match scanBoundModel(
            RulesSnapshot rules,
            String packageName,
            View itemView,
            Object holder
    ) {
        if (!rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] budget = new int[]{0};
        Match holderMatch = scanModelObject(rules, packageName, holder, 0, visited, budget, "");
        if (holderMatch.blocked) {
            return holderMatch;
        }
        Object tag = itemView.getTag();
        if (tag != null) {
            return scanModelObject(rules, packageName, tag, 0, visited, budget, "");
        }
        return Match.none();
    }

    private static Match scanModelObject(
            RulesSnapshot rules,
            String packageName,
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget,
            String fieldName
    ) {
        if (value == null || depth > MAX_MODEL_DEPTH || budget[0] > MAX_MODEL_FIELDS) {
            return Match.none();
        }
        Match fieldMatch = matchModelSignal(rules, packageName, fieldName, value);
        if (fieldMatch.blocked) {
            return fieldMatch;
        }
        if (value instanceof CharSequence) {
            return matchText(rules, packageName, (CharSequence) value);
        }
        Class<?> type = value.getClass();
        if (isTerminalModelValue(type)) {
            return Match.none();
        }
        if (visited.containsKey(value)) {
            return Match.none();
        }
        visited.put(value, Boolean.TRUE);
        Match identity = matchModelIdentity(rules, packageName, type.getName());
        if (identity.blocked) {
            return identity;
        }
        if (type.isArray()) {
            return scanModelArray(rules, packageName, value, depth, visited, budget, fieldName);
        }
        if (value instanceof Iterable) {
            return scanModelIterable(rules, packageName, (Iterable<?>) value, depth, visited, budget, fieldName);
        }
        if (value instanceof Map) {
            return scanModelMap(rules, packageName, (Map<?, ?>) value, depth, visited, budget, fieldName);
        }
        if (shouldSkipModelClass(type)) {
            return Match.none();
        }
        Match accessorMatch = scanModelAccessors(rules, packageName, value, type, depth, visited, budget);
        if (accessorMatch.blocked) {
            return accessorMatch;
        }
        String childPrefix = effectiveChildFieldPrefix(fieldName, type);
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= MAX_MODEL_FIELDS) {
            Field[] fields;
            try {
                fields = current.getDeclaredFields();
            } catch (Throwable ignored) {
                break;
            }
            for (Field field : fields) {
                if (budget[0]++ > MAX_MODEL_FIELDS || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object child;
                try {
                    field.setAccessible(true);
                    child = field.get(value);
                } catch (Throwable ignored) {
                    continue;
                }
                Match childMatch = scanModelObject(
                        rules,
                        packageName,
                        child,
                        depth + 1,
                        visited,
                        budget,
                        appendModelFieldName(childPrefix, field.getName())
                );
                if (childMatch.blocked) {
                    return childMatch;
                }
            }
            current = current.getSuperclass();
        }
        return Match.none();
    }

    private static Match scanModelAccessors(
            RulesSnapshot rules,
            String packageName,
            Object value,
            Class<?> type,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        if ("com.ss.android.ugc.aweme".equals(packageName)) {
            return Match.none();
        }
        int methodCount = 0;
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= MAX_MODEL_FIELDS) {
            Method[] methods;
            try {
                methods = current.getDeclaredMethods();
            } catch (Throwable ignored) {
                break;
            }
            for (Method method : methods) {
                if (!isSignalAccessor(rules, packageName, method)) {
                    continue;
                }
                if (methodCount++ >= MAX_MODEL_METHODS || budget[0]++ > MAX_MODEL_FIELDS) {
                    return Match.none();
                }
                Object child;
                try {
                    method.setAccessible(true);
                    child = method.invoke(value);
                } catch (Throwable ignored) {
                    continue;
                }
                Match signal = matchModelSignal(rules, packageName, method.getName(), child);
                if (signal.blocked) {
                    return signal;
                }
                String methodName = normalizeIdentityToken(method.getName());
                if (hasUserModelSignalName(rules, packageName, methodName)) {
                    Match childMatch = scanModelObject(
                            rules,
                            packageName,
                            child,
                            depth + 1,
                            visited,
                            budget,
                            methodName
                    );
                    if (childMatch.blocked) {
                        return childMatch;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return Match.none();
    }

    private static boolean isSignalAccessor(RulesSnapshot rules, String packageName, Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)
                || method.isSynthetic()
                || method.isBridge()
                || method.getParameterTypes().length != 0
                || method.getReturnType() == Void.TYPE) {
            return false;
        }
        String name = method.getName();
        if (!isAccessorName(name)) {
            return false;
        }
        return hasModelSignalName(rules, packageName, normalizeIdentityToken(name));
    }

    private static boolean isAccessorName(String name) {
        if (name == null) {
            return false;
        }
        return (name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2)
                || (name.startsWith("has") && name.length() > 3);
    }

    private static boolean hasModelSignalName(RulesSnapshot rules, String packageName, String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty() || !rules.hasActiveRules(packageName)) {
            return false;
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return false;
        }
        return (appRules.blockAds && containsAny(normalizedName, MODEL_AD_SIGNATURES) != null)
                || (appRules.blockLive && containsAny(normalizedName, MODEL_LIVE_SIGNATURES) != null)
                || hasUserModelSignalName(appRules, normalizedName);
    }

    private static Match scanModelArray(
            RulesSnapshot rules,
            String packageName,
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget,
            String fieldName
    ) {
        int length = Math.min(Array.getLength(value), MAX_MODEL_COLLECTION_ITEMS);
        for (int i = 0; i < length; i++) {
            Match match = scanModelObject(rules, packageName, Array.get(value, i), depth + 1, visited, budget, fieldName);
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match scanModelIterable(
            RulesSnapshot rules,
            String packageName,
            Iterable<?> values,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget,
            String fieldName
    ) {
        int count = 0;
        for (Object child : values) {
            if (count++ >= MAX_MODEL_COLLECTION_ITEMS) {
                break;
            }
            Match match = scanModelObject(rules, packageName, child, depth + 1, visited, budget, fieldName);
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match scanModelMap(
            RulesSnapshot rules,
            String packageName,
            Map<?, ?> values,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget,
            String fieldName
    ) {
        int count = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (count++ >= MAX_MODEL_COLLECTION_ITEMS) {
                break;
            }
            String key = entry.getKey() instanceof CharSequence
                    ? normalizeIdentityToken(entry.getKey().toString())
                    : "";
            Match match = scanModelObject(
                    rules,
                    packageName,
                    entry.getValue(),
                    depth + 1,
                    visited,
                    budget,
                    appendModelFieldName(fieldName, key)
            );
            if (match.blocked) {
                return match;
            }
        }
        return Match.none();
    }

    private static Match matchModelSignal(
            RulesSnapshot rules,
            String packageName,
            String rawName,
            Object value
    ) {
        if (rawName == null || rawName.isEmpty() || !rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String name = normalizeIdentityToken(rawName);
        if (appRules.blockUsers && isUserModelTextName(name) && value instanceof CharSequence) {
            String text = RulesSnapshot.normalize((CharSequence) value);
            if (!text.isEmpty() && text.length() <= MAX_USER_TEXT_LENGTH) {
                String keyword = containsAny(text, appRules.userKeywords);
                if (keyword != null) {
                    return Match.blocked("user_model", keyword);
                }
            }
        }
        if (appRules.blockAds) {
            String signal = containsAny(name, MODEL_AD_SIGNATURES);
            if (signal != null && hasPresentSignalValue(value)) {
                return Match.blocked("ad_model", rawName);
            }
        }
        if (appRules.blockLive) {
            String signal = containsAny(name, MODEL_LIVE_SIGNATURES);
            if (signal != null && hasPresentSignalValue(value)) {
                return Match.blocked("live_model", rawName);
            }
        }
        return Match.none();
    }

    private static boolean hasUserModelSignalName(RulesSnapshot rules, String packageName, String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty() || !rules.hasActiveRules(packageName)) {
            return false;
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        return hasUserModelSignalName(appRules, normalizedName);
    }

    private static boolean hasUserModelSignalName(RulesSnapshot.AppRules appRules, String normalizedName) {
        return appRules.enabled
                && appRules.blockUsers
                && !appRules.userKeywords.isEmpty()
                && isUserModelTextName(normalizedName);
    }

    private static boolean isUserModelTextName(String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty()) {
            return false;
        }
        if (containsAny(normalizedName, MODEL_USER_TEXT_SIGNATURES) != null) {
            return true;
        }
        if (containsAny(normalizedName, MODEL_USER_CONTAINER_SIGNATURES) != null) {
            for (String suffix : MODEL_USER_NAME_SUFFIXES) {
                if (normalizedName.endsWith(suffix)) {
                    return true;
                }
            }
        }
        for (String exactName : MODEL_USER_TEXT_EXACT_NAMES) {
            if (exactName.equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private static String appendModelFieldName(String parent, String child) {
        String normalizedChild = normalizeIdentityToken(child);
        if (parent == null || parent.isEmpty()) {
            return normalizedChild;
        }
        if (normalizedChild.isEmpty()) {
            return parent;
        }
        return parent + normalizedChild;
    }

    private static String effectiveChildFieldPrefix(String fieldName, Class<?> type) {
        if (fieldName != null && !fieldName.isEmpty()) {
            return fieldName;
        }
        if (type != null && containsAny(normalizeIdentityToken(type.getName()), MODEL_USER_CONTAINER_SIGNATURES) != null) {
            return "user";
        }
        return "";
    }

    private static Match matchModelIdentity(RulesSnapshot rules, String packageName, String className) {
        if (className == null || className.isEmpty() || !rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String identity = normalizeIdentityToken(className);
        if (appRules.blockAds) {
            String signal = containsAny(identity, MODEL_AD_SIGNATURES);
            if (signal != null) {
                return Match.blocked("ad_model", signal);
            }
        }
        if (appRules.blockLive) {
            String signal = containsAny(identity, MODEL_LIVE_SIGNATURES);
            if (signal != null) {
                return Match.blocked("live_model", signal);
            }
        }
        return Match.none();
    }

    private static boolean hasPresentSignalValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue() != 0L;
        }
        if (value instanceof CharSequence) {
            String normalized = RulesSnapshot.normalize((CharSequence) value);
            return !normalized.isEmpty()
                    && !"0".equals(normalized)
                    && !"false".equals(normalized)
                    && !"null".equals(normalized);
        }
        return true;
    }

    private static boolean isTerminalModelValue(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Enum.class.isAssignableFrom(type);
    }

    private static boolean shouldSkipModelClass(Class<?> type) {
        String name = type.getName();
        return View.class.isAssignableFrom(type)
                || Context.class.isAssignableFrom(type)
                || name.startsWith("android.")
                || name.startsWith("androidx.")
                || name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("kotlin.")
                || name.startsWith("dalvik.")
                || name.startsWith("de.robv.")
                || name.startsWith("moe.shiro.");
    }

    private static String normalizeIdentityToken(String value) {
        return value == null
                ? ""
                : RulesSnapshot.normalize(value).replace(".", "").replace("_", "").replace("-", "");
    }

    private static Match matchText(RulesSnapshot rules, String packageName, CharSequence rawText) {
        if (!rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String text = RulesSnapshot.normalize(rawText);
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            return Match.none();
        }
        if (appRules.blockUsers && text.length() <= MAX_USER_TEXT_LENGTH) {
            String keyword = containsAny(text, appRules.userKeywords);
            if (keyword != null) {
                return Match.blocked("user", keyword);
            }
        }
        if (appRules.blockTopics) {
            String keyword = containsAny(text, appRules.topicKeywords);
            if (keyword != null) {
                return Match.blocked("topic", keyword);
            }
        }
        if (appRules.blockSections) {
            String keyword = containsAny(text, appRules.sectionKeywords);
            if (keyword != null) {
                return Match.blocked("section", keyword);
            }
        }
        if (appRules.blockAds) {
            String ad = containsAny(text, appRules.adKeywords);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
            ad = containsAny(text, RuleKeys.defaultAdKeywordsForPackage(packageName));
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
            ad = containsAny(text, RuleKeys.AD_KEYWORDS);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
        }
        if (appRules.blockLive) {
            String live = containsAny(text, RuleKeys.LIVE_KEYWORDS);
            if (live != null) {
                return Match.blocked("live", live);
            }
        }
        return Match.none();
    }

    private static Match matchViewIdentity(
            Context context,
            RulesSnapshot rules,
            String packageName,
            View view
    ) {
        if (!rules.hasActiveRules(packageName) || view == null) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        StringBuilder identity = new StringBuilder(view.getClass().getName().toLowerCase());
        String resourceName = resourceName(context, view.getId());
        if (!resourceName.isEmpty()) {
            identity.append(' ').append(resourceName);
        }
        Object tag = view.getTag();
        if (tag != null && !(tag instanceof CharSequence)) {
            identity.append(' ').append(tag.getClass().getName().toLowerCase());
        }
        String raw = identity.toString();
        if (appRules.blockAds) {
            String ad = containsAny(raw, AD_VIEW_SIGNATURES);
            if (ad != null) {
                return Match.blocked("ad", ad);
            }
        }
        if (appRules.blockLive) {
            String live = containsAny(raw, LIVE_VIEW_SIGNATURES);
            if (live != null) {
                return Match.blocked("live", live);
            }
        }
        return Match.none();
    }

    private static Match matchResourceBadge(
            Context context,
            RulesSnapshot rules,
            String packageName,
            int resId
    ) {
        if (!rules.hasActiveRules(packageName)) {
            return Match.none();
        }
        RulesSnapshot.AppRules appRules = rules.forPackage(packageName);
        if (!appRules.enabled) {
            return Match.none();
        }
        String name = resourceName(context, resId);
        if (name.isEmpty()) {
            return Match.none();
        }
        if (appRules.blockAds && isAdResourceName(name)) {
            return Match.blocked("ad_resource", name);
        }
        if (appRules.blockLive && containsAny(name, LIVE_RESOURCE_SIGNATURES) != null) {
            return Match.blocked("live_resource", name);
        }
        return Match.none();
    }

    private static boolean isAdResourceName(String name) {
        return "ic_ad".equals(name)
                || name.startsWith("ic_ad_")
                || containsAny(name, AD_RESOURCE_SIGNATURES) != null;
    }

    private static String resourceName(Context context, int id) {
        if (context == null || id == View.NO_ID) {
            return "";
        }
        try {
            return context.getResources().getResourceEntryName(id).toLowerCase();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private static String containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private static void scheduleCollapse(
            Context context,
            TextView textView,
            Match match,
            RulesSnapshot rules,
            String packageName,
            String processName,
            String currentActivity
    ) {
        if (textView.getTag(TAG_COLLAPSED_REASON) instanceof String || textView.getTag(TAG_PENDING_CHECK) != null) {
            return;
        }
        textView.setTag(TAG_PENDING_CHECK, Boolean.TRUE);
        textView.post(() -> {
            textView.setTag(TAG_PENDING_CHECK, null);
            View target = findCollapsibleContainer(textView);
            if (target == null) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] matched but no feed container: " + match
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            Object adapter = adapterFromNearestRecyclerView(textView);
            if (isNonFeedBinding(context, target, adapter, null, packageName, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip non-feed text target: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            if (requiresFeedLikeTarget(match)
                    && !isFeedLikeBinding(context, target, adapter, null, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip user text target outside feed-like binding: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            if (isPagerPageItem(target, null, null)) {
                concealPagerPage(target, match);
                textView.setTag(TAG_COLLAPSED_REASON, match.toString());
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager text target concealed: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            if (looksLikeWholeScreen(target)) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip oversized text target: " + match
                            + " container=" + target.getClass().getName()
                            + " text=" + sanitize(textView.getText()));
                }
                return;
            }
            collapseView(target, match);
            textView.setTag(TAG_COLLAPSED_REASON, match.toString());
            if (rules.debugLog) {
                XposedBridge.log("[LCF] text collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " container=" + target.getClass().getName()
                        + " text=" + sanitize(textView.getText()));
            }
        });
    }

    private static void scheduleResourceCollapse(
            Context context,
            View leaf,
            Match match,
            RulesSnapshot rules,
            String packageName,
            String processName,
            String currentActivity,
            String source
    ) {
        if (leaf.getTag(TAG_COLLAPSED_REASON) instanceof String || leaf.getTag(TAG_PENDING_CHECK) != null) {
            return;
        }
        leaf.setTag(TAG_PENDING_CHECK, Boolean.TRUE);
        leaf.post(() -> {
            leaf.setTag(TAG_PENDING_CHECK, null);
            View target = findCollapsibleContainer(leaf);
            if (target == null) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] resource matched but no feed container: " + match
                            + " source=" + source
                            + " view=" + leaf.getClass().getName());
                }
                return;
            }
            Object adapter = adapterFromNearestRecyclerView(leaf);
            if (isNonFeedBinding(context, target, adapter, null, packageName, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip non-feed resource target: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName());
                }
                return;
            }
            if (requiresFeedLikeTarget(match)
                    && !isFeedLikeBinding(context, target, adapter, null, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip user resource target outside feed-like binding: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName());
                }
                return;
            }
            if (isPagerPageItem(target, adapter, null)) {
                concealPagerPage(target, match);
                leaf.setTag(TAG_COLLAPSED_REASON, match.toString());
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager resource target concealed: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName());
                }
                return;
            }
            if (looksLikeWholeScreen(target)) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip oversized resource target: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName());
                }
                return;
            }
            collapseView(target, match);
            leaf.setTag(TAG_COLLAPSED_REASON, match.toString());
            if (rules.debugLog) {
                XposedBridge.log("[LCF] resource collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " source=" + source
                        + " container=" + target.getClass().getName()
                        + " view=" + leaf.getClass().getName());
            }
        });
    }

    private static void scheduleSignalCollapse(
            Context context,
            View leaf,
            Match match,
            RulesSnapshot rules,
            String packageName,
            String processName,
            String currentActivity,
            String source,
            CharSequence rawValue
    ) {
        if (leaf.getTag(TAG_COLLAPSED_REASON) instanceof String || leaf.getTag(TAG_PENDING_CHECK) != null) {
            return;
        }
        leaf.setTag(TAG_PENDING_CHECK, Boolean.TRUE);
        leaf.post(() -> {
            leaf.setTag(TAG_PENDING_CHECK, null);
            View target = findCollapsibleContainer(leaf);
            if (target == null) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] signal matched but no feed container: " + match
                            + " source=" + source
                            + " value=" + sanitize(rawValue));
                }
                return;
            }
            Object adapter = adapterFromNearestRecyclerView(leaf);
            if (isNonFeedBinding(context, target, adapter, null, packageName, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip non-feed signal target: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName()
                            + " value=" + sanitize(rawValue));
                }
                return;
            }
            if (requiresFeedLikeTarget(match)
                    && !isFeedLikeBinding(context, target, adapter, null, currentActivity)) {
                restoreView(target);
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip user signal target outside feed-like binding: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName()
                            + " value=" + sanitize(rawValue));
                }
                return;
            }
            if (isPagerPageItem(target, adapter, null)) {
                concealPagerPage(target, match);
                leaf.setTag(TAG_COLLAPSED_REASON, match.toString());
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] pager signal target concealed: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName()
                            + " value=" + sanitize(rawValue));
                }
                return;
            }
            if (looksLikeWholeScreen(target)) {
                if (rules.debugLog) {
                    XposedBridge.log("[LCF] skip oversized signal target: " + match
                            + " source=" + source
                            + " container=" + target.getClass().getName()
                            + " value=" + sanitize(rawValue));
                }
                return;
            }
            collapseView(target, match);
            leaf.setTag(TAG_COLLAPSED_REASON, match.toString());
            if (rules.debugLog) {
                XposedBridge.log("[LCF] signal collapsed package=" + packageName
                        + " process=" + processName
                        + " activity=" + currentActivity
                        + " reason=" + match
                        + " source=" + source
                        + " container=" + target.getClass().getName()
                        + " view=" + leaf.getClass().getName()
                        + " value=" + sanitize(rawValue));
            }
        });
    }

    private static View findCollapsibleContainer(View leaf) {
        View current = leaf;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            ViewParent parent = current.getParent();
            if (parent == null) {
                return null;
            }
            String parentClass = parent.getClass().getName();
            if (isRecyclerContainerClass(parentClass)) {
                return current;
            }
            if (!(parent instanceof View)) {
                return null;
            }
            current = (View) parent;
        }
        return null;
    }

    private static boolean isRecyclerContainerClass(String className) {
        return className.contains("RecyclerView");
    }

    private static boolean isNonFeedBinding(
            Context context,
            View itemView,
            Object adapter,
            Object holder,
            String packageName,
            String currentActivity
    ) {
        if (itemView == null) {
            return false;
        }
        if (isInsideNonFeedSurface(context, itemView)
                || isKnownNonFeedAdapter(adapter)
                || containsNonFeedIdentity(adapter)
                || containsNonFeedIdentity(holder)) {
            return true;
        }
        Object tag = itemView.getTag();
        if (tag != null && !(tag instanceof CharSequence) && containsNonFeedIdentity(tag)) {
            return true;
        }
        if (!isBiliPackage(packageName) || !ActivityClassifier.isContentActivity(currentActivity)) {
            return false;
        }
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] budget = new int[]{0};
        return containsNonFeedModelIdentity(holder, 0, visited, budget)
                || containsNonFeedModelIdentity(tag, 0, visited, budget);
    }

    private static boolean isInsideNonFeedSurface(Context context, View view) {
        View current = view;
        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
            if (isNonFeedSurfaceView(context, current)) {
                return true;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static boolean isNonFeedSurfaceView(Context context, View view) {
        if (view == null) {
            return false;
        }
        if (containsAny(nonFeedIdentity(context, view), NON_FEED_SURFACE_SIGNATURES) != null) {
            return true;
        }
        return view instanceof TextView
                && containsShortVisibleText((TextView) view, NON_FEED_VISIBLE_TEXT_SIGNATURES);
    }

    private static String nonFeedIdentity(Context context, View view) {
        StringBuilder identity = new StringBuilder(view.getClass().getName().toLowerCase());
        String resourceName = resourceName(context, view.getId());
        if (!resourceName.isEmpty()) {
            identity.append(' ').append(resourceName);
        }
        CharSequence description = view.getContentDescription();
        if (description != null) {
            identity.append(' ').append(RulesSnapshot.normalize(description));
        }
        Object tag = view.getTag();
        if (tag instanceof CharSequence) {
            identity.append(' ').append(RulesSnapshot.normalize((CharSequence) tag));
        } else if (tag != null) {
            identity.append(' ').append(tag.getClass().getName().toLowerCase());
        }
        return identity.toString();
    }

    private static boolean isBiliPackage(String packageName) {
        return "tv.danmaku.bili".equals(packageName) || "com.bilibili.app.in".equals(packageName);
    }

    private static boolean isNonFeedActivity(String currentActivity) {
        return containsAny(normalizeIdentityToken(currentActivity), NON_FEED_BINDING_SIGNATURES) != null;
    }

    private static boolean containsNonFeedIdentity(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value instanceof Class ? (Class<?>) value : value.getClass();
        for (int depth = 0; depth < 5 && type != null && type != Object.class; depth++) {
            if (containsAny(normalizeIdentityToken(type.getName()), NON_FEED_BINDING_SIGNATURES) != null) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean containsNonFeedModelIdentity(
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            int[] budget
    ) {
        if (value == null || depth > 2 || budget[0] > 48) {
            return false;
        }
        Class<?> type = value.getClass();
        if (isTerminalModelValue(type) || View.class.isAssignableFrom(type) || Context.class.isAssignableFrom(type)) {
            return false;
        }
        if (visited.containsKey(value)) {
            return false;
        }
        visited.put(value, Boolean.TRUE);
        if (containsNonFeedIdentity(value)) {
            return true;
        }
        if (shouldSkipModelClass(type)) {
            return false;
        }
        Class<?> current = type;
        while (current != null && current != Object.class && budget[0] <= 48) {
            Field[] fields;
            try {
                fields = current.getDeclaredFields();
            } catch (Throwable ignored) {
                break;
            }
            for (Field field : fields) {
                if (budget[0]++ > 48 || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String fieldIdentity = normalizeIdentityToken(field.getName());
                if (containsAny(fieldIdentity, NON_FEED_MODEL_FIELD_SIGNATURES) != null) {
                    return true;
                }
                Object child;
                try {
                    field.setAccessible(true);
                    child = field.get(value);
                } catch (Throwable ignored) {
                    continue;
                }
                if (containsNonFeedModelIdentity(child, depth + 1, visited, budget)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static boolean requiresFeedLikeTarget(Match match) {
        return match != null && match.blocked;
    }

    private static boolean isFeedLikeBinding(
            Context context,
            View itemView,
            Object adapter,
            Object holder,
            String currentActivity
    ) {
        if (itemView == null) {
            return false;
        }
        if (isInsideFeedSurface(context, itemView)
                || containsFeedIdentity(adapter)
                || containsFeedIdentity(holder)
                || containsFeedIdentity(itemView.getTag())) {
            return true;
        }
        String activity = normalizeIdentityToken(currentActivity);
        return activity.contains("feed") || activity.contains("pegasus");
    }

    private static boolean isInsideFeedSurface(Context context, View view) {
        View current = view;
        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
            if (containsAny(nonFeedIdentity(context, current), FEED_BINDING_SIGNATURES) != null) {
                return true;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static boolean containsFeedIdentity(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value instanceof Class ? (Class<?>) value : value.getClass();
        for (int depth = 0; depth < 5 && type != null && type != Object.class; depth++) {
            if (containsAny(normalizeIdentityToken(type.getName()), FEED_BINDING_SIGNATURES) != null) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean containsShortVisibleText(TextView view, String[] signatures) {
        CharSequence raw = view.getText();
        if (raw == null) {
            return false;
        }
        String text = RulesSnapshot.normalize(raw);
        return !text.isEmpty()
                && text.length() <= 16
                && containsAny(text, signatures) != null;
    }

    private static void markNonFeedRecyclerAdapter(Object adapter, View recyclerView) {
        if (adapter == null) {
            return;
        }
        Context context = recyclerView == null ? null : recyclerView.getContext();
        if (!containsNonFeedIdentity(adapter) && !isInsideNonFeedSurface(context, recyclerView)) {
            return;
        }
        synchronized (NON_FEED_RECYCLER_ADAPTERS) {
            NON_FEED_RECYCLER_ADAPTERS.put(adapter, Boolean.TRUE);
        }
    }

    private static boolean isKnownNonFeedAdapter(Object adapter) {
        if (adapter == null) {
            return false;
        }
        synchronized (NON_FEED_RECYCLER_ADAPTERS) {
            return NON_FEED_RECYCLER_ADAPTERS.containsKey(adapter);
        }
    }

    private static Object adapterFromNearestRecyclerView(View leaf) {
        View current = leaf;
        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
            ViewParent parent = current.getParent();
            if (parent == null) {
                return null;
            }
            if (parent instanceof View && isRecyclerContainerClass(parent.getClass().getName())) {
                return adapterFromRecyclerView((View) parent);
            }
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private static Object adapterFromRecyclerView(View recyclerView) {
        if (recyclerView == null) {
            return null;
        }
        try {
            Method method = recyclerView.getClass().getMethod("getAdapter");
            return method.invoke(recyclerView);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPagerPageItem(View itemView, Object adapter, Object holder) {
        if (isKnownPagerAdapter(adapter) || containsPagerIdentity(adapter) || containsPagerIdentity(holder)) {
            return true;
        }
        if (itemView == null) {
            return false;
        }
        ViewParent parent = itemView.getParent();
        if (!(parent instanceof View)) {
            return false;
        }
        View parentView = (View) parent;
        ViewParent grandParent = parentView.getParent();
        if (!(grandParent instanceof View)) {
            return false;
        }
        String parentName = parentView.getClass().getName().toLowerCase();
        String grandParentName = grandParent.getClass().getName().toLowerCase();
        return parentName.contains("recyclerview")
                && (grandParentName.contains("viewpager2") || grandParentName.contains("viewpager"));
    }

    private static boolean markPagerRecyclerAdapter(Object adapter, View recyclerView) {
        ViewParent parent = recyclerView.getParent();
        if (parent == null) {
            return false;
        }
        String parentName = parent.getClass().getName().toLowerCase();
        if (!parentName.contains("viewpager2") && !parentName.contains("viewpager")) {
            return false;
        }
        synchronized (PAGER_RECYCLER_ADAPTERS) {
            if (PAGER_RECYCLER_ADAPTERS.containsKey(adapter)) {
                return true;
            }
            PAGER_RECYCLER_ADAPTERS.put(adapter, Boolean.TRUE);
        }
        XposedBridge.log("[LCF] pager RecyclerView adapter marked: " + adapter.getClass().getName());
        return true;
    }

    private static boolean isKnownPagerAdapter(Object adapter) {
        if (adapter == null) {
            return false;
        }
        synchronized (PAGER_RECYCLER_ADAPTERS) {
            return PAGER_RECYCLER_ADAPTERS.containsKey(adapter);
        }
    }

    private static boolean containsPagerIdentity(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value.getClass();
        for (int depth = 0; depth < 4 && type != null; depth++) {
            String name = type.getName().toLowerCase();
            if (name.contains("viewpager2")
                    || name.contains("viewpager")
                    || name.contains("fragmentstateadapter")
                    || name.contains("fragmentviewholder")) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean looksLikeWholeScreen(View view) {
        View root = view.getRootView();
        if (root == null || root == view || root.getWidth() <= 0 || root.getHeight() <= 0) {
            return false;
        }
        return view.getWidth() >= root.getWidth() * 0.85f
                && view.getHeight() >= root.getHeight() * 0.75f;
    }

    private static void maybeLogUnmatchedFeedItem(
            Context context,
            RulesSnapshot rules,
            String packageName,
            String processName,
            String currentActivity,
            View itemView,
            Object adapter,
            Object holder
    ) {
        if (!rules.debugLog
                || itemView == null
                || !isFeedLikeBinding(context, itemView, adapter, holder, currentActivity)) {
            return;
        }
        String key = packageName
                + "|" + normalizeIdentityToken(currentActivity)
                + "|" + className(adapter)
                + "|" + className(holder);
        if (!takeDebugFeedSample(key)) {
            return;
        }
        StringBuilder ids = new StringBuilder();
        StringBuilder texts = new StringBuilder();
        StringBuilder tags = new StringBuilder();
        collectDebugViewSignals(context, itemView, 0, new int[]{0}, ids, texts, tags);
        XposedBridge.log("[LCF] unmatched feed sample package=" + packageName
                + " process=" + processName
                + " activity=" + currentActivity
                + " adapter=" + className(adapter)
                + " holder=" + className(holder)
                + " container=" + itemView.getClass().getName()
                + " ids=" + ids
                + " texts=" + texts
                + " tags=" + tags);
    }

    private static boolean takeDebugFeedSample(String key) {
        synchronized (DEBUG_FEED_SAMPLE_COUNTS) {
            Integer count = DEBUG_FEED_SAMPLE_COUNTS.get(key);
            if (count == null) {
                if (DEBUG_FEED_SAMPLE_COUNTS.size() >= MAX_DEBUG_FEED_SAMPLE_KEYS) {
                    return false;
                }
                DEBUG_FEED_SAMPLE_COUNTS.put(key, 1);
                return true;
            }
            if (count >= MAX_DEBUG_FEED_SAMPLES_PER_KEY) {
                return false;
            }
            DEBUG_FEED_SAMPLE_COUNTS.put(key, count + 1);
            return true;
        }
    }

    private static void collectDebugViewSignals(
            Context context,
            View view,
            int depth,
            int[] views,
            StringBuilder ids,
            StringBuilder texts,
            StringBuilder tags
    ) {
        if (view == null
                || depth > MAX_DEBUG_SAMPLE_DEPTH
                || views[0]++ > MAX_DEBUG_SAMPLE_VIEWS
                || isNonFeedSurfaceView(context, view)) {
            return;
        }
        String id = resourceName(context, view.getId());
        if (!id.isEmpty()) {
            appendDebugValue(ids, id, 16);
        }
        if (view instanceof TextView && !(view instanceof EditText)) {
            appendDebugText(texts, ((TextView) view).getText());
        }
        appendDebugText(texts, view.getContentDescription());
        Object tag = view.getTag();
        if (tag instanceof CharSequence) {
            appendDebugText(texts, (CharSequence) tag);
        } else if (tag != null) {
            appendDebugValue(tags, className(tag), 10);
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            collectDebugViewSignals(context, group.getChildAt(i), depth + 1, views, ids, texts, tags);
        }
    }

    private static void appendDebugText(StringBuilder builder, CharSequence value) {
        String text = trimDebugText(value);
        if (!text.isEmpty()) {
            appendDebugValue(builder, text, 8);
        }
    }

    private static void appendDebugValue(StringBuilder builder, String value, int maxValues) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int count = 0;
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == '|') {
                count++;
            }
        }
        if (builder.length() > 0) {
            count++;
        }
        if (count >= maxValues || builder.indexOf(value) >= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('|');
        }
        builder.append(value);
    }

    private static String trimDebugText(CharSequence value) {
        if (value == null) {
            return "";
        }
        String text = sanitize(value);
        if (text.length() > MAX_DEBUG_SAMPLE_TEXT_CHARS) {
            return text.substring(0, MAX_DEBUG_SAMPLE_TEXT_CHARS) + "...";
        }
        return text;
    }

    private static String className(Object value) {
        return value == null ? "" : value.getClass().getName();
    }

    private static void collapseView(View view, Match match) {
        if (!(view.getTag(TAG_ORIGINAL_HEIGHT) instanceof Integer)) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            view.setTag(TAG_ORIGINAL_HEIGHT, params == null ? ViewGroup.LayoutParams.WRAP_CONTENT : params.height);
        }
        view.setTag(TAG_COLLAPSED_REASON, match.toString());
        view.setVisibility(View.GONE);
        view.setAlpha(0f);
        view.setMinimumHeight(0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.height != 0) {
            params.height = 0;
            view.setLayoutParams(params);
        }
        view.requestLayout();
    }

    private static void concealPagerPage(View view, Match match) {
        view.setTag(TAG_COLLAPSED_REASON, match.toString());
        view.setVisibility(View.INVISIBLE);
        view.setAlpha(0f);
        view.requestLayout();
    }

    private static void restoreView(View view) {
        if (!(view.getTag(TAG_COLLAPSED_REASON) instanceof String)) {
            return;
        }
        view.setTag(TAG_COLLAPSED_REASON, null);
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        Object originalHeight = view.getTag(TAG_ORIGINAL_HEIGHT);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && originalHeight instanceof Integer) {
            params.height = (Integer) originalHeight;
            view.setLayoutParams(params);
        }
        view.requestLayout();
    }

    private static String sanitize(CharSequence value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > 120) {
            return text.substring(0, 120) + "...";
        }
        return text;
    }

    private static boolean isInternalTagKey(int key) {
        return key == TAG_COLLAPSED_REASON || key == TAG_PENDING_CHECK || key == TAG_ORIGINAL_HEIGHT;
    }

    private static String tagSource(Context context, String source, int key) {
        if (key == 0) {
            return source;
        }
        String resource = resourceName(context, key);
        return resource.isEmpty() ? source + ":" + key : source + ":" + resource;
    }

    private static CharSequence describeTagValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence) {
            return (CharSequence) value;
        }
        return value.getClass().getName();
    }

    private static final class Match {
        final boolean blocked;
        final String type;
        final String keyword;

        private Match(boolean blocked, String type, String keyword) {
            this.blocked = blocked;
            this.type = type;
            this.keyword = keyword;
        }

        static Match none() {
            return new Match(false, "", "");
        }

        static Match blocked(String type, String keyword) {
            return new Match(true, type, keyword);
        }

        @Override
        public String toString() {
            return type + ":" + keyword;
        }
    }
}
