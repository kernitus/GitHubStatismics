package main.kotlin

import kweb.CanvasElement
import kweb.plugins.chartJs.ChartJsPlugin
import kweb.state.KVal
import kweb.state.KVar
import kweb.util.random
import kweb.util.toJson
import java.awt.Color
import java.time.Instant
import kotlin.random.Random

abstract class Chart(
    private val canvas: CanvasElement, chartConfig: ChartConfig,
    var loading: KVar<Boolean> = KVar(false)
) {
    private val chartVarName = "c${random.nextInt(10000000)}"

    init {
        canvas.creator?.require(ChartJsPlugin::class)
        canvas.execute("$chartVarName = new Chart(${canvas.jsExpression}.getContext('2d'), ${chartConfig.toJson()})")
    }

    protected fun update() = canvas.execute("$chartVarName.update()")

    protected fun setData(data: ChartData) =
        canvas.execute("${chartVarName}.data = ${data.toJson()}")

    protected fun setDataListener(data: KVal<ChartData>) {
        data.addListener { _, newData ->
            loading.value = false
            setData(newData)
            update()
        }
    }
}

class PieChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.pie, data)) {

    constructor(canvas: CanvasElement, data: KVal<PieData>) : this(canvas, data.value.toChartData()) {
        data.addListener { _, newData ->
            loading.value = false
            setData(newData.toChartData())
            update()
        }
    }

    data class PieData(val labels: Collection<String>, val dataLists: Collection<DataList>) {
        fun toChartData() = ChartData(labels, dataLists.map { dataSetFromDataList(it) })
    }

}

class LineChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.line, data)) {

    constructor(canvas: CanvasElement, data: KVal<ChartData>) : this(canvas, data.value) {
        super.setDataListener(data)
    }
}

class ScatterChart(canvas: CanvasElement, data: ChartData? = null) :
    Chart(canvas, ChartConfig(ChartType.scatter, data)) {

    constructor(canvas: CanvasElement, data: KVal<ChartData>) : this(canvas, data.value) {
        super.setDataListener(data)
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
    return PieDataSet(dataList = dataList, backgroundColours = colours)
}

data class ChartConfig(val type: ChartType, val data: ChartData?)

enum class ChartType {
    bar, line, pie, radar, polar, bubble, scatter, area
}

data class ChartData(
    val labels: Collection<String>? = null,
    val datasets: Collection<DataSet>
)

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
    val label: String? = null,
    dataList: DataList,
    backgroundColour: Color? = randomColour(),
    val order: Int? = null
) : DataSet {
    override val data = dataList.list
    val backgroundColor = backgroundColour?.toRgbString()
}

class ScatterDataSet(
    val label: String? = null,
    dataList: DataList.Points,
    backgroundColour: Color? = randomColour(),
    val order: Int? = null
) : DataSet {
    override val data = dataList.list
    val backgroundColor = backgroundColour?.toRgbString()
}

fun Color.toRgbString(): String = "rgba(${red}, ${green}, ${blue}, 0.7)"

data class Point(val x: Number, val y: Number)
data class DatePoint(val x: Instant, val y: Number)

sealed class DataList(val list: Collection<Any>) {
    class Numbers(numbers: Collection<Number>) : DataList(numbers)
    class Points(points: Collection<Point>) : DataList(points)
}


