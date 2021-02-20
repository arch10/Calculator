package com.gigaworks.tech.calculator.ui.main

import android.animation.*
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.gigaworks.tech.calculator.R
import com.gigaworks.tech.calculator.databinding.ActivityMainBinding
import com.gigaworks.tech.calculator.domain.History
import com.gigaworks.tech.calculator.ui.about.AboutActivity
import com.gigaworks.tech.calculator.ui.base.BaseActivity
import com.gigaworks.tech.calculator.ui.history.HistoryActivity
import com.gigaworks.tech.calculator.ui.main.helper.*
import com.gigaworks.tech.calculator.ui.main.viewmodel.MainViewModel
import com.gigaworks.tech.calculator.ui.settings.SettingsActivity
import com.gigaworks.tech.calculator.ui.view.CalculatorEditText
import com.gigaworks.tech.calculator.util.AngleType
import com.gigaworks.tech.calculator.util.AppTheme
import com.gigaworks.tech.calculator.util.NumberSeparator
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.math.sqrt


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()
    private var mCurrentAnimator: Animator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar(binding.toolbar)

        setupView()
        setupObservers()
        setClickListener()
        setAppTheme()

        binding.calculatorPadViewPager?.addScientificPadStateChangeListener {
            binding.scientificPad.arrow.animate().rotationBy(180F).setDuration(300).start()
        }
    }

    private val buttonClick = View.OnClickListener {
        it.isHapticFeedbackEnabled = true
        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val text = (it as Button).text.toString()
        val expression = removeNumberSeparator(getExpression())
        var newExpression = handleClick(expression, text, viewModel.isPrevResult)
        viewModel.isPrevResult = false
        if (viewModel.getNumberSeparator() != NumberSeparator.OFF) {
            newExpression = addNumberSeparator(
                expression = newExpression,
                isIndian = (viewModel.getNumberSeparator() == NumberSeparator.INDIAN)
            )
        }
        setExpression(newExpression)
    }

    private val textSizeChangeListener =
        CalculatorEditText.OnTextSizeChangeListener { textView, oldSize ->
            // Calculate the values needed to perform the scale and translation animations,
            // maintaining the same apparent baseline for the displayed text.
            val textScale = oldSize / textView.textSize
            val translationX = (1.0f - textScale) *
                    (textView.width / 2.0f - textView.paddingEnd)
            val translationY = (1.0f - textScale) *
                    (textView.height / 2.0f - textView.paddingBottom)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, View.SCALE_X, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, translationY, 0.0f)
            )
            animatorSet.duration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.start()
        }

    private val expressionChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            setResult("")
            getResultEditText().setTextColor(getResultTextColor())
            if (!removeNumberSeparator(s.toString()).isNumber()) {
                viewModel.calculateExpression(s.toString())
            }
        }

    }

    private fun setClickListener() {
        //number Pad
        with(binding.numPad) {
            //first row
            percent.setOnClickListener(buttonClick)
            openBracket.setOnClickListener(buttonClick)
            closeBracket.setOnClickListener(buttonClick)
            //second row
            seven.setOnClickListener(buttonClick)
            eight.setOnClickListener(buttonClick)
            nine.setOnClickListener(buttonClick)
            divide.setOnClickListener(buttonClick)
            //third row
            four.setOnClickListener(buttonClick)
            five.setOnClickListener(buttonClick)
            six.setOnClickListener(buttonClick)
            multiply.setOnClickListener(buttonClick)
            //fourth row
            one.setOnClickListener(buttonClick)
            two.setOnClickListener(buttonClick)
            three.setOnClickListener(buttonClick)
            plus.setOnClickListener(buttonClick)
            //fifth row
            decimal.setOnClickListener(buttonClick)
            zero.setOnClickListener(buttonClick)
            minus.setOnClickListener(buttonClick)
        }

        //scientific Pad
        with(binding.scientificPad) {
            //first row
            sin.setOnClickListener(buttonClick)
            cos.setOnClickListener(buttonClick)
            tan.setOnClickListener(buttonClick)
            //second row
            asin.setOnClickListener(buttonClick)
            acos.setOnClickListener(buttonClick)
            atan.setOnClickListener(buttonClick)
            //third row
            exponential.setOnClickListener(buttonClick)
            log.setOnClickListener(buttonClick)
            naturalLog.setOnClickListener(buttonClick)
            power.setOnClickListener(buttonClick)
            //fourth row
            factorial.setOnClickListener(buttonClick)
            squareRoot.setOnClickListener(buttonClick)
            cubeRoot.setOnClickListener(buttonClick)
            pi.setOnClickListener(buttonClick)
        }

        //delete onClick
        binding.numPad.delete.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val expression = removeNumberSeparator(getExpression())
            if (expression.isEmpty()) {
                return@setOnClickListener
            }
            val newExpression = if (viewModel.getNumberSeparator() != NumberSeparator.OFF) {
                addNumberSeparator(
                    expression = handleDelete(expression),
                    isIndian = (viewModel.getNumberSeparator() == NumberSeparator.INDIAN)
                )
            } else {
                handleDelete(expression)
            }
            setExpression(newExpression)
        }

        //delete long click
        binding.numPad.delete.setOnLongClickListener {
            if (getExpression().isNotEmpty()) {
                animateClear()
            }
            true
        }

        //equal onClick
        binding.numPad.equal.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val expression = removeNumberSeparator(getExpression())
            val result = getResult()
            if (expression.isNotEmpty()) {
                if (result.isEmpty() || !removeNumberSeparator(result).isNumber()) {
                    val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                    getResultEditText().setTextColor(getResultTextColor(true))
                    val errorStringId = viewModel.error.value ?: R.string.invalid
                    setResult(getString(errorStringId))
                    getResultEditText().startAnimation(shake)
                } else {
                    val balancedExpression = viewModel.getCalculatedExpression()
                    val history = History(
                        expression = balancedExpression,
                        result = result,
                        date = System.currentTimeMillis()
                    )
                    viewModel.insertHistory(history)
                    viewModel.isPrevResult = true
                    setExpressionAfterEqual(result)
                }
            }
        }

        //memory store click
        binding.scientificPad.memoryStore.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val result = removeNumberSeparator(getResult())
            if (result.isNumber()) {
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                viewModel.setMemory(result)
            }
        }

        //memory restore click
        binding.scientificPad.memoryRestore.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val memory = viewModel.getMemory()
            val expression = removeNumberSeparator(getExpression())
            var newExpression = handleConstantClick(expression, memory, viewModel.isPrevResult)
            viewModel.isPrevResult = false
            if (viewModel.getNumberSeparator() != NumberSeparator.OFF) {
                newExpression = addNumberSeparator(
                    expression = newExpression,
                    isIndian = (viewModel.getNumberSeparator() == NumberSeparator.INDIAN)
                )
            }
            setExpression(newExpression)
        }

        binding.scientificPad.memoryAdd.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val memory = viewModel.getMemory()
            val result = removeNumberSeparator(getResult())
            if (result.isNumber() && memory.isNumber()) {
                val newMemory = memory.toDouble() + result.toDouble()
                viewModel.setMemory(newMemory.toString())
            }
        }

        binding.scientificPad.memorySub.setOnClickListener {
            it.isHapticFeedbackEnabled = true
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val memory = viewModel.getMemory()
            val result = removeNumberSeparator(getResult())
            if (result.isNumber() && memory.isNumber()) {
                val newMemory = memory.toDouble() - result.toDouble()
                viewModel.setMemory(newMemory.toString())
            }
        }
    }

    /**
     * Setup viewModel observers to observe the data change
     * */
    private fun setupObservers() {
        viewModel.result.observe(this) {
            setResult(it)
        }
    }

    /**
     * Setup the views with saved or initial values
     * */
    private fun setupView() {
        binding.resultPad.expression.setOnTextSizeChangeListener(textSizeChangeListener)
        binding.resultPad.expression.addTextChangedListener(expressionChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            it.findItem(R.id.angleType).title = viewModel.getAngleType()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * On back pressed, close the scientific pad if it is open
     * else close the app.
     * */
    override fun onBackPressed() {
        if (binding.calculatorPadViewPager?.currentItem == 0 || binding.calculatorPadViewPager == null) {
            super.onBackPressed()
        } else {
            binding.calculatorPadViewPager?.currentItem = 0
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.share -> {
                val sharedEquation = getShareEquation()
                if (sharedEquation.isNotEmpty()) {
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Calculator Plus Expression")
                                putExtra(Intent.EXTRA_TEXT, sharedEquation)
                            },
                            getString(R.string.choose)
                        )
                    )
                } else {
                    Toast.makeText(this, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.tutorial -> {
                showTutorial()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showTutorial() {
        //close side panel before starting tutorial
        if (binding.calculatorPadViewPager?.currentItem == 1) {
            binding.calculatorPadViewPager?.currentItem = 0
        }
        val tapTargetSequence = TapTargetSequence(this)
        val delete = TapTarget
            .forView(
                binding.numPad.delete,
                getString(R.string.delete_button),
                getString(R.string.delete_button_desc)
            )
            .outerCircleColor(R.color.primary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(false)
            .titleTextColor(R.color.white)
            .descriptionTextColor(R.color.white)
            .descriptionTextSize(18)
            .cancelable(true)
        val angle: TapTarget = TapTarget
            .forToolbarMenuItem(
                binding.toolbar,
                R.id.angleType,
                getString(R.string.angle_button),
                getString(R.string.angle_button_desc)
            )
            .outerCircleColor(R.color.primary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(true)
            .titleTextColor(R.color.white)
            .descriptionTextColor(R.color.white)
            .descriptionTextSize(18)
            .cancelable(true)
        val options: TapTarget = TapTarget
            .forToolbarOverflow(
                binding.toolbar,
                getString(R.string.options_menu),
                getString(R.string.options_menu_desc)
            )
            .outerCircleColor(R.color.primary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(true)
            .titleTextColor(R.color.white)
            .descriptionTextColor(R.color.white)
            .descriptionTextSize(18)
            .cancelable(true)
            .id(56)
        val share: TapTarget = TapTarget
            .forToolbarMenuItem(
                binding.toolbar,
                R.id.share,
                getString(R.string.share_button),
                getString(R.string.share_button_desc)
            )
            .outerCircleColor(R.color.primary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(true)
            .titleTextColor(R.color.white)
            .descriptionTextColor(R.color.white)
            .descriptionTextSize(18)
            .cancelable(true)
        val ms = TapTarget.forView(
            binding.scientificPad.memoryStore,
            getString(R.string.memory_store), getString(R.string.memory_store_desc)
        )
            .outerCircleColor(R.color.numPadPrimary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(false)
            .titleTextColor(R.color.textPrimary)
            .descriptionTextColor(R.color.textPrimary)
            .descriptionTextSize(18)
            .cancelable(true)
        val mr = TapTarget.forView(
            binding.scientificPad.memoryRestore,
            getString(R.string.memory_restore), getString(R.string.memory_restore_desc)
        )
            .outerCircleColor(R.color.numPadPrimary)
            .outerCircleAlpha(1f)
            .targetCircleColor(R.color.white)
            .titleTextSize(28)
            .tintTarget(false)
            .titleTextColor(R.color.textPrimary)
            .descriptionTextColor(R.color.textPrimary)
            .descriptionTextSize(18)
            .cancelable(true)

        tapTargetSequence.targets(delete, angle, share, options, ms, mr)

        tapTargetSequence.listener(object : TapTargetSequence.Listener {
            override fun onSequenceFinish() {
                binding.calculatorPadViewPager?.currentItem = 0
            }

            override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
                if (lastTarget.id() == 56) {
                    binding.calculatorPadViewPager?.currentItem = 1
                }
            }

            override fun onSequenceCanceled(lastTarget: TapTarget) {}
        })
        tapTargetSequence.continueOnCancel(true).start()
    }

    private fun getShareEquation(): String {
        if (getExpression().isNotEmpty()) {
            val result = getResult()
            return if (result == "" || !removeNumberSeparator(result).isNumber()) {
                ""
            } else {
                val expression = viewModel.getCalculatedExpression()
                "$expression = $result"
            }
        }
        return ""
    }


    fun changeAngleType(menuItem: MenuItem) {
        val text = menuItem.title.toString()
        if (text == AngleType.DEG.name) {
            menuItem.title = AngleType.RAD.name
            viewModel.changeAngleType(AngleType.RAD)
        } else {
            menuItem.title = AngleType.DEG.name
            viewModel.changeAngleType(AngleType.DEG)
        }
        val currentExpression = getExpression()
        setExpression(currentExpression)
    }

    private fun getResultTextColor(isError: Boolean = false): Int {
        val typedValue = TypedValue()
        return if (isError) {
            val typedArray = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorError))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()
            color
        } else {
            val typedArray = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.textDisable))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()
            color
        }
    }

    private fun animateClear() {
        with(binding) {
            val cx = clearView.right
            val cy = clearView.bottom
            val l = clearView.height
            val b = clearView.width
            val finalRadius = sqrt((l * l + b * b).toDouble()).toInt()
            val anim = ViewAnimationUtils
                .createCircularReveal(clearView, cx, cy, 0f, finalRadius.toFloat())
            clearView.visibility = View.VISIBLE
            anim.duration = 300
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    setExpression("")
                    setResult("")
                    getResultEditText().setTextColor(getResultTextColor())
                    clearView.visibility = View.INVISIBLE
                    mCurrentAnimator = null
                }
            })
            mCurrentAnimator = anim
            anim.start()
        }
    }

    private fun setExpressionAfterEqual(answer: String) {
        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        val expression = getExpressionEditText()
        val result = getResultEditText()
        val resultScale = expression.getVariableTextSize(answer) / result.textSize
        val resultTranslationX = (1.0f - resultScale) * (result.width / 2.0f - result.paddingEnd)
        val resultTranslationY = (1.0f - resultScale) *
                (result.height / 2.0f - result.paddingBottom) +
                (expression.bottom - result.bottom) +
                (result.paddingBottom - expression.paddingBottom)
        val formulaTranslationY = -expression.bottom.toFloat()

        // Use a value animator to fade to the final text color over the course of the animation.
        val resultTextColor: Int = result.currentTextColor
        val formulaTextColor: Int = expression.currentTextColor
        val textColorAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), resultTextColor, formulaTextColor)
        textColorAnimator.addUpdateListener { valueAnimator ->
            result.setTextColor(valueAnimator.animatedValue as Int)
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            textColorAnimator,
            ObjectAnimator.ofFloat(result, View.SCALE_X, resultScale),
            ObjectAnimator.ofFloat(result, View.SCALE_Y, resultScale),
            ObjectAnimator.ofFloat(result, View.TRANSLATION_X, resultTranslationX),
            ObjectAnimator.ofFloat(result, View.TRANSLATION_Y, resultTranslationY),
            ObjectAnimator.ofFloat(expression, View.TRANSLATION_Y, formulaTranslationY)
        )
        animatorSet.duration =
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator) {
                // Reset all of the values modified during the animation.
                result.setTextColor(resultTextColor)
                result.scaleX = 1.0f
                result.scaleY = 1.0f
                result.translationX = 0.0f
                result.translationY = 0.0f
                expression.translationY = 0.0f

                // Finally update the formula to use the current result.
                expression.setText(answer)
                result.setText("")
                mCurrentAnimator = null
            }
        })
        mCurrentAnimator = animatorSet
        animatorSet.start()
    }

    private fun setAppTheme() {
        val themeMode = when (getSelectedTheme()) {
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun getSelectedTheme(): AppTheme {
        val themeName = viewModel.getAppTheme()
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM_DEFAULT
        }
    }

    private fun getExpressionEditText(): CalculatorEditText {
        return binding.resultPad.expression
    }

    private fun getResultEditText(): CalculatorEditText {
        return binding.resultPad.result
    }

    private fun setExpression(expression: String) {
        getExpressionEditText().setText(expression)
    }

    private fun setResult(result: String) {
        getResultEditText().setText(result)
    }

    private fun getExpression(): String {
        return binding.resultPad.expression.text.toString().trim()
    }

    private fun getResult(): String {
        return binding.resultPad.result.text.toString().trim()
    }

    override fun onStart() {
        super.onStart()
        var savedExpression = viewModel.getSavedExpression()
        if (viewModel.getNumberSeparator() != NumberSeparator.OFF) {
            savedExpression = addNumberSeparator(
                expression = savedExpression,
                isIndian = (viewModel.getNumberSeparator() == NumberSeparator.INDIAN)
            )
        }
        setExpression(savedExpression)
    }

    override fun onStop() {
        super.onStop()
        mCurrentAnimator?.end()
        val currentExpression = removeNumberSeparator(getExpression())
        viewModel.saveExpression(currentExpression)
    }

    override fun getViewBinding(inflater: LayoutInflater) = ActivityMainBinding.inflate(inflater)
}