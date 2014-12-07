/*********************************************************************/
/* Copyright (c) 2014 TOYOTA MOTOR CORPORATION. All rights reserved. */
/*********************************************************************/

package com.example.sample;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SampleActivity extends Activity implements ICommNotify{
	private static final int REQUEST_BTDEVICE_SELECT = 1;
	private Button _btnConnect;
	private Button _btnDisconnect;
	private Button _btnSelectDevice;
    private Button _btnStartOver;
    private TextView _tvTimestamp;
	private TextView _tvSteering;
    private TextView _tvSpeed;
    private TextView _tvAcceleration;
    private TextView _tvGear;

    private LinearLayout _llContent;
    private TextView _tvContentTop;
    private ImageView _ivContent;
    private TextView _tvContentBottom;

    private SoundPool soundPool;
    private int soundStopId;
    private int soundStraightForwardId;
    private int soundStraightReverseId;
    private int soundRightForwardId;
    private int soundRightReverseId;
    private int soundLeftForwardId;
    private int soundLeftReverseId;

    private int stage = 0;
    private long last_ts = 0;
    private double spaceSize = 0.0;
    private double arcDist = 0.0;
    private double offsetDist = 0.0;

    /* defaults are scion FR-S dimensions (in meters) */
    private final double carLength = 4.27;
    private final double wheelBase= 2.58;
    private final double turningCircle = 5.4;
    private final double frontOH = 0.7;
    private final double carWidth = 1.78;
    private final double alignDist = 0.7;
    private final double minSpace = 5.94;
    private final double turnCircum = 5.5*Math.PI/4;

	/* declaration of Communication class */
	private Communication _comm;

	private Timer _timer;
	private TimerTask _timerTask;

	/* variable of the CAN-Gateway ECU Address */
	private String _strDevAddress = "";

	private final String _tag = "SampleActivity";
	/* interval for sending vehicle signal request (milliseconds) */
	private final int TIMER_INTERVAL = 100;
    private final int TIMESTAMP_ID = 0x01;
    private final int STEERING_WHEEL_ANGLE_ID = 0x08;
    private final int VEHICLE_SPEED_ID = 0x0D;
    private final int ACCELERATION_ID = 0x0E;
    private final int GEAR_ID = 0x07;
	private ByteBuffer _buf = null;
    private static final int MAX_SOUND_POOL_STREAMS = 7;
    private static final int NORMAL_PRIORITY = 10;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* Create the Communication class */
        _comm = new Communication();
        /* Set the Notification interface */
        _comm.setICommNotify(this);

        _tvTimestamp = (TextView)findViewById(R.id.timestamp);
        _tvSteering = (TextView)findViewById(R.id.steering);
        _tvSpeed = (TextView)findViewById(R.id.speed);
        _tvAcceleration = (TextView)findViewById(R.id.acceleration);
        _tvGear = (TextView)findViewById(R.id.gear);

        _btnConnect = (Button)findViewById(R.id.button_connect);
        _btnDisconnect = (Button)findViewById(R.id.button_disconnect);
        _btnSelectDevice = (Button)findViewById(R.id.button_select);
        _btnStartOver = (Button)findViewById(R.id.button_startover);
        _btnConnect.setOnClickListener(_onClickListener);
        _btnDisconnect.setOnClickListener(_onClickListener);
        _btnSelectDevice.setOnClickListener(_onClickListener);
        _btnStartOver.setOnClickListener(_onClickListener);

        _llContent = (LinearLayout)findViewById(R.id.content);
        _llContent.setOnClickListener(_onContentListener);

        _tvContentTop = (TextView)findViewById(R.id.top_content);
        _ivContent = (ImageView)findViewById(R.id.imageView);
        _tvContentBottom = (TextView)findViewById(R.id.bottom_content);

        this.soundPool = new SoundPool(MAX_SOUND_POOL_STREAMS, AudioManager.STREAM_MUSIC, 100);
        this.soundStopId = this.soundPool.load(this.getApplicationContext(), R.raw.stop, 1);
        this.soundStraightForwardId = this.soundPool.load(this.getApplicationContext(), R.raw.straight_forward, 1);
        this.soundStraightReverseId = this.soundPool.load(this.getApplicationContext(), R.raw.straight_reverse, 1);
        this.soundRightForwardId = this.soundPool.load(this.getApplicationContext(), R.raw.right_forward, 1);
        this.soundRightReverseId = this.soundPool.load(this.getApplicationContext(), R.raw.right_reverse, 1);
        this.soundLeftForwardId = this.soundPool.load(this.getApplicationContext(), R.raw.left_forward, 1);
        this.soundLeftReverseId = this.soundPool.load(this.getApplicationContext(), R.raw.left_reverse, 1);

        updateView(0);
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void finish() {
        stopTimer();
        /* Set the Notification interface */
        _comm.setICommNotify(null);
		/* Close the session */
        _comm.closeSession();

        super.finish();
    }

    private void updateView(int content_id) {
        //first tap screen
        if(stage == 0){
            _tvContentTop.setText("Tap When");
            _tvContentBottom.setText("At First Bumper");
            _ivContent.setImageResource(R.drawable.first_tap);
        }
        //second tap screen
        else if(stage == 1){
            _tvContentTop.setText("Tap Again When");
            _tvContentBottom.setText("At Second Bumper");
            _ivContent.setImageResource(R.drawable.second_tap);
        }
        //content screens
        else if(content_id == 1){
            _tvContentTop.setText("Straight");
            _tvContentBottom.setText("Forward");
            _ivContent.setImageResource(R.drawable.straight_forward);
            this.soundPool.play(this.soundStraightForwardId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 2){
            _tvContentTop.setText("Straight");
            _tvContentBottom.setText("Reverse");
            _ivContent.setImageResource(R.drawable.straight_reverse);
            this.soundPool.play(this.soundStraightReverseId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 3){
            _tvContentTop.setText("Hard Right");
            _tvContentBottom.setText("Forward");
            _ivContent.setImageResource(R.drawable.right_forward);
            this.soundPool.play(this.soundRightForwardId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 4){
            _tvContentTop.setText("Hard Right");
            _tvContentBottom.setText("Reverse");
            _ivContent.setImageResource(R.drawable.right_reverse);
            this.soundPool.play(this.soundRightReverseId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 5){
            _tvContentTop.setText("Hard Left");
            _tvContentBottom.setText("Forward");
            _ivContent.setImageResource(R.drawable.left_forward);
            this.soundPool.play(this.soundLeftForwardId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 6){
            _tvContentTop.setText("Hard Left");
            _tvContentBottom.setText("Reverse");
            _ivContent.setImageResource(R.drawable.left_reverse);
            this.soundPool.play(this.soundLeftReverseId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 7){
            _tvContentTop.setText("Stop");
            _tvContentBottom.setText("");
            _ivContent.setImageResource(R.drawable.stop);
            this.soundPool.play(this.soundStopId, 1, 1, NORMAL_PRIORITY, 0, 1);
        }
        else if(content_id == 8){
            _tvContentTop.setText("Parked");
            _tvContentBottom.setText("");
            _ivContent.setImageResource(R.drawable.ok);
        }
    }

    OnClickListener _onClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
            Button btn = (Button) v;
            if (btn == _btnConnect) {
                if (_comm.isCommunication()) {
                    return;
                }
				/* Open the session */
                if (!_comm.openSession(_strDevAddress)) {
                    showAlertDialog("OpenSession Failed");
                }
            } else if (btn == _btnDisconnect) {
                stopTimer();
				/* Close the session */
                _comm.closeSession();
            } else if (btn == _btnSelectDevice) {
                Intent intent = new Intent(SampleActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_BTDEVICE_SELECT);
            } else if (btn == _btnStartOver) {
                stage = 0;
                updateView(0);
            }
        }
	};

    OnClickListener _onContentListener = new OnClickListener(){
        @Override
        public void onClick(View v) {
            LinearLayout ll = (LinearLayout)v;
            if (ll == _llContent){
                //first tap
                if(stage == 0){
                    stage = 1;
                    spaceSize = 0.0;
                    updateView(0);
                }
                //second tap
                else if(stage == 1){
                    //too small
                    if(spaceSize < minSpace){
                        stage = 0;
                    }
                    //enough room, go ahead and park
                    else{
                        stage = 2;
                    }
                }
            }
        }
    };

    private int monitor (long steeringWheelAngle,
                         long velocity,
                         long acceleration,
                         long gear,
                         long deltaT) {
        switch(stage) {
            case 1: return measure(velocity, acceleration, gear, deltaT);
            case 2: return align(velocity, acceleration, gear, deltaT);
            case 3: return rightLock(steeringWheelAngle, velocity, acceleration, gear, deltaT);
            case 4: return leftLock(steeringWheelAngle, velocity, acceleration, gear, deltaT);
            default: stage = 0;
                return 7;//stop
        }
    }

    private int measure(long velocity, long acceleration, long gear,long deltaT ){
        spaceSize += (12 == gear)? -(velocity*deltaT*0.001/3.6+ 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001/3.6+ 0.5*acceleration*deltaT*deltaT*0.0000001;
        return 1;//straight forward
    }

    private int align (long velocity, long acceleration, long gear, long deltaT){
        offsetDist += (12 == gear)? -(velocity*deltaT*0.001/3.6+ 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001/3.6+ 0.5*acceleration*deltaT*deltaT*0.0000001;
        if (alignDist < offsetDist)
            return 1;//straight forward

        if (alignDist > offsetDist){
            offsetDist = 0.0;
            stage = 3;
            return 4;//hard right reverse
        }
        return 7;//stop
    }

    private int rightLock(long steeringAngle, long velocity, long acceleration, long gear, long deltaT){
        arcDist += (12 == gear)? -(velocity*deltaT*0.001/3.6 + 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001/3.6 + 0.5*acceleration*deltaT*deltaT*0.0000001;
        if (turnCircum < arcDist)
            return 4;//hard right reverse

        stage = 4;
        return 6;//hard left reverse
    }

    private int leftLock(long steeringAngle, long velocity, long acceleration, long gear, long deltaT){
        arcDist += (12 == gear)? (velocity*deltaT*0.001/3.6 + 0.5*acceleration*deltaT*deltaT*0.0000001) : -velocity*deltaT*0.001/3.6+ 0.5*acceleration*deltaT*deltaT*0.0000001;
        if (arcDist > 0 )
            return 6;//hard left reverse

        stage = 5;
        return 8;//ok
    }

    @Override
	public void notifyReceiveData(Object data) {
		//Log.d(_tag,String.format("RECEIVE"));
		ByteBuffer rcvData = (ByteBuffer)data;

		/* Combine received messages */
		if(isCombineFrame(rcvData) == true){
			/* all data received */
			if (isFrameCheck(_buf) != true)
			{
				/* frame error */
				_buf.clear();
				_buf = null;
				return;
			}
			else
			{
				rcvData = _buf;
				_buf.clear();
				_buf = null;
			}
		}
		else
		{
			/* all data not received */
			return;
		}

		byte tmps[] = rcvData.array();
		int len = rcvData.limit();
		/* Analyze the message */
		if (isCarInfoGetFrame(rcvData) == true && len >= 8){
			/* Number of signals */
			int dataCount = (int)tmps[4] & 0xff;
			int index = 5;
            long intTimestamp = 0;
            long intSteering = 0;
            long intSpeed = 0;
            long intAcceleration = 0;
            long intGear = 0;
            String strTimestamp = "";
            String strSteering = "";
            String strSpeed = "";
            String strAcceleration = "";
            String strGear = "";
			/* Vehicle signal */
			for (int i = 0 ; i < dataCount ; i++){
				int tmpData = toUint16Value(tmps, index);
				long value   = toUint32Value(tmps, index + 2); 
				int signalID = (tmpData & 0x0fff);
				int stat 	 = ((tmpData >> 12) & 0x0f);
                if (TIMESTAMP_ID == signalID){
                    intTimestamp = value;
                    strTimestamp = String.valueOf(intTimestamp);
                }
				else if (STEERING_WHEEL_ANGLE_ID == signalID){
					value = value & 0x00000FFF;
                    value = ((value + 2048) & 0x00000FFF) - 2048;
                    intSteering = value;
                    strSteering = String.valueOf(intSteering);
				}
                else if(VEHICLE_SPEED_ID == signalID){
                    value = value & 0x000001FF;
                    intSpeed = value;
                    strSpeed = String.valueOf(intSpeed);
                }
                else if(ACCELERATION_ID == signalID){
                    value = value & 0x000007FF;
                    value = -1 * (((value + 1024) & 0x000007FF) - 1024);
                    intAcceleration = value;
                    strAcceleration = String.valueOf(intAcceleration);
                }
                else if(GEAR_ID == signalID){
                    value = value & 0x0000000F;
                    intGear = value;
                    strGear = String.valueOf(intGear);
                }
				//Log.d(_tag,String.format("SIGNALID = %d, SIGNALSTAT = %d, VALUE = %d", signalID,stat,value));
				index += 6;
			}

            updateContents(
                    intTimestamp, intSteering, intSpeed, intAcceleration, intGear,
                    strTimestamp, strSteering, strSpeed, strAcceleration, strGear);
		}else{
			Log.d(_tag,"UNKNOWN FRAME");
		}
	}

	/* Notify Bluetooth state of change */
	@Override
	public void notifyBluetoothState(int nState) {
		String strState;
		if (nState == Communication.STATE_NONE){
			/* non status */
			strState = "NOTE";
		}
		else if (nState == Communication.STATE_CONNECTING){
			/* connecting */
			strState = "CONNECTING";
		}
		else if (nState == Communication.STATE_CONNECTED){
			/* connected */
			strState = "CONNECTED";
		}
		else if (nState == Communication.STATE_CONNECT_FAILED){
			/* connect failed */
			strState = "CONNECT_FAILED";
		}
		else if (nState == Communication.STATE_DISCONNECTED){
			/* disconnected */
			_buf = null;
			strState = "DISCONNECTED";
		}
		else{
			/* unknown */
			strState = "UNKNOWN";
		}
		dspToast(strState);
		
		Log.d(_tag,String.format("STATE = %s",strState));
		if(nState == Communication.STATE_CONNECTED){
			/* delay time                                            */
			/* (Connect to the CAN-Gateway -> Send the first message */
			_handler.sendMessageDelayed(_handler.obtainMessage(), 2000);
		}
	}

	Handler _handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			/* Send the message of vehicle signal request */
			startTimer(TIMER_INTERVAL);
		}
	};
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BTDEVICE_SELECT){
			if (resultCode == Activity.RESULT_OK) {
				_strDevAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			}
		}
	}	
	
	private void updateContents(final long intTimestamp,
                                final long intSteering,
                                final long intSpeed,
                                final long intAcceleration,
                                final long intGear,
                                final String strTimestamp,
                                final String strSteering,
                                final String strSpeed,
                                final String strAcceleration,
                                final String strGear){
		_handler.post(new Runnable(){
			@Override
			public void run() {
                _tvTimestamp.setText(strTimestamp);
				_tvSteering.setText(strSteering);
                _tvSpeed.setText(strSpeed);
                _tvAcceleration.setText(strAcceleration);
                _tvGear.setText(strGear);

                //update view
                long t_delta = intTimestamp - last_ts;
                last_ts = intTimestamp;
                if(stage > 0) {
                    int result = monitor(intSteering, intSpeed, intAcceleration, intGear, t_delta);
                    updateView(result);
                }
			}
		});
	}

	/* Create the message of vehicle signal request */
	private ByteBuffer createCarInfoGetFrame(){
		/* e.g.) request of Engine Revolution Speed */
		byte[] buf = {
            0x7e,      //packet start
            0x00,0x0c, //size (12 bytes)
            0x01,      //type
            0x05,      //number of signal ids
            0x00,0x01, //timestamp
            0x00,0x08, //steering wheel angle
            0x00,0x0d, //vehicle speed
            0x00,0x0e, //acceleration front-back
            0x00,0x07, //transmission gear
            0x00,0x00, //checksum
            0x7f};     //packet end
		int length = buf.length;
		/* Set the message length */
		//buf[1] = (byte)(((length - 6) >> 8) & 0xff);
		//buf[2] = (byte)((length - 6) & 0xff);
		/* Set the request signal ID */
		//buf[6] = (byte)(device_id);
		/* Calculate and set the CRC */
		int crc = calcCRC(buf, 1, buf.length - 4);
		/* Convert endian from little to big */
		buf[length - 3] = (byte)((crc >> 8) & 0xff);
		buf[length - 2] = (byte)(crc & 0xff);
		return ByteBuffer.wrap(buf);
	}
	
	private void startTimer(int timerCount){
		stopTimer();
		_timer = new Timer(false);
		_timerTask = new TimerTask() {
			public void run(){
				/* Send the message of vehicle signal request */
                _comm.writeData(createCarInfoGetFrame());
			}
		};
		_timer.schedule(_timerTask,0,timerCount);
	}
	
	private void stopTimer(){
		if (_timer != null){
			_timer.cancel();
			_timer = null;
		}
	}
	
	private void showAlertDialog(String strMessage){
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle(strMessage);
		dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				/* non-treated */
			}
		});
		dlg.show();
	}

	/* Combine received messages */
	public boolean isCombineFrame(ByteBuffer frame){
		frame.position(0);
		byte[] rcv = new byte[frame.limit()];
		frame.get(rcv, 0, frame.limit());

		/* Buffer for received message */
		if(_buf == null){
			_buf = ByteBuffer.allocate(rcv.length);
			_buf.put(rcv);
		}else{
			byte[] tmp = _buf.array();
			ByteBuffer newBuf = ByteBuffer.allocate(tmp.length + rcv.length);
			newBuf.put(tmp);
			newBuf.put(rcv);
			_buf = newBuf;
		}

		/* Check the message length */
		byte[] tmps = _buf.array();
		int len = _buf.limit();
		int dataLen = this.toUint16Value(tmps, 1);
		if ((dataLen + 6) > len){
			/* all data not received */
			return false;
		}
		else
		{
			/* all data received */
			return true;
		}
	}
	
	private boolean isFrameCheck(ByteBuffer frame){
		byte[] tmps = frame.array();
		int len = frame.limit();
		if(len < 3){
			Log.d(_tag,"FRAME LENGTH ERROR1");
			return false;
		}
		int dataLen = this.toUint16Value(tmps, 1);
		if ((dataLen + 6) != len){
			Log.d(_tag,"FRAME LENGTH ERROR2");
			return false;
		}
		if (tmps[0] != 0x7E){
			Log.d(_tag,"HEADER ERROR");
			return false;
		}
		if (tmps[len - 1] != 0x7F){
			Log.d(_tag,"FOOTER ERROR");
			return false;
		}
		if (tmps[3] != 0x11){
			Log.d(_tag,"FRAME TYPE ERROR");
			return false;
		}
		int crc = this.toUint16Value(tmps, len - 3);
		int calcCrc = this.calcCRC(tmps, 1, len - 4);
		if (crc != calcCrc){
			Log.d(_tag,"CRC ERROR");
			return false;
		}
		return true;
	}
		
	private boolean isCarInfoGetFrame(ByteBuffer frame){
		byte tmp = frame.get(3);
		if (tmp == 0x11){
			return true;
		}
		return false;
	}
	
    private int calcCRC(byte[] buffer, int index, int length) {
		int crcValue = 0x0000;
	    boolean flag;
	    boolean c15;
	    for( int i = 0; i < length; i++ ) {
	        for(int j = 0; j < 8; j++){
	            flag = ( (buffer[i + index] >> (7 - j) ) & 0x0001)==1;
	            c15  = ((crcValue >> 15 & 1) == 1);
	            crcValue <<= 1;
	            if(c15 ^ flag){
	                crcValue ^= 0x1021;
	            }
	        }
	    }
	    crcValue ^= 0x0000;
	    crcValue &= 0x0000ffff;
	    return crcValue;
    } 	
		
    private int toUint16Value(byte[] buffer, int index) {
    	int value = 0;
    	value |= (buffer[index + 0] << 8) & 0x0000ff00;
    	value |= (buffer[index + 1] << 0) & 0x000000ff;
    	return value & 0xffff;
    }
    
    private long toUint32Value(byte[] buffer, int index) {
    	int value = 0;
    	value |= (buffer[index + 0] << 24) & 0xff000000;
    	value |= (buffer[index + 1] << 16) & 0x00ff0000;
    	value |= (buffer[index + 2] <<  8) & 0x0000ff00;
    	value |= (buffer[index + 3] <<  0) & 0x000000ff;
    	return value & 0xffffffffL;
    }
    
	private void dspToast(final String strToast){
		_handler.post(new Runnable(){
			@Override
			public void run() {
				Toast toast = Toast.makeText(SampleActivity.this, strToast, Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}
	
}