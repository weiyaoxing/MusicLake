package com.cyl.musiclake.view.lyric;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import com.cyl.musiclake.utils.LogUtil;

public class LyricTextView extends View {

    private int mDefaultColor = Color.parseColor("#FFFFFF");  // 默认字体颜色
    private int mHighLightColor = Color.parseColor("#ffb701");  // 当前播放位置的颜色

    private int mLineCount = 0;  // 行数
    private float mLineHeight;  // 行高

    private float mShaderWidth = 0;  // 渐变过渡的距离
    private int mCurrentPlayLine = 0;  // 当前播放位置对应的行数

    /***/
    private int mDefaultMargin = 12;
    private int mDefaultSize = 35; //默认歌词大小
    private float fontSizeScale = 16;    // 设置字体大小
    private int fontColorScale = Color.RED;    // 设置字体颜色
    private Rect mBtnBound, mTimerBound;

    private LyricInfo mLyricInfo;
    private String mDefaultHint = "音乐湖";
    private Paint mTextPaint, mHighLightPaint;//默认画笔、已读歌词画笔

    /**
     * 是否有歌词
     */
    private boolean blLrc = false;

    /**
     * 当前歌词的第几个字
     */
    private int lyricsWordIndex = -1;

    /**
     * 当前歌词第几个字 已经播放的时间
     */
    private int lyricsWordHLEDTime = 0;

    /**
     * 当前歌词第几个字 已经播放的长度
     */
    private float lineLyricsHLWidth = 0;

    private Context context;

    private String content;
    private long mStartMilis, mCurrentMilis, mEndMilis;


    public LyricTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public LyricTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LyricTextView(Context context) {
        super(context, null);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        mTextPaint = new Paint();
        mTextPaint.setDither(true);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTextPaint.setColor(Color.WHITE);
        // 设定阴影(柔边, X 轴位移, Y 轴位移, 阴影颜色)
        mTextPaint.setShadowLayer(5, 3, 3, 0xb5000000);

        mHighLightPaint = new Paint();
        mHighLightPaint.setDither(true);
        mHighLightPaint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {

        mTextPaint.setTextSize(fontSizeScale);
        mHighLightPaint.setTextSize(fontSizeScale);
        mHighLightPaint.setColor(fontColorScale);

        if (!blLrc) {

            float tipTextWidth = mTextPaint.measureText(mDefaultHint);
            Paint.FontMetrics fm = mHighLightPaint.getFontMetrics();
            int height = (int) Math.ceil(fm.descent - fm.top) + 2;

            canvas.drawText(mDefaultHint, (getWidth() - tipTextWidth) / 2,
                    (getHeight() + height) / 2, mTextPaint);

            canvas.clipRect((getWidth() - tipTextWidth) / 2,
                    (getHeight() + height) / 2 - height,
                    (float) ((getWidth() - tipTextWidth) / 2 + tipTextWidth / 2 + 5),
                    (getHeight() + height) / 2 + height);

            canvas.drawText(mDefaultHint, (getWidth() - tipTextWidth) / 2,
                    (getHeight() + height) / 2, mHighLightPaint);

        } else {
            if (mLyricInfo != null && mLyricInfo.song_lines != null && mLyricInfo.song_lines.size() > 0) {
                mStartMilis = mLyricInfo.song_lines.get(mCurrentPlayLine).start;
                if (mCurrentPlayLine == mCurrentPlayLine - 1) {
                    mEndMilis = mLyricInfo.duration;
                } else {
                    mEndMilis = mLyricInfo.song_lines.get(mCurrentPlayLine + 1).start;
                }
                content = mLyricInfo.song_lines.get(mCurrentPlayLine).content;
                float tipTextWidth = mTextPaint.measureText(content);
                Paint.FontMetrics fm = mHighLightPaint.getFontMetrics();
                int height = (int) Math.ceil(fm.descent - fm.top) + 2;
                mShaderWidth = (float) (1.0 * (mCurrentMilis - mStartMilis) / (mEndMilis - mStartMilis)) * tipTextWidth;
                LogUtil.e("tmp = " + mShaderWidth);

                canvas.drawText(content, (getWidth() - tipTextWidth) / 2,
                        (getHeight() + height) / 2, mTextPaint);

                canvas.clipRect((getWidth() - tipTextWidth) / 2,
                        (getHeight() + height) / 2 - height,
                        (getWidth() - tipTextWidth) / 2 + mShaderWidth,
                        (getHeight() + height) / 2 + height);
                canvas.drawText(content, (getWidth() - tipTextWidth) / 2,
                        (getHeight() + height) / 2, mHighLightPaint);
            }
        }
        super.draw(canvas);
    }

    public boolean getBlLrc() {
        return blLrc;
    }

    public void setBlLrc(boolean blLrc) {
        this.blLrc = blLrc;
    }

    public void setContent(String content) {
        this.content = content;
        setBlLrc(true);
        invalidateView();
    }

    public void setFontColorScale(int fontColorScale) {
        this.fontColorScale = fontColorScale;
        invalidateView();
    }

    public void setFontSizeScale(float fontSizeScale) {
        this.fontSizeScale = fontSizeScale;
        invalidateView();
    }

    /**
     * 设置歌词文件
     *
     * @param lyricInfo 歌词文件
     */
    public void setLyricInfo(LyricInfo lyricInfo) {
        mLyricInfo = lyricInfo;
        if (mLyricInfo != null) {
            blLrc = true;
            mLineCount = mLyricInfo.song_lines.size();
        } else {
            blLrc = false;
            mDefaultHint = "音乐湖，暂无歌词";
        }
        invalidateView();
    }

    /**
     * 刷新View
     */
    private void invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            //  当前线程是主UI线程，直接刷新。
            invalidate();
        } else {
            //  当前线程是非UI线程，post刷新。
            postInvalidate();
        }
    }

    /**
     * 根据当前给定的时间戳滑动到指定位置
     *
     * @param time 时间戳
     */
    private void scrollToCurrentTimeMillis(long time) {
        int position = 0;
        for (int i = 0, size = mLineCount; i < size; i++) {
            LyricInfo.LineInfo lineInfo = mLyricInfo.song_lines.get(i);
            if (lineInfo != null && lineInfo.start > time) {
                position = i;
                break;
            }
            if (i == mLineCount - 1) {
                position = mLineCount;
            }
        }
        if (position > 0)
            mCurrentPlayLine = position - 1;
    }

    /**
     * 设置当前时间显示位置
     *
     * @param current 时间戳
     */
    public void setCurrentTimeMillis(long current) {
        if (mLyricInfo == null) return;
        mCurrentMilis = current;
        scrollToCurrentTimeMillis(current);
        invalidateView();
    }

}