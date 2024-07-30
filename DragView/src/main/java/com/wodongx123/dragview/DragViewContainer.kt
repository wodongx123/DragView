package com.wodongx123.dragview

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.wodongx123.dragview.DragViewContainer.Companion.INVALID
import kotlin.math.abs
import kotlin.math.max

class DragViewContainer : FrameLayout {

    /** 起始X轴位置 */
    private var startX: Int = 0
    /** 起始Y轴位置 */
    private var startY: Int = 0

    /** 当前X位置 */
    private var curX: Int = 0
    /** 当前Y位置 */
    private var curY: Int = 0

    private var maxX: Int = 0
    private var maxY: Int = 0
    private var minX: Int = 0
    private var minY: Int = 0

    private var mHeight : Int = INVALID
    private var mWidth : Int = INVALID

    private var dragViewConfig: DragViewConfig = DragViewConfig()
    private val mAnimator: DefaultDragAnimator = DefaultDragAnimator()
    private var isMoving = false

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setConfig(config: DragViewConfig) {
        this.dragViewConfig = config
    }

    fun getConfig() : DragViewConfig {
        return dragViewConfig
    }

    fun isMoving() : Boolean {
        return isMoving
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 视图一旦滑动出屏幕，他的measure宽高或者实际宽高可能会发生变化，所以只有在开头的时候记录宽高
        if (isMoving) {
            return
        }

        val measureConfig = dragViewConfig.measureConfig
        measureConfig?.let {
            mHeight = if (it.customHeight != INVALID) {
                it.customHeight
            } else if (it.useGetMeasured) {
                measuredHeight
            } else if (it.useGet) {
                height
            } else {
                measuredHeight
            }

            mWidth = if (it.customWidth != INVALID) {
                it.customWidth
            } else if (it.useGetMeasured) {
                measuredWidth
            } else if (it.useGet) {
                width
            } else {
                measuredHeight
            }
        }
        if (measureConfig == null) {
            mHeight = measuredHeight
            mWidth = measuredWidth
        }
    }

    /**
     * 在事件分发的方法中处理滑动功能，不会影响到事件的正常监听
     */
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (!dragViewConfig.enableMove) {
            return super.dispatchTouchEvent(event)
        }

        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                setStartPos(event)
                setCurrentPos(event)
                setEdgePos()
                parent.requestDisallowInterceptTouchEvent(true)
                isMoving = true
            }
            MotionEvent.ACTION_MOVE -> {
                updatePosition(event)
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                onMoveEnd()
                parent.requestDisallowInterceptTouchEvent(false)
                if (isMove(event)) {
                    // return true会防止点击事件继续往下发，这样就不会触发子View的点击事件了
                    return true
                }
                isMoving = false
            }
        }

        if (event?.action == MotionEvent.ACTION_DOWN) {
            super.dispatchTouchEvent(event)
            return true
        }
        // 继续原先的点击事件分发流程，一般来说点击事件都是通过onClick或onLongClick，也就是ACTION_UP时才处理，所以不会影响
        return super.dispatchTouchEvent(event)
    }

    /**
     * 更新视图位置
     */
    private fun updatePosition(event: MotionEvent) {
        // 计算偏移量，用移动的位置减去原先的位置得到
        val dx = (event.rawX - curX).toInt()
        val dy = (event.rawY - curY).toInt()

        // 用当前的上下左右加上偏移量，得到新的View所在的位置
        var left = left + dx
        var right = right + dx
        var top = top + dy
        var bottom = bottom + dy

        // 如果开了边缘判断，将视图卡在边缘
        if (!dragViewConfig.canOutOfEdge) {
            if (left < minX) {
                left = minX
                right = minX + mWidth
            }
            if (top < minY) {
                top = minY
                bottom = minY + mHeight
            }
            if (right > maxX) {
                right = maxX
                left = right - mWidth
            }
            if (bottom > maxY) {
                bottom = maxY
                top = bottom - mHeight
            }
        }

        // 更新View位置
        layout(left, top, right, bottom)
        locatePosition(top, left)

        // 更新当前位置，否则视图会越划越远
        setCurrentPos(event)
    }

    private fun onMoveEnd() {
        // 没有贴边配置时，固定一下当前位置结束
        if (!DragViewUtil.hasAlignConfig(dragViewConfig)) {
            locatePosition()
            return
        }

        // 计算当前位置和最终目标位置
        val alignConfig = dragViewConfig.alignConfig!!
        val curPosition = DragPosition(left, top, right, bottom)
        var endPosition = DragPosition(left, top, right, bottom)
        endPosition = getHorizonPos(alignConfig, endPosition)
        endPosition = getVerticalPos(alignConfig, endPosition)

        if (alignConfig.animationConfig?.customAnimation != null) {
            //有自定义动画就通过自定义动画
            alignConfig.animationConfig?.customAnimation!!.onMove(curPosition, endPosition)
        } else if (DragViewUtil.hasAlignAnimation(alignConfig)) {
            // 如果有设置动画，就通过动画挪View
            mAnimator.setData(curPosition, endPosition, alignConfig.animationConfig!!)
            mAnimator.start()
        } else {
            // 没有动画就直接设置到目标位置
            locatePosition(endPosition.top, endPosition.left)
        }
    }

    /**
     * 根据配置信息计算最终的左右位置
     */
    private fun getHorizonPos(alignConfig: DragAlignConfig, position: DragPosition): DragPosition {
        val centerX = (maxX - minX) / 2
        val curX = (left + right) / 2
        val toRight = if ((alignConfig.alignToHorizonSide && curX >= centerX) || alignConfig.forceRight) {
                true
            } else if (alignConfig.alignToHorizonSide || alignConfig.forceLeft) {
                false
            } else {
                return position
            }

        // 在这里算出来的值最终是用作计算leftMargin和TopMargin的，right和bottom没有用
        if (toRight) {
            position.left = maxX - mWidth
            position.right = maxX
        } else {
            position.left = minX
            position.right = minX + mWidth
        }
        return position
    }

    /**
     * 根据配置信息计算最终的上下位置
     */
    private fun getVerticalPos(alignConfig: DragAlignConfig, position: DragPosition): DragPosition {
        val centerY = (maxY - minY) / 2
        val curY = (top + bottom) / 2

        val toTop = if ((alignConfig.alignToVerticalSide && curY <= centerY) || alignConfig.forceTop) {
            true
        } else if (alignConfig.alignToVerticalSide || alignConfig.forceBottom) {
            false
        } else {
            return position
        }

        if (toTop) {
            position.top = minY
            position.bottom = minY + mHeight
        } else {
            position.top = maxY - mHeight
            position.bottom = maxY
        }
        return position
    }

    /**
     * 记录起始位置，用于判断用户是否有滑动过
     */
    private fun setStartPos(event: MotionEvent) {
        startX = event.rawX.toInt()
        startY = event.rawY.toInt()
    }

    /**
     * 记录当前位置
     */
    private fun setCurrentPos(event: MotionEvent) {
        curX = event.rawX.toInt()
        curY = event.rawY.toInt()
    }

    /**
     * 设置边缘位置
     */
    private fun setEdgePos() {
        val p = parent as View
        minX = 0
        minY = 0
        maxY = p.bottom - p.top
        maxX = p.right - p.left
        dragViewConfig.customEdge?.let {
            minX = if (it.left != INVALID) it.left else minX
            minY = if (it.top != INVALID) it.top else minY
            maxX = if (it.right != INVALID) it.right else maxX
            maxY = if (it.bottom != INVALID) it.bottom else maxY
        }
    }

    /**
     * 通过Margin固定视图位置
     * 如果没有用Margin重新固定视图位置，他会在父布局重刷时跳回到初始位置
     */
    fun locatePosition(
        top: Int = getTop(),
        left: Int = getLeft()) {
        val layoutParams = layoutParams as MarginLayoutParams
        layoutParams.topMargin = top
        layoutParams.leftMargin = left
        this.layoutParams = layoutParams
    }

    /**
     * 判断是否有移动
     */
    private fun isMove(event: MotionEvent): Boolean {
        val interval = dragViewConfig.moveInterval
        return abs(event.rawX.toInt() - startX) >= interval
                && abs(event.rawY - startY) >= interval
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mAnimator.isRunning || mAnimator.isStarted) {
            mAnimator.cancel()
        }
        dragViewConfig.alignConfig?.animationConfig?.customAnimation?.onDetachedFromWindow()
    }

    /**
     * 动画操作类
     */
     inner class DefaultDragAnimator : ValueAnimator() {

        private lateinit var animationConfig: AnimationConfig
        private lateinit var start: DragPosition
        private lateinit var end: DragPosition

        private var horizonValue: Int = 0
        private var verticalValue: Int = 0

        init {
            addUpdateListener {
                val animatedValue = it.animatedValue as Int
                val curVerticalValue = if (animatedValue == 0) 0 else verticalValue / MAX_VALUE * animatedValue
                val curHorizontalValue = if (animatedValue == 0) 0 else horizonValue / MAX_VALUE * animatedValue

                val top = start.top + curVerticalValue.toInt()
                val left = start.left + curHorizontalValue.toInt()

                locatePosition(top, left)
            }
        }

        fun setData(start: DragPosition, end: DragPosition, animationConfig: AnimationConfig) {
            this.start = start
            this.end = end
            this.animationConfig = animationConfig

            // 先计算出垂直和水平的差值
            horizonValue = end.left - start.left
            verticalValue = end.top - start.top
            // 根据模式设置初始值
            if (animationConfig.timeMode && animationConfig.duration > 0) {
                setIntValues(0, animationConfig.maxValue.toInt()) // 实际的值在更新的时候手动计算，不在这里写
                setDuration(animationConfig.duration)
            } else if (animationConfig.speedMode && animationConfig.fixedSpeed > 0) {
                setIntValues(0, MAX_VALUE.toInt())
                val maxValue = max(abs(horizonValue), abs(verticalValue))
                val duration = maxValue / animationConfig.fixedSpeed * 1000
                setDuration(duration.toLong())
            }
        }

        override fun start() {
            if (isStarted) {
                cancel()
            }
            super.start()
        }

        override fun cancel() {
            super.cancel()
            locatePosition(end.top, end.left)
        }
    }

    companion object {
        const val TAG = "DragViewContainer"
        const val MAX_VALUE = 100.0
        const val INVALID = -1
    }
}

/**
 * 上下左右的位置类封装
 */
class DragPosition(
    var left: Int = INVALID,
    var top: Int = INVALID,
    var right: Int = INVALID,
    var bottom: Int = INVALID
)

/**
 * 自定义动画的接口
 */
interface DragAnimation {
    fun onMove(start: DragPosition, end: DragPosition)

    fun onDetachedFromWindow()
}
