package org.opencv.samples.imagemanipulations;

import java.util.ArrayList;
import java.util.Arrays;

import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class mainActivity extends Activity implements CvCameraViewListener2, View.OnClickListener{
    private static final String  TAG                 = "OCVSample::Activity";



    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewHist;
    private MenuItem             mItemPreviewCanny;
    private MenuItem             mItemPreviewSepia;
    private MenuItem             mItemPreviewSobel;
    private MenuItem             mItemPreviewZoom;
    private MenuItem             mItemPreviewPixelize;
    private MenuItem             mItemPreviewPosterize;
    private CameraBridgeViewBase mOpenCvCameraView;

    private Size                 mSize0;

    private Mat                  mIntermediateMat;
    private Mat                  mMat0;
    private MatOfInt             mChannels[];
    private MatOfInt             mHistSize;
    private int                  mHistSizeNum = 25;
    private MatOfFloat           mRanges;
    private Scalar               mColorsRGB[];
    private Scalar               mColorsHue[];
    private Scalar               mWhilte;
    private Point                mP1;
    private Point                mP2;
    private float                mBuff[];
    //private Mat                  mSepiaKernel;

    public static int           viewMode = FilterApplier.VIEW_MODE_RGBA;

    // 3/18/14 10:00 AM <-
    private HorizontalScrollView filterScroll;
    // holds the row of filters
    private LinearLayout       scrollLayout;

    // ->

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // at this point, we can start using the library, so add the filters (this is a temporary hack, since the filters are
                    // going to be added on a STATIC image -- so, really, the scroll view should be in the image editor activity, not the
                    // picture-taking activity, where we currently have it)
                    addFiltersToScrollView(new Mat());
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    private void addFiltersToScrollView(Mat image) {
        FilterScrollElement e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_RGBA, "Normal(temp)", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_CANNY, "Canny", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_SEPIA, "Sepia", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_SOBEL, "Sobel", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_ZOOM, "Zoom", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_PIXELIZE, "Pixelize", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_POSTERIZE, "Posterize", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_TEST_BLUE, "Sad Day", image);
        e.setOnClickListener(this);
        scrollLayout.addView(e);
        // uncomment this code to test the scrolling feature
        /*
        for (int i = 0; i < 20; i++) {
        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_PIXELIZE, "Pixelize", image);
        scrollLayout.addView(e);
        }
        */

    }


    // Handles the scroll view's filter clicks
    @Override
    public void onClick(View v) {
        for (int i = 0; i < scrollLayout.getChildCount(); i++) {
            if (v == scrollLayout.getChildAt(i)) {
                FilterScrollElement e = (FilterScrollElement) scrollLayout.getChildAt(i);
                viewMode = e.getFilterType();
                break;
            }
        }
    }


    public mainActivity() {
        /* ".getClass" will show up as an error but it still works! */
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.emulsify_camera_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // 3/18/14 10:00 AM <-
        // initialize the horizontal scroller (filterScroll) and its linear layout
        filterScroll = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        scrollLayout = (LinearLayout) findViewById(R.id.linearLayout);

    /*
        LinearLayout t = new LinearLayout(this, null);
        ImageView l = new ImageView(this, null);
        l.setImageResource(R.drawable.ic_launcher);
        t.addView(l);

        scrollLayout.addView(t);
        */

        // ->
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewHist  = menu.add("Histograms");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewSepia = menu.add("Sepia");
        mItemPreviewSobel = menu.add("Sobel");
        mItemPreviewZoom  = menu.add("Zoom");
        mItemPreviewPixelize  = menu.add("Pixelize");
        mItemPreviewPosterize = menu.add("Posterize");
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = FilterApplier.VIEW_MODE_RGBA;
        if (item == mItemPreviewHist)
            viewMode = FilterApplier.VIEW_MODE_HIST;
        else if (item == mItemPreviewCanny)
            viewMode = FilterApplier.VIEW_MODE_CANNY;
        else if (item == mItemPreviewSepia)
            viewMode = FilterApplier.VIEW_MODE_SEPIA;
        else if (item == mItemPreviewSobel)
            viewMode = FilterApplier.VIEW_MODE_SOBEL;
        else if (item == mItemPreviewZoom)
            viewMode = FilterApplier.VIEW_MODE_ZOOM;
        else if (item == mItemPreviewPixelize)
            viewMode = FilterApplier.VIEW_MODE_PIXELIZE;
        else if (item == mItemPreviewPosterize)
            viewMode = FilterApplier.VIEW_MODE_POSTERIZE;
        else if (item == mItemPreviewPosterize)
            viewMode = FilterApplier.VIEW_TEST_GRAYSCALE;
        else if (item == mItemPreviewPosterize)
            viewMode = FilterApplier.VIEW_TEST_BLUE;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0  = new Mat();
        mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
        mColorsHue = new Scalar[] {
                new Scalar(255, 0, 0, 255),   new Scalar(255, 60, 0, 255),  new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255),  new Scalar(20, 255, 0, 255),  new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255),  new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255),  new Scalar(0, 0, 255, 255),   new Scalar(64, 0, 255, 255),  new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255),  new Scalar(255, 0, 0, 255)
        };
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

        // This code has been moved to FilterApplier
        // Fill sepia kernel
        //mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        //mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        //mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        //mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        //mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        switch (mainActivity.viewMode) {
        case FilterApplier.VIEW_MODE_RGBA:
            break;

        /*
        case FilterApplier.VIEW_MODE_HIST:
            Mat hist = new Mat();
            int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
            if(thikness > 5) thikness = 5;
            int offset = (int) ((sizeRgba.width - (5*mHistSizeNum + 4*10)*thikness)/2);
            // RGB
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(rgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = sizeRgba.height-1;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Core.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
                }
            }
            // Value and Hue
            Imgproc.cvtColor(rgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);
            // Value
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[2], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Core.line(rgba, mP1, mP2, mWhilte, thikness);
            }
            // Hue
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[0], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (4 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Core.line(rgba, mP1, mP2, mColorsHue[h], thikness);
            }
            break;
        */
        case FilterApplier.VIEW_MODE_CANNY:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_CANNY, rgbaInnerWindow);
            rgbaInnerWindow.release();
            break;

        case FilterApplier.VIEW_MODE_SOBEL:
            Mat gray = inputFrame.gray();
            Mat grayInnerWindow = gray.submat(top, top + height, left, left + width);
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);

            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_SOBEL, rgbaInnerWindow, grayInnerWindow);
            grayInnerWindow.release();
            rgbaInnerWindow.release();
            break;

        case FilterApplier.VIEW_MODE_SEPIA:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_SEPIA, rgbaInnerWindow);
            rgbaInnerWindow.release();
            break;

        case FilterApplier.VIEW_MODE_ZOOM:
            Mat zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
            Mat mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_ZOOM, mZoomWindow, zoomCorner);
            zoomCorner.release();
            mZoomWindow.release();
            break;

        case FilterApplier.VIEW_MODE_PIXELIZE:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_PIXELIZE, rgbaInnerWindow);
            rgbaInnerWindow.release();
            break;

        case FilterApplier.VIEW_MODE_POSTERIZE:
            /*
            Imgproc.cvtColor(rgbaInnerWindow, mIntermediateMat, Imgproc.COLOR_RGBA2RGB);
            Imgproc.pyrMeanShiftFiltering(mIntermediateMat, mIntermediateMat, 5, 50);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_RGB2RGBA);
            */
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            FilterApplier.applyFilter(FilterApplier.VIEW_MODE_POSTERIZE, rgbaInnerWindow);
            rgbaInnerWindow.release();
            break;

        case FilterApplier.VIEW_TEST_BLUE:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            FilterApplier.applyFilter(FilterApplier.VIEW_TEST_BLUE, rgbaInnerWindow);
            rgbaInnerWindow.release();
            break;
        }

        return rgba;
    }

}
