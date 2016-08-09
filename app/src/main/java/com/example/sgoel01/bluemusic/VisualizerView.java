package com.example.sgoel01.bluemusic;

import java.security.Policy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.View;

/**
 * A class that draws visualizations of data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture } and
 * {@link Visualizer.OnDataCaptureListener#onFftDataCapture }
 */
public class VisualizerView extends View {
    private Camera cam;

    private byte[] mBytes;
    private byte[] mFFTBytes;
    private Rect mRect = new Rect();
    private Visualizer mVisualizer;
    private MediaPlayer mPlayer;

    private Set<Renderer> mRenderers;

    private Paint mFlashPaint = new Paint();
    private Paint mFadePaint = new Paint();


    // TESTING VARIABLES.

    private double mSystemTimeStartSec;
    private int windowRMS;
    private int windowCount;
    private List<Double> windowList = new ArrayList<Double>();
    private List<Double> medWindowList = new ArrayList<Double>();
    private List<Double> highWindowList = new ArrayList<Double>();
    private int prevBeat = -1;
    // FREQS
    private static final int LOW_FREQUENCY = 300;
    private static final int MID_FREQUENCY = 2500;
    private static final int HIGH_FREQUENCY = 10000;
    private boolean isCountDownStart=false;

    // CATCH HIGH FREQUENCY BEATS + MEDIUM FREQUENCY + LOW FREQUENCY BEAT.
    // VARIABLES FOR TESTING

    public VisualizerView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public VisualizerView(Context context)
    {
        this(context, null, 0);
    }

    private void init() {
        mBytes = null;
        mFFTBytes = null;

        mFlashPaint.setColor(Color.argb(122, 255, 255, 255));
        mFadePaint.setColor(Color.argb(238, 255, 255, 255)); // Adjust alpha to change how quickly the image fades
        mFadePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));

        mRenderers = new HashSet<Renderer>();
        windowRMS=6;
        windowCount=0;
    }

    /**
     * Links the visualizer to a player
     * @param player - MediaPlayer instance to link to
     */
    public void link(MediaPlayer player)
    {
        if(player == null)
        {
            throw new NullPointerException("Cannot link to null MediaPlayer");
        }

        // Create the Visualizer object and attach it to our media player.
        mPlayer=player;
        mVisualizer = new Visualizer(player.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        // Pass through Visualizer data to VisualizerView
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener()
        {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate)
            {
                updateVisualizer(bytes);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                                         int samplingRate)
            {
                updateVisualizerFFT(bytes);
            }
        };

        mVisualizer.setDataCaptureListener(captureListener,
                Visualizer.getMaxCaptureRate() / 2, true, true);

        // Enabled Visualizer and disable when we're done with the stream
        mVisualizer.setEnabled(true);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                mVisualizer.setEnabled(false);
            }
        });
        mSystemTimeStartSec = System.currentTimeMillis();
    }

    public void addRenderer(Renderer renderer)
    {
        if(renderer != null)
        {
            mRenderers.add(renderer);
        }
    }

    public void clearRenderers()
    {
        mRenderers.clear();
    }

    public void release()
    {
        mVisualizer.release();
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }


    public void updateVisualizerFFT(byte[] audioBytes) {
        mFFTBytes = audioBytes;
        // STARTING TESTINg
        int energySum = 0;
        double totalPower = 0.0;
        double medPower, highPower;
        int k = 2;
        double captureSize=1024;
        try{
            captureSize = mVisualizer.getCaptureSize()/2;
        }catch (Exception e) {

        }
        int sampleRate = 44000;
        try {
            sampleRate = mVisualizer.getSamplingRate() / 2000;
        }catch (Exception e){

        }
        double nextFrequency = ((k / 2) * sampleRate) / (captureSize);


        energySum += Math.abs(audioBytes[0]);
        while (nextFrequency < LOW_FREQUENCY) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }
        double sampleAvgAudioEnergy = (double) energySum
                / (double) ((k * 1.0) / 2.0);



        while (nextFrequency < MID_FREQUENCY) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }
        sampleAvgAudioEnergy = (double) energySum / (double) ((k * 1.0) / 2.0);
        medPower = sampleAvgAudioEnergy;
        totalPower += sampleAvgAudioEnergy;


        energySum = Math.abs(audioBytes[1]);
        while ((nextFrequency < HIGH_FREQUENCY) && (k < audioBytes.length)) {
            energySum += Math.sqrt((audioBytes[k] * audioBytes[k])
                    * (audioBytes[k + 1] * audioBytes[k + 1]));
            k += 2;
            nextFrequency = ((k / 2) * sampleRate) / (captureSize);
        }
        sampleAvgAudioEnergy = (double) energySum / (double) ((k * 1.0) / 2.0);
        totalPower += sampleAvgAudioEnergy;
        highPower = sampleAvgAudioEnergy;

        windowList.add(highPower);
        medWindowList.add(medPower);
        highWindowList.add(highPower);
        if (windowCount <= windowRMS) {
            windowCount++;
        } else {
            windowList.remove(0);
            medWindowList.remove(0);
            highWindowList.remove(0);
            double temp = 0, high = 0,med = 0;

            for (int i = 0; i < windowList.size(); i++) {
                temp += (Math.pow(windowList.get(i), 2));
                med += (Math.pow(medWindowList.get(i), 2));
                high += (Math.pow(highWindowList.get(i), 2));
            }
            temp = Math.sqrt(temp);
            temp = temp / windowList.size();
            med = Math.sqrt(med);
            med = med / medWindowList.size();
            high = Math.sqrt(high);
            high = high / highWindowList.size();


            if((highWindowList.get(windowRMS) - highWindowList.get(windowRMS-1) > high) ) {
                if(prevBeat != 1) {
                    cam = Camera.open();
                    Parameters p = cam.getParameters();
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    cam.setParameters(p);
                    cam.startPreview();
                }
                prevBeat = 0;
            }
            else if((medWindowList.get(windowRMS) - medWindowList.get(windowRMS-1) > med) ) {
                if(prevBeat != 0) {
                    cam = Camera.open();
                    Parameters p = cam.getParameters();
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    cam.setParameters(p);
                    cam.startPreview();
                }
                prevBeat = 1;
            }
            else {
                if(cam!=null) {
                    cam.stopPreview();
                    cam.release();
                }
                cam=null;
            }
            /*if(isCountDownStart == false) {
                counter.start();
                if(highWindowList.get(windowRMS) - highWindowList.get(windowRMS-1) >= high) {
                    cam = Camera.open();
                    Parameters p = cam.getParameters();
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    cam.setParameters(p);
                    prevBeat = 0;
                }
                else if(medWindowList.get(windowRMS) - medWindowList.get(windowRMS-1) > med) {
                    if(prevBeat != 0) {
                        OpenCam();
                    }
                    prevBeat=1;
                }
                else  {
                    CloseCam();
                }
            }
            else {
                CloseCam();
            }*/
        }

    }

    private void OpenCam() {
        cam = Camera.open();
        Parameters p = cam.getParameters();
        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
        cam.setParameters(p);
        cam.startPreview();
    }

    private void CloseCam() {
        if(cam != null) {
            cam.stopPreview();
            cam.release();
        }
        cam=null;
    }
    boolean mFlash = false;
    /**
     * Call this to make the visualizer flash. Useful for flashing at the start
     * of a song/loop etc...
     */
    public void flash() {
        mFlash = true;
        invalidate();
    }

    Bitmap mCanvasBitmap;
    Canvas mCanvas;


   /* private CountDownTimer counter = new CountDownTimer(10, 1) {
        @Override
        public void onTick(long millisUntilFinished) {
           // isCountDownStart=true;
        }

        @Override
        public void onFinish() {
            CloseCam();
        }
    };*/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Create canvas once we're ready to draw
        mRect.set(0, 0, getWidth(), getHeight());

        if(mCanvasBitmap == null)
        {
            mCanvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888);
        }
        if(mCanvas == null)
        {
            mCanvas = new Canvas(mCanvasBitmap);
        }

        if (mBytes != null) {
            // Render all audio renderers
            AudioData audioData = new AudioData(mBytes);
            for(Renderer r : mRenderers)
            {
                r.render(mCanvas, audioData, mRect);
            }
        }

        if (mFFTBytes != null) {
            // Render all FFT renderers
            FFTData fftData = new FFTData(mFFTBytes);
            for(Renderer r : mRenderers)
            {
                r.render(mCanvas, fftData, mRect);
            }
        }

        // Fade out old contents
        mCanvas.drawPaint(mFadePaint);

        if(mFlash)
        {
            mFlash = false;
            mCanvas.drawPaint(mFlashPaint);
        }

        canvas.drawBitmap(mCanvasBitmap, new Matrix(), null);
    }
}