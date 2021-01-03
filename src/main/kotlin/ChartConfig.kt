package main.kotlin

import kweb.CanvasElement
import kweb.plugins.chartJs.ChartJsPlugin
import kweb.state.KVal
import kweb.state.KVar
import kweb.util.random
import kweb.util.toJson
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

abstract class Chart(
    private val canvas: CanvasElement, chartConfig: ChartConfig, dataKVal: KVal<out ChartData>? = null
) {
    private val chartVarName = "c${random.nextInt(10000000)}"
    val loading: KVar<Boolean> = KVar(false)

    init {
        canvas.creator?.require(ChartJsPlugin::class)
        canvas.execute("$chartVarName = new Chart(${canvas.jsExpression}.getContext('2d'), ${chartConfig.toJson()})")
        if (dataKVal != null) setDataListener(dataKVal)
    }

    private fun update() = canvas.execute("$chartVarName.update()")
    private fun setData(data: ChartData) = canvas.execute("${chartVarName}.data = ${data.toJson()}")
    private fun setDataListener(data: KVal<out ChartData>) {
        data.addListener { _, newData ->
            loading.value = false
            setData(newData)
            update()
        }
    }
}

//////////////////////////////// Chart classes //////////////////////////////////

class PieChart(canvas: CanvasElement, dataKVal: KVal<PieChartData>? = null, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.pie, data), dataKVal) {

    companion object {
        fun dataSetFromDataList(dataList: DataList): DataSet {
            val colours: Array<Color> = Array(dataList.list.size) { randomColour() }
            return PieChart.PieDataSet(dataList = dataList, backgroundColours = colours)
        }
    }

    class PieChartData(
        override val labels: Collection<String>, dataLists: Collection<DataList>,
    ) : ChartData {
        override val datasets: Collection<DataSet> = dataLists.map { dataSetFromDataList(it) }
    }

    class PieDataSet(
        val label: String? = null,
        dataList: DataList,
        backgroundColours: Array<Color>? = null,
        borderColours: Array<Color>? = null,
    ) : DataSet {
        override val data = dataList.list
        val backgroundColor: List<String>? = backgroundColours?.map { it.toRgbString() }
        val borderColor: List<String>? = borderColours?.map { it.toRgbString() }
    }
}

class LineChart(canvas: CanvasElement, dataKVal: KVal<LineChartData>? = null, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.line, data), dataKVal) {

    class LineChartData(
        override val labels: Collection<String>? = null, override val datasets: Collection<LineDataSet>
    ) : ChartData

    class LineDataSet(
        val label: String? = null, dataList: DataList, backgroundColour: Color? = randomColour(), val order: Int? = null
    ) : DataSet {
        override val data = dataList.list
        val backgroundColor = backgroundColour?.toRgbString()
    }
}

class ScatterChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.scatter, data)) {

    class ScatterDataSet(
        val label: String? = null, dataList: DataList.Points, backgroundColour: Color? = randomColour(),
        val order: Int? = null
    ) : DataSet {
        override val data = dataList.list
        val backgroundColor = backgroundColour?.toRgbString()
    }
}

class BarChart(canvas: CanvasElement, dataKVal: KVal<BarChartData>?, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.bar, data), dataKVal) {

    class BarChartData(
        override val labels: Collection<String> = emptyList(), override val datasets: Collection<BarDataSet>
    ) : ChartData

    class BarDataSet(
        val label: String? = null, val backgroundColour: Color? = randomColour(), override val data: Collection<Any>
    ) : DataSet {
        val backgroundColor = backgroundColour?.toRgbString()
    }
}

class StackedBarChart(canvas: CanvasElement, dataKVal: KVal<StackedBarChartData>?, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.bar, data, StackedBarChartOptions()), dataKVal) {

    class StackedBarChartData(
        override val labels: Collection<String> = emptyList(), override val datasets: Collection<StackedBarDataSet>
    ) : ChartData

    class StackedBarDataSet(
        val label: String? = null, dataList: DataList, backgroundColour: Color? = randomColour(), val stack: String,
        val order: Int? = null
    ) : DataSet {
        override val data = dataList.list
        val backgroundColor = backgroundColour?.toRgbString()
    }

    data class StackedBarChartOptions(
        val scales: ChartScales = ChartScales(listOf(ChartAxesOptions(true)), listOf(ChartAxesOptions(true))
        )
    ) : ChartOptions

}

class StackedTimeBarChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.bar, data, StackedTimeBarChartOptions())) {

    data class StackedTimeBarChartOptions(
        val scales: ChartScales = ChartScales(listOf(ChartAxesOptions(true, "time", time = ChartAxisTime("day"))),
            listOf(ChartAxesOptions(true))
        )
    ) : ChartOptions
}

////////////////////////// ChartOptions ////////////////////////////////////
interface ChartOptions

data class ChartAxisTime(val unit: String)
data class ChartScales(val xAxes: List<ChartAxesOptions>, val yAxes: List<ChartAxesOptions>)
data class ChartAxesOptions(
    val stacked: Boolean? = null, val type: String? = null, val distribution: String? = null,
    val time: ChartAxisTime? = null
)

////////////////////////////////// ChartConfig /////////////////////////////////////////////
data class ChartConfig(val type: ChartType, val data: ChartData?, val options: ChartOptions? = null)

enum class ChartType {
    bar, horizontalBar, line, pie, radar, polar, bubble, scatter, area
}

///////////////////////////////// ChartData ////////////////////////////////////////////////
interface ChartData {
    val labels: Collection<String>?
    val datasets: Collection<DataSet>
}

interface DataSet {
    val data: Collection<Any>
}

data class Point(val x: Number, val y: Number)
class DatePoint(instant: Instant, val y: Number) {
    val x = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.from(ZoneOffset.UTC)).format(instant)
}

sealed class DataList(val list: Collection<Any>) {
    class Numbers(numbers: Collection<Number>) : DataList(numbers)
    class Points(points: Collection<Point>) : DataList(points)
    class DatePoints(points: Collection<DatePoint>) : DataList(points)
}

//////////////////// Utility functions ////////////////////////////////
private fun randomColour(): Color = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255))

fun Color.toRgbString(): String = "rgba(${red}, ${green}, ${blue}, 0.7)"


