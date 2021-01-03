package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.html.Document
import kweb.plugins.chartJs.chartJs
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import kweb.state.KVar
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpException

fun main() {
    GitHubStatismics()
}

class GitHubStatismics {
    private val github = GitHub.connect()
    private lateinit var document: Document

    private val plugins = listOf(fomanticUIPlugin, chartJs)
    val server = Kweb(port = 16097, debug = true, plugins = plugins, buildPage = {
        document = doc

        doc.head {
            title().text("GitHub Statismics")
        }

        doc.body {
            pageBorderAndTitle()
        }
    })

    private fun ElementCreator<*>.pageBorderAndTitle() {
        div(fomantic.ui.main.container) {
            div(fomantic.column) {
                div(fomantic.ui.vertical.segment) {
                    div(fomantic.ui.message) {
                        p().innerHTML("""
                            A simple GitHub statistics visualiser. Enter a username or repository below to view some statistics.
                            <p>
                            Authentication via a property file ~/.github is necessary. Please see <a href=https://github-api.kohsuke.org/index.html> here </a> for more details.
                            An OAuth Personal Access Token with no additional scopes is recommended.
                            """.trimIndent()
                        )
                    }
                }

                div(fomantic.ui.center.aligned.vertical.segment) {
                    val watchedUser = WatchedUser()
                    val usernameKVar = watchedUser.name
                    usernameInput(watchedUser)

                    div(fomantic.ui.top.attached.tabular.menu) {
                        val statsTab = a(fomantic.item.active)
                        statsTab.setAttributeRaw("data-tab", "one")
                        statsTab.text("Stats")
                        val pieChartsTab = a(fomantic.item)
                        pieChartsTab.setAttributeRaw("data-tab", "two")
                        pieChartsTab.text("Pie Charts")
                        val lineChartsTab = a(fomantic.item)
                        lineChartsTab.setAttributeRaw("data-tab", "three")
                        lineChartsTab.text("Line Charts")
                    }

                    val statsTab = div(fomantic.ui.bottom.attached.tab.segment.active)
                    statsTab.setAttributeRaw("data-tab", "one")
                    statsTab.new {
                        watchedUser.show.addListener { _, show ->
                            if (show) {
                                h1(fomantic.ui.centered.dividing.header).text(usernameKVar)
                                userStatsHeader(watchedUser)
                                userStats(watchedUser)
                            }
                        }
                    }

                    val pieChartsTab = div(fomantic.ui.bottom.attached.tab.segment)
                    pieChartsTab.setAttributeRaw("data-tab", "two")
                    pieChartsTab.new {
                        pieChartsTab(watchedUser)
                    }
                    val lineChartsTab = div(fomantic.ui.bottom.attached.tab.segment)
                    lineChartsTab.setAttributeRaw("data-tab", "three")
                    lineChartsTab.new {
                        lineChartsTab(watchedUser)
                    }

                    // JavaScript needed to make the tabs interactive
                    document.body.execute("${'$'}('.menu .item').tab();")
                }
            }
        }
    }


    //////////////////////// CHARTS  //////////////////////////////////////////////
    private fun pieChart(chartDataKVar: KVar<PieChart.PieChartData>): (CanvasElement) -> Chart =
        { canvas -> PieChart(canvas, chartDataKVar) }

    private fun lineChart(chartDataKVar: KVar<LineChart.LineChartData>): (CanvasElement) -> Chart =
        { canvas -> LineChart(canvas, chartDataKVar) }

    private fun ElementCreator<*>.chartContainer(
        chartId: String, label: String, loading: KVar<Boolean>, chartFunc: (CanvasElement) -> Chart,
        sizeClass: String = "eight"
    ) {
        val column = div(fomantic)
        column.addClasses(sizeClass, "wide", "column")
        column.new {
            val segment = div(fomantic.ui.disabled.center.aligned.segment)
            segment.new {
                label(fomantic.ui.horizontal.label).text(label)
                val chart = chartFunc(canvas(mapOf("id" to chartId), 400, 400))
                chart.loading.addListener { _, isLoading ->
                    segment.removeClasses("disabled")
                    if (isLoading) segment.addClasses("loading")
                    else segment.removeClasses("loading")
                }
                loading.addListener { _, isLoading -> chart.loading.value = isLoading }
            }
        }
    }

    private fun ElementCreator<*>.pieChartsTab(watchedUser: WatchedUser) {
        div(fomantic.ui.centered.grid) {
            chartContainer("languageBytes", "Languages by amount of bytes", watchedUser.loading,
                pieChart(watchedUser.languageBytesData)
            )
            chartContainer("sizePerRepo", "Size per repo", watchedUser.loading, pieChart(watchedUser.repoSizeData))
            chartContainer("forksPerRepo", "Amount of forks per repo", watchedUser.loading,
                pieChart(watchedUser.forksCountPerRepoData)
            )
            chartContainer("stargazersRepo", "Amount of stargazers per repo", watchedUser.loading,
                pieChart(watchedUser.stargazersPerRepoData)
            )
            chartContainer("watchersPerRepo", "Amount of watchers per repo", watchedUser.loading,
                pieChart(watchedUser.watchersPerRepoData)
            )
            chartContainer("openIssuesPerRepo", "Amount of open issues per repo", watchedUser.loading,
                pieChart(watchedUser.openIssuesPerRepoData)
            )
        }
    }

    private fun ElementCreator<*>.lineChartsTab(watchedUser: WatchedUser) {
        div(fomantic.ui.centered.grid) {
            chartContainer("commitsPerWeek", "Amount of commits per week for all users for the past year",
                watchedUser.loading, lineChart(watchedUser.commitsPerWeek), "ten"
            )
            chartContainer("commitsPerWeekAggregate", "Amount of commits per week for the past year",
                watchedUser.loading, lineChart(watchedUser.commitsPerWeekAggregate), "ten"
            )
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun ElementCreator<*>.usernameInput(watchedUser: WatchedUser) {
        div(fomantic.ui.centered.action.input) {
            val input = input(type = InputType.text, placeholder = "Username")
            input.on.keypress { if (it.code == "Enter") handleChooseUsername(input, watchedUser) }
            button(fomantic.ui.button).text("Load").apply {
                on.click {
                    handleChooseUsername(input, watchedUser)
                }
            }
        }
    }

    private fun getUser(username: String) = github.getUser(username)

    private fun ElementCreator<*>.userStatsHeader(watchedUser: WatchedUser) {
        val segment = div(fomantic.ui.disabled.center.aligned.segment)
        segment.new {
            div(fomantic.ui.four.column.centered.grid) {
                div(fomantic.row) {
                    div(fomantic.column) {
                        val link = a()
                        link.new {
                            val image = img(fomantic.ui.medium.circular.image)
                            image.setAttribute("src", watchedUser.avatarUrl)
                        }
                        link.setAttribute("href", watchedUser.pageUrl)
                    }
                }
                div(fomantic.row) {
                    div(fomantic.column) {
                        // Bio
                        div(fomantic.ui.teal.horizontal.label).text("Bio")
                        span().text(watchedUser.bio)
                    }
                }
                div(fomantic.row) {
                    div(fomantic.column) {
                        // Location
                        div(fomantic.ui.teal.horizontal.label).text("Location")
                        span().text(watchedUser.location)
                    }
                }
            }
        }
        watchedUser.areImageBioLocationLoading.addListener { _, isLoading ->
            segment.removeClasses("disabled")
            if (isLoading) segment.addClasses("loading")
            else segment.removeClasses("loading")
        }
    }

    private fun ElementCreator<*>.userStats(watchedUser: WatchedUser) {
        div(fomantic.ui.centered.grid) {
            div(fomantic.four.wide.column) {
                followsTable(watchedUser)
            }
            div(fomantic.four.wide.column) {
                followersTable(watchedUser)
            }
            div(fomantic.four.wide.column) {
                repositoriesTable(watchedUser)
            }
        }
    }

    private fun ElementCreator<*>.followersTable(watchedUser: WatchedUser) {
        thead().new {
            tr().new {
                th().new {
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.followersCount)
                    span().text("Followers")
                    val loader = div(fomantic.ui.loader.inline.small.double)
                    watchedUser.isFollowersLoading.addListener { _, isLoading ->
                        if (isLoading) loader.addClasses("active")
                        else loader.removeClasses("active")
                    }
                }
            }
        }
        val tableBody = tbody()

        tableBody.new {
            watchedUser.followers.addListener { _, ghPersonSet ->
                tableBody.removeChildren() // Clear all rows
                ghPersonSet.forEach { follower ->
                    tr().new {
                        td().new {
                            div(fomantic.ui.image.label).new {
                                val link = a()
                                link.setAttributeRaw("href", follower.htmlUrl)
                                link.new {
                                    img(mapOf("src" to follower.avatarUrl))
                                }
                                a().new {
                                    span().text(follower.name ?: follower.login)
                                        .apply { on.click { setWatchedUser(follower.login, watchedUser) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementCreator<*>.repositoriesTable(watchedUser: WatchedUser) {
        thead().new {
            tr().new {
                th().new {
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.repositoriesCount)
                    span().text("Repositories")
                    val loader = div(fomantic.ui.loader.inline.small.double)
                    watchedUser.isRepositoriesLoading.addListener { _, isLoading ->
                        if (isLoading) loader.addClasses("active")
                        else loader.removeClasses("active")
                    }
                }
            }
        }
        val tableBody = tbody()

        tableBody.new {
            watchedUser.repositories.addListener { _, repositoriesList ->
                tableBody.removeChildren() // Clear all rows
                repositoriesList.forEach { repo ->
                    tr().new {
                        td().new {
                            val link = a(fomantic.ui.image.label).text(repo.name)
                            link.setAttributeRaw("href", repo.htmlUrl)
                        }
                    }
                }
            }
        }
    }


    private fun ElementCreator<*>.followsTable(watchedUser: WatchedUser) {
        thead().new {
            tr().new {
                th().new {
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.followingCount)
                    span().text("Follows")
                    val loader = div(fomantic.ui.loader.inline.small.double)
                    watchedUser.isFollowsLoading.addListener { _, isLoading ->
                        if (isLoading) loader.addClasses("active")
                        else loader.removeClasses("active")
                    }
                }
            }
        }
        val tableBody = tbody()

        tableBody.new {
            watchedUser.follows.addListener { _, ghPersonSet ->
                tableBody.removeChildren() // Clear all rows
                ghPersonSet.forEach { follows ->
                    tr().new {
                        td().new {
                            div(fomantic.ui.image.label).new {
                                val link = a()
                                link.setAttributeRaw("href", follows.htmlUrl)
                                link.new {
                                    img(mapOf("src" to follows.avatarUrl))
                                }
                                a().new {
                                    span().text(follows.name ?: follows.login)
                                        .apply { on.click { setWatchedUser(follows.login, watchedUser) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleChooseUsername(input: InputElement, watchedUser: WatchedUser) {
        GlobalScope.launch {
            val username = input.getValue().await()
            input.setValue("")
            setWatchedUser(username, watchedUser)
        }
    }

    private fun setWatchedUser(username: String, watchedUser: WatchedUser) {
        GlobalScope.launch {
            try {
                val user = getUser(username)
                watchedUser.setValuesFromGHUser(user)
            } catch (e: HttpException) {
                sendToast("error", "Http Exception", "Something went wrong loading the user!", "white")
                e.printStackTrace()
            } catch (e: Exception) {
                //TODO 404 is FileNotFoundException, IOException throws an HttpException
                sendToast("error", "User load error", "Something went wrong loading the user!", "white")
                e.printStackTrace()
            }
        }
    }

    private fun sendToast(className: String, title: String, message: String, progressColour: String) {
        document.body.execute(
            "$('body').toast({showProgress: 'bottom', classProgress: 'i$progressColour', message: '$message', class: '$className', title: '$title'});"
        )
    }
}


