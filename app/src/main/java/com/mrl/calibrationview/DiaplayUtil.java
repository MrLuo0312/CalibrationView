package com.mrl.calibrationview;

import android.content.Context;
import android.util.TypedValue;

/**
 * @author : MrL
 * @date : 2018/6/30  10:19
 * @remark :
 **/
public class DiaplayUtil {
    public static float dip2px(float dip, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip
                , context.getResources().getDisplayMetrics());
    }
}
