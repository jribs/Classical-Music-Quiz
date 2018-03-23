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

package com.example.android.classicalmusicquiz

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import java.util.*

class QuizActivity : AppCompatActivity(), View.OnClickListener {
    private val mButtonIDs = intArrayOf(R.id.buttonA, R.id.buttonB, R.id.buttonC, R.id.buttonD)
    private lateinit var mRemainingSampleIDs: ArrayList<Int>
    private lateinit var mQuestionSampleIDs: ArrayList<Int>
    private var mAnswerSampleID: Int = 0
    private var mCurrentScore: Int = 0
    private var mHighScore: Int = 0
    private lateinit var mButtons: Array<Button>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        val composerView = findViewById<View>(R.id.composerView) as ImageView

        val isNewGame = !intent.hasExtra(REMAINING_SONGS_KEY)

        // If it's a new game, set the current score to 0 and load all samples.
        if (isNewGame) {
            QuizUtils.setCurrentScore(this, 0)
            mRemainingSampleIDs = Sample.getAllSampleIDs(this)
            // Otherwise, get the remaining songs from the Intent.
        } else {
            mRemainingSampleIDs = intent.getIntegerArrayListExtra(REMAINING_SONGS_KEY)
        }

        // Get current and high scores.
        mCurrentScore = QuizUtils.getCurrentScore(this)
        mHighScore = QuizUtils.getHighScore(this)

        // Generate a question and get the correct answer.
        mQuestionSampleIDs = QuizUtils.generateQuestion(mRemainingSampleIDs)
        mAnswerSampleID = QuizUtils?.getCorrectAnswerID(mQuestionSampleIDs)

        // Load the image of the composer for the answer into the ImageView.
        composerView.setImageBitmap(Sample.getComposerArtBySampleID(this, mAnswerSampleID))

        // If there is only one answer left, end the game.
        if (mQuestionSampleIDs!!.size < 2) {
            QuizUtils.endGame(this)
            finish()
        }

        // Initialize the buttons with the composers names.
        mButtons = initializeButtons(mQuestionSampleIDs)
    }


    /**
     * Initializes the button to the correct views, and sets the text to the composers names,
     * and set's the OnClick listener to the buttons.
     *
     * @param answerSampleIDs The IDs of the possible answers to the question.
     * @return The Array of initialized buttons.
     */
    private fun initializeButtons(answerSampleIDs: ArrayList<Int>): Array<Button> {
        val buttons = mutableListOf<Button>()
        for (i in answerSampleIDs.indices) {
            val currentButton = findViewById<View>(mButtonIDs[i]) as Button
            val currentSample = Sample.getSampleByID(this, answerSampleIDs[i])
            buttons[i] = currentButton
            currentButton.setOnClickListener(this)
            if (currentSample != null) {
                currentButton.text = currentSample.composer
            }
        }
        return buttons.toTypedArray()
    }


    /**
     * The OnClick method for all of the answer buttons. The method uses the index of the button
     * in button array to to get the ID of the sample from the array of question IDs. It also
     * toggles the UI to show the correct answer.
     *
     * @param v The button that was clicked.
     */
    override fun onClick(v: View) {

        // Show the correct answer.
        showCorrectAnswer()

        // Get the button that was pressed.
        val pressedButton = v as Button

        // Get the index of the pressed button
        var userAnswerIndex = -1
        for (i in mButtons!!.indices) {
            if (pressedButton.id == mButtonIDs[i]) {
                userAnswerIndex = i
            }
        }

        // Get the ID of the sample that the user selected.
        val userAnswerSampleID = mQuestionSampleIDs!![userAnswerIndex]

        // If the user is correct, increase there score and update high score.
        if (QuizUtils.userCorrect(mAnswerSampleID, userAnswerSampleID)) {
            mCurrentScore++
            QuizUtils.setCurrentScore(this, mCurrentScore)
            if (mCurrentScore > mHighScore) {
                mHighScore = mCurrentScore
                QuizUtils.setHighScore(this, mHighScore)
            }
        }

        // Remove the answer sample from the list of all samples, so it doesn't get asked again.
        mRemainingSampleIDs!!.remove(Integer.valueOf(mAnswerSampleID))

        // Wait some time so the user can see the correct answer, then go to the next question.
        val handler = Handler()
        handler.postDelayed({
            val nextQuestionIntent = Intent(this@QuizActivity, QuizActivity::class.java)
            nextQuestionIntent.putExtra(REMAINING_SONGS_KEY, mRemainingSampleIDs)
            finish()
            startActivity(nextQuestionIntent)
        }, CORRECT_ANSWER_DELAY_MILLIS.toLong())

    }

    /**
     * Disables the buttons and changes the background colors to show the correct answer.
     */
    private fun showCorrectAnswer() {
        for (i in mQuestionSampleIDs!!.indices) {
            val buttonSampleID = mQuestionSampleIDs!![i]

            mButtons!![i].isEnabled = false
            if (buttonSampleID == mAnswerSampleID) {
                mButtons!![i].background.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light),
                        PorterDuff.Mode.MULTIPLY)
                mButtons!![i].setTextColor(Color.WHITE)
            } else {
                mButtons!![i].background.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light),
                        PorterDuff.Mode.MULTIPLY)
                mButtons!![i].setTextColor(Color.WHITE)

            }
        }
    }

    companion object {

        private val CORRECT_ANSWER_DELAY_MILLIS = 1000
        private val REMAINING_SONGS_KEY = "remaining_songs"
    }
}
