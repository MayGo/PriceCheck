package ee.maix.pricecheck;

import java.util.List;
import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public abstract class SampleCvViewBase extends SurfaceView implements
		SurfaceHolder.Callback, Runnable {
	private static final String TAG = "Sample::SurfaceView";

	private SurfaceHolder mHolder;
	private VideoCapture mCamera;

	
	
	public SampleCvViewBase(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		Log.i(TAG, "surfaceCreated");
		synchronized (this) {
			if (mCamera != null && mCamera.isOpened()) {
				Log.i(TAG, "before mCamera.getSupportedPreviewSizes()");
				List<Size> sizes = mCamera.getSupportedPreviewSizes();
				Log.i(TAG, "after mCamera.getSupportedPreviewSizes()");
				int mFrameWidth = width;
				int mFrameHeight = height;

				// selecting optimal camera preview size
				{
					double minDiff = Double.MAX_VALUE;
					for (Size size : sizes) {
						if (Math.abs(size.height - height) < minDiff) {
							mFrameWidth = (int) size.width;
							mFrameHeight = (int) size.height;
							minDiff = Math.abs(size.height - height);
						}
					}
				}

				mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
				mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
		if (mCamera.isOpened()) {
			(new Thread(this)).start();
		} else {
			mCamera.release();
			mCamera = null;
			Log.e(TAG, "Failed to open native camera");
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
		if (mCamera != null) {
			synchronized (this) {
				mCamera.release();
				mCamera = null;
			}
		}
	}

	protected abstract Bitmap processFrame(VideoCapture capture);

	public void run() {
		Log.i(TAG, "Starting processing thread");
		while (true) {

			switch (Sample2NativeCamera.viewMode) {
			case Sample2NativeCamera.VIEW_MODE_PREVIEW:
				Bitmap lastBmp = Sample2View.lastBmp;
				if (lastBmp != null) {
					Canvas canvas = mHolder.lockCanvas();
					if (canvas != null) {
						canvas.drawBitmap(lastBmp,
								(canvas.getWidth() - lastBmp.getWidth()) / 2,
								(canvas.getHeight() - lastBmp.getHeight()) / 2,
								null);
						mHolder.unlockCanvasAndPost(canvas);
					}
					lastBmp.recycle();

				}
				break;
			case Sample2NativeCamera.VIEW_MODE_SCAN:
				Bitmap bmp = null;

				synchronized (this) {
					if (mCamera == null)
						break;

					if (!mCamera.grab()) {
						Log.e(TAG, "mCamera.grab() failed");
						break;
					}

					bmp = processFrame(mCamera);
				}

				if (bmp != null) {
					Canvas canvas = mHolder.lockCanvas();
					if (canvas != null) {
						canvas.drawBitmap(bmp,
								(canvas.getWidth() - bmp.getWidth()) / 2,
								(canvas.getHeight() - bmp.getHeight()) / 2,
								null);
						mHolder.unlockCanvasAndPost(canvas);
					}
					bmp.recycle();
				}
				break;
			}

			Log.i(TAG, "Finishing processing thread");
		}
	}
}