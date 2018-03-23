package com.example.android.classicalmusicquiz

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.JsonReader
import android.widget.Toast
import com.google.android.exoplayer2.upstream.DataSourceInputStream
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Java Object representing a single sample. Also includes utility methods for obtaining samples
 * from assets.
 */
internal class Sample private constructor(var sampleID: Int, var composer: String?,
        // Getters and Setters

                                          var title: String?, var uri: String?, var albumArtID: String?) {
    companion object {

        /**
         * Gets portrait of the composer for a sample by the sample ID.
         * @param context The application context.
         * @param sampleID The sample ID.
         * @return The portrait Bitmap.
         */
        fun getComposerArtBySampleID(context: Context, sampleID: Int): Bitmap {
            val sample = Sample.getSampleByID(context, sampleID)
            val albumArtID = context.resources.getIdentifier(
                    sample?.albumArtID, "drawable",
                    context.packageName)
            return BitmapFactory.decodeResource(context.resources, albumArtID)
        }

        /**
         * Gets a single sample by its ID.
         * @param context The application context.
         * @param sampleID The sample ID.
         * @return The sample object.
         */
        fun getSampleByID(context: Context, sampleID: Int): Sample? {
            val reader: JsonReader
            try {
                reader = readJSONFile(context)
                reader.beginArray()
                while (reader.hasNext()) {
                    val currentSample = readEntry(reader)
                    if (currentSample.sampleID == sampleID) {
                        reader.close()
                        return currentSample
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        /**
         * Gets and ArrayList of the IDs for all of the Samples from the JSON file.
         * @param context The application context.
         * @return The ArrayList of all sample IDs.
         */
        fun getAllSampleIDs(context: Context): ArrayList<Int> {
            val reader: JsonReader
            val sampleIDs = ArrayList<Int>()
            try {
                reader = readJSONFile(context)
                reader.beginArray()
                while (reader.hasNext()) {
                    sampleIDs.add(readEntry(reader).sampleID)
                }
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return sampleIDs
        }

        /**
         * Method used for obtaining a single sample from the JSON file.
         * @param reader The JSON reader object pointing a single sample JSON object.
         * @return The Sample the JsonReader is pointing to.
         */
        private fun readEntry(reader: JsonReader): Sample {
            var id: Int? = -1
            var composer: String? = null
            var title: String? = null
            var uri: String? = null
            var albumArtID: String? = null

            try {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    when (name) {
                        "name" -> title = reader.nextString()
                        "id" -> id = reader.nextInt()
                        "composer" -> composer = reader.nextString()
                        "uri" -> uri = reader.nextString()
                        "albumArtID" -> albumArtID = reader.nextString()
                        else -> {
                        }
                    }
                }
                reader.endObject()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return Sample(id!!, composer, title, uri, albumArtID)
        }

        /**
         * Method for creating a JsonReader object that points to the JSON array of samples.
         * @param context The application context.
         * @return The JsonReader object pointing to the JSON array of samples.
         * @throws IOException Exception thrown if the sample file can't be found.
         */
        @Throws(IOException::class)
        private fun readJSONFile(context: Context): JsonReader {
            val assetManager = context.assets
            var uri: String? = null

            try {
                for (asset in assetManager.list("")) {
                    if (asset.endsWith(".exolist.json")) {
                        uri = "asset:///$asset"
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(context, R.string.sample_list_load_error, Toast.LENGTH_LONG)
                        .show()
            }

            val userAgent = Util.getUserAgent(context, "ClassicalMusicQuiz")
            val dataSource = DefaultDataSource(context, null, userAgent, false)
            val dataSpec = DataSpec(Uri.parse(uri))
            val inputStream = DataSourceInputStream(dataSource, dataSpec)

            var reader =
                try {
                    JsonReader(InputStreamReader(inputStream, "UTF-8"))
                } finally {
                    Util.closeQuietly(dataSource)
                }

            return reader
        }
    }
}
