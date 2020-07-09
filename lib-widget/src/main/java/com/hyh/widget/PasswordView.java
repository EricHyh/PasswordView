package com.hyh.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("AppCompatCustomView")
public class PasswordView extends EditText implements TextWatcher {

    private static final String TAG = "PasswordView_";

    public static final int PASSWORD_TYPE_STARS = 0;
    public static final int PASSWORD_TYPE_CIRCLE = 1;
    public static final int PASSWORD_TYPE_TEXT = 2;

    private static final int BOX_MEASURE_MODE_BOUND = 0;
    private static final int BOX_MEASURE_MODE_FILL = 1;
    private static final int BOX_MEASURE_MODE_FREE = 2;

    private static final int BOX_TYPE_RECT = 0;
    private static final int BOX_TYPE_OVAL = 1;
    private static final int BOX_TYPE_UNDERLINE = 2;

    private static final int BOX_CHAIN_STYLE_FREE = 0;
    private static final int BOX_CHAIN_STYLE_SPREAD = 1;
    private static final int BOX_CHAIN_STYLE_SPREAD_INSIDE = 2;
    private static final int BOX_CHAIN_STYLE_PACKET = 3;

    private final DrawCursorToggleTask mDrawCursorToggleTask = new DrawCursorToggleTask();

    private int mPasswordType = PASSWORD_TYPE_STARS;

    private int mBoxBackgroundColor;
    private int mBoxBorderColor;
    private float mRectBoxRadius;

    private float mCursorWidth;
    private float mCursorMarginTop;
    private float mCursorMarginBottom;
    private int mCursorColor = Color.BLUE;
    private boolean mCursorEnabled = true;

    private boolean mDrawCursor;

    private IMeasurer mMeasurer = new BoundMeasurer();
    private final MeasureInfo mMeasureInfo = new MeasureInfo();
    private final MeasureResult mMeasuring = new MeasureResult();
    private final MeasureResult mMeasured = new MeasureResult();

    private final Paint mBoxBoardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBoxBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PorterDuffXfermode mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

    private final RectF mTempRectF = new RectF();
    private final Path mTempPath = new Path();
    private final float[] mBoxRadii = new float[8];
    private final List<RectF> mBoxRectFs = new ArrayList<>();

    private float mDensity;

    private PasswordListener mPasswordListener;

    public PasswordView(Context context) {
        super(context);
        init(null);
    }

    public PasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PasswordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mDensity = getResources().getDisplayMetrics().density;
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PasswordView);
            mMeasureInfo.passwordLength = typedArray.getInteger(R.styleable.PasswordView_passwordLength, 6);
            mPasswordType = typedArray.getInt(R.styleable.PasswordView_passwordType, PASSWORD_TYPE_STARS);

            mMeasureInfo.boxType = typedArray.getInt(R.styleable.PasswordView_boxType, BOX_TYPE_RECT);
            int boxMeasureMode = typedArray.getInt(R.styleable.PasswordView_boxMeasureMode, BOX_MEASURE_MODE_BOUND);
            switch (boxMeasureMode) {
                default:
                case BOX_MEASURE_MODE_BOUND: {
                    mMeasurer = new BoundMeasurer();
                    break;
                }
                case BOX_MEASURE_MODE_FILL: {
                    mMeasurer = new FillMeasurer();
                    break;
                }
                case BOX_MEASURE_MODE_FREE: {
                    mMeasurer = new FreeMeasurer();
                    break;
                }
            }

            mMeasureInfo.boxChainStyle = typedArray.getInt(R.styleable.PasswordView_boxChainStyle, BOX_CHAIN_STYLE_FREE);

            mMeasureInfo.boxWidth = typedArray.getDimension(R.styleable.PasswordView_boxWidth, 0);
            mMeasureInfo.boxHeight = typedArray.getDimension(R.styleable.PasswordView_boxHeight, 0);
            mMeasureInfo.boxWidthPercent = typedArray.getFloat(R.styleable.PasswordView_boxWidthPercent, 0);
            mMeasureInfo.boxHeightRatio = typedArray.getFloat(R.styleable.PasswordView_boxHeightRatio, 1.0f);


            mMeasureInfo.boxBorderSize = typedArray.getDimension(R.styleable.PasswordView_boxBorderSize, mDensity * 1);
            mMeasureInfo.boxSpace = typedArray.getDimension(R.styleable.PasswordView_boxSpace, 0);
            mMeasureInfo.boxSpacePercent = typedArray.getFloat(R.styleable.PasswordView_boxSpacePercent, 0);
            mMeasureInfo.mergeRectBoxEnabled = typedArray.getBoolean(R.styleable.PasswordView_mergeRectBoxEnabled, true);
            mMeasureInfo.mergedRectBoxDividerWidth = typedArray.getDimension(R.styleable.PasswordView_mergedRectBoxDividerWidth, mDensity * 1);

            mBoxBackgroundColor = typedArray.getColor(R.styleable.PasswordView_boxBackgroundColor, Color.TRANSPARENT);
            mBoxBorderColor = typedArray.getColor(R.styleable.PasswordView_boxBordColor, Color.BLACK);
            mRectBoxRadius = typedArray.getDimension(R.styleable.PasswordView_rectBoxRadius, 0);

            mCursorWidth = typedArray.getDimension(R.styleable.PasswordView_cursorWidth, mDensity * 2);
            mCursorMarginTop = typedArray.getDimension(R.styleable.PasswordView_cursorMarginTop, mDensity * 8);
            mCursorMarginBottom = typedArray.getDimension(R.styleable.PasswordView_cursorMarginBottom, mDensity * 8);
            mCursorColor = typedArray.getColor(R.styleable.PasswordView_cursorColor, Color.BLACK);
            mCursorEnabled = typedArray.getBoolean(R.styleable.PasswordView_cursorEnabled, true);

            typedArray.recycle();
        } else {
            mMeasureInfo.boxBorderSize = mDensity * 1;
            mCursorWidth = mDensity * 2;
            mMeasureInfo.mergedRectBoxDividerWidth = mDensity * 1;
            mCursorMarginTop = mCursorMarginBottom = mDensity * 8;

            setBackgroundDrawable(null);
        }

        InputFilter[] filters = {new InputFilter.LengthFilter(mMeasureInfo.passwordLength)};
        setFilters(filters);

        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
        setLongClickable(false);
        setCursorVisible(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setCursorEnabled(boolean enabled) {
        mCursorEnabled = enabled;
        if (enabled) {
            if (getWindowToken() != null) {
                Handler handler = getHandler();
                if (handler != null) {
                    handler.post(mDrawCursorToggleTask);
                }
            }
        } else {
            Handler handler = getHandler();
            if (handler != null) {
                handler.removeCallbacks(mDrawCursorToggleTask);
            }
        }
    }

    public void setPasswordListener(PasswordListener passwordListener) {
        mPasswordListener = passwordListener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeTextChangedListener(this);
        addTextChangedListener(this);
        if (mCursorEnabled) {
            Handler handler = getHandler();
            if (handler != null) {
                handler.post(mDrawCursorToggleTask);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeTextChangedListener(this);
        Handler handler = getHandler();
        if (handler != null) {
            handler.removeCallbacks(mDrawCursorToggleTask);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMeasuring.clear();
        IMeasurer measurer = mMeasurer;
        if (measurer == null) {
            setMeasuredDimension(0, 0);
            mMeasured.clear();
            return;
        }

        measurer.measure(this, widthMeasureSpec, heightMeasureSpec, mMeasureInfo, mMeasuring);

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int passwordLength = mMeasureInfo.passwordLength;

        mMeasuring.measureContentWidth = horizontalPadding
                + mMeasured.measureBoxWidth * passwordLength
                + mMeasureInfo.boxBorderSize * 2 * passwordLength
                + (mMeasured.mergedRectBox ? (mMeasureInfo.mergedRectBoxDividerWidth - 2 * mMeasureInfo.boxBorderSize) * (passwordLength - 1) : 0)
                + mMeasured.measureBoxSpace * (passwordLength - 1)
                + mMeasured.measureBoxChainMargin * 2;

        mMeasuring.measureContentHeight = verticalPadding
                + mMeasured.measureBoxHeight
                + 2 * mMeasureInfo.boxBorderSize;


        mMeasured.copy(mMeasuring);

        setMeasuredDimension(Math.round(mMeasured.measureWidth), Math.round(mMeasured.measureHeight));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);不绘制EditText本身的文字
        canvas.save();

        float dx = (getMeasuredWidth() - mMeasured.measureContentWidth) * 0.5f + getPaddingLeft() + mMeasured.measureBoxChainMargin;
        float dy = (getMeasuredHeight() - mMeasured.measureContentHeight) * 0.5f + getPaddingTop();
        canvas.translate(dx, dy);
        List<RectF> boxRectFs = drawBox(canvas);
        if (boxRectFs == null) return;
        drawCursor(canvas, boxRectFs);
        drawText(canvas, boxRectFs);
        canvas.restore();
    }

    private List<RectF> drawBox(Canvas canvas) {
        switch (mMeasureInfo.boxType) {
            case BOX_TYPE_RECT: {
                return drawRectBox(canvas);
            }
            case BOX_TYPE_OVAL: {
                return drawOvalBox(canvas);
            }
            case BOX_TYPE_UNDERLINE: {
                return drawUnderlineBox(canvas);
            }
        }
        return null;
    }

    private List<RectF> drawRectBox(Canvas canvas) {
        mBoxBoardPaint.setColor(mBoxBorderColor);
        mBoxBoardPaint.setStyle(Paint.Style.STROKE);

        mBoxBackgroundPaint.setColor(mBoxBackgroundColor);
        mBoxBackgroundPaint.setStyle(Paint.Style.FILL);

        int passwordLength = mMeasureInfo.passwordLength;
        float boxBorderSize = mMeasureInfo.boxBorderSize;

        float measureBoxWidth = mMeasured.measureBoxWidth;
        float measureBoxHeight = mMeasured.measureBoxHeight;
        float measureBoxSpace = mMeasured.measureBoxSpace;
        boolean mergedRectBox = mMeasured.mergedRectBox;

        if (mergedRectBox) {

            mBoxBoardPaint.setStrokeWidth(boxBorderSize);

            float left = boxBorderSize * 0.5f;
            float top = boxBorderSize * 0.5f;
            float right = mMeasured.measureContentWidth - mMeasured.measureBoxChainMargin * 2 - (getPaddingLeft() + getPaddingRight()) - boxBorderSize * 0.5f;
            float bottom = mMeasured.measureContentHeight - (getPaddingTop() + getPaddingBottom()) - boxBorderSize * 0.5f;
            mTempRectF.set(left, top, right, bottom);

            if (boxBorderSize > 0) {
                canvas.drawRoundRect(mTempRectF, mRectBoxRadius, mRectBoxRadius, mBoxBoardPaint);
            }

            RectF lastBoxRectF = null;

            mBoxBoardPaint.setStrokeWidth(mMeasureInfo.mergedRectBoxDividerWidth);
            mBoxBoardPaint.setColor(mBoxBorderColor);
            mBoxBoardPaint.setStyle(Paint.Style.STROKE);
            for (int index = 0; index < passwordLength; index++) {
                if (index == 0) {
                    left = boxBorderSize;
                    top = boxBorderSize;
                    right = left + measureBoxWidth;
                    bottom = top + measureBoxHeight;
                } else {
                    left = lastBoxRectF.right + mMeasureInfo.mergedRectBoxDividerWidth;
                    top = boxBorderSize;
                    right = left + measureBoxWidth;
                    bottom = top + measureBoxHeight;
                }
                RectF boxRectF;
                if (mBoxRectFs.size() > index) {
                    boxRectF = mBoxRectFs.get(index);
                } else {
                    boxRectF = new RectF();
                    mBoxRectFs.add(boxRectF);
                }
                boxRectF.set(left, top, right, bottom);
                lastBoxRectF = boxRectF;

                if (boxBorderSize > 0) {
                    if (index < passwordLength - 1) {
                        float startX = boxRectF.right + mMeasureInfo.mergedRectBoxDividerWidth * 0.5f;
                        float startY = boxRectF.top;
                        float stopX = startX;
                        float stopY = boxRectF.bottom;
                        canvas.drawLine(startX, startY, stopX, stopY, mBoxBoardPaint);
                    }
                }
                drawMergedRectBoxBackground(canvas, index, boxRectF);
            }

        } else {
            mBoxBoardPaint.setStrokeWidth(boxBorderSize);

            for (int index = 0; index < passwordLength; index++) {

                float left = measureBoxWidth * index + boxBorderSize * 2 * index + measureBoxSpace * index + boxBorderSize * 0.5f;
                float top = boxBorderSize * 0.5f;
                float right = left + measureBoxWidth + boxBorderSize;
                float bottom = top + measureBoxHeight + boxBorderSize;

                mTempRectF.set(left, top, right, bottom);

                if (boxBorderSize == 0) {
                    RectF boxRectF;
                    if (mBoxRectFs.size() > index) {
                        boxRectF = mBoxRectFs.get(index);
                    } else {
                        boxRectF = new RectF();
                        mBoxRectFs.add(boxRectF);
                    }
                    boxRectF.set(left, top, right, bottom);
                    canvas.drawRoundRect(mTempRectF, mRectBoxRadius, mRectBoxRadius, mBoxBackgroundPaint);

                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        int saveLayer = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mBoxBackgroundPaint);
                        canvas.drawRoundRect(mTempRectF, mRectBoxRadius, mRectBoxRadius, mBoxBoardPaint);

                        RectF boxRectF;
                        if (mBoxRectFs.size() > index) {
                            boxRectF = mBoxRectFs.get(index);
                        } else {
                            boxRectF = new RectF();
                            mBoxRectFs.add(boxRectF);
                        }
                        left += boxBorderSize * 0.5f;
                        top += boxBorderSize * 0.5f;
                        right -= boxBorderSize * 0.5f;
                        bottom -= boxBorderSize * 0.5f;
                        boxRectF.set(left, top, right, bottom);


                        mBoxBackgroundPaint.setXfermode(mXfermode);
                        canvas.drawRoundRect(mTempRectF, mRectBoxRadius, mRectBoxRadius, mBoxBackgroundPaint);
                        mBoxBackgroundPaint.setXfermode(null);

                        canvas.restoreToCount(saveLayer);
                    } else {
                        canvas.drawRoundRect(mTempRectF, mRectBoxRadius, mRectBoxRadius, mBoxBoardPaint);

                        RectF boxRectF;
                        if (mBoxRectFs.size() > index) {
                            boxRectF = mBoxRectFs.get(index);
                        } else {
                            boxRectF = new RectF();
                            mBoxRectFs.add(boxRectF);
                        }
                        left += boxBorderSize * 0.5f;
                        top += boxBorderSize * 0.5f;
                        right -= boxBorderSize * 0.5f;
                        bottom -= boxBorderSize * 0.5f;
                        boxRectF.set(left, top, right, bottom);

                        float boxBackgroundRadius = getBoxBackgroundRadius();
                        canvas.drawRoundRect(boxRectF, boxBackgroundRadius, boxBackgroundRadius, mBoxBackgroundPaint);
                    }
                }
            }
        }

        return mBoxRectFs;
    }

    private void drawMergedRectBoxBackground(Canvas canvas, int index, RectF boxRectF) {
        float boxBackgroundRadius = getBoxBackgroundRadius();
        mTempPath.reset();
        if (index == 0) {
            mBoxRadii[0] = boxBackgroundRadius;
            mBoxRadii[1] = boxBackgroundRadius;

            mBoxRadii[2] = 0;
            mBoxRadii[3] = 0;

            mBoxRadii[4] = 0;
            mBoxRadii[5] = 0;

            mBoxRadii[6] = boxBackgroundRadius;
            mBoxRadii[7] = boxBackgroundRadius;

            mTempPath.addRoundRect(boxRectF, mBoxRadii, Path.Direction.CW);
        } else if (index == mMeasureInfo.passwordLength - 1) {
            mBoxRadii[0] = 0;
            mBoxRadii[1] = 0;

            mBoxRadii[2] = boxBackgroundRadius;
            mBoxRadii[3] = boxBackgroundRadius;

            mBoxRadii[4] = boxBackgroundRadius;
            mBoxRadii[5] = boxBackgroundRadius;

            mBoxRadii[6] = 0;
            mBoxRadii[7] = 0;

            mTempPath.addRoundRect(boxRectF, mBoxRadii, Path.Direction.CW);
        } else {
            mBoxRadii[0] = 0;
            mBoxRadii[1] = 0;

            mBoxRadii[2] = 0;
            mBoxRadii[3] = 0;

            mBoxRadii[4] = 0;
            mBoxRadii[5] = 0;

            mBoxRadii[6] = 0;
            mBoxRadii[7] = 0;

            mTempPath.addRoundRect(boxRectF, mBoxRadii, Path.Direction.CW);
        }
        canvas.drawPath(mTempPath, mBoxBackgroundPaint);
    }

    private float getBoxBackgroundRadius() {
        float boxBorderSize = mMeasureInfo.boxBorderSize;

        if (mRectBoxRadius == 0) return 0;
        if (boxBorderSize == 0) return 0;
        float radius = 1.5f * (boxBorderSize / mDensity - 1) + 0.5f;
        return mRectBoxRadius - Math.max(0, radius);
    }

    private List<RectF> drawOvalBox(Canvas canvas) {
        int passwordLength = mMeasureInfo.passwordLength;
        float boxBorderSize = mMeasureInfo.boxBorderSize;

        float measureBoxWidth = mMeasured.measureBoxWidth;
        float measureBoxHeight = mMeasured.measureBoxHeight;
        float measureBoxSpace = mMeasured.measureBoxSpace;


        mBoxBoardPaint.setColor(mBoxBorderColor);
        mBoxBoardPaint.setStyle(Paint.Style.STROKE);
        mBoxBoardPaint.setStrokeWidth(boxBorderSize);

        mBoxBackgroundPaint.setColor(mBoxBackgroundColor);
        mBoxBackgroundPaint.setStyle(Paint.Style.FILL);

        for (int index = 0; index < passwordLength; index++) {

            float left = measureBoxWidth * index + boxBorderSize * 2 * index + measureBoxSpace * index + boxBorderSize * 0.5f;
            float top = boxBorderSize * 0.5f;
            float right = left + measureBoxWidth + boxBorderSize;
            float bottom = top + measureBoxHeight + boxBorderSize;

            mTempRectF.set(left, top, right, bottom);

            if (boxBorderSize == 0) {
                RectF boxRect;
                if (mBoxRectFs.size() > index) {
                    boxRect = mBoxRectFs.get(index);
                } else {
                    boxRect = new RectF();
                    mBoxRectFs.add(boxRect);
                }
                boxRect.set(left, top, right, bottom);
                canvas.drawOval(boxRect, mBoxBackgroundPaint);
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    int saveLayer = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mBoxBackgroundPaint);

                    canvas.drawOval(mTempRectF, mBoxBoardPaint);

                    RectF boxRect;
                    if (mBoxRectFs.size() > index) {
                        boxRect = mBoxRectFs.get(index);
                    } else {
                        boxRect = new RectF();
                        mBoxRectFs.add(boxRect);
                    }
                    left += boxBorderSize * 0.5f;
                    top += boxBorderSize * 0.5f;
                    right -= boxBorderSize * 0.5f;
                    bottom -= boxBorderSize * 0.5f;
                    boxRect.set(left, top, right, bottom);

                    mBoxBackgroundPaint.setXfermode(mXfermode);
                    canvas.drawOval(mTempRectF, mBoxBackgroundPaint);
                    mBoxBackgroundPaint.setXfermode(null);

                    canvas.restoreToCount(saveLayer);
                } else {

                    canvas.drawOval(mTempRectF, mBoxBoardPaint);

                    RectF boxRect;
                    if (mBoxRectFs.size() > index) {
                        boxRect = mBoxRectFs.get(index);
                    } else {
                        boxRect = new RectF();
                        mBoxRectFs.add(boxRect);
                    }
                    left += boxBorderSize * 0.5f;
                    top += boxBorderSize * 0.5f;
                    right -= boxBorderSize * 0.5f;
                    bottom -= boxBorderSize * 0.5f;
                    boxRect.set(left, top, right, bottom);

                    canvas.drawOval(boxRect, mBoxBackgroundPaint);
                }
            }
        }

        return mBoxRectFs;
    }

    private List<RectF> drawUnderlineBox(Canvas canvas) {
        int passwordLength = mMeasureInfo.passwordLength;
        float boxBorderSize = mMeasureInfo.boxBorderSize;

        float measureBoxWidth = mMeasured.measureBoxWidth;
        float measureBoxHeight = mMeasured.measureBoxHeight;
        float measureBoxSpace = mMeasured.measureBoxSpace;


        mBoxBoardPaint.setColor(mBoxBorderColor);
        mBoxBoardPaint.setStyle(Paint.Style.FILL);
        mBoxBoardPaint.setStrokeWidth(boxBorderSize);

        mBoxBackgroundPaint.setColor(mBoxBackgroundColor);
        mBoxBackgroundPaint.setStyle(Paint.Style.FILL);

        for (int index = 0; index < passwordLength; index++) {

            float startX = measureBoxWidth * index + boxBorderSize * 2 * index + measureBoxSpace * index;
            float startY = measureBoxHeight + boxBorderSize * 1.5f;
            float stopX = startX + measureBoxWidth + boxBorderSize * 2;
            float stopY = startY;

            if (boxBorderSize > 0) {
                canvas.drawLine(startX, startY, stopX, stopY, mBoxBoardPaint);
            }

            RectF boxRect;
            if (mBoxRectFs.size() > index) {
                boxRect = mBoxRectFs.get(index);
            } else {
                boxRect = new RectF();
                mBoxRectFs.add(boxRect);
            }

            boxRect.set(startX, boxBorderSize, stopX, boxBorderSize + measureBoxHeight);

            canvas.drawRect(boxRect, mBoxBackgroundPaint);
        }

        return mBoxRectFs;
    }

    private void drawCursor(Canvas canvas, List<RectF> boxRectFs) {
        if (!isFocused()) return;
        if (mDrawCursor) {
            int passwordLength = mMeasureInfo.passwordLength;

            mCursorPaint.setColor(mCursorColor);
            mCursorPaint.setStyle(Paint.Style.FILL);
            mCursorPaint.setStrokeWidth(mCursorWidth);

            Editable text = getText();
            int textLength = text == null ? 0 : text.length();
            if (textLength >= passwordLength) return;

            RectF rectF = boxRectFs.get(textLength);


            float startX = rectF.centerX() - mCursorWidth * 0.5f;
            float startY = rectF.top + mCursorMarginTop;
            float stopX = startX;
            float stopY = rectF.bottom - mCursorMarginBottom;

            canvas.drawLine(startX, startY, stopX, stopY, mCursorPaint);
        }
    }

    private void drawText(Canvas canvas, List<RectF> boxRectFs) {
        Editable text = getText();
        int textLength = text == null ? 0 : text.length();
        if (textLength == 0) return;

        String str = text.toString();

        textLength = Math.min(mMeasureInfo.passwordLength, textLength);
        float textSize = getTextSize();
        int textColor = getTextColors().getDefaultColor();

        if (mPasswordType == PASSWORD_TYPE_STARS) textSize *= 1.5f;

        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();

        for (int index = 0; index < textLength; index++) {
            RectF rectF = boxRectFs.get(index);
            switch (mPasswordType) {
                case PASSWORD_TYPE_STARS: {
                    float fontWidth = mTextPaint.measureText("*");
                    float baseX = rectF.centerX();
                    float baseY = (rectF.bottom + rectF.top - fontMetrics.bottom - fontMetrics.top) * 0.5f;
                    baseY += fontWidth * 0.26f;
                    canvas.drawText("*", baseX, baseY, mTextPaint);
                    break;
                }
                case PASSWORD_TYPE_CIRCLE: {
                    float cx = rectF.centerX();
                    float cy = rectF.centerY();
                    float radius = textSize * 0.5f;
                    canvas.drawCircle(cx, cy, radius, mTextPaint);
                    break;
                }
                case PASSWORD_TYPE_TEXT: {
                    char charAt = str.charAt(index);
                    String strAt = String.valueOf(charAt);
                    float baseX = rectF.centerX();
                    float baseY = (rectF.bottom + rectF.top - fontMetrics.bottom - fontMetrics.top) * 0.5f;
                    canvas.drawText(strAt, baseX, baseY, mTextPaint);
                    break;
                }
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        PasswordListener passwordListener = mPasswordListener;
        if (passwordListener != null) {
            int length = s == null ? 0 : s.length();
            String str = s == null ? null : s.toString();
            passwordListener.onChanged(str);
            if (length == 0) {
                passwordListener.onCleared();
            } else if (length == mMeasureInfo.passwordLength) {
                passwordListener.onFinished(str);
            }
        }
    }

    private class DrawCursorToggleTask implements Runnable {

        @Override
        public void run() {
            if (!mCursorEnabled) return;
            mDrawCursor = !mDrawCursor;
            postInvalidate();
            Handler handler = getHandler();
            if (handler != null) {
                handler.postDelayed(this, 500);
            }
        }
    }

    public interface PasswordListener {

        void onCleared();

        void onChanged(String password);

        void onFinished(String password);

    }

    public interface IMeasurer {

        void measure(PasswordView passwordView,
                     int widthMeasureSpec, int heightMeasureSpec,
                     MeasureInfo measureInfo,
                     MeasureResult result);

    }

    public static class MeasureInfo implements Cloneable {

        public int passwordLength = 6;

        public int boxType;
        public int boxChainStyle;

        public float boxWidth, boxHeight;
        public float boxWidthPercent;
        public float boxHeightRatio = 1.0f;

        public float boxBorderSize;
        public float boxSpace;
        public float boxSpacePercent;

        public boolean mergeRectBoxEnabled;
        public float mergedRectBoxDividerWidth;

        @Override
        public MeasureInfo clone() {
            try {
                return (MeasureInfo) super.clone();
            } catch (Exception e) {
                e.printStackTrace();
            }
            MeasureInfo measureInfo = new MeasureInfo();
            measureInfo.passwordLength = this.passwordLength;
            measureInfo.boxType = this.boxType;
            measureInfo.boxChainStyle = this.boxChainStyle;
            measureInfo.boxWidth = this.boxWidth;
            measureInfo.boxHeight = this.boxHeight;
            measureInfo.boxWidthPercent = this.boxWidthPercent;
            measureInfo.boxHeightRatio = this.boxHeightRatio;
            measureInfo.boxBorderSize = this.boxBorderSize;
            measureInfo.boxSpace = this.boxSpace;
            measureInfo.boxSpacePercent = this.boxSpacePercent;
            measureInfo.mergeRectBoxEnabled = this.mergeRectBoxEnabled;
            measureInfo.mergedRectBoxDividerWidth = this.mergedRectBoxDividerWidth;
            return measureInfo;
        }
    }

    public static class MeasureResult {

        public float measureWidth, measureHeight;
        public float measureContentWidth, measureContentHeight;

        public float measureBoxWidth, measureBoxHeight;
        public float measureBoxSpace;
        public float measureBoxChainMargin;

        public boolean mergedRectBox;

        public void clear() {
            measureWidth = measureBoxHeight = 0.0f;
            measureContentWidth = measureContentHeight = 0.0f;
            measureBoxWidth = measureBoxHeight = 0.0f;
            measureBoxSpace = measureBoxChainMargin = 0.0f;
            mergedRectBox = false;
        }

        public void copy(MeasureResult result) {
            this.measureWidth = result.measureWidth;
            this.measureHeight = result.measureHeight;
            this.measureContentWidth = result.measureContentWidth;
            this.measureContentHeight = result.measureContentHeight;
            this.measureBoxWidth = result.measureBoxWidth;
            this.measureBoxHeight = result.measureBoxHeight;
            this.measureBoxSpace = result.measureBoxSpace;
            this.measureBoxChainMargin = result.measureBoxChainMargin;
            this.mergedRectBox = result.mergedRectBox;
        }
    }

    public static class FillMeasurer implements IMeasurer {

        @Override
        public void measure(PasswordView passwordView,
                            int widthMeasureSpec, int heightMeasureSpec,
                            MeasureInfo measureInfo,
                            MeasureResult result) {
            measureWidth(passwordView, widthMeasureSpec, measureInfo, result);
            measureHeight(passwordView, heightMeasureSpec, measureInfo, result);
        }

        private void measureWidth(PasswordView view,
                                  int widthMeasureSpec,
                                  MeasureInfo info,
                                  MeasureResult result) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();

            if (widthMode == MeasureSpec.UNSPECIFIED) {
                float width = ChainHelper.computeChainWidth(info, horizontalPadding);
                result.measureWidth = width;

                float boxWidth = info.boxWidth;
                if (info.boxWidthPercent > 0) {
                    boxWidth = width * info.boxWidthPercent;
                }
                result.measureBoxWidth = boxWidth;

                ChainHelper.measureBoxChainExactly(width, boxWidth, horizontalPadding, info, result);
            } else {
                float width = getDefaultSize(view.getSuggestedMinimumWidth(), widthMeasureSpec);
                result.measureWidth = width;
                float boxWidth = info.boxWidth;
                if (info.boxWidthPercent > 0) {
                    boxWidth = width * info.boxWidthPercent;
                }
                result.measureBoxWidth = boxWidth;

                ChainHelper.measureBoxChainExactly(width, boxWidth, horizontalPadding, info, result);
            }
        }

        private void measureHeight(PasswordView view, int heightMeasureSpec, MeasureInfo info, MeasureResult result) {
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
            switch (heightMode) {
                case MeasureSpec.UNSPECIFIED: {
                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    if (boxHeight == 0) {
                        float height = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);
                        boxHeight = height
                                - verticalPadding
                                - 2 * info.boxBorderSize;
                        boxHeight = Math.max(0, boxHeight);
                    }
                    result.measureBoxHeight = boxHeight;
                    result.measureHeight = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    break;
                }
                case MeasureSpec.EXACTLY: {
                    float height = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);
                    result.measureHeight = height;
                    float maxBoxHeight = height
                            - verticalPadding
                            - 2 * info.boxBorderSize;
                    maxBoxHeight = Math.max(0, maxBoxHeight);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    if (boxHeight == 0) {
                        boxHeight = maxBoxHeight;
                    } else {
                        boxHeight = Math.min(maxBoxHeight, boxHeight);
                    }
                    result.measureBoxHeight = boxHeight;
                    break;
                }
                case MeasureSpec.AT_MOST: {
                    float maxHeight = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);
                    float maxBoxHeight = maxHeight
                            - verticalPadding
                            - 2 * info.boxBorderSize;
                    maxBoxHeight = Math.max(0, maxBoxHeight);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    if (boxHeight == 0) {
                        boxHeight = maxBoxHeight;
                    } else {
                        boxHeight = Math.min(maxBoxHeight, boxHeight);
                    }
                    result.measureBoxHeight = boxHeight;
                    result.measureHeight = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    break;
                }
            }
        }
    }

    public static class BoundMeasurer implements IMeasurer {

        @Override
        public void measure(PasswordView passwordView, int widthMeasureSpec, int heightMeasureSpec, MeasureInfo measureInfo, MeasureResult result) {
            measureWidth(passwordView, widthMeasureSpec, measureInfo, result);
            measureHeight(passwordView, heightMeasureSpec, measureInfo, result);
        }

        private void measureWidth(PasswordView view, int widthMeasureSpec, MeasureInfo info, MeasureResult result) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();

            switch (widthMode) {
                case MeasureSpec.UNSPECIFIED: {
                    float width = ChainHelper.computeChainWidth(info, horizontalPadding);
                    result.measureWidth = width;

                    float boxWidth = info.boxWidth;
                    if (info.boxWidthPercent > 0) {
                        boxWidth = width * info.boxWidthPercent;
                    }
                    result.measureBoxWidth = boxWidth;

                    ChainHelper.measureBoxChainExactly(width, boxWidth, horizontalPadding, info, result);
                    break;
                }
                case MeasureSpec.EXACTLY: {
                    float width = getDefaultSize(view.getSuggestedMinimumWidth(), widthMeasureSpec);
                    result.measureWidth = width;
                    float expectedBoxWidth = info.boxWidth;
                    if (info.boxWidthPercent > 0) {
                        expectedBoxWidth = width * info.boxWidthPercent;
                    }

                    ChainHelper.measureBoxChainExactly(width, expectedBoxWidth, horizontalPadding, info, result);

                    boolean mergeRectBox = result.mergedRectBox;

                    float maxBoxWidth = (width
                            - info.boxBorderSize * 2 * info.passwordLength
                            - (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                            - result.measureBoxSpace * (info.passwordLength - 1)
                            - horizontalPadding
                            - result.measureBoxChainMargin * 2)
                            / info.passwordLength;

                    result.measureBoxWidth = Math.min(expectedBoxWidth, maxBoxWidth);

                    break;
                }
                case MeasureSpec.AT_MOST: {
                    float maxWidth = getDefaultSize(view.getSuggestedMinimumWidth(), widthMeasureSpec);
                    if (info.boxChainStyle == BOX_CHAIN_STYLE_FREE) {
                        float width = ChainHelper.computeFreeChainWidth(info, horizontalPadding);
                        width = Math.min(maxWidth, width);

                        result.measureWidth = width;

                        float boxSpace = info.boxSpace;
                        if (info.boxSpacePercent > 0) {
                            boxSpace = width * info.boxSpacePercent;
                        }

                        boolean mergeRectBox = info.mergeRectBoxEnabled
                                && info.boxType == BOX_TYPE_RECT
                                && result.measureBoxSpace == 0;

                        float maxBoxWidth = (maxWidth
                                - info.boxBorderSize * 2 * info.passwordLength
                                - (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                                - result.measureBoxSpace * (info.passwordLength - 1)
                                - horizontalPadding
                                - result.measureBoxChainMargin * 2)
                                / info.passwordLength;
                        maxBoxWidth = Math.max(0, maxBoxWidth);

                        float boxWidth = info.boxWidth;
                        if (info.boxWidthPercent > 0) {
                            boxWidth = width * info.boxWidthPercent;
                        }
                        boxWidth = Math.min(maxBoxWidth, boxWidth);

                        result.measureBoxSpace = boxSpace;
                        result.measureBoxChainMargin = 0;
                        result.mergedRectBox = mergeRectBox;
                        result.measureBoxWidth = boxWidth;
                    } else {
                        result.measureWidth = maxWidth;

                        float expectedBoxWidth = info.boxWidth;
                        if (info.boxWidthPercent > 0) {
                            expectedBoxWidth = result.measureWidth * info.boxWidthPercent;
                        }

                        ChainHelper.measureBoxChainExactly(result.measureWidth, expectedBoxWidth, horizontalPadding, info, result);
                        boolean mergeRectBox = result.mergedRectBox;

                        float maxBoxWidth = (result.measureWidth
                                - info.boxBorderSize * 2 * info.passwordLength
                                - (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                                - result.measureBoxSpace * (info.passwordLength - 1)
                                - horizontalPadding
                                - result.measureBoxChainMargin * 2)
                                / info.passwordLength;

                        result.measureBoxWidth = Math.min(expectedBoxWidth, maxBoxWidth);
                    }
                    break;
                }
            }
        }

        private void measureHeight(PasswordView view, int heightMeasureSpec, MeasureInfo info, MeasureResult result) {
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
            switch (heightMode) {
                case MeasureSpec.UNSPECIFIED: {
                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    result.measureBoxHeight = boxHeight;
                    result.measureHeight = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    break;
                }
                case MeasureSpec.EXACTLY: {
                    float height = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);
                    result.measureHeight = height;
                    float maxBoxHeight = height
                            - verticalPadding
                            - 2 * info.boxBorderSize;
                    maxBoxHeight = Math.max(0, maxBoxHeight);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    if (boxHeight == 0) {
                        boxHeight = maxBoxHeight;
                    } else {
                        boxHeight = Math.min(maxBoxHeight, boxHeight);
                    }
                    result.measureBoxHeight = boxHeight;
                    break;
                }
                case MeasureSpec.AT_MOST: {
                    float maxHeight = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);
                    float maxBoxHeight = maxHeight
                            - verticalPadding
                            - 2 * info.boxBorderSize;
                    maxBoxHeight = Math.max(0, maxBoxHeight);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    if (boxHeight == 0) {
                        boxHeight = maxBoxHeight;
                    } else {
                        boxHeight = Math.min(maxBoxHeight, boxHeight);
                    }
                    result.measureBoxHeight = boxHeight;
                    result.measureHeight = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    break;
                }
            }
        }
    }

    public static class FreeMeasurer implements IMeasurer {

        @Override
        public void measure(PasswordView passwordView, int widthMeasureSpec, int heightMeasureSpec, MeasureInfo measureInfo, MeasureResult result) {
            measureWidth(passwordView, widthMeasureSpec, measureInfo, result);
            measureHeight(passwordView, heightMeasureSpec, measureInfo, result);
        }

        private void measureWidth(PasswordView view, int widthMeasureSpec, MeasureInfo info, MeasureResult result) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();

            switch (widthMode) {
                case MeasureSpec.UNSPECIFIED: {
                    float width = ChainHelper.computeChainWidth(info, horizontalPadding);
                    result.measureWidth = width;

                    float boxWidth = info.boxWidth;
                    if (info.boxWidthPercent > 0) {
                        boxWidth = width * info.boxWidthPercent;
                    }
                    result.measureBoxWidth = boxWidth;

                    ChainHelper.measureBoxChainExactly(result.measureWidth, boxWidth, horizontalPadding, info, result);
                    break;
                }
                case MeasureSpec.EXACTLY: {
                    float width = getDefaultSize(view.getSuggestedMinimumWidth(), widthMeasureSpec);
                    result.measureWidth = width;
                    float boxWidth = info.boxWidth;
                    if (info.boxWidthPercent > 0) {
                        boxWidth = width * info.boxWidthPercent;
                    }
                    result.measureBoxWidth = boxWidth;

                    ChainHelper.measureBoxChainExactly(result.measureWidth, boxWidth, horizontalPadding, info, result);
                    break;
                }
                case MeasureSpec.AT_MOST: {
                    if (info.boxChainStyle == BOX_CHAIN_STYLE_FREE) {
                        float width = ChainHelper.computeFreeChainWidth(info, horizontalPadding);

                        result.measureWidth = width;

                        float boxSpace = info.boxSpace;
                        if (info.boxSpacePercent > 0) {
                            boxSpace = width * info.boxSpacePercent;
                        }

                        boolean mergeRectBox = info.mergeRectBoxEnabled
                                && info.boxType == BOX_TYPE_RECT
                                && boxSpace == 0;

                        float boxWidth = info.boxWidth;
                        if (info.boxWidthPercent > 0) {
                            boxWidth = width * info.boxWidthPercent;
                        }

                        result.measureBoxSpace = boxSpace;
                        result.measureBoxChainMargin = 0;
                        result.mergedRectBox = mergeRectBox;
                        result.measureBoxWidth = boxWidth;
                    } else {
                        result.measureWidth = getDefaultSize(view.getSuggestedMinimumWidth(), widthMeasureSpec);

                        float boxWidth = info.boxWidth;
                        if (info.boxWidthPercent > 0) {
                            boxWidth = result.measureWidth * info.boxWidthPercent;
                        }
                        result.measureBoxWidth = boxWidth;

                        ChainHelper.measureBoxChainExactly(result.measureWidth, boxWidth, horizontalPadding, info, result);
                    }
                    break;
                }
            }
        }

        private void measureHeight(PasswordView view, int heightMeasureSpec, MeasureInfo info, MeasureResult result) {
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
            switch (heightMode) {
                case MeasureSpec.UNSPECIFIED: {
                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }
                    result.measureBoxHeight = boxHeight;
                    result.measureHeight = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    break;
                }
                case MeasureSpec.EXACTLY: {
                    result.measureHeight = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }

                    boxHeight = Math.max(0, boxHeight);
                    result.measureBoxHeight = boxHeight;
                    break;
                }
                case MeasureSpec.AT_MOST: {
                    float maxHeight = getDefaultSize(view.getSuggestedMinimumHeight(), heightMeasureSpec);

                    float boxHeight = info.boxHeight;
                    if (info.boxHeightRatio > 0) {
                        boxHeight = result.measureBoxWidth * info.boxHeightRatio;
                    }

                    result.measureBoxHeight = boxHeight;
                    float height = boxHeight + verticalPadding + 2 * info.boxBorderSize;
                    height = Math.min(maxHeight, height);
                    result.measureHeight = height;
                    break;
                }
            }
        }
    }

    private static class ChainHelper {

        static float computeChainWidth(MeasureInfo info, int horizontalPadding) {
            switch (info.boxChainStyle) {
                default:
                case BOX_CHAIN_STYLE_FREE: {
                    return computeFreeChainWidth(info, horizontalPadding);
                }
                case BOX_CHAIN_STYLE_SPREAD: {
                    return computeSpreadChainWidth(info, horizontalPadding);
                }
                case BOX_CHAIN_STYLE_SPREAD_INSIDE: {
                    return computeSpreadInsideChainWidth(info, horizontalPadding);
                }
                case BOX_CHAIN_STYLE_PACKET: {
                    return computePacketChainWidth(info, horizontalPadding);
                }
            }
        }

        static float computeFreeChainWidth(MeasureInfo info, int horizontalPadding) {
            float boxWidth = info.boxWidth;
            float boxWidthPercent = info.boxWidthPercent;

            float boxSpace = info.boxSpace;
            float boxSpacePercent = info.boxSpacePercent;
            boolean mergeRectBox = info.mergeRectBoxEnabled
                    && info.boxType == BOX_TYPE_RECT
                    && boxSpace == 0
                    && boxSpacePercent == 0;

            return (horizontalPadding
                    + (boxWidthPercent == 0 ? boxWidth : 0) * info.passwordLength
                    + info.boxBorderSize * info.passwordLength * 2
                    + (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                    + (boxSpacePercent == 0 ? boxSpace : 0) * (info.passwordLength - 1))
                    / (1 - boxWidthPercent * info.passwordLength - boxSpacePercent * (info.passwordLength - 1));
        }

        static float computeSpreadChainWidth(MeasureInfo info, int horizontalPadding) {
            float boxWidth = info.boxWidth;
            float boxWidthPercent = info.boxWidthPercent;

            float boxSpace = info.boxSpace;
            float boxSpacePercent = info.boxSpacePercent;
            boolean mergeRectBox = info.mergeRectBoxEnabled
                    && info.boxType == BOX_TYPE_RECT
                    && boxSpace == 0
                    && boxSpacePercent == 0;

            return (horizontalPadding
                    + (boxWidthPercent == 0 ? boxWidth : 0) * info.passwordLength
                    + info.boxBorderSize * info.passwordLength * 2
                    + (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                    + (boxSpacePercent == 0 ? boxSpace : 0) * (info.passwordLength + 1))
                    / (1 - boxWidthPercent * info.passwordLength - boxSpacePercent * (info.passwordLength + 1));
        }

        static float computeSpreadInsideChainWidth(MeasureInfo info, int horizontalPadding) {
            float boxWidth = info.boxWidth;
            float boxWidthPercent = info.boxWidthPercent;

            float boxSpace = info.boxSpace;
            float boxSpacePercent = info.boxSpacePercent;
            boolean mergeRectBox = info.mergeRectBoxEnabled
                    && info.boxType == BOX_TYPE_RECT
                    && boxSpace == 0
                    && boxSpacePercent == 0;


            return (horizontalPadding
                    + (boxWidthPercent == 0 ? boxWidth : 0) * info.passwordLength
                    + info.boxBorderSize * info.passwordLength * 2
                    + (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                    + (boxSpacePercent == 0 ? boxSpace : 0) * (info.passwordLength - 1))
                    / (1 - boxWidthPercent * info.passwordLength - boxSpacePercent * (info.passwordLength - 1));
        }

        static float computePacketChainWidth(MeasureInfo info, int horizontalPadding) {
            float boxWidth = info.boxWidth;
            float boxWidthPercent = info.boxWidthPercent;

            float boxSpace = info.boxSpace;
            float boxSpacePercent = info.boxSpacePercent;
            boolean mergeRectBox = info.mergeRectBoxEnabled && info.boxType == BOX_TYPE_RECT;

            return (horizontalPadding
                    + (boxWidthPercent == 0 ? boxWidth : 0) * info.passwordLength + info.boxBorderSize * info.passwordLength * 2
                    + (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0)
                    + (boxSpacePercent == 0 ? boxSpace : 0) * 2)
                    / (1 - boxWidthPercent * info.passwordLength - boxSpacePercent * 2);
        }

        /*static void measureBoxChainExactly(float width, MeasureInfo info, MeasureResult result) {
            switch (info.boxChainStyle) {
                case BOX_CHAIN_STYLE_FREE:
                case BOX_CHAIN_STYLE_SPREAD_INSIDE: {
                    float boxSpace = info.boxSpace;
                    if (info.boxSpacePercent > 0) {
                        boxSpace = width * info.boxSpacePercent;
                    }
                    result.measureBoxSpace = boxSpace;
                    result.measureBoxChainMargin = 0;
                    break;
                }
                case BOX_CHAIN_STYLE_SPREAD: {
                    float boxSpace = info.boxSpace;
                    if (info.boxSpacePercent > 0) {
                        boxSpace = width * info.boxSpacePercent;
                    }
                    result.measureBoxSpace = boxSpace;
                    result.measureBoxChainMargin = boxSpace;
                    break;
                }
                case BOX_CHAIN_STYLE_PACKET: {
                    float boxSpace = info.boxSpace;
                    if (info.boxSpacePercent > 0) {
                        boxSpace = width * info.boxSpacePercent;
                    }
                    result.measureBoxSpace = 0;
                    result.measureBoxChainMargin = boxSpace;
                    break;
                }
            }
            result.mergedRectBox = info.mergeRectBoxEnabled
                    && info.boxType == BOX_TYPE_RECT
                    && result.measureBoxSpace == 0;
        }*/

        static void measureBoxChainExactly(float width, float expectedBoxWidth, int horizontalPadding, MeasureInfo info, MeasureResult result) {
            switch (info.boxChainStyle) {
                case BOX_CHAIN_STYLE_FREE: {
                    float boxSpace = info.boxSpace;
                    if (info.boxSpacePercent > 0) {
                        boxSpace = width * info.boxSpacePercent;
                    }
                    result.measureBoxSpace = boxSpace;
                    result.measureBoxChainMargin = 0;
                    result.mergedRectBox = info.mergeRectBoxEnabled
                            && info.boxType == BOX_TYPE_RECT
                            && result.measureBoxSpace == 0;
                    break;
                }
                case BOX_CHAIN_STYLE_SPREAD: {
                    float surplusWidth = width
                            - horizontalPadding
                            - expectedBoxWidth * info.passwordLength
                            - info.boxBorderSize * 2 * info.passwordLength;
                    if (surplusWidth <= 0) {
                        result.measureBoxSpace = result.measureBoxChainMargin = 0;
                        if (info.passwordLength > 1) {
                            boolean mergeRectBox = info.mergeRectBoxEnabled && info.boxType == BOX_TYPE_RECT;
                            if (mergeRectBox) {
                                surplusWidth += (2 * info.boxBorderSize - info.mergedRectBoxDividerWidth) * (info.passwordLength - 1);
                                if (surplusWidth > 0) {
                                    result.measureBoxChainMargin = surplusWidth / 2;
                                }
                            }
                        }
                    } else {
                        if (info.passwordLength <= 1) {
                            result.measureBoxSpace = 0;
                            result.measureBoxChainMargin = surplusWidth / 2;
                        } else {
                            result.measureBoxSpace = result.measureBoxChainMargin = surplusWidth / (info.passwordLength + 1);
                        }
                    }
                    result.mergedRectBox = info.mergeRectBoxEnabled
                            && info.boxType == BOX_TYPE_RECT
                            && result.measureBoxSpace == 0;
                    break;
                }
                case BOX_CHAIN_STYLE_SPREAD_INSIDE: {
                    if (info.passwordLength <= 1) {
                        result.measureBoxSpace = result.measureBoxChainMargin = 0;
                        result.mergedRectBox = info.mergeRectBoxEnabled && info.boxType == BOX_TYPE_RECT;
                    } else {
                        float surplusWidth = width
                                - horizontalPadding
                                - expectedBoxWidth * info.passwordLength
                                - info.boxBorderSize * 2 * info.passwordLength;
                        if (surplusWidth <= 0) {
                            result.measureBoxSpace = result.measureBoxChainMargin = 0;
                            boolean mergeRectBox = info.mergeRectBoxEnabled && info.boxType == BOX_TYPE_RECT;
                            if (mergeRectBox) {
                                surplusWidth += (2 * info.boxBorderSize - info.mergedRectBoxDividerWidth) * (info.passwordLength - 1);
                                if (surplusWidth > 0) {
                                    mergeRectBox = false;
                                }
                            }
                            result.mergedRectBox = mergeRectBox;
                        } else {
                            result.measureBoxSpace = surplusWidth / (info.passwordLength - 1);
                            result.measureBoxChainMargin = 0;
                            result.mergedRectBox = info.mergeRectBoxEnabled
                                    && info.boxType == BOX_TYPE_RECT
                                    && result.measureBoxSpace == 0;
                        }
                    }
                    break;
                }
                case BOX_CHAIN_STYLE_PACKET: {
                    boolean mergeRectBox = info.mergeRectBoxEnabled && info.boxType == BOX_TYPE_RECT;
                    result.mergedRectBox = mergeRectBox;
                    float surplusWidth = width
                            - horizontalPadding
                            - expectedBoxWidth * info.passwordLength
                            - info.boxBorderSize * 2 * info.passwordLength
                            - (mergeRectBox ? (info.mergedRectBoxDividerWidth - 2 * info.boxBorderSize) * (info.passwordLength - 1) : 0);
                    if (surplusWidth <= 0) {
                        result.measureBoxSpace = result.measureBoxChainMargin = 0;
                    } else {
                        result.measureBoxSpace = 0;
                        result.measureBoxChainMargin = surplusWidth / 2;
                    }
                }
            }
        }
    }
}