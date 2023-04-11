/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.muellerma.nfcreader.record

import android.app.Activity
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.muellerma.nfcreader.NdefMessageParser
import com.github.muellerma.nfcreader.R
import java.util.*

/**
 * A representation of an NFC Forum "Smart Poster".
 */
class SmartPoster private constructor(
    private val uri: UriRecord,
    private val title: TextRecord?
) : ParsedNdefRecord {

    override fun getView(
        activity: Activity,
        inflater: LayoutInflater,
        parent: ViewGroup,
        offset: Int
    ): View {
        return if (title != null) {
            // Build a container to hold the title and the URI
            val container = LinearLayout(activity)
            container.orientation = LinearLayout.VERTICAL
            container.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            container.addView(title.getView(activity, inflater, container, offset))
            inflater.inflate(R.layout.tag_divider, container)
            container.addView(uri.getView(activity, inflater, container, offset))
            container
        } else {
            // Just a URI, return a view for it directly
            uri.getView(activity, inflater, parent, offset)
        }
    }

    companion object {
        fun parse(record: NdefRecord): SmartPoster {
            require(record.tnf == NdefRecord.TNF_WELL_KNOWN)
            require(Arrays.equals(record.type, NdefRecord.RTD_SMART_POSTER))

            return try {
                val subRecords = NdefMessage(record.payload)
                parse(subRecords.records)
            } catch (e: FormatException) {
                throw IllegalArgumentException(e)
            }
        }

        private fun parse(recordsRaw: Array<NdefRecord>): SmartPoster {
            return try {
                val records: Iterable<ParsedNdefRecord> = NdefMessageParser.getRecords(recordsRaw)
                val uri = records.filterIsInstance<UriRecord>().firstOrNull() as UriRecord
                val title = records.filterIsInstance<TextRecord>().firstOrNull()
                SmartPoster(uri, title)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(e)
            }
        }

        fun isPoster(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}