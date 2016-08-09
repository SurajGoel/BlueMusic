package com.example.sgoel01.bluemusic;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

/**
 * Demo to show how to use VisualizerView
 */
public class MainActivity extends Activity {
    private MediaPlayer mPlayer;
    private MediaPlayer mSilentPlayer;  /* to avoid tunnel player issue */
    private VisualizerView mVisualizerView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        init();
    }

    @Override
    protected void onPause()
    {
        cleanUp();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        cleanUp();
        super.onDestroy();
    }

    private void init()
    {
        mPlayer = MediaPlayer.create(this, R.raw.test1);
        mPlayer.setLooping(true);
        mPlayer.start();

        // We need to link the visualizer view to the media player so that
        // it displays something
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        mVisualizerView.link(mPlayer);

        // Start with just line renderer
        addLineRenderer();
    }

    private void cleanUp()
    {
        if (mPlayer != null)
        {
            mVisualizerView.release();
            mPlayer.release();
            mPlayer = null;
        }

        if (mSilentPlayer != null)
        {
            mSilentPlayer.release();
            mSilentPlayer = null;
        }
    }

    private void addLineRenderer()
    {
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1f);
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.argb(88, 0, 128, 255));

        Paint lineFlashPaint = new Paint();
        lineFlashPaint.setStrokeWidth(5f);
        lineFlashPaint.setAntiAlias(true);
        lineFlashPaint.setColor(Color.argb(188, 255, 255, 255));
        LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
        mVisualizerView.addRenderer(lineRenderer);
    }

    // Actions for buttons defined in xml
    public void startPressed(View view) throws IllegalStateException, IOException
    {
        if(mPlayer.isPlaying())
        {
            return;
        }
        mPlayer.prepare();
        mPlayer.start();
    }

    public void stopPressed(View view)
    {
        mPlayer.stop();
    }


    public void linePressed(View view)
    {
        addLineRenderer();
    }

    public void clearPressed(View view)
    {
        mVisualizerView.clearRenderers();
    }
}