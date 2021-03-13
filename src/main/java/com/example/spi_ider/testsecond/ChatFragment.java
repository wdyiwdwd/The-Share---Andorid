package com.example.spi_ider.testsecond;


import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseBaseFragment;
import com.hyphenate.EMMessageListener;
import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.easeui.widget.EaseChatExtendMenu;
import com.hyphenate.easeui.widget.EaseChatInputMenu;
import com.hyphenate.easeui.widget.EaseChatMessageList;
import com.hyphenate.easeui.widget.EaseVoiceRecorderView;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.easeui.widget.chatrow.EaseCustomChatRowProvider;

import java.util.List;

/**
 * Created by Spi-ider on 2017/5/2.
 */

public class ChatFragment extends EaseBaseFragment implements EMMessageListener,EaseChatFragment.EaseChatFragmentHelper {
    protected static final String TAG = "EaseChatFragment";
    protected static final int REQUEST_CODE_LOCAL = 3;
    protected static final int REQUEST_CODE_VOICE_CALL=4;

    final String isAssisting = "isAssisting";

    private static final int MESSAGE_TYPE_SENT_VOICE_CALL = 1;
    private static final int MESSAGE_TYPE_RECV_VOICE_CALL = 2;
    protected int chatType ;

    protected Bundle fragmentArgs;
    protected String toChatUsername;
    protected EaseChatMessageList messageList;
    protected EaseChatInputMenu inputMenu;
    protected SwipeRefreshLayout swipeRefreshLayout;

    protected EMConversation conversation;

    protected InputMethodManager inputManager;
    protected ClipboardManager clipboard;
    //protected Handler handler = new Handler();
    protected EaseVoiceRecorderView voiceRecorderView;
   // protected SwipeRefreshLayout swipeRefreshLayout;
    protected ListView listView;

    protected int pagesize = 20;

    protected EMMessage contextMenuMessage;

    static final int ITEM_PICTURE = 1;
    static final int ITEM_VOICE_CALL = 4;//注册的时候文档要求自己添加的id 要大于3（when keep exist item）..

    protected int[] itemStrings = {com.hyphenate.easeui.R.string.attach_picture, R.string.remote_assist};
    protected int[] itemdrawables = { com.hyphenate.easeui.R.drawable.ease_chat_image_selector,R.drawable.voicecall_item_selector };
    protected int[] itemIds = { ITEM_PICTURE,ITEM_VOICE_CALL};
    private boolean isMessageListInited;
    protected MyItemClickListener extendMenuItemClickListener;
    protected EaseChatFragment.EaseChatFragmentHelper chatFragmentHelper;

//
//    public void setChatFragmentListener(EaseChatFragment.EaseChatFragmentHelper chatFragmentHelper){
//        this.chatFragmentHelper = chatFragmentHelper;
//    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        fragmentArgs = getArguments();
        // check if single chat or group chat ------single chat only! (For Now)
        chatType = fragmentArgs.getInt(EaseConstant.EXTRA_CHAT_TYPE, EaseConstant.CHATTYPE_SINGLE);
        //          最关键的一个属性！
        toChatUsername = fragmentArgs.getString(EaseConstant.EXTRA_USER_ID);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isMessageListInited)
            messageList.refresh();
        EaseUI.getInstance().pushActivity(getActivity());
        // register the event listener when enter the foreground
        EMClient.getInstance().chatManager().addMessageListener(this);
    }
    @Override
    public void onStop() {
        super.onStop();
        // unregister this event listener when this activity enters the
        // background
        EMClient.getInstance().chatManager().removeMessageListener(this);

        // remove activity from foreground activity list
        EaseUI.getInstance().popActivity(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void onSetMessageAttributes(EMMessage message){}

    /**
     * enter to chat detail
     */
    public void onEnterToChatDetails(){}

    /**
     * on avatar clicked
     * @param username
     */
    public void onAvatarClick(String username){    }//按人名的时候

    /**
     * on avatar long pressed
     * @param username
     */
    public void onAvatarLongClick(String username){}

    /**
     * on message bubble clicked
     */
    public boolean onMessageBubbleClick(EMMessage message){return true;}

    /**
     * on message bubble long pressed
     */
    public void onMessageBubbleLongClick(EMMessage message){}

    /**
     * on extend menu item clicked, return true if you want to override
     * @param view
     * @param itemId
     * @return
     */
    public boolean onExtendMenuItemClick(int itemId, View view){
        return false;//直接返回false  因为下面MyItemClickListener已经写了相应方法，想覆盖时才返回true并上面写出相应方式
    }

    /**
     * on set custom chat row provider
     * @return
     */
    public EaseCustomChatRowProvider onSetCustomChatRowProvider(){return new CustomChatRowProvider();}

    @Override
    public void onMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            String username= message.getFrom();
            // if the message is for current conversation
            if(!message.getBooleanAttribute(isAssisting,false)) {
                if (username.equals(toChatUsername) || message.getTo().equals(toChatUsername)) {
                    Log.v("接收消息 onMessageReceived", "" + message.getBooleanAttribute(isAssisting, false));
                    //如果不是远程协助时的消息(坐标信息)时
                    messageList.refreshSelectLast();
                    EaseUI.getInstance().getNotifier().vibrateAndPlayTone(message);
                    conversation.markMessageAsRead(message.getMsgId());
                } else {
                    EaseUI.getInstance().getNotifier().onNewMsg(message);
                }
            }
        }
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {

    }

    @Override
    public void onMessageRead(List<EMMessage> messages) {
        if(isMessageListInited) {
            messageList.refresh();
        }
    }

    @Override
    public void onMessageDelivered(List<EMMessage> messages) {
        if(isMessageListInited) {
            messageList.refresh();
        }
    }

    @Override
    public void onMessageChanged(EMMessage emMessage, Object change) {
        if(isMessageListInited) {
            messageList.refresh();
        }
    }


    /**
     * init view
     */
    protected void initView() {
        // 发送语音消息按钮
        //noinspection ConstantConditions
        voiceRecorderView = (EaseVoiceRecorderView) getView().findViewById(R.id.voice_recorder);

        // message list layout
        messageList = (EaseChatMessageList) getView().findViewById(R.id.message_list);
        listView = messageList.getListView();

        extendMenuItemClickListener = new MyItemClickListener();
        inputMenu = (EaseChatInputMenu) getView().findViewById(R.id.input_menu);
        registerExtendMenuItem();
        // init input menu
        inputMenu.init(null);
        inputMenu.setChatInputMenuListener(new EaseChatInputMenu.ChatInputMenuListener() {

            @Override
            public void onSendMessage(String content) {
                sendTextMessage(content);
            }

            @Override
            public boolean onPressToSpeakBtnTouch(View v, MotionEvent event) {
                return voiceRecorderView.onPressToSpeakBtnTouch(v, event, new EaseVoiceRecorderView.EaseVoiceRecorderCallback() {

                    @Override
                    public void onVoiceRecordComplete(String voiceFilePath, int voiceTimeLength) {
                        sendVoiceMessage(voiceFilePath, voiceTimeLength);
                    }
                });
            }

            @Override
            public void onBigExpressionClicked(EaseEmojicon emojicon) {
                sendBigExpressionMessage(emojicon.getName(), emojicon.getIdentityCode());
            }
        });
//不知道有没有必要
        swipeRefreshLayout = messageList.getSwipeRefreshLayout();
        swipeRefreshLayout.setColorSchemeResources(com.hyphenate.easeui.R.color.holo_blue_bright, com.hyphenate.easeui.R.color.holo_green_light,
                com.hyphenate.easeui.R.color.holo_orange_light, com.hyphenate.easeui.R.color.holo_red_light);
        setRefreshLayoutListener();
        inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    protected void setUpView() {
        titleBar.setTitle(toChatUsername);//最上面的标题栏
        //setChatFragmentListener(this);
            // set title
        if(EaseUserUtils.getUserInfo(toChatUsername) != null){
            EaseUser user = EaseUserUtils.getUserInfo(toChatUsername);
            if (user != null) {
                titleBar.setTitle(user.getNick());
            }
        }
        if (chatType != EaseConstant.CHATTYPE_CHATROOM) {
            onConversationInit();
            onMessageListInit();
        }

        titleBar.setRightImageResource(com.hyphenate.easeui.R.drawable.ease_mm_title_remove);
        titleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        // show forward message if the message is not null
        String forward_msg_id = getArguments().getString("forward_msg_id");
        if (forward_msg_id != null) {
            forwardMessage(forward_msg_id);
        }
    }
    
    protected void setRefreshLayoutListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (listView.getFirstVisiblePosition() == 0 ) {
                            List<EMMessage> messages = null;
                            try {
                                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                                    messages = conversation.loadMoreMsgFromDB(messageList.getItem(0).getMsgId(),
                                            pagesize);
                                }
                            } catch (Exception e1) {
                                swipeRefreshLayout.setRefreshing(false);
                                return;
                            }
                            if (messages!=null && messages.size() > 0) {
                                messageList.refreshSeekTo(messages.size() - 1);
                            }
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 600);
            }
        });
    }

    protected void forwardMessage(String forward_msg_id) {
        final EMMessage forward_msg = EMClient.getInstance().chatManager().getMessage(forward_msg_id);
        EMMessage.Type type = forward_msg.getType();
        switch (type) {
            case TXT:
                Log.v("接收消息 forwardMessage",""+forward_msg.getBooleanAttribute(isAssisting,false));
                if(forward_msg.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)){
                    sendBigExpressionMessage(((EMTextMessageBody) forward_msg.getBody()).getMessage(),
                            forward_msg.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXPRESSION_ID, null));
                }else if(!forward_msg.getBooleanAttribute(isAssisting, false)){
                    // get the content and send it
                    String content = ((EMTextMessageBody) forward_msg.getBody()).getMessage();
                    sendTextMessage(content);
                }
                break;

            default:
                Log.v(TAG,"other forward_msg type ");
                break;
        }

        if(forward_msg.getChatType() == EMMessage.ChatType.ChatRoom){
            EMClient.getInstance().chatroomManager().leaveChatRoom(forward_msg.getTo());
        }
    }
    public void onBackPressed() {
        if (inputMenu.onBackPressed()) {
            getActivity().finish();
        }
    }

    protected void onConversationInit(){
        conversation = EMClient.getInstance().chatManager().getConversation(toChatUsername, EaseCommonUtils.getConversationType(chatType), true);
        conversation.markAllMessagesAsRead();
        // the number of messages loaded into conversation is getChatOptions().getNumberOfMessagesLoaded
        // you can change this number
        final List<EMMessage> msgs = conversation.getAllMessages();
        int msgCount = msgs != null ? msgs.size() : 0;
        if (msgCount < conversation.getAllMsgCount() && msgCount < pagesize) {
            String msgId = null;
            if (msgs != null && msgs.size() > 0) {
                msgId = msgs.get(0).getMsgId();
            }
            conversation.loadMoreMsgFromDB(msgId, pagesize - msgCount);
        }

    }

    protected void onMessageListInit(){
        messageList.init(toChatUsername,chatType, chatFragmentHelper != null ?
                chatFragmentHelper.onSetCustomChatRowProvider() : null);
        setListItemClickListener();

        messageList.getListView().setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                inputMenu.hideExtendMenuContainer();
                return false;
            }
        });

        isMessageListInited = true;
    }
    /**
     * hide
     */
    protected void hideKeyboard() {
        if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getActivity().getCurrentFocus() != null)
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    protected void setListItemClickListener() {
        messageList.setItemClickListener(new EaseChatMessageList.MessageListItemClickListener() {

            @Override
            public void onUserAvatarClick(String username) {
                if(chatFragmentHelper != null){
                    chatFragmentHelper.onAvatarClick(username);
                }
            }

            @Override
            public void onUserAvatarLongClick(String username) {
                if(chatFragmentHelper != null){
                    chatFragmentHelper.onAvatarLongClick(username);
                }
            }

            @Override
            public void onResendClick(final EMMessage message) {
                new EaseAlertDialog(getActivity(), com.hyphenate.easeui.R.string.resend, com.hyphenate.easeui.R.string.confirm_resend, null, new EaseAlertDialog.AlertDialogUser() {
                    @Override
                    public void onResult(boolean confirmed, Bundle bundle) {
                        if (!confirmed) {
                            return;
                        }
                        resendMessage(message);
                    }
                }, true).show();
            }

            @Override
            public void onBubbleLongClick(EMMessage message) {
                contextMenuMessage = message;
                if(chatFragmentHelper != null){
                    chatFragmentHelper.onMessageBubbleLongClick(message);
                }
            }

            @Override
            public boolean onBubbleClick(EMMessage message) {
                if(chatFragmentHelper == null){
                    return false;
                }
                return chatFragmentHelper.onMessageBubbleClick(message);
            }

        });
    }

    /**
     * register extend menu, item id need > 3 if you override this method and keep exist item
     */
    protected void registerExtendMenuItem(){
        for(int i = 0; i < itemStrings.length; i++){
            inputMenu.registerExtendMenuItem(itemStrings[i], itemdrawables[i], itemIds[i], extendMenuItemClickListener);
        }
    }
    protected void sendBigExpressionMessage(String name, String identityCode){
        EMMessage message = EaseCommonUtils.createExpressionMessage(toChatUsername, name, identityCode);
        sendMessage(message);
    }

    protected void sendVoiceMessage(String filePath, int length) {
        EMMessage message = EMMessage.createVoiceSendMessage(filePath, length, toChatUsername);
        sendMessage(message);
    }

    protected void sendTextMessage(String content) {
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        message.setAttribute(isAssisting,true);
        sendMessage(message);
    }
    protected void sendMessage(EMMessage message){
        if (message == null)
            return;

        message.setChatType(EMMessage.ChatType.Chat);//默认就是单聊  不再设置group 或者 chatroom了
        //send message
        EMClient.getInstance().chatManager().sendMessage(message);
        //refresh ui
        if(isMessageListInited) {
            messageList.refreshSelectLast();
        }
    }

    public void resendMessage(EMMessage message){
        message.setStatus(EMMessage.Status.CREATE);
        EMClient.getInstance().chatManager().sendMessage(message);
        messageList.refresh();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            //获取图片
            switch (requestCode) {
                case REQUEST_CODE_VOICE_CALL:
                    if (data != null) {
                        startActivity(new Intent(getActivity(), VoiceCallActivity.class).putExtra("username", toChatUsername).putExtra("isComingCall", false));
                    }
                    break;
            }
        }

    }
    /**
     * handle the click event for extend menu
     *
     */
    class MyItemClickListener implements EaseChatExtendMenu.EaseChatExtendMenuItemClickListener{

        @Override
        public void onClick(int itemId, View view) {
            if(chatFragmentHelper != null){
                if(chatFragmentHelper.onExtendMenuItemClick(itemId, view)){
                    return;
                }
            }
            switch (itemId) {
                case ITEM_PICTURE:
                    selectPicFromLocal(REQUEST_CODE_LOCAL);
                    break;
                case ITEM_VOICE_CALL:
                    makeVoiceCall();
                    break;
                default:
                    break;
            }
        }

    }
    protected void makeVoiceCall(){
        if (!EMClient.getInstance().isConnected()) {
            Toast.makeText(getActivity(),"未连接服务器，请稍候再试", Toast.LENGTH_SHORT).show();
        } else {
           // selectPicFromLocal(REQUEST_CODE_VOICE_CALL);
            startActivity(new Intent(getActivity(), VoiceCallActivity.class).putExtra("username", toChatUsername).putExtra("isComingCall", false));
            inputMenu.hideExtendMenuContainer();
        }
    }
    protected void selectPicFromLocal(int code) {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, code);
    }


    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override
        public int getCustomChatRowTypeCount() {
            //here the number is the message type in EMMessage::Type
            //which is used to count the number of different chat row
            return 10;
        }

        @Override
        public int getCustomChatRowType(EMMessage message) {
            if(message.getType() == EMMessage.Type.TXT){
                //voice call
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)){
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VOICE_CALL : MESSAGE_TYPE_SENT_VOICE_CALL;
                }
            }
            return 0;
        }

        @Override
        public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            Log.v("接收消息 getCustomCharRow",""+message.getBooleanAttribute(isAssisting,false));
            if(message.getType() == EMMessage.Type.TXT && !message.getBooleanAttribute(isAssisting,false)){
                // voice call or video call
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false) ||
                        message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)){
                    return new ChatRowVoiceCall(getActivity(), message, position, adapter);
                }
            }
            return null;
        }

    }

}
