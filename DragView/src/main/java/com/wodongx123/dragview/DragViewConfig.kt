package com.wodongx123.dragview

/**
 * dragView配置类
 */
class DragViewConfig(
    /** 判断是否有滑动的阈值 */
    var moveInterval: Int = 5,
    /** 视图能否出界，出界部分看不见 */
    var canOutOfEdge: Boolean = false,
    /** 能否拖动 */
    var enableMove : Boolean = true,
    /** 贴边设置，为空时不贴边 */
    var alignConfig: DragAlignConfig? = null,
    /**
     * 自定义边缘值，单位PX
     * 不需要限制的部分传[DragViewContainer.INVALID]
     * 这个边缘值会同时对[canOutOfEdge]和[alignConfig]生效
     * 上下左右的边缘对应minx,miny,maxX,maxY
     */
    var customEdge : DragPosition? = null,
    /**
     * 测量配置
     */
    var measureConfig: MeasureConfig? = MeasureConfig()
)

/**
 * 贴边配置，设置冲突时的优先级为
 * [alignToHorizonSide] > [forceRight] > [forceLeft]
 * [alignToVerticalSide] > [forceTop] > [forceBottom]
 */
class DragAlignConfig(
    /** 拖动结束后是否自动贴边X轴，根据是否超越了中间来进行判断 */
    var alignToHorizonSide : Boolean = false,
    /** 拖动结束后是否自动贴边Y轴，根据是否超越了中间来进行判断 */
    var alignToVerticalSide : Boolean = false,
    /** 强制贴到左边 */
    var forceLeft: Boolean = false,
    /** 强制贴到右边 */
    var forceRight: Boolean = false,
    /** 强制贴到上面 */
    var forceTop: Boolean = false,
    /** 强制贴到下面 */
    var forceBottom : Boolean = false,
    /** 动画配置 */
    var animationConfig: AnimationConfig? = null)

/**
 * 贴边动画配置
 * 冲突时优先级：[customAnimation] > [timeMode] > [speedMode]
 */
class AnimationConfig(
    /** 固定时间模式，无论间隔多远都会在固定的时间内贴边 */
    var timeMode: Boolean = false,
    /** 固定速度模式，无论间隔多远都会以固定的速度贴边 */
    var speedMode: Boolean = false,
    /** 固定间隔，单位毫秒 */
    var duration: Long = 1000,
    /** 固定速度，单位px/秒 */
    var fixedSpeed: Int = 200,
    /** 动画的最大值，如果觉得动画很卡就把这个值改大一点 */
    var maxValue: Double = DragViewContainer.MAX_VALUE,
    /** 如果默认的实现没办法满足要求，可以自定义动画，接口会将起点和终点的上下左右传过来 */
    var customAnimation: DragAnimation? = null)


/**
 * 视图宽高测量配置，他会影响数据的判断
 * 冲突时优先级：[customWidth] > [useGetMeasured] > [useGet] > 都不设置时用Measured
 */
class MeasureConfig(
    /**
     * 使用getWidth和getHeight来获取视图宽高
     */
    var useGet: Boolean = false,
    /**
     * 使用getMeasuredWidth和getMeasuredHeight来获取视图宽高
     */
    var useGetMeasured: Boolean = true,
    /**
     * 自定义的视图实际宽度，单位px
     * container是通过MeasureWidth，如果出现了一些宽高方面的异常，只能手动固定宽度了
     */
    var customWidth: Int = DragViewContainer.INVALID,
    /**
     * 自定义的视图实际高度，单位px
     * container是通过MeasureWidth，如果出现了一些宽高方面的异常，只能手动固定高度了
     */
    var customHeight : Int = DragViewContainer.INVALID
)