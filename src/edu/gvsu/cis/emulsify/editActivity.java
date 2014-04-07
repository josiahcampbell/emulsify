package edu.gvsu.cis.emulsify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author emulsify team
 * @version Update 2014-04-01
 */
public class editActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "Emulsify:Image Editor";
    private final int FILTER_HEIGHT = 80;
    private final int IMAGE_HEIGHT = 80;
    private HorizontalScrollView filterScroll;
    // holds the row of filters
    private LinearLayout filterScrollLayout;
    private ScrollView imageScroll;
    private LinearLayout imageScrollLayout;
    private int currentImageIndex = 0;
    public static int viewMode = FilterApplier.VIEW_MODE_RGBA;
    //Image
    private ImageView mainPhoto;
    private Bitmap originalBitmap;
    private Bitmap viewedBitmap;

    private boolean initialized = false;
    private OnSwipeTouchListener onSwipeTouchListener;
    /* Menu Share Provider */
    private MenuItem shareMenuItem;
    private ShareActionProvider shareProvider;
    private String originalBitmapString, viewedBitmapString;

    @Override
    public void onBackPressed() {
        final Intent GOHOME = new Intent(this, homeActivity.class);
        new AlertDialog.Builder(this)
                //.setTitle("Photo and changes will be erased. Continue?")
                .setMessage(R.string.exit_dialog)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int which) {
                                if (which == -1)
                                    startActivity(GOHOME);
                                finish();
                            }
                        })
                .setNegativeButton(android.R.string.no, null).show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_actionbar, menu);
       /* shareMenuItem = menu.findItem(R.id.share_image);
        shareProvider = (ShareActionProvider) shareMenuItem.getActionProvider();
        if (!viewedBitmapString.isEmpty()){
            shareProvider.setShareIntent(createImageShareIntent(originalBitmapString));
        }*/
        return true;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //TODO: prevent the user from using openCV methods until openCV is initialized (for instance, after
                    /* the screen is turned back on)
                       this can be accomplished by toggling the click listeners */
                    if (!initialized) {
                        initialize();
                        initialized = true;
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public void setImages(String filename) {
        originalBitmap = BitmapFactory.decodeFile(filename);
        // set the main image
        mainPhoto.setImageBitmap(originalBitmap);

        //reset the filters
        filterScrollLayout.removeAllViews();

        Mat mat = new Mat(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getDensity());
        //from mat --> CvType.CV_8UC1);
        Utils.bitmapToMat(originalBitmap, mat);

        double width = mat.size().width;
        double height = mat.size().height;

        Mat filterMat = new Mat();

        Imgproc.resize(mat,
                filterMat, new Size(),
                (FILTER_HEIGHT
                        * (width / height))
                        / width, (double) (FILTER_HEIGHT)
                / height, Imgproc.INTER_NEAREST);
        addFiltersToScrollView(filterMat);
    }

    public class PictureLoader extends AsyncTask<ArrayList<String>, Object, Void> {
        private Context mContext;

        public PictureLoader(Context context) {
            mContext = context;
        }


        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            ArrayList<String> filenames = params[0];
            for (int i = 0; i < filenames.size(); i++) {
                Bitmap bit = BitmapFactory.decodeFile(filenames.get(i));
                Mat mat = new Mat(bit.getWidth(), bit.getHeight(), bit.getDensity());
                Utils.bitmapToMat(bit, mat);

                double width = mat.size().width;
                double height = mat.size().height;

                Mat imageMat = new Mat();
                Imgproc.resize(mat, imageMat, new Size(), (double) ((IMAGE_HEIGHT * (width / height)) / width), (double) ((IMAGE_HEIGHT) / height), Imgproc.INTER_NEAREST);

                //"percent" = ((double) (i+1)/ filenames.size()));
                publishProgress(filenames.get(i), imageMat, i);


            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {//HashMap<String, Object>... values) {
            super.onProgressUpdate(values);
            //HashMap<String, Object> value = values[0];

            String filename = (String) values[0];
            Mat mat = (Mat) values[1];
            int index = (Integer) values[2];

            PictureScrollElement p = new PictureScrollElement(mContext);
            p.initialize(filename, mat);
            if (index == 0) p.box();

            //p.setDensity(bit.getDensity());
            p.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = 0; i < imageScrollLayout.getChildCount(); i++) {
                        PictureScrollElement e = (PictureScrollElement) imageScrollLayout.getChildAt(i);
                        if (i != currentImageIndex) {
                            if (v == e) {
                                e.box();
                                setImages(e.getFile());
                                PictureScrollElement a = (PictureScrollElement) imageScrollLayout.getChildAt(currentImageIndex);
                                a.unBox();
                                currentImageIndex = i;
                                onSwipeTouchListener.putIndex(-1);// base);
                            } else if (e.isBoxed()) {
                                e.unBox();
                            }
                        }
                    }
                }
            });

            imageScrollLayout.addView(p);
        }
    }

    public void initialize() {
        Intent temp = getIntent();
        if (temp != null) {
            ArrayList<String> filenames = temp.getStringArrayListExtra("filename");

            Bitmap bm = null;
            if (filenames != null) {
                bm = BitmapFactory.decodeFile(filenames.get(0));

                if (filenames.size() > 1) {
                    PictureLoader loader = new PictureLoader(this);
                    loader.execute(filenames);
                }

            } else {
                String filename = temp.getStringExtra("filename");
                bm = BitmapFactory.decodeFile(filename);
            }
            try {
                originalBitmap = bm;
            viewedBitmap = originalBitmap.copy(originalBitmap.getConfig(), originalBitmap.isMutable());
            } catch (NullPointerException e) {
                Log.e("NullPointerException", "Initialize bmp null");
                Toast.makeText(this, "Loading error. Try again!", Toast.LENGTH_SHORT).show();
            }
            //ContentValues values = new ContentValues();

            //values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            //values.put(MediaStore.Images.Media.MIME_TYPE, "image/bmp");
            //values.put(MediaStore.MediaColumns.DATA, mainPhotoBitmap);

            //this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // set the main image
            mainPhoto.setImageBitmap(originalBitmap);

            Mat mat = new Mat(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getDensity());// CvType.CV_8UC1);
            Utils.bitmapToMat(originalBitmap, mat);
            double width = mat.size().width;
            double height = mat.size().height;

            Mat filterMat = new Mat();

            Imgproc.resize(mat, filterMat, new Size(), (double) (FILTER_HEIGHT * (width / height)) / width, (double) (FILTER_HEIGHT) / height, Imgproc.INTER_NEAREST);

            // add the filters now
            addFiltersToScrollView(filterMat);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* if (getIntent() != null) {
            originalBitmapString = getIntent().getStringExtra("filename");

        }*/
        setContentView(R.layout.activity_editor);

        // initialize the horizontal scroller (filterScroll) and its linear layout
        filterScroll = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        filterScrollLayout = (LinearLayout) findViewById(R.id.linearLayout);

        imageScroll = (ScrollView) findViewById(R.id.pictureScrollView);
        imageScrollLayout = (LinearLayout) findViewById(R.id.pictureLinearLayout);

        onSwipeTouchListener = new OnSwipeTouchListener(this, imageScrollLayout) {
            public void onSwipeRight() {
                // TODO: ask the user if the photo should be deleted IF the photo has been saved to the gallery (they
                // temporarily lie in the app's storage space, which should be cleaned out upon exit of the editor activity)
                if (index != -1) {
                    imageScrollLayout.removeViewAt(index);
                    if (imageScrollLayout.getChildCount() == 0) {
                        //nothing left to edit!
                        //TODO: clear the app's storage space before exit
                        onBackPressed();
                        //finish();
                    } else if (index == currentImageIndex) {
                        PictureScrollElement e = null;
                        if (index < imageScrollLayout.getChildCount()) {
                            e = (PictureScrollElement) imageScrollLayout.getChildAt(currentImageIndex);
                        } else {
                            e = (PictureScrollElement) imageScrollLayout.getChildAt(--currentImageIndex);
                        }
                        e.box();
                        setImages(e.getFile());

                    } else if (index < currentImageIndex) {
                        currentImageIndex--;
                    }
                    index = -1;
                }
            }

        };
        imageScrollLayout.setOnTouchListener(onSwipeTouchListener);

        mainPhoto = (ImageView) findViewById(R.id.Picture);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case R.id.action_share:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this,
                            "Image saved and shared.",
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                MediaStore.Images.Media.insertImage(getContentResolver(),
                        viewedBitmap,
                        createImageName(),
                        "Generated by Emulsify!");
                Toast.makeText(this,
                        "Image saved.",
                        Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_undo:
                viewedBitmap =
                        originalBitmap.copy(originalBitmap.getConfig(), originalBitmap.isMutable());
                mainPhoto.setImageBitmap(viewedBitmap);
                return true;
            case R.id.action_share:
                startActivity(createShareIntent(createImageName()));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addFiltersToScrollView(Mat image) {
        /*FilterScrollElement e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_RGBA, "Original", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);*/

        FilterScrollElement e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_CANNY, "Canny", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_GRAY, "Black & White", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);


        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_SEPIA, "Sepia", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_SOBEL, "Sobel", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_PIXELIZE, "Pixelize", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_POSTERIZE, "Posterize", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_INVERSE, "Inverse", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_WASH, "Washed Out", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_SAT, "Saturate", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_HUE, "Hue Rotate", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_BLUE, "Sad Day", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_RED, "Warm Day", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);

        e = new FilterScrollElement(this);
        e.initialize(FilterApplier.VIEW_MODE_PURPLE, "Purple Haze", image);
        e.setOnClickListener(this);
        filterScrollLayout.addView(e);
    }

    @Override
    public void onClick(View v) {
        for (int i = 0; i < filterScrollLayout.getChildCount(); i++) {
            if (v == filterScrollLayout.getChildAt(i)) {
                FilterScrollElement e = (FilterScrollElement) filterScrollLayout.getChildAt(i);
                viewMode = e.getFilterType();
                applyFilter();
                break;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        ev.getPointerCoords(0, coords);
        int x = (int) coords.x;
        int y = (int) coords.y;

        int base = -1;
        for (int i = 0; i < imageScrollLayout.getChildCount(); i++) {
            if (imageScrollLayout.getChildAt(i).getClass() == PictureScrollElement.class && base == -1)
                base = i;
            PictureScrollElement a = (PictureScrollElement) imageScrollLayout.getChildAt(i);
            int y1 = a.getTop() + getStatusBarHeight() + getActionBar().getHeight();
            int y2 = a.getBottom() + getStatusBarHeight() + getActionBar().getHeight();
            // tests the bounds of each image to determine where the swipe (if it WAS a swipe) took place
            if (y >= y1 && y < y2 && x >= a.getLeft() && x < a.getRight()) {
                onSwipeTouchListener.putIndex(i);// base);
                break;
            }
        }

        onSwipeTouchListener.getGestureDetector().onTouchEvent(ev);

        return super.dispatchTouchEvent(ev);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void applyFilter() {
        Mat mat = new Mat(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getDensity());// CvType.CV_8UC1);
        Utils.bitmapToMat(viewedBitmap, mat);

        switch (viewMode) {
            case FilterApplier.VIEW_MODE_SOBEL:
                break;

            case FilterApplier.VIEW_MODE_ZOOM:
                break;

            default:
                FilterApplier.applyFilter(viewMode, mat, mat);
        }

        Utils.matToBitmap(mat, viewedBitmap);
        mainPhoto.setImageBitmap(viewedBitmap);
    }

    public Intent createShareIntent(String imgName) {
        /* ACTION_SEND = share */
        String path = MediaStore.Images.Media.insertImage(getContentResolver(),
                viewedBitmap,
                createImageName(),
                "Generated by Emulsify!");
        Uri uri = Uri.parse(path);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/png");

        return shareIntent;
    }

    private String createImageName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentTime = sdf.format(new Date());
        File file = getFilesDir();
        String path = file.getPath();
        String imageString = path + "/emulsify_picture_" + currentTime + ".jpg";

        return imageString;
    }
}