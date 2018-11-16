package com.diagenetix.abestat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.diagenetix.abestat.Real_Time_Activity.POTENTIAL_KEY;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService{// implements Parcelable {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothSecure";

    // Unique UUID for this application- for serial bluetooth connection...
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Data key names for bundled data passed back by handler
    public static final String SAT_CODE_KEY = "Saturation_Code";
    public static final String ATTENUATION_CODE_KEY = "Attenuation_Code";
    public static final String INDEX_KEY = "Index";
    public static final String TEMPERATURE_KEY = "Temperature";
    public static final String CURRENT_KEY = "Current";
    public static final String VOLTAGE_KEY = "Voltage";
    public static final String RESISTANCE_KEY = "Resistance";
    public static final String CAPACITANCE_KEY = "Capacitance";
    public static final String PHASE_KEY = "Phase";
    public static final String CALIBRATION_SLOPES = "Slopes";
    
    private static final double MCP9701A_OFFSET = - 400 / 19.5;	// temperature correction for MCP9701A- output at 0C = 400 mV, Sensitivity = 19.5 mV/C

    // Member fields
    private final BluetoothAdapter mAdapter;
    private Handler btHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    /**
     * Constructor. Prepares a new Bluetooth session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        btHandler = handler;
        setState(STATE_NONE);
    }
    
    /*
     * Required call for Parcelable objects...
     */ 
    /*public int describeContents() {
        return 0;
    }
    
    /*
     * Required call for Parcelable objects(?)...
     */ 
    /*public static final Parcelable.Creator<BluetoothService> CREATOR
    	= new Parcelable.Creator<BluetoothService>() {
    	public BluetoothService createFromParcel(Parcel in) {
    		return new BluetoothService(in);
    		}
    	public BluetoothService[] newArray(int size) {
    		return new BluetoothService[size];
    		}
    	};

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.e(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        btHandler.obtainMessage(ABEStatActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Change the handler (route handler info to new activity). */
    public synchronized void newHandler(Handler handle) {
        btHandler = handle;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
//        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, true);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    
    public synchronized void disconnectDevice() {
        if (D) Log.d(TAG, "disconnecting");
        
        stop();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity, and set Kinect flag to disconnect next time DeviceListActivity is called
        Message msg = btHandler.obtainMessage(ABEStatActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ABEStatActivity.DEVICE_NAME, device.getName());
        bundle.putString(ABEStatActivity.DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        btHandler.sendMessage(msg);

        ABEStatActivity.Kinect = false;
        setState(STATE_CONNECTED);
    }
    
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        ABEStatActivity.Kinect = true;	// set flag so android knows its ready to try connecting again
        
        flushInStream();
        mConnectedThread.StopThread = true;
        
        if (mConnectedThread.mmInStream != null) {
        	if (D) Log.e(TAG, "Closing Input Stream");
        	try {
        		mConnectedThread.mmInStream.close();
        		}
        	catch (Exception e) {}
        	mConnectedThread.mmInStream = null;
            }
      
        if (D) Log.e(TAG, "Input Stream Closed");    
      
        if (mConnectedThread.mmOutStream != null) {
        	if (D) Log.e(TAG, "Closing Output Stream");
            try {
            	mConnectedThread.mmOutStream.close();
            	}
            catch (Exception e) {}
            mConnectedThread.mmOutStream = null;
            }
        
        if (D) Log.e(TAG, "Output Stream Closed");  
       
        if (mConnectedThread.mmSocket != null) {
        	if (D) Log.e(TAG, "Closing BT Socket");
            try {
            	mConnectedThread.mmSocket.close();
            	}
            catch (Exception e) {}
            mConnectedThread.mmSocket = null;
            }
        
        if (D) Log.e(TAG, "BT Socket Closed");
        
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (D) Log.e(TAG, "Connect Thread Closed");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (D) Log.e(TAG, "Connected Thread Closed");
        
        // Set flag in ABEStatActivity to allow connection next time in DeviceListActivity...
    //    ABEStatActivity.mBTService = null;

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void flushInStream() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED);
            r = mConnectedThread;
        }
        // call flushinstream function on connected thread
        r.flushinstream();
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
    /**
     * Read from the ConnectedThread in an unsynchronized manner
     */
    public String read() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return "";
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        String dat = r.read();
        return dat;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = btHandler.obtainMessage(ABEStatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ABEStatActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        btHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        setState(STATE_NONE);
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = btHandler.obtainMessage(ABEStatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        // if device was connected (and lost unintentionally), send appropriate message and setup DeviceListActivity for connection menu
        if (!ABEStatActivity.Kinect) {
        	bundle.putString(ABEStatActivity.TOAST, "device connection was lost...");
        	ABEStatActivity.Kinect = true;
        }
        else {
        	bundle.putString(ABEStatActivity.TOAST, "device disconnected...");
        }
        msg.setData(bundle);
        btHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.e(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            /*try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }*/
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean StopThread = false;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.e(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            int index;
            double Temp[];
            while (!StopThread) {
                try {
                    // Check to see if bytes available on Bluetooth input stream- if so read the first character
                  if (mState == STATE_CONNECTED) {	// first make sure we're still connected, or instream is null
                	  if (mmInStream == null) return;
                	  if (mmInStream.available() > 0) {
                    	Log.i(TAG, "byte available");
                    	Real_Time_Activity.BTComm = true;	// set flag that communications incoming from BT (still connected)
                    	ABEStatActivity.BTCommLoss = 0;
                    	ABEStatActivity.ABEbusy = false;   // we're getting info from ABE-Stat- default is that some analysis is complete
                            // so device is not busy- clears us to send another request for analysis (prevents us from sending backlog of instructions)
                    	Real_Time_Activity.BT_Break = false;	// connection with BT not broken...
                    	int byter = mmInStream.read();
                    	char instruction = (char) byter;
                    	Log.i(TAG, "read character = " + instruction);
                    	Bundle DataBundle = new Bundle();
                    	switch (instruction) {
                            /*case 'a': // code that the coded calibration slopes are incoming
                                double slopes[] = readDouble(2);
                                int ch = (int) readLong();
                                if (D) Log.d(TAG, "calibration slopes, ch(" + ch + ") specific- " + slopes[0] + ", non-specific  " + slopes[1]);
                                btHandler.obtainMessage(ABEStatActivity.CALIBRATIONS_READ, ch, -1, slopes).sendToTarget();
                                break;*/
                            case 'b': // device is counting down equilibration time (argument is # seconds left in equilibration)
                                int eqTimeRemaining = (int) readLong();
                                btHandler.obtainMessage(Real_Time_Activity.EQ_TIME_LEFT, eqTimeRemaining, -1).sendToTarget();
                                break;
                            case 'c':	// Code that a measurement cycle has completed
                                btHandler.obtainMessage(Real_Time_Activity.CYCLE_COMPLETE).sendToTarget();
                                break;
                            case 'd':	// Code that CV Data is coming
                                DataBundle.putString(POTENTIAL_KEY, read());
                                DataBundle.putString(Real_Time_Activity.CURRENT_KEY, read());
                                btHandler.obtainMessage(Real_Time_Activity.CV_DATA, DataBundle).sendToTarget();
                                break;
                            case 'e':	// code that a subroutine on device has exited...
                                if (D) Log.d(TAG, "Subroutine in Smart-DART exited...");
                                btHandler.obtainMessage(ABEStatActivity.SUBROUTINE_EXIT).sendToTarget();
                                break;
                            case 'f':	// code that EIS data (frequency impedance magnitude and phase) are coming
                                String f = read();
                                String z = read();
                                String p = read();
                                DataBundle.putString(Real_Time_Activity.FREQUENCY_KEY, f);
                                DataBundle.putString(Real_Time_Activity.IMPEDANCE_MAGNITUDE_KEY, z);
                                DataBundle.putString(Real_Time_Activity.IMPEDANCE_PHASE_KEY, p);
                                if (D) Log.e(TAG, z + " Ohms " + p + " deg @" + f + "Hz");
                                btHandler.obtainMessage(Real_Time_Activity.EIS_DATA, DataBundle).sendToTarget();
                                break;
                            case 'g':	// instruction from Smart-DART to disconnect bluetooth (about to shut down)
                                if (D) Log.d(TAG, "Smart-DART shutting down, disconnecting bluetooth");
                                disconnectDevice();
                                break;
                            case 'h':   // code for incoming battery % charge remaining
                                int batt = (int) readLong();
                                btHandler.obtainMessage(ABEStatActivity.BATT_READ, batt, 0).sendToTarget();
                                break;
                            case 'i':	// Code that indexed Temperature data is incoming (for storage in indexed data file)
                                String current = read();
                                String potential = read();
                                DataBundle.putString(CURRENT_KEY, current);
                                DataBundle.putString(VOLTAGE_KEY, potential);
                                btHandler.obtainMessage(ABEStatActivity.CELL_CURRENT_READ, DataBundle).sendToTarget();
                                break;
                            case 'j':   // code from ABE-Stat that it is in middle of iterative analysis (don't timeout and disconnect!)
                                btHandler.obtainMessage(ABEStatActivity.ENGAGED).sendToTarget();
                                break;
                            case 'k':	// code that sensor board temperature is coming from device
                                long Board_Temp = readLong();
                                DataBundle.putLong(TEMPERATURE_KEY, Board_Temp);
                                if (D) Log.d(TAG, "Board temperature read : " + Board_Temp);
                                btHandler.obtainMessage(ABEStatActivity.BOARD_TEMPERATURE_READ, DataBundle).sendToTarget();
                                break;
                            case 'm': // incoming message from instrument
                                String StatMessage = read();	// read message from instrument
                                if (D) Log.d(TAG, "Message from ABE-Stat: " + StatMessage);
                                break;
                            case 'o':	// Code that indexed Temperature data is incoming (for storage in indexed data file)
                                String voltage = read();
                                DataBundle.putString(VOLTAGE_KEY, voltage);
                                btHandler.obtainMessage(ABEStatActivity.OPEN_CIRCUIT_VOLTAGE_READ, DataBundle).sendToTarget();
                                break;
                            case 'r':	// Attenuation code- 1 in corresponding bit means that high gain is saturated, so measurement is at low gain
                                DataBundle.putString(RESISTANCE_KEY, read());
                                btHandler.obtainMessage(ABEStatActivity.RESISTANCE_READ, DataBundle).sendToTarget();
                                break;
                            case 's':	// for STATIC_LOGGING Method to indicate that equilibration time has elapsed

                                btHandler.obtainMessage(ABEStatActivity.SAT_CODE_READ, -1, -1, DataBundle).sendToTarget();
                                break;
                            case 't':  // Code that Temperature data is incoming (only for display, not indexed for data storage)
                                double Temps[] = readDouble(2);
                                //if (D) Log.d(TAG, "Temperature read : " + T);
                                btHandler.obtainMessage(ABEStatActivity.TEMPERATURE_READ, Temps).sendToTarget();
                                break;
                            //case 'n': // code that the coded calibration slopes are incoming- used by ABEStatCal, not ABEStat
                            //	int device = (int) readLong();
                            //	if (D) Log.d(TAG, "device version " + device);
                            //	btHandler.obtainMessage(ABEStatActivity.DEVICE_CODE, device).sendToTarget();
                            //	break;
                            case 'v': // code that device has reported it's firmware version name
                                //DataBundle.putString(FIRMWARE_VERSION_KEY, read());
                                String FirmwareVersion = read();
                                btHandler.obtainMessage(ABEStatActivity.FIRMWARE_VERSION_READ, FirmwareVersion).sendToTarget();
                                break;
                            case 'z':
                                DataBundle.putString(CAPACITANCE_KEY, read());
                                DataBundle.putString(PHASE_KEY, read());
                                btHandler.obtainMessage(ABEStatActivity.CAPACITANCE_READ, DataBundle).sendToTarget();
                                break;
                            default:
                                break;
                        }	// switch
                    }
                	  else {
                    	//do nothing
                    }
                  }
                }
                    
                catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            if (!StopThread) try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(ABEStatActivity.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
            ABEStatActivity.writingMessage = false;
        }
        
        public void flushinstream() {
        	int garbage;
        	try {
        		while(mmInStream.available() != 0) garbage = mmInStream.read();
        	}
        	catch (IOException e) {
          		Log.e(TAG, "disconnected", e);
          		connectionLost();
          		} 
        }
        
        public String read() {
    	  // puclicly available function reads a string of data terminated with tab ('\t') character
     	// First, initialize byte buffer and string to handle read data
      	int byter;
      	char caracter;
      	String data = "";
      	if (!StopThread) try {
      		do {
      			// first, wait for data to become available on InStream (assumes this function is only called when data is expected)
      			// note that the simple call to InStream.read function returns an abstract integer, that must be recast as a character...
      			while(mmInStream.available() == 0);
      			// then read byte...
      			byter = mmInStream.read();
      			caracter = (char) byter;
    			// and tack read byte to end of data string it is not the terminal character
      			if (caracter != '\t') data = data + caracter;
      		}
      		while (caracter != '\t');
    		// as long as the terminal character /t is not read...
      	}
      	catch (IOException e) {
      		Log.e(TAG, "disconnected", e);
      		connectionLost();
      		}
      	return data;
      	}
        
        /*
         * function to read a tab terminated string and decode as double precision number
         */
        private double readDouble() {
        	double myDoubles = 0;
	        	try {
					myDoubles = (double) Double.parseDouble(read());
					// TODO Auto-generated catch block
	        	} catch (NumberFormatException e) {
					e.printStackTrace();
					myDoubles = -1000;
				}
        	return myDoubles;
        }
        
        /*
         * function to read array tab terminated strings and decode as double precision numbers
         */
        private double[] readDouble(int howmany) {
        	double[] myDoubles = new double[howmany];
        	for (int i = 0; i < howmany; i++) {
        		try {
					myDoubles[i] = Double.parseDouble(read());
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	return myDoubles;
        }
        
        /*
         * function to read a tab terminated string and decode as long integer (some RFU values may be "long")
         */
        private long readLong() {
        	long myInts = 0;
        	String mybyte = read();
	        	try {
					myInts = (long) Long.parseLong(mybyte);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					myInts = -1000;
				}
        	return myInts;
        }
        
        /*
         * function to read an array of tab terminated strings and decode as long integers
         */
        private long[] readLong(int howmany) {
        	long[] myInts = new long[howmany];
        	for(int i = 0; i<howmany; i++) {
	        	try {
					myInts[i] = Long.parseLong(read());
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	return myInts;
        }

        public void cancel() {
            /*if (D) Log.d(TAG, "disconnecting");
            
            if (D) Log.e(TAG, "Closing Input Stream");
            try {
            	mConnectedThread.mmInStream.close();
            	} 
            catch (Exception e) {
            	Log.e(TAG, "close() of InStream failed", e);
            }
            mConnectedThread.mmInStream = null;
            
            if (D) Log.e(TAG, "Input Stream Closed");
            
            try {
            	mmOutStream.close();
            	}
            catch (Exception e) {
            	Log.e(TAG, "close() of OutStream failed", e);
            }
            
            try {
            	mmSocket.close();
            	}
            catch (Exception e) {
            	Log.e(TAG, "close() of connect socket failed", e);
            	}
            
            if (D) Log.e(TAG, "BT Socket Closed");
            */
        }
    }
}
