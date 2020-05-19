package com.programmersbox.mangaworld.utils

import android.content.Context
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)