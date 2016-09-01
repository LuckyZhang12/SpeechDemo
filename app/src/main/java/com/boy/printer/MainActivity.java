package com.boy.printer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.DetectionType;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.Recognition;
import com.nuance.speechkit.RecognitionType;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;
import com.nuance.speechkitsample.Configuration;
import com.nuance.speechkitsample.R;

public class MainActivity extends Activity implements View.OnClickListener{

	private static final String TAG = "MainActivity";

	String mMsg;
	String mIp;
	int mPort = 9100;

	Printer mPrinter;

	Button mInit;
	Button mSend;
	EditText mTxtIp;
	EditText mContent;
	TextView mStatus;
	
	Button mStartSpeak;
    TextView mText;
    ImageView mImageView;

    CheckBox cbCheck;

	// 语音识别 start

    private Audio startEarcon;
    private Audio stopEarcon;
    private Audio errorEarcon;

    private Session speechSession;
    private Transaction recoTransaction;
    private State state = State.IDLE;

    private String language = "kor-KOR";// 默认是别韩语

    private boolean isCancled;
    // 语音识别 end


	public static final int PRINTER_INIT_FINISH = 1;
	public static final int SEND_MESSAGE_FINISH = 2;

	public static final int PRINTER_INIT_ERROR = -1;
	public static final int SEND_MESSAGE_ERROR= -2;

	Handler mUIHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what){
				case PRINTER_INIT_FINISH:
					mStatus.setText("打印机初始化成功，可以打印了");
					break ;
				case SEND_MESSAGE_FINISH:
					mStatus.setText("打印成功");
                    mImageView.setImageBitmap(getTextBitmap());
					break ;
				case PRINTER_INIT_ERROR:
					Toast.makeText(MainActivity.this,"初始化打印机错误，退出重新进入",Toast.LENGTH_LONG).show();
                    if (msg.obj != null){
                        mStatus.setText((String)msg.obj);
                    }
					break;
				case SEND_MESSAGE_ERROR:
					Toast.makeText(MainActivity.this,"打印失败",Toast.LENGTH_LONG).show();
					if (mHandler != null){
						mHandler.sendEmptyMessage(DESTROY_PRINTER);
					}
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_asr_printer);

		initView();


        //Create a session
        speechSession = Session.Factory.session(this, Configuration.SERVER_URI, Configuration.APP_KEY);

        loadEarcons();

        setState(State.IDLE);
	}

	private void printResult(Recognition recognition) {
		String text = recognition.getText();
		System.out.println("====text====" + text);

        if (cbCheck.isChecked()){
            return;
        }
		
		if (mHandler == null || mPrinter == null){
			prepareLooper();
			mHandler.sendEmptyMessage(INIT_PRINTER);
		}

		mMsg = text;
		mHandler.sendEmptyMessage(SEND_MESSAGE);

	}

	private void initView(){
		mInit = (Button)findViewById(R.id.btn_init);
		mSend = (Button)findViewById(R.id.btn_send);
		mTxtIp = (EditText)findViewById(R.id.et_ip);
		mContent = (EditText)findViewById(R.id.et_content);
		mStatus = (TextView)findViewById(R.id.status);
        mText = (TextView)findViewById(R.id.tv_text);
        cbCheck = (CheckBox)findViewById(R.id.cb_only_speech);
        mImageView = (ImageView)findViewById(R.id.iv_image);
		
		mStartSpeak = (Button)findViewById(R.id.btn_start_speak);

		mInit.setOnClickListener(this);
		mSend.setOnClickListener(this);
		
		mStartSpeak.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		if (v == mInit){
			mIp = mTxtIp.getText().toString();
			if (TextUtils.isEmpty(mIp)){
				Toast.makeText(this,"请输入打印机的ip",Toast.LENGTH_LONG).show();
				return;
			}

			prepareLooper();
			if(mHandler != null){
				mHandler.sendEmptyMessage(INIT_PRINTER);
			}
			return;
		}

		if (v == mSend){
			mMsg = mContent.getText().toString();
			if (TextUtils.isEmpty(mMsg)){
				Toast.makeText(this,"请输入要打印的内容",Toast.LENGTH_LONG).show();
				return;
			}
			
			if(mHandler != null){
                mText.setText(mMsg);
				mHandler.sendEmptyMessage(SEND_MESSAGE);
			}
			return;
		}
		
		if(v == mStartSpeak){
			// TODO:添加初始化语音识别
            toggleReco();
			return;
		}
	}

	private void initPrinter(){
		try{
			mPrinter = new BitmapPrinter(mIp,mPort);
			mUIHandler.sendEmptyMessage(PRINTER_INIT_FINISH);
		}catch (Exception e){
			e.printStackTrace();
			Log.e(TAG, "初始化打印机错误，退出重新进入");
            Message msg = mUIHandler.obtainMessage(PRINTER_INIT_ERROR,e.toString());
			mUIHandler.sendMessage(msg);
		}
	}

	private void destroyPrinter(){
		if (mPrinter != null){
			mPrinter.destroyPrinter();
		}
	}

	private void printMsg(String msg){
		if (TextUtils.isEmpty(msg)){
			return;
		}
		
		try{
			mPrinter.printMsg(msg);
			mUIHandler.sendEmptyMessage(SEND_MESSAGE_FINISH);
		}catch (Exception e){
			e.printStackTrace();
			Log.e(TAG, "打印出错了");
			mUIHandler.sendEmptyMessage(SEND_MESSAGE_ERROR);
		}

	}

    private void printBitmap(){
        try{
            mPrinter.printBitmap(getTextBitmap());
            mUIHandler.sendEmptyMessage(SEND_MESSAGE_FINISH);
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "打印出错了");
            mUIHandler.sendEmptyMessage(SEND_MESSAGE_ERROR);
        }
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mHandler != null){
			mHandler.sendEmptyMessage(DESTROY_PRINTER);
		}
		
		// 退出时释放连接
        if (recoTransaction != null){
            stopRecording();
        }

	}
	
	private static final int INIT_PRINTER = 0;
	private static final int DESTROY_PRINTER = 1;
	private static final int SEND_MESSAGE = 2;
	
	private void interalHandleMessage(Message msg) {
		switch (msg.what) {
			case INIT_PRINTER:
				initPrinter();
				break;
			case DESTROY_PRINTER:
				if (!releasing && !looperQuited) {
					releasing = true;
					destroyPrinter();
				}
				quitLooper();
				break;
			case SEND_MESSAGE:
//				printMsg(mMsg);
                printBitmap();
				break;
		
		}
	}
	
	private boolean looperQuited = true;
	private boolean releasing = false;
	private	boolean	cancelQuitLooper = false;
	private Handler mHandler;
	private synchronized void prepareLooper() {
		if (releasing) {
			cancelQuitLooper = true;
			return;
		}
		if(!looperQuited){
			return;
		}

		new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {

					@Override
					public void handleMessage(Message msg) {
						interalHandleMessage(msg);
					}
				};
				looperPrepared();
				Looper.loop();
			}
		}.start();
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/** The Looper has been prepared already. */
	}
	private synchronized void looperPrepared() {
		looperQuited = false;
		this.notify();
	}

	protected synchronized void quitLooper() {

		if (cancelQuitLooper) {
			cancelQuitLooper = false;
			releasing = false;
			return;
		}
		mHandler.removeCallbacksAndMessages(null);
		mHandler = null;
		Looper.myLooper().quit();
		notify();
		looperQuited = true;
		releasing = false;
	}

    private enum State {
        IDLE,
        LISTENING,
        PROCESSING
    }

    /* Earcons */

    private void loadEarcons() {
        //Load all the earcons from disk
        startEarcon = new Audio(this, R.raw.sk_start, Configuration.PCM_FORMAT);
        stopEarcon = new Audio(this, R.raw.sk_stop, Configuration.PCM_FORMAT);
        errorEarcon = new Audio(this, R.raw.sk_error, Configuration.PCM_FORMAT);
    }

    /**
     * Set the state and update the button text.
     */
    private void setState(State newState) {
        state = newState;
        switch (newState) {
            case IDLE:
                mStartSpeak.setText("开始说话");
                break;
            case LISTENING:
                mStartSpeak.setText("停止说话");
                break;
            case PROCESSING:
                mStartSpeak.setText("停止说话");
                break;
        }
    }

     /* Reco transactions */

    private void toggleReco() {
        switch (state) {
            case IDLE:
                recognize();
                break;
            case LISTENING:
                stopRecording();
                break;
            case PROCESSING:
                cancel();
                break;
        }
    }

    /**
     * Start listening to the user and streaming their voice to the server.
     */
    private void recognize() {
        //Setup our Reco transaction options.
        Transaction.Options options = new Transaction.Options();
        options.setRecognitionType(RecognitionType.SEARCH);
        options.setDetection(DetectionType.Long);
        options.setLanguage(new Language(language));
        options.setEarcons(startEarcon, stopEarcon, errorEarcon, null);

        //Start listening
        recoTransaction = speechSession.recognize(options, recoListener);

        isCancled = false;
    }

    private Transaction.Listener recoListener = new Transaction.Listener() {
        @Override
        public void onStartedRecording(Transaction transaction) {
            Log.d(TAG,"\nonStartedRecording");

            //We have started recording the users voice.
            //We should update our state and start polling their volume.
            setState(State.LISTENING);
        }

        @Override
        public void onFinishedRecording(Transaction transaction) {
            Log.d(TAG,"\nonFinishedRecording");

            //We have finished recording the users voice.
            //We should update our state and stop polling their volume.
            setState(State.PROCESSING);
        }

        @Override
        public void onRecognition(Transaction transaction, Recognition recognition) {
            Log.d(TAG,"\nonRecognition: " + recognition.getText());
            mText.setText(recognition.getText());
            //We have received a transcription of the users voice from the server.
            setState(State.IDLE);
            printResult(recognition);
            toggleReco();
        }

        @Override
        public void onSuccess(Transaction transaction, String s) {
            Log.d(TAG,"\nonSuccess");

            //Notification of a successful transaction. Nothing to do here.
        }

        @Override
        public void onError(Transaction transaction, String s, TransactionException e) {
            Log.d(TAG,"\nonError: " + e.getMessage() + ". " + s);
            mText.setText("识别出错");

            //Something went wrong. Check Configuration.java to ensure that your settings are correct.
            //The user could also be offline, so be sure to handle this case appropriately.
            //We will simply reset to the idle state.
            setState(State.IDLE);

            if (isCancled){
                return;
            }
            try {
                Thread.currentThread().sleep(500);
            } catch (Exception ex) {
                // TODO: handle exception
                ex.printStackTrace();
            }
            toggleReco();
        }
    };

    /**
     * Stop recording the user
     */
    private void stopRecording() {
        recoTransaction.stopRecording();
        isCancled = true;
    }

    /**
     * Cancel the Reco transaction.
     * This will only cancel if we have not received a response from the server yet.
     */
    private void cancel() {
        recoTransaction.cancel();
        setState(State.IDLE);
        isCancled = true;
    }


    private Bitmap getTextBitmap(){
        int newWidth = 240;
        int newHeight = 240;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.LEFT);

        float textWidth = paint.measureText(mMsg);
        int textNum = mMsg.length();
        float singleTextWidth = textWidth/textNum;// 每个字占的宽度
        // 计算每行有几个字
        int lineNum = (int) (newWidth/singleTextWidth);

        float baseline = -paint.ascent(); // ascent() is negative

        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);

        if (textNum < lineNum){
            targetCanvas.drawText(mMsg,0,baseline,paint);
            return targetBmp;
        }

        int j = 1;
        for (int i=0; i<textNum; i+=lineNum){
            targetCanvas.drawText(mMsg.substring(i,i+lineNum>textNum?textNum:i+lineNum),0,baseline*j,paint);
            j++;
        }
        return targetBmp;
    }


    @Override
    public void onResume() {
        super.onResume();
        //此方法回收截屏资源,如果不不调用，截屏显示一直为第一次截取图片
//        mText.destroyDrawingCache();
    }
}
