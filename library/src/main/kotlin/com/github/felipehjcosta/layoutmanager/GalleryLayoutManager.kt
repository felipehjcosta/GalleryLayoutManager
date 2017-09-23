package com.github.felipehjcosta.layoutmanager

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup


/**
 * A custom LayoutManager to build a [android.widget.Gallery] or a [android.support.v4.view.ViewPager]like [RecyclerView] and
 * support both [GalleryLayoutManager.HORIZONTAL] and [GalleryLayoutManager.VERTICAL] scroll.
 * Created by chensuilun on 2016/11/18.
 */
class GalleryLayoutManager(val orientation: Int = GalleryLayoutManager.HORIZONTAL) :
        RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {

    private lateinit var recyclerView: RecyclerView

    private var firstVisiblePosition = 0
    private var lastVisiblePos = 0
    private var initialSelectedPosition = 0

    private var curSelectedView: View? = null

    private val state: State by lazy { State() }

    private val snapHelper = LinearSnapHelper()

    private val innerScrollListener = InnerScrollListener()

    private val horizontalHelper: OrientationHelper by lazy { OrientationHelper.createHorizontalHelper(this) }
    private val verticalHelper: OrientationHelper by lazy { OrientationHelper.createVerticalHelper(this) }

    private val orientationHelper: OrientationHelper
        get() = when (orientation) {
            HORIZONTAL -> horizontalHelper
            else -> verticalHelper
        }

    private val horizontalSpace: Int
        get() = width - paddingRight - paddingLeft

    private val verticalSpace: Int
        get() = height - paddingBottom - paddingTop

    var callbackInFling = false

    var itemTransformer: ItemTransformer? = null

    var onItemSelectedListener: OnItemSelectedListener? = null

    var currentSelectedPosition = -1
        private set

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
            if (orientation == VERTICAL) {
                LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
            } else {
                LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
            }

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams {
        return LayoutParams(c, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): RecyclerView.LayoutParams =
            if (lp is ViewGroup.MarginLayoutParams) {
                LayoutParams(lp as ViewGroup.MarginLayoutParams?)
            } else {
                LayoutParams(lp)
            }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean = lp is LayoutParams

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLayoutChildren() called with: state = [$state]")
        }
        if (itemCount == 0) {
            reset()
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (state!!.isPreLayout) {
            return
        }
        if (state.itemCount != 0 && !state.didStructureChange()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLayoutChildren: ignore extra layout step")
            }
            return
        }
        if (childCount == 0 || state.didStructureChange()) {
            reset()
        }
        initialSelectedPosition = Math.min(Math.max(0, initialSelectedPosition), itemCount - 1)
        detachAndScrapAttachedViews(recycler)
        firstFillCover(recycler, 0)
    }


    private fun reset() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reset: ")
        }

        state.itemsFrames.clear()

        //when data set update keep the last selected position
        if (currentSelectedPosition != -1) {
            initialSelectedPosition = currentSelectedPosition
        }
        initialSelectedPosition = Math.min(Math.max(0, initialSelectedPosition), itemCount - 1)
        firstVisiblePosition = initialSelectedPosition
        lastVisiblePos = initialSelectedPosition
        currentSelectedPosition = -1

        curSelectedView?.let {
            it.isSelected = false
            curSelectedView = null
        }
    }

    private fun firstFillCover(recycler: RecyclerView.Recycler?, scrollDelta: Int) {
        if (orientation == HORIZONTAL) {
            firstFillWithHorizontal(recycler)
        } else {
            firstFillWithVertical(recycler)
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "firstFillCover finished: (first: $firstVisiblePosition, last:$lastVisiblePos)")
        }

        applyTransformationOnChildrenBy(scrollDelta)
        innerScrollListener.onScrolled(recyclerView, 0, 0)
    }

    private fun firstFillWithHorizontal(recycler: RecyclerView.Recycler?) {
        detachAndScrapAttachedViews(recycler)
        val startPosition = initialSelectedPosition
        val scrap = recycler!!.getViewForPosition(initialSelectedPosition).apply {
            addView(this, 0)
            measureChildWithMargins(this, 0, 0)
        }
        val height = verticalSpace
        val scrapWidth = getDecoratedMeasuredWidth(scrap)
        val scrapHeight = getDecoratedMeasuredHeight(scrap)
        val topOffset = (paddingTop + (height - scrapHeight) / 2.0f).toInt()
        val left = (paddingLeft + (horizontalSpace - scrapWidth) / 2f).toInt()
        Rect().apply {
            set(left, topOffset, left + scrapWidth, topOffset + scrapHeight)
            layoutDecorated(scrap, left, top, right, bottom)
            state.putOrSet(startPosition, this)
        }
        lastVisiblePos = startPosition
        firstVisiblePosition = lastVisiblePos
        val leftStartOffset = getDecoratedLeft(scrap)
        val rightStartOffset = getDecoratedRight(scrap)
        val leftEdge = orientationHelper.startAfterPadding
        val rightEdge = orientationHelper.endAfterPadding
        fillLeft(recycler, initialSelectedPosition - 1, leftStartOffset, leftEdge)
        fillRight(recycler, initialSelectedPosition + 1, rightStartOffset, rightEdge)
    }

    private fun firstFillWithVertical(recycler: RecyclerView.Recycler?) {
        detachAndScrapAttachedViews(recycler)
        val startPosition = initialSelectedPosition
        val scrap = recycler!!.getViewForPosition(initialSelectedPosition).apply {
            addView(this, 0)
            measureChildWithMargins(this, 0, 0)
        }
        val width = horizontalSpace
        val scrapWidth = getDecoratedMeasuredWidth(scrap)
        val scrapHeight = getDecoratedMeasuredHeight(scrap)
        val leftOffset = (paddingLeft + (width - scrapWidth) / 2.0f).toInt()
        val top = (paddingTop + (verticalSpace - scrapHeight) / 2f).toInt()
        Rect().apply {
            set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight)
            layoutDecorated(scrap, left, top, right, bottom)
            state.putOrSet(startPosition, this)

        }
        lastVisiblePos = startPosition
        firstVisiblePosition = lastVisiblePos
        val topStartOffset = getDecoratedTop(scrap)
        val bottomStartOffset = getDecoratedBottom(scrap)
        val topEdge = orientationHelper.startAfterPadding
        val bottomEdge = orientationHelper.endAfterPadding
        fillTop(recycler, initialSelectedPosition - 1, topStartOffset, topEdge)
        fillBottom(recycler, initialSelectedPosition + 1, bottomStartOffset, bottomEdge)
    }

    private fun fillLeft(recycler: RecyclerView.Recycler, startPosition: Int, leftStartOffset: Int, leftEdge: Int) {
        var startOffset = leftStartOffset
        var i = startPosition
        while (i >= 0 && startOffset >= leftEdge) {
            val scrap = recycler.getViewForPosition(i).apply {
                addView(this, 0)
                measureChildWithMargins(this, 0, 0)
            }
            val height = verticalSpace
            val scrapWidth = getDecoratedMeasuredWidth(scrap)
            val scrapHeight = getDecoratedMeasuredHeight(scrap)
            val topOffset = (paddingTop + (height - scrapHeight) / 2.0f).toInt()
            Rect().apply {
                set(startOffset - scrapWidth, topOffset, startOffset, topOffset + scrapHeight)
                layoutDecorated(scrap, left, top, right, bottom)
                startOffset = left
                state.putOrSet(i, this)
            }
            firstVisiblePosition = i
            i--
        }
    }

    private fun fillRight(recycler: RecyclerView.Recycler, startPosition: Int, rightStartOffset: Int, rightEdge: Int) {
        var startOffset = rightStartOffset
        var i = startPosition
        while (i < itemCount && startOffset <= rightEdge) {
            val scrap = recycler.getViewForPosition(i).apply {
                addView(this)
                measureChildWithMargins(this, 0, 0)
            }
            val height = verticalSpace
            val scrapWidth = getDecoratedMeasuredWidth(scrap)
            val scrapHeight = getDecoratedMeasuredHeight(scrap)
            val topOffset = (paddingTop + (height - scrapHeight) / 2.0f).toInt()
            Rect().apply {
                set(startOffset, topOffset, startOffset + scrapWidth, topOffset + scrapHeight)
                layoutDecorated(scrap, left, top, right, bottom)
                startOffset = right
                state.putOrSet(i, this)
            }
            lastVisiblePos = i
            i++
        }
    }

    private fun fillTop(recycler: RecyclerView.Recycler, startPosition: Int, topStartOffset: Int, topEdge: Int) {
        var startOffset = topStartOffset
        var i = startPosition
        while (i >= 0 && startOffset > topEdge) {
            val scrap = recycler.getViewForPosition(i)
            addView(scrap, 0)
            measureChildWithMargins(scrap, 0, 0)
            val width = horizontalSpace
            val scrapWidth = getDecoratedMeasuredWidth(scrap)
            val scrapHeight = getDecoratedMeasuredHeight(scrap)
            val leftOffset = (paddingLeft + (width - scrapWidth) / 2.0f).toInt()
            Rect().apply {
                set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset)
                layoutDecorated(scrap, left, top, right, bottom)
                startOffset = top
                state.putOrSet(i, this)
            }
            firstVisiblePosition = i
            i--
        }
    }

    private fun fillBottom(recycler: RecyclerView.Recycler, startPosition: Int, bottomStartOffset: Int, bottomEdge: Int) {
        var startOffset = bottomStartOffset
        var i = startPosition
        while (i < itemCount && startOffset < bottomEdge) {
            val scrap = recycler.getViewForPosition(i)
            addView(scrap)
            measureChildWithMargins(scrap, 0, 0)
            val width = horizontalSpace
            val scrapWidth = getDecoratedMeasuredWidth(scrap)
            val scrapHeight = getDecoratedMeasuredHeight(scrap)
            val leftOffset = (paddingLeft + (width - scrapWidth) / 2.0f).toInt()
            Rect().apply {
                set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight)
                layoutDecorated(scrap, left, top, right, bottom)
                startOffset = bottom
                state.putOrSet(i, this)
            }
            lastVisiblePos = i
            i++
        }
    }


    private fun fillCover(recycler: RecyclerView.Recycler?, scrollDelta: Int) {
        if (itemCount == 0) {
            return
        }

        if (orientation == HORIZONTAL) {
            fillWithHorizontal(recycler, scrollDelta)
        } else {
            fillWithVertical(recycler, scrollDelta)
        }

        applyTransformationOnChildrenBy(scrollDelta)
    }

    private fun applyTransformationOnChildrenBy(scrollDelta: Int) = itemTransformer?.run {
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                transformItem(this@GalleryLayoutManager, it, i, calculateToCenterFraction(it, scrollDelta.toFloat()))
            }
        }
    }

    private fun calculateToCenterFraction(child: View, pendingOffset: Float): Float {
        val distance = calculateDistanceCenter(child, pendingOffset)
        val childLength = if (orientation == HORIZONTAL) child.width else child.height

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "calculateToCenterFraction: distance:$distance,childLength:$childLength")
        }
        return Math.max(-1f, Math.min(1f, distance * 1f / childLength))
    }

    private fun calculateDistanceCenter(child: View, pendingOffset: Float): Int {
        val orientationHelper = orientationHelper
        val parentCenter = (orientationHelper.endAfterPadding - orientationHelper.startAfterPadding) / 2 + orientationHelper.startAfterPadding
        if (orientation == HORIZONTAL) {
            return (child.width / 2 - pendingOffset + child.left - parentCenter).toInt()
        } else {
            return (child.height / 2 - pendingOffset + child.top - parentCenter).toInt()
        }

    }

    private fun fillWithVertical(recycler: RecyclerView.Recycler?, dy: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "fillWithVertical: dy:" + dy)
        }
        val topEdge = orientationHelper.startAfterPadding
        val bottomEdge = orientationHelper.endAfterPadding

        //1.remove and recycle the view that disappear in screen
        var child: View
        if (childCount > 0) {
            if (dy >= 0) {
                //remove and recycle the top off screen view
                var fixIndex = 0
                for (i in 0..childCount - 1) {
                    child = getChildAt(i + fixIndex)
                    if (getDecoratedBottom(child) - dy < topEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child))
                        }
                        removeAndRecycleView(child, recycler)
                        firstVisiblePosition++
                        fixIndex--
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "fillWithVertical: break:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child))
                        }
                        break
                    }
                }
            } else { //dy<0
                //remove and recycle the bottom off screen view
                for (i in childCount - 1 downTo 0) {
                    child = getChildAt(i)
                    if (getDecoratedTop(child) - dy > bottomEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child))
                        }
                        removeAndRecycleView(child, recycler)
                        lastVisiblePos--
                    } else {
                        break
                    }
                }
            }

        }
        var startPosition = firstVisiblePosition
        var startOffset = -1
        var scrapWidth: Int
        var scrapHeight: Int
        var scrapRect: Rect?
        val width = horizontalSpace
        var leftOffset: Int
        var scrap: View
        //2.Add or reattach item view to fill screen
        if (dy >= 0) {
            if (childCount != 0) {
                val lastView = getChildAt(childCount - 1)
                startPosition = getPosition(lastView) + 1
                startOffset = getDecoratedBottom(lastView)
            }
            var i = startPosition
            while (i < itemCount && startOffset < bottomEdge + dy) {
                scrapRect = this.state.itemsFrames.get(i)
                scrap = recycler!!.getViewForPosition(i)
                addView(scrap)
                if (scrapRect == null) {
                    scrapRect = Rect()
                    this.state.itemsFrames.put(i, scrapRect)
                }
                measureChildWithMargins(scrap, 0, 0)
                scrapWidth = getDecoratedMeasuredWidth(scrap)
                scrapHeight = getDecoratedMeasuredHeight(scrap)
                leftOffset = (paddingLeft + (width - scrapWidth) / 2.0f).toInt()
                if (startOffset == -1 && startPosition == 0) {
                    //layout the first position item in center
                    val top = (paddingTop + (verticalSpace - scrapHeight) / 2f).toInt()
                    scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight)
                } else {
                    scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight)
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom)
                startOffset = scrapRect.bottom
                lastVisiblePos = i
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithVertical: add view:$i,startOffset:$startOffset,lastVisiblePos:$lastVisiblePos,bottomEdge$bottomEdge")
                }
                i++
            }
        } else {
            //dy<0
            if (childCount > 0) {
                val firstView = getChildAt(0)
                startPosition = getPosition(firstView) - 1 //前一个View的position
                startOffset = getDecoratedTop(firstView)
            }
            var i = startPosition
            while (i >= 0 && startOffset > topEdge + dy) {
                scrapRect = this.state.itemsFrames.get(i)
                scrap = recycler!!.getViewForPosition(i)
                addView(scrap, 0)
                if (scrapRect == null) {
                    scrapRect = Rect()
                    this.state.itemsFrames.put(i, scrapRect)
                }
                measureChildWithMargins(scrap, 0, 0)
                scrapWidth = getDecoratedMeasuredWidth(scrap)
                scrapHeight = getDecoratedMeasuredHeight(scrap)
                leftOffset = (paddingLeft + (width - scrapWidth) / 2.0f).toInt()
                scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset)
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom)
                startOffset = scrapRect.top
                firstVisiblePosition = i
                i--
            }
        }
    }

    private fun fillWithHorizontal(recycler: RecyclerView.Recycler?, dx: Int) {
        val leftEdge = orientationHelper.startAfterPadding
        val rightEdge = orientationHelper.endAfterPadding
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "fillWithHorizontal() called with: dx = [$dx],leftEdge:$leftEdge,rightEdge:$rightEdge")
        }
        //1.remove and recycle the view that disappear in screen
        var child: View
        if (childCount > 0) {
            if (dx >= 0) {
                //remove and recycle the left off screen view
                var fixIndex = 0
                for (i in 0..childCount - 1) {
                    child = getChildAt(i + fixIndex)
                    if (getDecoratedRight(child) - dx < leftEdge) {
                        removeAndRecycleView(child, recycler)
                        firstVisiblePosition++
                        fixIndex--
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + " firstVisiblePosition change to:" + firstVisiblePosition)
                        }
                    } else {
                        break
                    }
                }
            } else { //dx<0
                //remove and recycle the right off screen view
                for (i in childCount - 1 downTo 0) {
                    child = getChildAt(i)
                    if (getDecoratedLeft(child) - dx > rightEdge) {
                        removeAndRecycleView(child, recycler)
                        lastVisiblePos--
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + "lastVisiblePos change to:" + lastVisiblePos)
                        }
                    }
                }
            }

        }
        //2.Add or reattach item view to fill screen
        var startPosition = firstVisiblePosition
        var startOffset = -1
        var scrapWidth: Int
        var scrapHeight: Int
        var scrapRect: Rect?
        val height = verticalSpace
        var topOffset: Int
        var scrap: View
        if (dx >= 0) {
            if (childCount != 0) {
                val lastView = getChildAt(childCount - 1)
                startPosition = getPosition(lastView) + 1 //start layout from next position item
                startOffset = getDecoratedRight(lastView)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:to right startPosition:$startPosition,startOffset:$startOffset,rightEdge:$rightEdge")
                }
            }
            var i = startPosition
            while (i < itemCount && startOffset < rightEdge + dx) {
                scrapRect = this.state.itemsFrames.get(i)
                scrap = recycler!!.getViewForPosition(i)
                addView(scrap)
                if (scrapRect == null) {
                    scrapRect = Rect()
                    this.state.itemsFrames.put(i, scrapRect)
                }
                measureChildWithMargins(scrap, 0, 0)
                scrapWidth = getDecoratedMeasuredWidth(scrap)
                scrapHeight = getDecoratedMeasuredHeight(scrap)
                topOffset = (paddingTop + (height - scrapHeight) / 2.0f).toInt()
                if (startOffset == -1 && startPosition == 0) {
                    // layout the first position item in center
                    val left = (paddingLeft + (horizontalSpace - scrapWidth) / 2f).toInt()
                    scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight)
                } else {
                    scrapRect.set(startOffset, topOffset, startOffset + scrapWidth, topOffset + scrapHeight)
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom)
                startOffset = scrapRect.right
                lastVisiblePos = i
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal,layout:lastVisiblePos: " + lastVisiblePos)
                }
                i++
            }
        } else {
            //dx<0
            if (childCount > 0) {
                val firstView = getChildAt(0)
                startPosition = getPosition(firstView) - 1 //start layout from previous position item
                startOffset = getDecoratedLeft(firstView)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:to left startPosition:$startPosition,startOffset:$startOffset,leftEdge:$leftEdge,child count:$childCount")
                }
            }
            var i = startPosition
            while (i >= 0 && startOffset > leftEdge + dx) {
                scrapRect = this.state.itemsFrames.get(i)
                scrap = recycler!!.getViewForPosition(i)
                addView(scrap, 0)
                if (scrapRect == null) {
                    scrapRect = Rect()
                    this.state.itemsFrames.put(i, scrapRect)
                }
                measureChildWithMargins(scrap, 0, 0)
                scrapWidth = getDecoratedMeasuredWidth(scrap)
                scrapHeight = getDecoratedMeasuredHeight(scrap)
                topOffset = (paddingTop + (height - scrapHeight) / 2.0f).toInt()
                scrapRect.set(startOffset - scrapWidth, topOffset, startOffset, topOffset + scrapHeight)
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom)
                startOffset = scrapRect.left
                firstVisiblePosition = i
                i--
            }
        }
    }

    private fun calculateScrollDirectionForPosition(position: Int): Int {
        if (childCount == 0) {
            return LAYOUT_START
        }
        val firstChildPos = firstVisiblePosition
        return if (position < firstChildPos) LAYOUT_START else LAYOUT_END
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        val direction = calculateScrollDirectionForPosition(targetPosition)
        val outVector = PointF()
        if (direction == 0) {
            return null
        }
        if (orientation == HORIZONTAL) {
            outVector.x = direction.toFloat()
            outVector.y = 0f
        } else {
            outVector.x = 0f
            outVector.y = direction.toFloat()
        }
        return outVector
    }

    /**
     * @author chensuilun
     */
    internal inner class State(val itemsFrames: SparseArray<Rect> = SparseArray<Rect>(), var scrollDelta: Int = 0) {
        fun putOrSet(index: Int, frameRect: Rect) {
            if (itemsFrames.get(index) == null) {
                itemsFrames.put(index, frameRect)
            } else {
                itemsFrames.get(index).set(frameRect)
            }
        }
    }


    override fun canScrollHorizontally(): Boolean {
        return orientation == HORIZONTAL
    }


    override fun canScrollVertically(): Boolean {
        return orientation == VERTICAL
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // When dx is positive，finger fling from right to left(←)，scrollX+
        if (childCount == 0 || dx == 0) {
            return 0
        }
        var delta = -dx
        val parentCenter = (orientationHelper.endAfterPadding - orientationHelper.startAfterPadding) / 2 + orientationHelper.startAfterPadding
        val child: View
        if (dx > 0) {
            //If we've reached the last item, enforce limits
            if (getPosition(getChildAt(childCount - 1)) == itemCount - 1) {
                child = getChildAt(childCount - 1)
                delta = -Math.max(0, Math.min(dx, (child.right - child.left) / 2 + child.left - parentCenter))
            }
        } else {
            //If we've reached the first item, enforce limits
            if (firstVisiblePosition == 0) {
                child = getChildAt(0)
                delta = -Math.min(0, Math.max(dx, (child.right - child.left) / 2 + child.left - parentCenter))
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollHorizontallyBy: dx:$dx,fixed:$delta")
        }
        this.state.scrollDelta = -delta
        fillCover(recycler, -delta)
        offsetChildrenHorizontal(delta)
        return -delta
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        if (childCount == 0 || dy == 0) {
            return 0
        }
        var delta = -dy
        val parentCenter = (orientationHelper.endAfterPadding - orientationHelper.startAfterPadding) / 2 + orientationHelper.startAfterPadding
        val child: View
        if (dy > 0) {
            //If we've reached the last item, enforce limits
            if (getPosition(getChildAt(childCount - 1)) == itemCount - 1) {
                child = getChildAt(childCount - 1)
                delta = -Math.max(0, Math.min(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter))
            }
        } else {
            //If we've reached the first item, enforce limits
            if (firstVisiblePosition == 0) {
                child = getChildAt(0)
                delta = -Math.min(0, Math.max(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter))
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollVerticallyBy: dy:$dy,fixed:$delta")
        }
        this.state.scrollDelta = -delta
        fillCover(recycler, -delta)
        offsetChildrenVertical(delta)
        return -delta
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val linearSmoothScroller = GallerySmoothScroller(recyclerView!!.context)
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    fun attach(recyclerView: RecyclerView?, selectedPosition: Int = 0) {
        if (recyclerView == null) {
            throw IllegalArgumentException("The attach RecycleView must not null!!")
        }
        this.recyclerView = recyclerView
        initialSelectedPosition = Math.max(0, selectedPosition)
        recyclerView.layoutManager = this
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(innerScrollListener)
    }

    /**
     * @author chensuilun
     */
    class LayoutParams : RecyclerView.LayoutParams {

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }

    /**
     * A ItemTransformer is invoked whenever a attached item is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the item views using animation properties.
     */
    interface ItemTransformer {

        /**
         * Apply a property transformation to the given item.

         * @param layoutManager Current LayoutManager
         * *
         * @param item          Apply the transformation to this item
         *
         * @param viewPosition  The view's position
         * *
         * @param fraction      of page relative to the current front-and-center position of the pager.
         * *                      0 is front and center. 1 is one full
         * *                      page position to the right, and -1 is one page position to the left.
         */
        fun transformItem(layoutManager: GalleryLayoutManager, item: View, viewPosition: Int, fraction: Float)
    }

    /**
     * Listen for changes to the selected item

     * @author chensuilun
     */
    interface OnItemSelectedListener {
        /**
         * @param recyclerView The RecyclerView which item view belong to.
         * *
         * @param item         The current selected view
         * *
         * @param position     The current selected view's position
         */
        fun onItemSelected(recyclerView: RecyclerView?, item: View, position: Int)
    }

    /**
     * Inner Listener to listen for changes to the selected item

     * @author chensuilun
     */
    private inner class InnerScrollListener : RecyclerView.OnScrollListener() {
        internal var mState: Int = 0
        internal var mCallbackOnIdle: Boolean = false

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val snap = snapHelper.findSnapView(recyclerView!!.layoutManager)
            if (snap != null) {
                val selectedPosition = recyclerView.layoutManager.getPosition(snap)
                if (selectedPosition != currentSelectedPosition) {
                    curSelectedView?.let { it.isSelected = false }
                    curSelectedView = snap
                    curSelectedView?.let { it.isSelected = true }
                    currentSelectedPosition = selectedPosition
                    if (!callbackInFling && mState != RecyclerView.SCROLL_STATE_IDLE) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "ignore selection change callback when fling ")
                        }
                        mCallbackOnIdle = true
                        return
                    }
                    onItemSelectedListener?.apply { onItemSelected(recyclerView, snap, currentSelectedPosition) }
                }
            }
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "onScrolled: dx:$dx,dy:$dy")
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            mState = newState
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "onScrollStateChanged: " + newState)
            }
            if (mState == RecyclerView.SCROLL_STATE_IDLE) {
                val snap = snapHelper.findSnapView(recyclerView!!.layoutManager)
                if (snap != null) {
                    val selectedPosition = recyclerView.layoutManager.getPosition(snap)
                    if (selectedPosition != currentSelectedPosition) {
                        curSelectedView?.let { it.isSelected = false }
                        curSelectedView = snap
                        curSelectedView?.let { it.isSelected = true }
                        currentSelectedPosition = selectedPosition
                        onItemSelectedListener?.apply { onItemSelected(recyclerView, snap, currentSelectedPosition) }
                    } else if (!callbackInFling && mCallbackOnIdle) {
                        mCallbackOnIdle = false
                        onItemSelectedListener?.apply { onItemSelected(recyclerView, snap, currentSelectedPosition) }
                    }
                } else {
                    Log.e(TAG, "onScrollStateChanged: snap null")
                }
            }
        }
    }

    /**
     * Implement to support [GalleryLayoutManager.smoothScrollToPosition]
     */
    private inner class GallerySmoothScroller(context: Context) : LinearSmoothScroller(context) {

        /**
         * Calculates the horizontal scroll amount necessary to make the given view in center of the RecycleView

         * @param view The view which we want to make in center of the RecycleView
         * *
         * @return The horizontal scroll amount necessary to make the view in center of the RecycleView
         */
        fun calculateDxToMakeCentral(view: View): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0
            }
            val params = view.layoutParams as RecyclerView.LayoutParams
            val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
            val right = layoutManager.getDecoratedRight(view) + params.rightMargin
            val start = layoutManager.paddingLeft
            val end = layoutManager.width - layoutManager.paddingRight
            val childCenter = left + ((right - left) / 2.0f).toInt()
            val containerCenter = ((end - start) / 2f).toInt()
            return containerCenter - childCenter
        }

        /**
         * Calculates the vertical scroll amount necessary to make the given view in center of the RecycleView

         * @param view The view which we want to make in center of the RecycleView
         * *
         * @return The vertical scroll amount necessary to make the view in center of the RecycleView
         */
        fun calculateDyToMakeCentral(view: View): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollVertically()) {
                return 0
            }
            val params = view.layoutParams as RecyclerView.LayoutParams
            val top = layoutManager.getDecoratedTop(view) - params.topMargin
            val bottom = layoutManager.getDecoratedBottom(view) + params.bottomMargin
            val start = layoutManager.paddingTop
            val end = layoutManager.height - layoutManager.paddingBottom
            val childCenter = top + ((bottom - top) / 2.0f).toInt()
            val containerCenter = ((end - start) / 2f).toInt()
            return containerCenter - childCenter
        }


        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: RecyclerView.SmoothScroller.Action) {
            val dx = calculateDxToMakeCentral(targetView)
            val dy = calculateDyToMakeCentral(targetView)
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
            val time = calculateTimeForDeceleration(distance)
            if (time > 0) {
                action.update(-dx, -dy, time, mDecelerateInterpolator)
            }
        }
    }

    companion object {
        const private val TAG = "GalleryLayoutManager"
        const internal val LAYOUT_START = -1

        const internal val LAYOUT_END = 1

        const val HORIZONTAL = OrientationHelper.HORIZONTAL

        const val VERTICAL = OrientationHelper.VERTICAL
    }
}