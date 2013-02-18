package ee.maix.pricecheck;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import ee.maix.pricecheck.R;

public class Sample2NativeCamera extends Activity {
    private static final String TAG             = "Sample::Activity";

    public static final int     VIEW_MODE_SCAN  = 0;
    public static final int     VIEW_MODE_PREVIEW  = 1;
    public static final int     VIEW_MODE_CANNY = 2;
    public static final int     VIEW_MODE_LINES = 3;

    private MenuItem            mItemScan;
    private MenuItem            mItemPreview;
    private MenuItem            mItemCanny;
    private MenuItem            mItemLines;

    public static int           viewMode        = VIEW_MODE_SCAN;

    public Sample2NativeCamera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(new Sample2View(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        mItemScan = menu.add("Scan");
        mItemPreview = menu.add("Preview");
        mItemCanny = menu.add("Canny");
        mItemLines = menu.add("Lines");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemScan)
            viewMode = VIEW_MODE_SCAN;
        else if (item == mItemPreview)
            viewMode = VIEW_MODE_PREVIEW;
        else if (item == mItemCanny)
            viewMode = VIEW_MODE_CANNY;
        else if (item == mItemLines)
            viewMode = VIEW_MODE_LINES;
        return true;
    }
}
