package com.kunzisoft.androidclearchroma;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.kunzisoft.androidclearchroma.colormode.ColorMode;

/**
 * Compatibility DialogPreference class that handles preferences, selected color display, and dialog box for selecting this color.
 * @author JJamet - Pavel Sikun
 */
public class ChromaPreferenceCompat extends DialogPreference {

    private static final String TAG = "ChromaPreferenceCompat";

    private AppCompatImageView backgroundPreview;
    private AppCompatImageView colorPreview;

    private static final int DEFAULT_COLOR = Color.WHITE;
    private static final ColorMode DEFAULT_COLOR_MODE = ColorMode.RGB;
    private static final IndicatorMode DEFAULT_INDICATOR_MODE = IndicatorMode.DECIMAL;
    private static final ShapePreviewPreference DEFAULT_SHAPE_PREVIEW = ShapePreviewPreference.CIRCLE;
    private static final String COLOR_TAG_SUMMARY = "[color]";

    private int color;
    private ColorMode colorMode;
    private IndicatorMode indicatorMode;
    private ShapePreviewPreference shapePreviewPreference;
    private CharSequence summaryPreference;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChromaPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public ChromaPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ChromaPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ChromaPreferenceCompat(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        setWidgetLayoutResource(R.layout.preference_layout);
        loadValuesFromXml(attrs);
        updatePreview();
    }

    private void loadValuesFromXml(AttributeSet attrs) {
        if(attrs == null) {
            color = DEFAULT_COLOR;
            colorMode = DEFAULT_COLOR_MODE;
            indicatorMode = DEFAULT_INDICATOR_MODE;
            shapePreviewPreference = DEFAULT_SHAPE_PREVIEW;
            summaryPreference = COLOR_TAG_SUMMARY;
        }
        else {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChromaPreference);
            try {
                color = a.getColor(R.styleable.ChromaPreference_chromaInitialColor, DEFAULT_COLOR);

                colorMode = ColorMode.values()[
                        a.getInt(R.styleable.ChromaPreference_chromaColorMode,
                                DEFAULT_COLOR_MODE.ordinal())];

                indicatorMode = IndicatorMode.values()[
                        a.getInt(R.styleable.ChromaPreference_chromaIndicatorMode,
                                DEFAULT_INDICATOR_MODE.ordinal())];

                shapePreviewPreference = ShapePreviewPreference.values()[
                        a.getInt(R.styleable.ChromaPreference_chromaShapePreview,
                                DEFAULT_SHAPE_PREVIEW.ordinal())];

                summaryPreference = getSummary();
            }
            finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            color = getPersistedInt(color);
        } else {
            color = (int) defaultValue;
            persistInt(color);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Color.parseColor(a.getString(index));
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        backgroundPreview = holder.itemView.findViewById(R.id.backgroundPreview);
        colorPreview = holder.itemView.findViewById(R.id.colorPreview);
        updatePreview();

        if(!isEnabled()) {
            colorPreview.getDrawable().mutate().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updatePreview();
    }

    private Bitmap getRoundedCroppedBitmap(Bitmap bitmap, int widthLight, int heightLight, float radius) {
        Bitmap output = Bitmap.createBitmap(widthLight, heightLight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paintColor = new Paint();
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF(new Rect(0, 0, widthLight, heightLight));

        canvas.drawRoundRect(rectF, radius, radius, paintColor);

        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, -(bitmap.getWidth() - widthLight)/2 , -(bitmap.getHeight() - heightLight)/2, paintImage);

        return output;
    }

    synchronized private void updatePreview() {
        try {
            if(colorPreview != null) {
                int shapeWidth = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.shape_preference_width);
                float radius = shapeWidth/2;

                // Update shape of color preview
                switch (shapePreviewPreference) {
                    default:
                    case CIRCLE:
                        colorPreview.setImageResource(R.drawable.circle);
                        break;
                    case SQUARE:
                        colorPreview.setImageResource(R.drawable.square);
                        radius = 0;
                        break;
                    case ROUNDED_SQUARE:
                        colorPreview.setImageResource(R.drawable.rounded_square);
                        radius = getContext().getResources().getDimension(R.dimen.shape_radius_preference);
                        break;
                }

                // Update color
                colorPreview.getDrawable()
                        .setColorFilter(new PorterDuffColorFilter(
                                color, PorterDuff.Mode.MULTIPLY));

                // Bitmap to crop for background
                Bitmap draughtboard = BitmapFactory.decodeResource(
                        getContext().getResources(), R.drawable.draughtboard);
                draughtboard = getRoundedCroppedBitmap(draughtboard, shapeWidth, shapeWidth, radius);
                backgroundPreview.setImageBitmap(draughtboard);

                colorPreview.invalidate();
                backgroundPreview.invalidate();
            }
            setSummary(summaryPreference);
        }
        catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Cannot update preview: " + e.toString());
        }
    }

    @Override
    protected void onClick() {
        getPreferenceManager().showDialog(this);
    }

    @Override
    protected boolean persistInt(int value) {
        color = value;
        updatePreview();
        return super.persistInt(value);
    }

    /**
     * {@inheritDoc}
     * If [color] is present in summary, it's replaced by string of color
     */
    @Override
    public void setSummary(CharSequence summary) {
        String summaryWithColor = null;
        if(summary != null) {
            summaryWithColor = summary.toString().replace(COLOR_TAG_SUMMARY,
                    ChromaUtil.getFormattedColorString(color, colorMode == ColorMode.ARGB));
        }
        super.setSummary(summaryWithColor);
    }

    /**
     * Get current color of preference
     * @return IntColor
     */
    public int getColor() {
        return color;
    }

    /**
     * Assign color of preference
     * @param color IntColor
     */
    public void setColor(@ColorInt int color) {
        persistInt(color);
        notifyChanged();
    }

    /**
     * Get current color mode
     * @return ColorMode
     */
    public ColorMode getColorMode() {
        return colorMode;
    }

    /**
     * Assign color mode
     * @param colorMode ColorMode
     */
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
        notifyChanged();
    }

    /**
     * Get current indicator mode
     * @return IndicatorMode
     */
    public IndicatorMode getIndicatorMode() {
        return indicatorMode;
    }

    /**
     * Assign indicator mode
     * @param indicatorMode IndicatorMode
     */
    public void setIndicatorMode(IndicatorMode indicatorMode) {
        this.indicatorMode = indicatorMode;
        notifyChanged();
    }

    /**
     * Get shape of preview
     * @return The shape for preview
     */
    public ShapePreviewPreference getShapePreviewPreference() {
        return shapePreviewPreference;
    }

    /**
     * Defined a shape container for preview of color
     * @param shapePreviewPreference Shape wanted for preview
     */
    public void setShapePreviewPreference(ShapePreviewPreference shapePreviewPreference) {
        this.shapePreviewPreference = shapePreviewPreference;
        notifyChanged();
    }
}
