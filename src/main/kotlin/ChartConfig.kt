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

    class PieChartData(
        override val labels: Collection<String>,
        dataLists: Collection<DataList>,
    ) : ChartData {
        override val datasets: Collection<DataSet> = dataLists.map { dataSetFromDataList(it) }
    }

}

class LineChart(canvas: CanvasElement, dataKVal: KVal<LineChartData>? = null, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.line, data), dataKVal) {

    class LineChartData(
        override val labels: Collection<String>? = null, override val datasets: Collection<LineDataSet>
    ) : ChartData
}

class ScatterChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.scatter, data)) {

}

class BarChart(canvas: CanvasElement, data: ChartData? = null) : Chart(canvas, ChartConfig(ChartType.bar, data)) {

}

class StackedBarChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.bar, data, StackedBarChartOptions())) {}

class StackedTimeBarChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.bar, data, StackedTimeBarChartOptions())) {

}

////////////////////////// ChartOptions ////////////////////////////////////
interface ChartOptions
data class StackedBarChartOptions(
    val scales: ChartScales = ChartScales(listOf(ChartAxesOptions(true)), listOf(ChartAxesOptions(true))
    )
) : ChartOptions

data class StackedTimeBarChartOptions(
    val scales: ChartScales = ChartScales(listOf(ChartAxesOptions(true, "time", time = ChartAxisTime("day"))),
        listOf(ChartAxesOptions(true))
    )
) : ChartOptions

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

class LineDataSet(
    val label: String? = null, dataList: DataList, backgroundColour: Color? = randomColour(), val order: Int? = null
) : DataSet {
    override val data = dataList.list
    val backgroundColor = backgroundColour?.toRgbString()
}

class ScatterDataSet(
    val label: String? = null, dataList: DataList.Points, backgroundColour: Color? = randomColour(),
    val order: Int? = null
) : DataSet {
    override val data = dataList.list
    val backgroundColor = backgroundColour?.toRgbString()
}

class StackedBarDataSet(
    val label: String? = null, dataList: DataList, backgroundColour: Color? = randomColour(), val stack: String,
    val order: Int? = null
) : DataSet {
    override val data = dataList.list
    val backgroundColor = backgroundColour?.toRgbString()
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

private fun dataSetFromDataList(dataList: DataList): DataSet {
    val colours: Array<Color> = Array(dataList.list.size) { randomColour() }
    return PieDataSet(dataList = dataList, backgroundColours = colours)
}

fun Color.toRgbString(): String = "rgba(${red}, ${green}, ${blue}, 0.7)"


