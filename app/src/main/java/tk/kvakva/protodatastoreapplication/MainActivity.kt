package tk.kvakva.protodatastoreapplication

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

var keyExecutor = Executors.newSingleThreadScheduledExecutor()

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener,
    View.OnTouchListener {

    private val mConstraintLayout by lazy {
        findViewById<ConstraintLayout>(R.id.main_constraint)
    }
    private lateinit var scheduledFutureToLeft: ScheduledFuture<*>
    private lateinit var scheduledFutureToRight: ScheduledFuture<*>

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v?.id) {
            R.id.arrowRightBtn -> when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    keyExecutor.schedule({
                        lifecycleScope.launch {
                            viewModel.sussetBias(LeftRight.RIGHT, 0.02f)
                        }
                    }, 0, TimeUnit.MILLISECONDS)
                    scheduledFutureToRight = keyExecutor.scheduleAtFixedRate({
                        lifecycleScope.launch {
                            viewModel.sussetBias(LeftRight.RIGHT, 0.05f)
                        }
                    }, 200, 100, TimeUnit.MILLISECONDS)
                }
                MotionEvent.ACTION_UP -> {
                    scheduledFutureToRight.cancel(false)
                }
            }
            R.id.arrowLeftBtn -> when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    keyExecutor.schedule({
                        lifecycleScope.launch {
                            viewModel.sussetBias(LeftRight.LEFT, 0.02f)
                        }
                    }, 0, TimeUnit.MILLISECONDS)
                    scheduledFutureToLeft = keyExecutor.scheduleAtFixedRate({
                        lifecycleScope.launch {
                            viewModel.sussetBias(LeftRight.LEFT, 0.05f)
                        }
                    }, 200, 100, TimeUnit.MILLISECONDS)
                }
                MotionEvent.ACTION_UP -> {
                    scheduledFutureToLeft.cancel(false)
                }
            }

        }
        return true
    }

    val bombSize =
        Resources.getSystem().displayMetrics.densityDpi * 48 / DisplayMetrics.DENSITY_DEFAULT
    private var carEnd =
        Resources.getSystem().displayMetrics.widthPixels -
        Resources.getSystem().displayMetrics.densityDpi * 32 / DisplayMetrics.DENSITY_DEFAULT
    val mArrowRightBtn by lazy {
        findViewById<MaterialButton>(R.id.arrowRightBtn)
    }
    val mArrowLeftBtn by lazy {
        findViewById<MaterialButton>(R.id.arrowLeftBtn)
    }
    private var runnable = object : Runnable {
        override fun run() {
            runOnUiThread {
                Log.v(TAG,"runnable start")
                val imageView = ImageView(this@MainActivity).apply {
                    id = View.generateViewId()
                    tag = "bomb"
                    maxHeight = bombSize
                    maxWidth = bombSize
                    adjustViewBounds = true
                    setImageResource(R.drawable.anonymous_little_boy_atomic_bomb)
                    scaleType = ImageView.ScaleType.FIT_XY
                    //background=ColorDrawable(Color.GRAY)
                }

                val ra = Random.nextFloat()
//                Log.v(
//                    TAG,
//                    "runnable run continue ra=$ra\nResources.getSystem().displayMetrics.widthPixels = ${Resources.getSystem().displayMetrics.widthPixels}"
//                )
                mConstraintLayout.addView(imageView)

                imageView.x = (mConstraintLayout.width - imageView.maxWidth) * ra
                //(((Resources.getSystem().displayMetrics.widthPixels - imageView.maxWidth).toFloat()) * ra)
                imageView.y =
                    (findViewById<Flow>(R.id.lifes_flow).height
                            //+ 20 + imageView.maxHeight / 2
                            ).toFloat()
                val anim = ObjectAnimator.ofFloat(imageView, "y", viewModel.bot).setDuration(4000)
                arrayDeque.add(anim)
                anim.interpolator=LinearInterpolator()

                anim.doOnEnd {
                    Log.v(TAG,"animation doOnEnd Start")
                    arrayDeque.remove(anim)
                    val carRealX = mCarIv.x + mCarIv.width / 2
                    val bombRealX = imageView.x + imageView.width / 2
//                    Log.v(
//                        TAG,
//                        "carRealX: $carRealX, bombRealX: $bombRealX, imageView.width: ${imageView.width}"
//                    )
                    //if (abs(imageView.x - mCarIv.x) < imageView.maxWidth) {
                    if (abs(bombRealX - carRealX) < mCarIv.width / 2 + imageView.width / 2) {
                        viewModel.lifes.value?.let { lifes ->
                            if (lifes == 1) {
                                executor.shutdown()
                                viewModel.startedStopped.value = StartedStopped.STOPPED
                            }
                            viewModel.lifes.value = lifes - 1
                            //                                        findViewById<ImageView>(R.id.life_three_iv).visibility =
                            //                                            View.GONE
                            //                                        if (lifes == 2) {
                            //                                            findViewById<ImageView>(R.id.life_two_iv).visibility =
                            //                                                View.GONE
                            //                                        }
                        }

                        imageView.alpha = 0.0f
                        imageView.setImageDrawable(
                            AppCompatResources.getDrawable(
                                this@MainActivity,
                                R.drawable.explosion_fire_mushroom_cloud
                            )
                        )
                        imageView.scaleX = 0f
                        imageView.scaleY = 0f
                        imageView.animate().scaleX(5f).scaleY(5f).alpha(1f)
                            .setDuration(3000).withEndAction {
                                imageView.setImageDrawable(
                                    AppCompatResources.getDrawable(
                                        this@MainActivity,
                                        R.drawable.dead_woman_silhouette
                                    )
                                )
                                imageView.scaleX = 1f
                                imageView.scaleY = 1f
                            }.start()

                    } else {
                        mConstraintLayout.removeView(imageView)
                    }
                    Log.v(TAG,"animation doOnEnd End")
                }
                anim.start()
                //                        anim.repeatCount = 5
                //                        anim.repeatMode = ObjectAnimator.RESTART


                //                        val a = imageView.animate()
                //                        a.setDuration(6000).y(viewModel.bot)
                ////                            .withStartAction {
                ////                                viewModel.lifes.value?.let { lifes ->
                ////                                    if (lifes < 3)
                ////                                        findViewById<ImageView>(R.id.life_three_iv).visibility =
                ////                                            View.GONE
                ////                                    if (lifes < 2) {
                ////                                        findViewById<ImageView>(R.id.life_two_iv).visibility =
                ////                                            View.GONE
                ////                                    }
                ////                                }
                ////                            }
                //                            .withEndAction {
                //                                val carRealX = mCarIv.x + mCarIv.width / 3
                //                                val bombRealX = imageView.x + imageView.width / 3
                //                                Log.v(
                //                                    TAG,
                //                                    "carRealX: $carRealX, bombRealX: $bombRealX, imageView.width: ${imageView.width}"
                //                                )
                //                                //if (abs(imageView.x - mCarIv.x) < imageView.maxWidth) {
                //                                if (abs(bombRealX - carRealX) < mCarIv.width * .6) {
                //                                    viewModel.lifes.value?.let { lifes ->
                //                                        if (lifes == 1) {
                //                                            executor.shutdown()
                //                                        }
                //                                        viewModel.lifes.value = lifes - 1
                ////                                        findViewById<ImageView>(R.id.life_three_iv).visibility =
                ////                                            View.GONE
                ////                                        if (lifes == 2) {
                ////                                            findViewById<ImageView>(R.id.life_two_iv).visibility =
                ////                                                View.GONE
                ////                                        }
                //                                    }
                //
                //                                    imageView.alpha = 0.0f
                //                                    imageView.setImageDrawable(
                //                                        AppCompatResources.getDrawable(
                //                                            this@MainActivity,
                //                                            R.drawable.explosion_fire_mushroom_cloud
                //                                        )
                //                                    )
                //                                    imageView.scaleX = 0f
                //                                    imageView.scaleY = 0f
                //                                    imageView.animate().scaleX(5f).scaleY(5f).alpha(1f)
                //                                        .setDuration(3000).withEndAction {
                //                                            imageView.setImageDrawable(
                //                                                AppCompatResources.getDrawable(
                //                                                    this@MainActivity,
                //                                                    R.drawable.dead_woman_silhouette
                //                                                )
                //                                            )
                //                                            imageView.scaleX = 1f
                //                                            imageView.scaleY = 1f
                //                                        }.start()
                //
                //                                } else {
                //                                    layout.removeView(imageView)
                //                                }
                //                            }
                //                            .start()
                Log.v(TAG,"runnable end")
            }
        }
    }
    val arrayDeque = ArrayDeque<ObjectAnimator>()

    val mCarIv: ImageView by lazy {
        findViewById<ImageView>(R.id.carIv)
    }
    val mStartStopBtn: MaterialButton by lazy {
        findViewById<MaterialButton>(R.id.startStopBtn)
    }
    lateinit var viewModel: MainActivityViewModel

    private val lifeOneIv by lazy {
        findViewById<ImageView>(R.id.life_one_iv)
    }
    private val lifeTweIv by lazy {
        findViewById<ImageView>(R.id.life_two_iv)
    }
    private val lifeThreeIv by lazy {
        findViewById<ImageView>(R.id.life_three_iv)
    }
    var bot = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]


        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.carBiasFlow.collect {
                    //this@MainActivity.carBiasDataStore.data.collect { carbias ->
                    runOnUiThread {
                        mCarIv.animate().setInterpolator(LinearInterpolator()).x(it * carEnd)
                        //findViewById<Flow>(R.id.carFlow).setHorizontalBias(it)
                    }
                    //}
                }
            }
        }
        //mArrowLeftBtn.setOnLongClickListener(this)
        //mArrowRightBtn.setOnLongClickListener(this)
        mArrowRightBtn.setOnTouchListener(this)
        mArrowLeftBtn.setOnTouchListener(this)


        viewModel.lifes.observe(this) {
            when (it) {
                3 -> {
                    lifeThreeIv.visibility = View.VISIBLE
                    lifeTweIv.visibility = View.VISIBLE
                    lifeOneIv.visibility = View.VISIBLE
                }
                2 -> {
                    lifeThreeIv.visibility = View.GONE
                    lifeTweIv.visibility = View.VISIBLE
                    lifeOneIv.visibility = View.VISIBLE
                }
                1 -> {
                    lifeThreeIv.visibility = View.GONE
                    lifeTweIv.visibility = View.GONE
                    lifeOneIv.visibility = View.VISIBLE
                }
                0 -> {
                    lifeThreeIv.visibility = View.GONE
                    lifeTweIv.visibility = View.GONE
                    lifeOneIv.visibility = View.GONE
                }
            }
        }

        viewModel.startedStopped.observe(this) {
            mStartStopBtn.icon = when (it) {
                StartedStopped.STOPPED -> AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_baseline_not_started_24
                )
                StartedStopped.STARTED -> AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_baseline_stop_24
                )
                StartedStopped.PAUSED -> AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_baseline_pause_circle_24
                )
            }
        }
        mStartStopBtn.setOnClickListener {
            if (viewModel.startedStopped.value == StartedStopped.STOPPED) {
                viewModel.startedStopped.value = StartedStopped.STARTED

                if (executor.isShutdown) {
                    executor = Executors.newSingleThreadScheduledExecutor()
                    viewModel.lifes.value = 3
                }
                executor.scheduleAtFixedRate(
                    runnable,
                    0,
                    4,
                    TimeUnit.SECONDS
                ) // schedule(runnable, 4, TimeUnit.SECONDS)
            } else if (viewModel.startedStopped.value == StartedStopped.STARTED) {
                viewModel.startedStopped.value = StartedStopped.PAUSED
                arrayDeque.forEach { a ->
                    a.pause()
                }
                executor.shutdown()
            } else if (viewModel.startedStopped.value == StartedStopped.PAUSED) {
                viewModel.startedStopped.value = StartedStopped.STARTED
                arrayDeque.forEach { a ->
                    a.resume()
                }
                if (executor.isShutdown) {
                    executor = Executors.newSingleThreadScheduledExecutor()
                    executor.scheduleAtFixedRate(
                        runnable,
                        0,
                        4,
                        TimeUnit.SECONDS
                    )
                }
            }
        }
        mConstraintLayout.viewTreeObserver.addOnGlobalLayoutListener(
            object :
                ViewTreeObserver.OnGlobalLayoutListener {

                /**
                 * Callback method to be invoked when the global layout state or the visibility of views
                 * within the view tree changes
                 */
                override fun onGlobalLayout() {

                    Log.v(TAG,"Height: ${window.decorView.height}")
                    Log.v(TAG,"Width: ${window.decorView.width}")
                    Log.v(TAG,"mConstraintLayout.width: ${mConstraintLayout.width}")
                    carEnd =
                        mConstraintLayout.width - Resources.getSystem().displayMetrics.densityDpi * 32 / DisplayMetrics.DENSITY_DEFAULT
                    viewModel.bot = mCarIv.y - bombSize
                    viewModel.top =
                        findViewById<Flow>(R.id.lifes_flow).y + findViewById<Flow>(R.id.lifes_flow).height / 2
                    mConstraintLayout.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    );
                }
            });

    }

    override fun onClick(v: View?) {
        Log.v(TAG, "onCLick")
        lifecycleScope.launch(Dispatchers.IO) {
            when (v?.id) {
                R.id.arrowLeftBtn -> viewModel.sussetBias(LeftRight.LEFT, .1f)
                R.id.arrowRightBtn -> viewModel.sussetBias(LeftRight.RIGHT, .1f)
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        lifecycleScope.launch(Dispatchers.IO) {
            when (v?.id) {
                R.id.arrowLeftBtn -> viewModel.sussetBias(LeftRight.LEFT, .2f)
                R.id.arrowRightBtn -> viewModel.sussetBias(LeftRight.RIGHT, .2f)
            }
        }
        return true
    }
}

private const val TAG = "MainActivity"