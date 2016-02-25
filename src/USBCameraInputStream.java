import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Arrays;

/**
 * Created by Ricardo on 2016-02-22.
 */
public class USBCameraInputStream implements Runnable {


    //Uses protocols from SmartDashboard and GRIP
    private final static int PORT = 1180;
    private final static String ADRESS = "10.8.65.52";
    private final static byte[] MAGIC_NUMBERS = {0x01, 0x00, 0x00, 0x00};
    private final static int HW_COMPRESSION = -1;
    private final static int SIZE_640x480 = 0;

    private byte[] dataBuffer = new byte[64 * 1024];
    private byte[] magicNumbersBuffer = new byte[4];
    private int fps;
    private DriverStream dashboard;

    private boolean useLocal = false;
    private VideoCapture localCapturer;

    private boolean shutdown = false;

    public USBCameraInputStream(DriverStream dash, int fps) {
        this.fps = fps;
        dashboard = dash;
    }

    public USBCameraInputStream(DriverStream dash, boolean useLocal, int port) {
        dashboard = dash;
        this.useLocal = true;
        localCapturer = new VideoCapture(port);
    }



    @Override
    public void run() {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                sendMsg("Uncaught exception in USB camera stream: " + e.getMessage());
                shutdownThread();
                dashboard.interruptedThread = this;
            }
        });
        while(!shutdown) {
            try {
                if(useLocal) {
                    Mat m = new Mat();
                    localCapturer.read(m);
                    sendFrame(DriverStream.matToBufferedImage(m), m.rows() * m.cols() * m.channels());
                } else {
                    try (
                            Socket socket = new Socket(ADRESS, PORT);
                            DataInputStream in = new DataInputStream(socket.getInputStream());
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                        sendMsg("Connected to " + ADRESS + ":" + PORT);
                        out.writeInt(fps);
                        out.writeInt(HW_COMPRESSION);
                        out.writeInt(SIZE_640x480);

                        while (!Thread.currentThread().isInterrupted()) {
                            in.readFully(magicNumbersBuffer);
                            if (!Arrays.equals(magicNumbersBuffer, MAGIC_NUMBERS)) {
                                throw new IOException("Wrong magic numbers! Bad input.");
                            }
                            int imageSize = in.readInt();
                            dataBuffer = growIfNecessary(dataBuffer, imageSize);
                            in.readFully(dataBuffer, 0, imageSize);
                            //sendMsg("Got frame with " + imageSize + " bytes.");
                            sendFrame(ImageIO.read(new ByteArrayInputStream(dataBuffer)), imageSize);
                        }
                    } catch (IOException e) {
                        sendMsg(e.getMessage());
                        //e.printStackTrace();
                    } finally {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                sendMsg("Data thread interrupted!!");
            }
        }
    }

    public void shutdownThread() {
        shutdown = true;
    }

    public void sendFrame(BufferedImage img, int byteAmt) {
        try {
            dashboard.frameBuffer = img;
            dashboard.frameUpdated = true;
            dashboard.timestamp = System.currentTimeMillis();
            } catch(NullPointerException e) { sendMsg("Null BufferedImage!"); }
    }

    public void sendMsg(String msg) {

        dashboard.consoleBuffer += (msg + "\n");
        //System.out.println(msg);
    }

    /**
     * Return an array big enough to hold least at least "capacity" elements.  If the supplied buffer is big enough,
     * it will be reused to avoid unnecessary allocations.
     */
    private byte[] growIfNecessary(byte[] buffer, int capacity) {
        if (capacity > buffer.length) {
            int newCapacity = buffer.length;
            while (newCapacity < capacity) {
                newCapacity *= 1.5;
            }
            sendMsg("Growing to " + newCapacity);
            return new byte[newCapacity];
        }

        return buffer;
    }

    public static int testLocalCameraPorts(int limit) {
        for(int i = limit; i >= 0; i--) {
            boolean b = false;
            try {
                VideoCapture tester = new VideoCapture(i);
                Mat m = new Mat();
                tester.read(m);
                BufferedImage testImg = DriverStream.matToBufferedImage(m);
                b = true;
            } catch (Exception e) {
                b = false;
            }
            if(b) {
                System.out.println("Local cam" + i + " works.");
                return i;
            }
        }
        return -1;
    }

}
