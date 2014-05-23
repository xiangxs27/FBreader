package org.geometerplus.zlibrary.ui.android.view;

import org.geometerplus.android.fbreader.CancelActivity;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.jayqqaa12.abase.view.toast.T;
import com.jayqqaa12.reader.R;

public class ZLAndroidWidget extends View implements ZLViewWidget
{
	private final Paint myPaint = new Paint();
	private final BitmapManager myBitmapManager = new BitmapManager(this);
	private Bitmap myFooterBitmap;

	private FBReader mActivity;

	private Handler mHandler = new Handler();
	private boolean mIsAllowExit = false;

	public FBReader getmActivity()
	{
		return mActivity;
	}

	public void setmActivity(FBReader mActivity)
	{
		this.mActivity = mActivity;
	}

	public ZLAndroidWidget(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	public ZLAndroidWidget(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public ZLAndroidWidget(Context context)
	{
		super(context);
		init();
	}

	private void init()
	{
		setFocusableInTouchMode(true);
		setDrawingCacheEnabled(false);
		// setOnLongClickListener(this);
	}

	public void setFBActivity(FBReader activity)
	{
		mActivity = activity;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		getAnimationProvider().terminate();
		if (myScreenIsTouched)
		{
			final ZLView view = ZLApplication.Instance().getCurrentView();
			myScreenIsTouched = false;
			view.onScrollingFinished(ZLView.PageIndex.current);
		}
	}

	@Override
	protected void onDraw(final Canvas canvas)
	{
		final Context context = getContext();
		if (context instanceof FBReader) ((FBReader) context).createWakeLock();
		else System.err.println("A surprise: view's context is not an Reader");
		super.onDraw(canvas);

		if (getAnimationProvider().inProgress()) onDrawInScrolling(canvas);
		else
		{
			onDrawStatic(canvas);
			ZLApplication.Instance().onRepaintFinished();
		}
	}

	private AnimationProvider myAnimationProvider;
	private ZLView.Animation myAnimationType;

	private AnimationProvider getAnimationProvider()
	{
		final ZLView.Animation type = ZLApplication.Instance().getCurrentView().getAnimationType();
		if (myAnimationProvider == null || myAnimationType != type)
		{
			myAnimationType = type;
			switch (type)
			{
			case none:
				myAnimationProvider = new NoneAnimationProvider(myBitmapManager);
				break;
			case curl:
				myAnimationProvider = new CurlAnimationProvider(myBitmapManager);
				break;
			case slide:
				myAnimationProvider = new SlideAnimationProvider(myBitmapManager);
				break;
			case shift:
				myAnimationProvider = new ShiftAnimationProvider(myBitmapManager);
				break;
			}
		}
		return myAnimationProvider;
	}

	private void onDrawInScrolling(Canvas canvas)
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();

		final AnimationProvider animator = getAnimationProvider();
		final AnimationProvider.Mode oldMode = animator.getMode();
		animator.doStep();
		if (animator.inProgress())
		{
			animator.draw(canvas);
			if (animator.getMode().Auto)
			{
				postInvalidate();
			}
			drawFooter(canvas);
		}
		else
		{
			switch (oldMode)
			{
			case AnimatedScrollingForward:
			{
				final ZLView.PageIndex index = animator.getPageToScrollTo();
				myBitmapManager.shift(index == ZLView.PageIndex.next);
				view.onScrollingFinished(index);
				ZLApplication.Instance().onRepaintFinished();
				break;
			}
			case AnimatedScrollingBackward:
				view.onScrollingFinished(ZLView.PageIndex.current);
				break;
			}
			onDrawStatic(canvas);
		}
	}

	@Override
	public void reset()
	{
		myBitmapManager.reset();
	}

	@Override
	public void repaint()
	{
		postInvalidate();
	}

	@Override
	public void startManualScrolling(int x, int y, ZLView.Direction direction)
	{
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startManualScrolling(x, y);
	}

	@Override
	public void scrollManuallyTo(int x, int y)
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (view.canScroll(animator.getPageToScrollTo(x, y)))
		{
			animator.scrollTo(x, y);
			postInvalidate();
		}
	}

	@Override
	public void startAnimatedScrolling(ZLView.PageIndex pageIndex, int x, int y, ZLView.Direction direction, int speed)
	{

		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex))
		{

			if (pageIndex == ZLView.PageIndex.next) T.ShortToast(R.string.last_page);

			else if (pageIndex == ZLView.PageIndex.previous) T.ShortToast(R.string.first_page);

			return;
		}
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startAnimatedScrolling(pageIndex, x, y, speed);
		if (animator.getMode().Auto)
		{
			postInvalidate();
		}
	}

	@Override
	public void startAnimatedScrolling(ZLView.PageIndex pageIndex, ZLView.Direction direction, int speed)
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex)) { return; }
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startAnimatedScrolling(pageIndex, null, null, speed);
		if (animator.getMode().Auto)
		{
			postInvalidate();
		}

	}

	@Override
	public void startAnimatedScrolling(int x, int y, int speed)
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (!view.canScroll(animator.getPageToScrollTo(x, y)))
		{
			animator.terminate();
			return;
		}
		animator.startAnimatedScrolling(x, y, speed);
		postInvalidate();
	}

	void drawOnBitmap(Bitmap bitmap, ZLView.PageIndex index)
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view == null) { return; }

		final ZLAndroidPaintContext context = new ZLAndroidPaintContext(new Canvas(bitmap), getWidth(), getMainAreaHeight(),
				view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0);
		view.paint(context, index);
	}

	private void drawFooter(Canvas canvas)
	{

		final ZLView view = ZLApplication.Instance().getCurrentView();
		final ZLView.FooterArea footer = view.getFooterArea();

		if (footer == null)
		{
			myFooterBitmap = null;
			return;
		}
		if (myFooterBitmap != null && (myFooterBitmap.getWidth() != getWidth() || myFooterBitmap.getHeight() != footer.getHeight())) myFooterBitmap = null;
		if (myFooterBitmap == null) myFooterBitmap = Bitmap.createBitmap(getWidth(), footer.getHeight(), Bitmap.Config.RGB_565);

		final ZLAndroidPaintContext context = new ZLAndroidPaintContext(new Canvas(myFooterBitmap), getWidth(), footer.getHeight(),
				view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0);
		footer.paint(context);
		canvas.drawBitmap(myFooterBitmap, 0, getHeight() - footer.getHeight(), myPaint);
	}

	private void onDrawStatic(final Canvas canvas)
	{
		myBitmapManager.setSize(getWidth(), getMainAreaHeight());
		canvas.drawBitmap(myBitmapManager.getBitmap(ZLView.PageIndex.current), 0, 0, myPaint);
		drawFooter(canvas);
		new Thread()
		{
			@Override
			public void run()
			{
				final ZLView view = ZLApplication.Instance().getCurrentView();
				final ZLAndroidPaintContext context = new ZLAndroidPaintContext(canvas, getWidth(), getMainAreaHeight(),
						view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0);
				view.preparePage(context, ZLView.PageIndex.next);
			}
		}.start();
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
		}
		else
		{
			ZLApplication.Instance().getCurrentView().onTrackballRotated((int) (10 * event.getX()), (int) (10 * event.getY()));
		}
		return true;
	}

	private class LongClickRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if (performLongClick())
			{
				myLongClickPerformed = true;
			}
		}
	}

	private volatile LongClickRunnable myPendingLongClickRunnable;
	private volatile boolean myLongClickPerformed;

	private void postLongClickRunnable()
	{
		myLongClickPerformed = false;
		myPendingPress = false;
		if (myPendingLongClickRunnable == null)
		{
			myPendingLongClickRunnable = new LongClickRunnable();
		}
		postDelayed(myPendingLongClickRunnable, 2 * ViewConfiguration.getLongPressTimeout());
	}

	private class ShortClickRunnable implements Runnable
	{
		@Override
		public void run()
		{
			final ZLView view = ZLApplication.Instance().getCurrentView();
			view.onFingerSingleTap(myPressedX, myPressedY);
			myPendingPress = false;
			myPendingShortClickRunnable = null;
		}
	}

	private volatile ShortClickRunnable myPendingShortClickRunnable;

	private volatile boolean myPendingPress;
	private volatile boolean myPendingDoubleTap;
	private int myPressedX, myPressedY;
	private boolean myScreenIsTouched;

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int x = (int) event.getX();
		int y = (int) event.getY();

		final ZLView view = ZLApplication.Instance().getCurrentView();
		switch (event.getAction())
		{
		case MotionEvent.ACTION_UP:
			if (myPendingDoubleTap)
			{
				view.onFingerDoubleTap(x, y);
			}
			else if (myLongClickPerformed)
			{
				view.onFingerReleaseAfterLongPress(x, y);
			}
			else
			{
				if (myPendingLongClickRunnable != null)
				{
					removeCallbacks(myPendingLongClickRunnable);
					myPendingLongClickRunnable = null;
				}
				if (myPendingPress)
				{
					if (view.isDoubleTapSupported())
					{
						if (myPendingShortClickRunnable == null)
						{
							myPendingShortClickRunnable = new ShortClickRunnable();
						}
						postDelayed(myPendingShortClickRunnable, ViewConfiguration.getDoubleTapTimeout());
					}
					else
					{
						view.onFingerSingleTap(x, y);
					}
				}
				else
				{
					view.onFingerRelease(x, y);
				}
			}
			myPendingDoubleTap = false;
			myPendingPress = false;
			myScreenIsTouched = false;
			break;
		case MotionEvent.ACTION_DOWN:
			if (myPendingShortClickRunnable != null)
			{
				removeCallbacks(myPendingShortClickRunnable);
				myPendingShortClickRunnable = null;
				myPendingDoubleTap = true;
			}
			else
			{
				postLongClickRunnable();
				myPendingPress = true;
			}
			myScreenIsTouched = true;
			myPressedX = x;
			myPressedY = y;
			break;
		case MotionEvent.ACTION_MOVE:
		{
			final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
			final boolean isAMove = Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
			if (isAMove)
			{
				myPendingDoubleTap = false;
			}
			if (myLongClickPerformed)
			{
				view.onFingerMoveAfterLongPress(x, y);
			}
			else
			{
				if (myPendingPress)
				{
					if (isAMove)
					{
						if (myPendingShortClickRunnable != null)
						{
							removeCallbacks(myPendingShortClickRunnable);
							myPendingShortClickRunnable = null;
						}
						if (myPendingLongClickRunnable != null)
						{
							removeCallbacks(myPendingLongClickRunnable);
						}
						view.onFingerPress(myPressedX, myPressedY);
						myPendingPress = false;
					}
				}
				if (!myPendingPress)
				{
					view.onFingerMove(x, y);
				}
			}
			break;
		}
		}

		return true;
	}

	// @Override
	// public boolean onLongClick(View v)
	// {
	// final ZLView view = ZLApplication.Instance().getCurrentView();
	// return view.onFingerLongPress(myPressedX, myPressedY);
	// }

	private int myKeyUnderTracking = -1;

	// private long myTrackingStartTime;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (!mIsAllowExit)
			{
				T.ShortToast(R.string.exit_reader);
				mIsAllowExit = true;

				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						mIsAllowExit = false;
					}
				}, 2000);
			}
			else
			{
				Intent intent = new Intent();
				intent.putExtra(CancelActivity.TYPE_KEY, "close");
				mActivity.runCancelAction(intent);
			}
			return true;
		}

		final ZLApplication application = ZLApplication.Instance();

		if (application.hasActionForKey(keyCode, true) || application.hasActionForKey(keyCode, false))
		{
			if (myKeyUnderTracking != -1)
			{
				if (myKeyUnderTracking == keyCode)
				{
					return true;
				}
				else
				{
					myKeyUnderTracking = -1;
				}
			}
			if (application.hasActionForKey(keyCode, true))
			{
				myKeyUnderTracking = keyCode;
				// myTrackingStartTime = System.currentTimeMillis();
				return true;
			}
			else
			{
				return application.runActionByKey(keyCode, false);
			}
		}
		else
		{
			return false;
		}
	}

	// @Override
	// public boolean onKeyUp(int keyCode, KeyEvent event)
	// {
	// if (myKeyUnderTracking != -1)
	// {
	// if (myKeyUnderTracking == keyCode)
	// {
	// final boolean longPress = System.currentTimeMillis() >
	// myTrackingStartTime + ViewConfiguration.getLongPressTimeout();
	// ZLApplication.Instance().runActionByKey(keyCode, longPress);
	// }
	// myKeyUnderTracking = -1;
	// return true;
	// }
	// else
	// {
	// final ZLApplication application = ZLApplication.Instance();
	// return application.hasActionForKey(keyCode, false) ||
	// application.hasActionForKey(keyCode, true);
	// }
	// }

	@Override
	protected int computeVerticalScrollExtent()
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) { return 0; }
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress())
		{
			final int from = view.getScrollbarThumbLength(ZLView.PageIndex.current);
			final int to = view.getScrollbarThumbLength(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		}
		else
		{
			return view.getScrollbarThumbLength(ZLView.PageIndex.current);
		}
	}

	@Override
	protected int computeVerticalScrollOffset()
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) { return 0; }
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress())
		{
			final int from = view.getScrollbarThumbPosition(ZLView.PageIndex.current);
			final int to = view.getScrollbarThumbPosition(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		}
		else
		{
			return view.getScrollbarThumbPosition(ZLView.PageIndex.current);
		}
	}

	@Override
	protected int computeVerticalScrollRange()
	{
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) { return 0; }
		return view.getScrollbarFullSize();
	}

	private int getMainAreaHeight()
	{
		final ZLView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
		return footer != null ? getHeight() - footer.getHeight() : getHeight();
	}
}
