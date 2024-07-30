package com.wodongx123.dragview

object DragViewUtil {
    /**
     * 是否有贴边设置
     */
    fun hasAlignConfig(config: DragViewConfig): Boolean {
        config.alignConfig?.let {
            return hasAlignVerticalConfig(it) || hasAlignHorizonConfig(it)
        }
        return false
    }

    /**
     * 是否有水平贴边设置
     */
    fun hasAlignHorizonConfig(config: DragAlignConfig): Boolean {
        return config.alignToHorizonSide || config.forceLeft || config.forceRight
    }

    /**
     * 是否有垂直贴边设置
     */
    fun hasAlignVerticalConfig(config: DragAlignConfig): Boolean {
        return config.alignToVerticalSide || config.forceTop || config.forceBottom
    }

    /**
     * 是否有贴边动画设置
     */
    fun hasAlignAnimation(alignConfig: DragAlignConfig?): Boolean {
        alignConfig?.animationConfig?.let {
            return (it.timeMode && it.duration > 0) || (it.speedMode || it.fixedSpeed > 0)
        }
        return false
    }
}