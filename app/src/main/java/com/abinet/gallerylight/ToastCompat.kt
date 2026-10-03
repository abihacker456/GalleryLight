package com.abinet.gallerylight

import android.content.Context
import android.widget.Toast

fun ToastCompat(context: Context, resId: Int) =
    Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()

fun ToastCompat(context: Context, text: String) =
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()