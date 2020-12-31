package main.kotlin

import kweb.CanvasElement
import kweb.plugins.chartJs.ChartJsPlugin
import kweb.util.random
import kweb.util.toJson
import java.awt.Color
import java.time.Instant
import kotlin.random.Random

class Chart(private val canvas: CanvasElement, chartConfig: ChartConfig) {
    private val chartVarName = "c${random.nextInt(10000000)}"

    init {
        canvas.creator?.require(ChartJsPlugin::class)
        canvas.execute("$chartVarName = new Chart(${canvas.jsExpression}.getContext('2d'), ${chartConfig.toJson()})")
    }

    private fun update() = canvas.execute("$chartVarName.update()")

    fun setNewData(datasets: List<DataSet>) {
        canvas.execute("${chartVarName}.data.datasets = ${datasets.toJson()}")
        update()
    }

    fun setPieData(map: Map<String, Number>) {
        canvas.execute("${chartVarName}.data.labels = ${map.keys.toJson()}")
        val dataSets: List<DataSet> = listOf(dataSetFromList(map.values.toTypedArray()))
        setNewData(dataSets)
    }
}

data class ChartConfig(val type: ChartType, val data: ChartData)

private fun dataSetFromList(list: Array<out Number>): DataSet {
    val colours: Array<Color> = Array(list.size) {
        Color(
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255)
        )
    }
    return DataSet(dataList = DataList.Numbers(*list), backgroundColours = colours)
}

enum class ChartType {
    bar, line, pie, radar, polar, bubble, scatter, area
}

data class ChartData(
    val labels: List<String>,
    val datasets: List<DataSet>
)

class DataSet(
    val label: String? = null,
    dataList: DataList,
    backgroundColours: Array<Color>? = null,
    borderColours: Array<Color>? = null,
    val type: ChartType? = null
) {
    val data: Array<out Any> = dataList.list
    val backgroundColor: List<String>? = backgroundColours?.map { it.toRgbString() }
    val borderColor: List<String>? = borderColours?.map { it.toRgbString() }
}

fun Color.toRgbString(): String = "rgb(${red}, ${green}, ${blue})"

data class Point(val x: Number, val y: Number)
data class DatePoint(val x: Instant, val y: Number)

sealed class DataList(val list: Array<out Any>) {
    class Numbers(vararg numbers: Number) : DataList(numbers)
    class Points(vararg points: Point) : DataList(points)
}


