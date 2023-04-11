package com.github.muellerma.nfcreader.record

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

interface ParsedNdefRecord {
    /**
     * Returns a view to display this record.
     */
    fun getView(
        activity: Activity,
        inflater: LayoutInflater,
        parent: ViewGroup,
        offset: Int
    ): View
}