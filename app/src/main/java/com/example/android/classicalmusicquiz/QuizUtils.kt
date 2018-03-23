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

import android.content.Context
import android.content.Intent
import java.util.*

internal object QuizUtils {

    private val CURRENT_SCORE_KEY = "current_score"
    private val HIGH_SCORE_KEY = "high_score"
    private val GAME_FINISHED = "game_finished"
    private val NUM_ANSWERS = 4

    /**
     * Generates an ArrayList of Integers that contains IDs to NUM_ANSWERS samples. These samples
     * constitute the possible answers to the question.
     * @param remainingSampleIDs The ArrayList of Integers which contains the IDs of all
     * samples that haven't been used yet.
     * @return The ArrayList of possible answers.
     */
    fun generateQuestion(remainingSampleIDs: ArrayList<Int>): ArrayList<Int> {

        // Shuffle the remaining sample ID's.
        Collections.shuffle(remainingSampleIDs)

        val answers = ArrayList<Int>()

        // Pick the first four random Sample ID's.
        for (i in 0 until NUM_ANSWERS) {
            if (i < remainingSampleIDs.size) {
                answers.add(remainingSampleIDs[i])
            }
        }

        return answers
    }

    /**
     * Helper method for getting the user's high score.
     * @param context The application context.
     * @return The user's high score.
     */
    fun getHighScore(context: Context): Int {
        val mPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        return mPreferences.getInt(HIGH_SCORE_KEY, 0)
    }

    /**
     * Helper method for setting the user's high score.
     * @param context The application context.
     * @param highScore The user's high score.
     */
    fun setHighScore(context: Context, highScore: Int) {
        val mPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val editor = mPreferences.edit()
        editor.putInt(HIGH_SCORE_KEY, highScore)
        editor.apply()
    }

    /**
     * Helper method for getting the user's current score.
     * @param context The application context.
     * @return The user's current score.
     */
    fun getCurrentScore(context: Context): Int {
        val mPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        return mPreferences.getInt(CURRENT_SCORE_KEY, 0)
    }

    /**
     * Helper method for setting the user's current score.
     * @param context The application context.
     * @param currentScore The user's current score.
     */
    fun setCurrentScore(context: Context, currentScore: Int) {
        val mPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val editor = mPreferences.edit()
        editor.putInt(CURRENT_SCORE_KEY, currentScore)
        editor.apply()
    }

    /**
     * Picks one of the possible answers to be the correct one at random.
     * @param answers The possible answers to the question.
     * @return The correct answer.
     */
    fun getCorrectAnswerID(answers: ArrayList<Int>): Int {
        val r = Random()
        val answerIndex = r.nextInt(answers.size)
        return answers[answerIndex]
    }

    /**
     * Checks that the user's selected answer is the correct one.
     * @param correctAnswer The correct answer.
     * @param userAnswer The user's answer
     * @return true if the user is correct, false otherwise.
     */
    fun userCorrect(correctAnswer: Int, userAnswer: Int): Boolean {
        return userAnswer == correctAnswer
    }


    /**
     * Helper method for ending the game.
     * @param context The application method.
     */
    fun endGame(context: Context) {
        val endGame = Intent(context, MainActivity::class.java)
        endGame.putExtra(GAME_FINISHED, true)
        context.startActivity(endGame)
    }

}
