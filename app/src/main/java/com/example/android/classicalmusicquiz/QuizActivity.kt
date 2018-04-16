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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_quiz.*
import java.util.*

class QuizActivity : AppCompatActivity(), View.OnClickListener, Player.EventListener {


    private val mButtonIDs = intArrayOf(R.id.buttonA, R.id.buttonB, R.id.buttonC, R.id.buttonD)
    private lateinit var mRemainingSampleIDs: ArrayList<Int>
    private lateinit var mQuestionSampleIDs: ArrayList<Int>
    private var mAnswerSampleID: Int = 0
    private var mCurrentScore: Int = 0
    private var mHighScore: Int = 0
    private lateinit var mButtons: Array<Button>
    private lateinit var mExoPlayerView: PlayerView
    private var mExoPlayer: SimpleExoPlayer? = null
    private val mMediaSession: MediaSessionCompat by lazy {
        MediaSessionCompat(this, TAG)
    }
    private var mNotificationManager: NotificationManager? = null

    private val mPlaybackStateBuilder: PlaybackStateCompat.Builder by lazy {
        PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    }

    companion object {
        private val TAG = this.javaClass.simpleName
        private val CORRECT_ANSWER_DELAY_MILLIS = 1000
        private val REMAINING_SONGS_KEY = "remaining_songs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        mExoPlayerView = exoPlayerWidget
        mExoPlayerView.defaultArtwork = BitmapFactory.decodeResource(resources, R.drawable.question_mark)

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


        // If there is only one answer left, end the game.
        if (mQuestionSampleIDs!!.size < 2) {
            QuizUtils.endGame(this)
            finish()
        }

        // Initialize the buttons with the composers names.
        mButtons = initializeButtons(mQuestionSampleIDs)

        val answerSample = Sample.getSampleByID(this, mAnswerSampleID)
        if(answerSample == null){
            Toast.makeText(this, getString(R.string.toast_no_sample_answer), Toast.LENGTH_LONG).show()
            return
        }

        if(mExoPlayer==null) {
            initializePlayer(Uri.parse(answerSample.uri))
        }

        initializeMediaSession()
    }

    override fun onResume() {
        super.onResume()
        mMediaSession.isActive = true
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        mMediaSession.isActive = false

    }



    private fun releasePlayer(){
        if(mExoPlayer!=null){
            with(mExoPlayer!!){
                stop()
                release()
            }
        }
        mExoPlayer = null
        mNotificationManager?.cancelAll()
    }

    private fun initializeButtons(answerSampleIDs: ArrayList<Int>): Array<Button> {
        val buttons = mutableListOf<Button>()
        for (i in answerSampleIDs.indices) {
            val currentButton = findViewById<View>(mButtonIDs[i]) as Button
            val currentSample = Sample.getSampleByID(this, answerSampleIDs[i])
            buttons.add(i, currentButton)
            currentButton.setOnClickListener(this)
            if (currentSample != null) {
                currentButton.text = currentSample.composer
            }
        }
        return buttons.toTypedArray()
    }

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

    private fun showNotification(stateOfPlayback: PlaybackStateCompat){
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "Media")

        var icon: Int
        var playpause: String
        if(stateOfPlayback.state == PlaybackStateCompat.STATE_PLAYING){
            icon = R.drawable.exo_controls_pause
            playpause = getString(R.string.pause)
        } else {
            icon = R.drawable.exo_controls_play
            playpause = getString(R.string.play)
        }

        //Define some actions
        val playpauseAction: NotificationCompat.Action = NotificationCompat.Action(icon, playpause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))

        val restartAction: NotificationCompat.Action = NotificationCompat.Action(
                                        R.drawable.exo_controls_previous, getString(R.string.restart),
                                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                                this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )


        val pendingContentIntent: PendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, QuizActivity.javaClass), 0)


        with(notificationBuilder){
            setContentTitle("Guess")
            setContentText(getString(R.string.notification_text))
            setContentIntent(pendingContentIntent)
            setSmallIcon(R.drawable.ic_music_note_white_24dp)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            addAction(playpauseAction)
            addAction(restartAction)
            setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle().setMediaSession(
                    mMediaSession.sessionToken
            ).setShowActionsInCompactView(0,1))
        }

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager!!.notify(0, notificationBuilder.build())
    }

    /**
     * Disables the buttons and changes the background colors to show the correct answer.
     */
    private fun showCorrectAnswer() {
        for (i in mQuestionSampleIDs!!.indices) {
            val buttonSampleID = mQuestionSampleIDs!![i]
            mButtons!![i].isEnabled = false
            mExoPlayerView.defaultArtwork= Sample.getComposerArtBySampleID(this, mAnswerSampleID)

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

    private fun initializePlayer(mediaUri: Uri) {
        val trackSelector = DefaultTrackSelector()
        val loadControl = DefaultLoadControl()
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl)
        mExoPlayerView.player = mExoPlayer
        val userAgent = Util.getUserAgent(this, "ClassicalMusicQuiz")
        val mediaSource: MediaSource = ExtractorMediaSource(mediaUri,
                DefaultDataSourceFactory(this, userAgent),
                DefaultExtractorsFactory(), null, null
        )
        mExoPlayer?.prepare(mediaSource)
        mExoPlayer?.playWhenReady = true
        mExoPlayer?.addListener(this)
    }

    private fun initializeMediaSession(){
        with(mMediaSession){
            setCallback(QuizMediaSessionCallback())
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setMediaButtonReceiver(null)
            setPlaybackState(mPlaybackStateBuilder.build())
            isActive=true
        }
    }

//region Exoplayer Listener Methods
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

    }

    override fun onSeekProcessed() {

    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        if(playWhenReady && playbackState == Player.STATE_READY){
            mPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer!!.currentPosition, 1f)
        } else if (playbackState == Player.STATE_READY){
            mPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer!!.currentPosition, 1f)
        }
        mMediaSession.setPlaybackState(mPlaybackStateBuilder.build())
        showNotification(mPlaybackStateBuilder.build())

    }

//endregion

    inner class QuizMediaSessionCallback(): MediaSessionCompat.Callback() {
        override fun onPlay() {
            mExoPlayer?.playWhenReady = true
        }

        override fun onPause() {
            mExoPlayer?.playWhenReady = false
        }

        override fun onSkipToPrevious() {
            mExoPlayer?.seekTo(0)
        }
    }


}
