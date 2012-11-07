package org.opencv.samples.tutorial2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;
import android.view.SurfaceHolder;

class Sample2View extends SampleCvViewBase {
	private Mat mRgba;
	private Mat mGray;
	private Mat mIntermediateMat;

	private Mat mBG;
	private Mat mBsub;

	public static Point detection;
	public static Point detectionoffset;

	public Sample2View(Context context) {
		super(context);
	}

	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		super.surfaceChanged(_holder, format, width, height);

		synchronized (this) {
			// initialize Mats before usage
			mGray = new Mat();
			mRgba = new Mat();
			mIntermediateMat = new Mat();

			mBG = new Mat();
			mBsub = new Mat();
			detection = new Point(0, 0);
			detectionoffset = new Point(0, 0);
		}
	}

	private void warp(Mat in, Mat out, Size size, Vector<Point> points) {
		Mat pointsIn = new Mat(4, 1, CvType.CV_32FC2);
		Mat pointsRes = new Mat(4, 1, CvType.CV_32FC2);
		pointsIn.put(0, 0, points.get(0).x, points.get(0).y, points.get(1).x,
				points.get(1).y, points.get(2).x, points.get(2).y,
				points.get(3).x, points.get(3).y);
		pointsRes.put(0, 0, 0, 0, size.width - 1, 0, size.width - 1,
				size.height - 1, 0, size.height - 1);
		Mat m = new Mat();
		m = Imgproc.getPerspectiveTransform(pointsIn, pointsRes);
		Imgproc.warpPerspective(in, out, m, size);
	}

	private Vector<Point> findRectangle(Mat grayMat) {
		List<Mat> newContours = new ArrayList<Mat>();
		List<Mat> contours = new ArrayList<Mat>();
		Mat hierarchy = new Mat(200, 200, CvType.CV_8UC1, new Scalar(0));

		Imgproc.Canny(grayMat, grayMat, 20, 150);

		Imgproc.findContours(grayMat, contours, hierarchy,
				Imgproc.RETR_EXTERNAL, Imgproc.CV_CONTOURS_MATCH_I1);

		Vector<Point> candidateMarker = new Vector<Point>();
		for (int i = 0; i < contours.size(); i++) {
			List<Point> approxPoints = new ArrayList<Point>();
			Mat approxCurve = new Mat();
			Mat contour = contours.get(i);

			double MIN_DISTANCE = 10;
			// first check if it has enough points
			int contourSize = (int) contour.total();
			if (contourSize > grayMat.cols() / 5) {
				Imgproc.approxPolyDP(contour, approxCurve, contourSize * 0.05,
						true);
				Converters.Mat_to_vector_Point(approxCurve, approxPoints);
				// check the polygon has 4 points
				if (approxCurve.total() == 4) {
					// and if it is convex
					if (Imgproc.isContourConvex(approxPoints)) {
						// ensure the distance between consecutive points is
						// large enough
						double minDistFound = Double.MAX_VALUE;
						int[] points = new int[8];// [x1 y1 x2 y2 x3 y3 x4
													// y4]
						approxCurve.get(0, 0, points);
						// look for the min distance
						for (int j = 0; j <= 4; j += 2) {
							double d = Math
									.sqrt((points[j] - points[(j + 2) % 4])
											* (points[j] - points[(j + 2) % 4])
											+ (points[j + 1] - points[(j + 3) % 4])
											* (points[j + 1] - points[(j + 3) % 4]));
							if (d < minDistFound)
								minDistFound = d;
						}
						if (minDistFound > MIN_DISTANCE) {
							// newContours.add(approxCurve);

							candidateMarker
									.add(new Point(points[0], points[1]));
							candidateMarker
									.add(new Point(points[2], points[3]));
							candidateMarker
									.add(new Point(points[4], points[5]));
							candidateMarker
									.add(new Point(points[6], points[7]));

							double dx1 = candidateMarker.get(1).x
									- candidateMarker.get(0).x;
							double dy1 = candidateMarker.get(1).y
									- candidateMarker.get(0).y;
							double dx2 = candidateMarker.get(2).x
									- candidateMarker.get(0).x;
							double dy2 = candidateMarker.get(2).y
									- candidateMarker.get(0).y;
							double o = dx1 * dy2 - dy1 * dx2;
							if (o < 0.0) // the third point is in the left side,
											// we have to swap
								Collections.swap(candidateMarker, 1, 3);
						}
					}
				}
			}
		}
		// if (newContours.size() > 0) {
		// // Imgproc.drawContours(mRgba, newContours, -1, new Scalar(0, 0,
		// // 255), 3);
		// }

		if (candidateMarker.size() == 4) {
			
			return candidateMarker;
		}
		return null;
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		List<Mat> newContours = new ArrayList<Mat>();
		List<Mat> contours = new ArrayList<Mat>();
		Mat hierarchy = new Mat(200, 200, CvType.CV_8UC1, new Scalar(0));
		switch (Sample2NativeCamera.viewMode) {
		case Sample2NativeCamera.VIEW_MODE_GRAY:
			capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
			Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
			break;
		case Sample2NativeCamera.VIEW_MODE_RGBA:
			capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);

			Vector<Point> marker = findRectangle(mGray);
			if (marker != null) {
				Mat canonicalMarker = new Mat();
				warp(mRgba, canonicalMarker, mRgba.size(), marker);
				mRgba = canonicalMarker;
			}
			break;
		case Sample2NativeCamera.VIEW_MODE_CANNY:
			capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
			Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
			Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA,
					4);
			break;
		/*
		 * 
		 * Convert from RGB to grayscale (cvCvtColor) Smooth (cvSmooth)
		 * Threshold (cvThreshold) Detect edges (cvCanny) Find contours
		 * (cvFindContours) Approximate contours with linear features
		 * (cvApproxPoly)
		 */
		case Sample2NativeCamera.VIEW_MODE_LINES:
			capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);

			// Mat mHSV =new Mat();
			// Mat mHSVThreshed =new Mat();
			// Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV,0);
			// Core.inRange(mHSV, new Scalar(20, 100, 100), new Scalar(30, 255,
			// 255), mHSVThreshed);
			// Imgproc.cvtColor(mHSVThreshed, mRgba, Imgproc.COLOR_GRAY2RGB, 0);
			// Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2RGBA, 0);

			Log.v("maigo", "1");

			// 1) Apply gaussian blur to remove noise
			Imgproc.GaussianBlur(mGray, mIntermediateMat, new Size(11, 11), 0);

			// 2) AdaptiveThreshold -> classify as either black or white
			Imgproc.adaptiveThreshold(mIntermediateMat, mIntermediateMat, 255,
					Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);

			// 3) Invert the image -> so most of the image is black
			Core.bitwise_not(mIntermediateMat, mIntermediateMat);

			// 4) Dilate -> fill the image using the MORPH_DILATE
			Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE,
					new Size(3, 3), new Point(1, 1));

			Imgproc.dilate(mIntermediateMat, mIntermediateMat, kernel);

			Imgproc.cvtColor(mIntermediateMat, mGray, Imgproc.COLOR_GRAY2RGBA,
					4);
			// Imgproc.Canny(mIntermediateMat, mGray, 20, 150);

			// Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);

			Log.v("maigo", "" + mGray.size());

			// Imgproc.findContours(mGray, contours, hierarchy,
			// Imgproc.RETR_EXTERNAL, Imgproc.CV_CONTOURS_MATCH_I1);
			Log.v("maigo", "contours.size() " + contours.size());

			for (int i = 0; i < contours.size(); i++) {
				// Log.v("maigo", "asdasd");

				Mat approxCurve = new Mat();
				List<Point> points = new ArrayList<Point>();
				Converters.Mat_to_vector_Point(contours.get(i), points);
				Imgproc.approxPolyDP(contours.get(i), approxCurve,
						Imgproc.arcLength(points, true) * 0.02, true);

				List<Point> isConvex = new ArrayList<Point>();
				Converters.Mat_to_vector_Point(approxCurve, isConvex);

				if ((Math.abs(Imgproc.contourArea(approxCurve)) > 200)
						&& Imgproc.isContourConvex(isConvex)) {
					Log.v("maigo", "isConvex.size() " + isConvex.size());
					isConvex.size();
					newContours.add(approxCurve);

				}
			}
			// Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
			if (newContours.size() > 0) {
				Log.v("maigo", "newContours.........................."
						+ newContours);
				Imgproc.drawContours(mRgba, newContours, -1, new Scalar(0, 0,
						255), 3);
			}

			break;
		/*
		 * capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
		 * 
		 * Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 0);
		 * 
		 * Imgproc.Canny(mGray, mIntermediateMat, 20, 150);
		 * 
		 * Log.v("maigo", "" + mGray.size()); double len = mGray.size().height;
		 * Mat lines = new Mat(); Log.d("HoughLinesP", "lines"); double rho = 1,
		 * theta = 3.14 / 180, minLineLength = len / 2, maxLineGap = 20; int
		 * threshold = 50; Imgproc.HoughLinesP(mIntermediateMat, lines, rho,
		 * theta, threshold, minLineLength, maxLineGap);
		 * Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA,
		 * 4); if (lines != null) { Log.d("HoughLinesP", "lines=" +
		 * lines.total());
		 * 
		 * int[] point = new int[4]; if (lines.total() >= 4) { for (int i = 0; i
		 * < lines.total(); i++) { lines.get(0, i, point); // double[] point =
		 * lines.get(0, i); Scalar sca = new Scalar(0, 0, 255); // Color to draw
		 * line Point pt1 = new Point(point[0], point[1]); Point pt2 = new
		 * Point(point[2], point[3]); Core.line(mRgba, pt1, pt2, sca, 3); //
		 * Log.d("HoughLinesP", "drawing line"); // Draw the line } } } break;
		 */
		}

		Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),
				Bitmap.Config.ARGB_8888);

		if (Utils.matToBitmap(mRgba, bmp))
			return bmp;

		bmp.recycle();
		return null;
	}

	@Override
	public void run() {
		super.run();

		synchronized (this) {
			// Explicitly deallocate Mats
			if (mRgba != null)
				mRgba.release();
			if (mGray != null)
				mGray.release();
			if (mIntermediateMat != null)
				mIntermediateMat.release();

			mRgba = null;
			mGray = null;
			mIntermediateMat = null;
		}
	}
}
