package main.kotlin

import kweb.CanvasElement
import kweb.plugins.chartJs.ChartJsPlugin
import kweb.state.KVal
import kweb.util.random
import kweb.util.toJson
import java.awt.Color
import java.time.Instant
import kotlin.random.Random

abstract class Chart(private val canvas: CanvasElement, chartConfig: ChartConfig) {
    private val chartVarName = "c${random.nextInt(10000000)}"

    init {
        canvas.creator?.require(ChartJsPlugin::class)
        canvas.execute("$chartVarName = new Chart(${canvas.jsExpression}.getContext('2d'), ${chartConfig.toJson()})")
    }

    protected fun update() = canvas.execute("$chartVarName.update()")

    protected fun setDataSets(datasets: Collection<DataSet>) =
        canvas.execute("${chartVarName}.data.datasets = ${datasets.toJson()}")

    protected fun setLabels(labels: Collection<String>) =
        canvas.execute("${chartVarName}.data.labels = ${labels.toJson()}")

    protected fun setData(data: ChartData) =
        canvas.execute("${chartVarName}.data = ${data.toJson()}")

}

class PieChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.pie, data)) {

    constructor(canvas: CanvasElement, data: KVal<PieData>) : this(canvas, data.value.toChartData()) {
        data.addListener { _, newData ->
            setData(newData.toChartData())
            update()
        }
    }

    data class PieData(val labels: Collection<String>, val dataLists: Collection<DataList>) {
        fun toChartData() = ChartData(labels, dataLists.map { dataSetFromDataList(it) })
    }

}

private fun randomColour(): Color =
    Color(
        Random.nextInt(0, 255),
        Random.nextInt(0, 255),
        Random.nextInt(0, 255)
    )

private fun dataSetFromDataList(dataList: DataList): DataSet {
    val colours: Array<Color> = Array(dataList.list.size) { randomColour() }
    return DataSet(dataList = dataList, backgroundColours = colours)
}

data class ChartConfig(val type: ChartType, val data: ChartData?)

enum class ChartType {
    bar, line, pie, radar, polar, bubble, scatter, area
}

data class ChartData(
    val labels: Collection<String>,
    val datasets: Collection<DataSet>
)

class DataSet(
    val label: String? = null,
    dataList: DataList,
    backgroundColours: Array<Color>? = null,
    borderColours: Array<Color>? = null,
    val type: ChartType? = null
) {
    val data: Collection<Any> = dataList.list
    val backgroundColor: List<String>? = backgroundColours?.map { it.toRgbString() }
    val borderColor: List<String>? = borderColours?.map { it.toRgbString() }
}

fun Color.toRgbString(): String = "rgb(${red}, ${green}, ${blue})"

data class Point(val x: Number, val y: Number)
data class DatePoint(val x: Instant, val y: Number)

sealed class DataList(val list: Collection<Any>) {
    class Numbers(numbers: Collection<Number>) : DataList(numbers)
    class Points(points: Collection<Point>) : DataList(points)
}


