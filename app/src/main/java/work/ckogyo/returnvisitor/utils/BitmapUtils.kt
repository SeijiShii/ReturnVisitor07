package work.ckogyo.returnvisitor.utils

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun getBitmap(vectorDrawable: VectorDrawable): Bitmap? {
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return bitmap
}

fun getBitmap(vectorDrawable: VectorDrawableCompat): Bitmap? {
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return bitmap
}

fun getBitmap(context: Context, @DrawableRes drawableResId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableResId)
    return if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else if (drawable is VectorDrawableCompat) {
        getBitmap(drawable)
    } else if (drawable is VectorDrawable) {
        getBitmap(drawable)
    } else {
        throw IllegalArgumentException("Unsupported drawable type")
    }
}