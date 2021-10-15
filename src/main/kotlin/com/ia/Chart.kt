package com.ia

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.ui.ApplicationFrame
import org.jfree.chart.ui.UIUtils
import org.jfree.data.xy.XYDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension

class Chart(title: String?) : ApplicationFrame(title) {
    fun showChart(fitnesses: List<Int>) {
        val s1 = XYSeries("Aptitud")
        fitnesses.forEachIndexed { index, fitness -> s1.add(index.toDouble(), fitness.toDouble()) }

        val dataset = XYSeriesCollection()
        dataset.addSeries(s1)

        val chart = createChart(dataset)

        val panel = ChartPanel(chart, false)
        panel.fillZoomRectangle = true
        panel.isMouseWheelEnabled = true
        panel.preferredSize = Dimension(500, 270)

        contentPane = panel

        pack()
        UIUtils.centerFrameOnScreen(this)
        isVisible = true
    }

    companion object {
        private fun createChart(dataset: XYDataset): JFreeChart {
            val chart = ChartFactory.createXYLineChart(
                "Mejor aptitud por corrida",  // title
                "Corrida",  // x-axis label
                "Aptitud",  // y-axis label
                dataset
            )

            chart.backgroundPaint = Color.WHITE

            val plot = chart.plot as XYPlot
            plot.backgroundPaint = Color.LIGHT_GRAY
            plot.domainGridlinePaint = Color.WHITE
            plot.rangeGridlinePaint = Color.WHITE
//            plot.axisOffset = RectangleInsets(5.0, 5.0, 5.0, 5.0)
            plot.isDomainCrosshairVisible = true
            plot.isRangeCrosshairVisible = true

            plot.renderer.let {
                if (it is XYLineAndShapeRenderer) {
                    it.defaultShapesVisible = false
                    it.defaultShapesFilled = true
                    it.drawSeriesLineAsPath = true

                    it.defaultStroke = BasicStroke(50.0f)
                    it.autoPopulateSeriesFillPaint = false
                }
            }

            return chart
        }
    }
}
