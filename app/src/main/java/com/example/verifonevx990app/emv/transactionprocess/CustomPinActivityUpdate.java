package com.example.verifonevx990app.emv.transactionprocess;

//import com.socsi.smartposapi.ped.KeyCoorInfo;
//import com.socsi.utils.StringUtil;

/*
public class CustomPinActivityUpdate extends AppCompatActivity {
    private static final String TAG = "CustomPin";
    private View v;
    private PinpadPopUpWindow mPinpadPopUpWindow;
    IPinpad iPinpad;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate...");
        setContentView(R.layout.activity_keyboard);
        iPinpad = VFService.getVfPinPad();
//        v = View.inflate(this, R.layout.activity_keyboard, null);
        v = LayoutInflater.from(this).inflate(R.layout.activity_keyboard, null);

//        try {
//            iPinpad = TransBasic.getInstance().idevice.getPinpad(0);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

        mPinpadPopUpWindow = new PinpadPopUpWindow(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("TAG", "onAttachedToWindow");
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mPinpadPopUpWindow.showPopupWindow(v);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG, "onWindowFocusChanged forcuse:" + hasFocus);
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus)
            return;

        final Bundle param = new Bundle();
        Bundle globleparam = new Bundle();
        String panBlock = "6226901508781352";
        byte[] pinLimit = {0, 4, 6};
        param.putByteArray("pinLimit", pinLimit);
        param.putInt("timeout", 60);
        param.putBoolean("isOnline", true);
        param.putString("pan", panBlock);
        param.putString("promptString", "please input pin:");
        param.putInt("desType", 1);
        byte[] num = {0,1,2,3,4,5,6,7,8,9};
        param.putByteArray("displayKeyValue", num);  // TODO - Demo, this is the demo using static sequence of the PIN numbers

        final PinInputListener pinInputListener = new PinInputListener.Stub() {
            @Override
            public void onInput(int len, int key) throws RemoteException {
                // the key is * always.
                Log.d("TAG", "onInput, length:" + len + ",key:" + key);
                char[] buf = new char[len];
                Arrays.fill(buf, '#');
                final String s = String.valueOf(buf);
                Log.d("TAG", s );
                // call up UI to update the view

                View contentView = mPinpadPopUpWindow.getContentView();
                contentView.post(new Runnable() {

                    @Override
                    public void run() {
                        mPinpadPopUpWindow.dsp_pwd.setText(s);
                    }
                });

            }

            @Override
            public void onConfirm(byte[] data, boolean isNonePin) throws RemoteException {
                Log.d("TAG", "onConfirm");
                exitKeyBoardOnUI();
//                Log.d("TAG", data.toString());
                Log.d("TAG", Utility.byte2HexStr(data));
            }

            @Override
            public void onCancel() throws RemoteException {
                Log.d("TAG", "onCannel");
                exitKeyBoardOnUI();
            }

            @Override
            public void onError(int errorCode) throws RemoteException {
                Log.d("TAG", "onError:" + errorCode);
                exitKeyBoardOnUI();
            }
        };

        Log.d(TAG, "before contentView.post" );
        View contentView = mPinpadPopUpWindow.getContentView();
        Handler handler = new Handler();
        boolean ret = handler.post(
//        boolean ret = contentView.post(
                new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "start the pinpad");
                List<PinKeyCoorInfo> pinKeys = mPinpadPopUpWindow.getKeyCoorInfos();
                try {
                    Log.d(TAG, "pinKeys size:" + pinKeys.size() );
                    Map<String, String> keyMap = iPinpad.initPinInputCustomView(1, param, pinKeys, pinInputListener);
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
                    iPinpad.startPinInputCustomView();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        Log.d(TAG, "post return: " + ret);

    }

    public void exitKeyBoardOnUI() {
        Log.i("TAG", "exitKeyBoardOnUI");
        runOnUiThread(new Runnable() {
            public void run() {
                CustomPinActivityUpdate.this.mPinpadPopUpWindow.dismiss();
                CustomPinActivityUpdate.this.finish();
            }
        });
        try {
            iPinpad.endPinInputCustomView();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    class PinpadPopUpWindow extends PopupWindow implements View.OnClickListener {
        private View conentView;
        public ImageButton[] btn = new ImageButton[10];
        public ImageButton btn_conf, btn_cancel, btn_del;
        public EditText dsp_pwd;
        private Map<String, ImageButton> keysMap = new HashMap<>();

        public PinpadPopUpWindow(final Activity context) {
            Log.d(TAG, "PinpadPopUpWindow");
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            conentView = inflater.inflate(R.layout.activity_inner_pwd_layout, null);
            this.setContentView(conentView);

            setBackgroundAlpha(0.5f);               // 设置背景透明度
            this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            this.setFocusable(true);
            this.setOutsideTouchable(false);
            this.update();

            //背景
            ColorDrawable dw = new ColorDrawable(0);
            this.setBackgroundDrawable(dw);
            findView(conentView);
        }

        public void showPopupWindow(View parent) {
            if (!this.isShowing()) {
                this.showAtLocation(parent, Gravity.BOTTOM, 0, 0);
            } else {
                this.dismiss();
            }
        }
        public View getConentView() {
            return conentView;
        }

        // 设置屏幕背景透明度
        private void setBackgroundAlpha(float alpha) {
            WindowManager.LayoutParams mLayoutParams = getWindow().getAttributes();
            mLayoutParams.alpha = alpha;
            getWindow().setAttributes(mLayoutParams);
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
    }

    public static final int TYPE_NUM = 0;
    public static final int TYPE_CONF = 1;
    public static final int TYPE_CANCEL = 2;
    public static final int TYPE_DEL = 3;
    public static final int TYPE_DEL_ALL = 4;

}
*/
