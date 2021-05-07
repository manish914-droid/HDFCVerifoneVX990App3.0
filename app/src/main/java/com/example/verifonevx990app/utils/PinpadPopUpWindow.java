package com.example.verifonevx990app.utils;

/*
public class PinpadPopUpWindow extends AppCompatDialog implements View.OnClickListener {
    private Handler handler;
    private View conentView;
    public static OnPwdListener listener;
    public ImageButton[] btn = new ImageButton[10];
    public ImageButton btn_conf, btn_cancel, btn_del;
    public EditText dsp_pwd;
    private Map<String, ImageButton> keysMap = new HashMap<>();
    private Boolean isFirstStart = true;
    private CardProcessedDataModal cardProcessedDataModal;

    public PinpadPopUpWindow(@NonNull Context context) {
        super(context);
    }

    public PinpadPopUpWindow(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected PinpadPopUpWindow(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public PinpadPopUpWindow(Context context, Handler handler, CardProcessedDataModal cardProcessedDataModal, String title, String prompt) {
        this(context, R.style.popup_dialog);
        this.handler = handler;
        this.cardProcessedDataModal = cardProcessedDataModal;
      //  this.title = title;
      //  this.prompt = prompt;

    }

    public static OnPwdListener getPwdListener() {
        return listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View convertView = getLayoutInflater().inflate(R.layout.activity_inner_pwd_layout, null);
        setContentView(convertView);
        getWindow().setGravity(Gravity.BOTTOM); // 显示在底部
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setWindowAnimations(R.style.popup_anim_style); // 添加动画
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = 750;
        isFirstStart = true;
        getWindow().setAttributes(lp);
        Log.i("InputPwdDialog", "onCreate");

        findView(convertView);
        Log.i("InputPwdDialog", "initViews");
        //TickTimer.cancle();
    }

    //当页面加载完成之后再执行弹出键盘的动作
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {

            final Bundle param = new Bundle();
            Bundle globleparam = new Bundle();
            String panBlock = cardProcessedDataModal.getPanNumberData();
            byte[] pinLimit = {4,5,6};
            param.putByteArray("pinLimit", pinLimit);
            param.putInt("timeout", 60);
            switch (cardProcessedDataModal.getIsOnline()) {
              case  1:
                  param.putBoolean(ConstIPinpad.startPinInput.param.KEY_isOnline_boolean, true);
              break;
             case   2 : param.putBoolean(ConstIPinpad.startPinInput.param.KEY_isOnline_boolean, false);
               break;
            }
            param.putBoolean("isOnline", true);
            param.putString("pan", panBlock);
            param.putString("promptString", "please input pin:");
            param.putInt("desType", ConstIPinpad.startPinInput.param.Value_desType_3DES);
            byte[] num = {0,1,2,3,4,5,6,7,8,9};
            param.putByteArray("displayKeyValue", num);  // TODO - Demo, this is the demo using static sequence of the PIN numbers

            final PinInputListener pinInputListener = new PinInputListener.Stub() {
                @Override
                public void onInput(int len, int key) throws RemoteException {
                    // the key is * always.
                    Log.d("TAG", "onInput, length:" + len + ",key:" + key);
                    char[] buf = new char[len];
                    Arrays.fill(buf, '*');
                    final String s = String.valueOf(buf);
                    Log.d("TAG", s );
                    // call up UI to update the view

                    setContentText(s);

                }

                @Override
                public void onConfirm(byte[] data, boolean isNonePin) throws RemoteException {
                    Log.d("TAG", "onConfirm");
                    VFService.getVfIEMV().importPin(1,data);
              //      exitKeyBoardOnUI();
//                Log.d("TAG", data.toString());
                    Log.d("TAG", Utility.byte2HexStr(data));
                    listener.onSucc(data);
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d("TAG", "onCannel");
               //     exitKeyBoardOnUI();
                    listener.onErr();
                }

                @Override
                public void onError(int errorCode) throws RemoteException {
                    Log.d("TAG", "onError:" + errorCode);
                    //exitKeyBoardOnUI();
                    listener.onErr();
                }
            };


            Handler handler = new Handler();
            boolean ret = handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "start the pinpad");
                            List<PinKeyCoorInfo> pinKeys = getKeyCoorInfos();
                            try {
                                Log.d(TAG, "pinKeys size:" + pinKeys.size() );
                                Map<String, String> keyMap = VFService.getVfPinPad().initPinInputCustomView(2, param, pinKeys, pinInputListener);
                                Log.d("TAG", "size:" + keyMap.size());
                                Set<Map.Entry<String, String>> entrys = keyMap.entrySet();
                                for (Map.Entry<String, String> entry : entrys) {
                                    Log.d("TAG", entry.getKey() + "--" + entry.getValue());
                                    Button btn = null;
                                    int index = entry.getKey().charAt(4) - '0';
                                    if( index >= 0 && index < 10 ) {
//                            mPinpadPopUpWindow.btn[index].setText(entry.getValue() + "\n[" + index + "]");
                                        if( entry.getValue().equals("2") ) {
                                            //    mPinpadPopUpWindow.btn[index].setText(entry.getValue());
                                        } else {
                                            //  mPinpadPopUpWindow.btn[index].setText(entry.getValue());
                                        }
                                    }
                                }
                                VFService.getVfPinPad().startPinInputCustomView();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        }
                    });

        }
        else listener.onErr();
    }



    public void setContentText(final String content) {
        if (handler != null) {
            handler.post(() -> {
                if (dsp_pwd != null) {
                    dsp_pwd.setText(content);
                    dsp_pwd.setTextSize(25);
                }
            });
        }

    }

    private void findView(View keyboardView) {

        btn[0] = keyboardView.findViewById(R.id.keyboard_btn0);
        btn[1] = keyboardView.findViewById(R.id.keyboard_btn1);
        btn[2] = keyboardView.findViewById(R.id.keyboard_btn2);
        btn[3] = keyboardView.findViewById(R.id.keyboard_btn3);
        btn[4] = keyboardView.findViewById(R.id.keyboard_btn4);
        btn[5] = keyboardView.findViewById(R.id.keyboard_btn5);
        btn[6] = keyboardView.findViewById(R.id.keyboard_btn6);
        btn[7] = keyboardView.findViewById(R.id.keyboard_btn7);
        btn[8] = keyboardView.findViewById(R.id.keyboard_btn8);
        btn[9] = keyboardView.findViewById(R.id.keyboard_btn9);
        btn_del = keyboardView.findViewById(R.id.keyboard_btn_delete);
        btn_cancel = keyboardView.findViewById(R.id.keyboard_btn_cancel);
        btn_conf = keyboardView.findViewById(R.id.keyboard_btn_confirm);
        for (int i = 0; i < 10; i++) {
            btn[i].setOnClickListener(this);
        }

        btn_conf.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_del.setOnClickListener(this);

        dsp_pwd = keyboardView.findViewById(R.id.dsp_pwd);
    }

    @Override
    public void onClick(View v) {
        Log.d("TAG", "onclick...");
    }

    public List<PinKeyCoorInfo> getKeyCoorInfos() {
        List<PinKeyCoorInfo> keyCoorInfos = new ArrayList<>();

        for( int i =0; i< 10 ; i++ ) {
            keyCoorInfos.add(getKeyCoorInfo("btn_" + i, btn[i], TYPE_NUM));
        }
        keyCoorInfos.add(getKeyCoorInfo("btn_10", btn_conf, TYPE_CONF));
        keyCoorInfos.add(getKeyCoorInfo("btn_11", btn_cancel, TYPE_CANCEL));
        keyCoorInfos.add(getKeyCoorInfo("btn_12", btn_del, TYPE_DEL));
//            keyCoorInfos.add(getKeyCoorInfo("btn_12", btn_del, KeyCoorInfo.TYPE_DEL_ALL));
        return keyCoorInfos;
    }

    private PinKeyCoorInfo getKeyCoorInfo(String keyId, ImageButton btn, int type) {
        keysMap.put(keyId, btn); // get the coordinate information and save to may
        //按钮保存到Map中，keyid与返回的map中keyid对应从而找到那个按钮，修改该button的显示内容
        int[] location = new int[2];
        btn.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        return new PinKeyCoorInfo(keyId, x, y, x + btn.getWidth(), y + btn.getHeight(), type);
    }

    public static final int TYPE_NUM = 0;
    public static final int TYPE_CONF = 1;
    public static final int TYPE_CANCEL = 2;
    public static final int TYPE_DEL = 3;
    public static final int TYPE_DEL_ALL = 4;

    public interface OnPwdListener {
        void onSucc(byte[] data);

        void onErr();
    }

}*/
