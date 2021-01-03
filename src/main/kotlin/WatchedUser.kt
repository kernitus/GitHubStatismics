package main.kotlin

import kweb.state.KVar
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser
import java.net.URL
import java.time.Instant
import java.time.Period

data class WatchedUser(
    // Loading variables
    val loading: KVar<Boolean> = KVar(false),
    val areImageBioLocationLoading: KVar<Boolean> = KVar(false),
    val isFollowsLoading: KVar<Boolean> = KVar(false),
    val isFollowersLoading: KVar<Boolean> = KVar(false),
    val isRepositoriesLoading: KVar<Boolean> = KVar(false),

    var show: KVar<Boolean> = KVar(false),
    var name: KVar<String> = KVar(""),
    var bio: KVar<String> = KVar(""),
    var location: KVar<String> = KVar(""),
    var avatarUrl: KVar<String> = KVar(""),
    var followers: KVar<List<GHUser>> = KVar(emptyList()),
    var follows: KVar<List<GHUser>> = KVar(emptyList()),
    var repositories: KVar<List<GHRepository>> = KVar(emptyList()),
    var pageUrl: KVar<URL> = KVar(URL("http://0.0.0.0:16097")),
    var followersCount: KVar<String> = KVar(""),
    var followingCount: KVar<String> = KVar(""),
    var repositoriesCount: KVar<String> = KVar(""),

    // Pie charts
    var languageBytesData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),
    var forksCountPerRepoData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),
    var stargazersPerRepoData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),
    var openIssuesPerRepoData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),
    var watchersPerRepoData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),
    var repoSizeData: KVar<PieChart.PieChartData> = KVar(PieChart.PieChartData(emptyList(), emptyList())),

    // Line graphs
    var commitsPerWeek: KVar<LineChart.LineChartData> = KVar(LineChart.LineChartData(datasets = emptyList())),
    var commitsPerWeekAggregate: KVar<LineChart.LineChartData> = KVar(LineChart.LineChartData(datasets = emptyList())),

    // Bar charts
    var commitsPerWeekDay: KVar<BarChart.BarChartData> = KVar(BarChart.BarChartData(datasets = emptyList()
    )
    ),
    //var stackedCommitsData: KVar<ChartData> = KVar(StackedBarChartData(datasets = emptyList())),
) {
    fun setValuesFromGHUser(user: GHUser) {
        // Set to loading
        show.value = true
        loading.value = true
        areImageBioLocationLoading.value = true
        isFollowersLoading.value = true
        isFollowsLoading.value = true
        isRepositoriesLoading.value = true

        name.value = user.name ?: user.login
        bio.value = user.bio ?: ""
        location.value = user.location ?: ""
        pageUrl.value = user.htmlUrl ?: URL("http://0.0.0.0:16097")
        avatarUrl.value = user.avatarUrl ?: ""
        areImageBioLocationLoading.value = false

        followersCount.value = user.followersCount.toString()
        followingCount.value = user.followingCount.toString()
        repositoriesCount.value = user.publicRepoCount.toString()

        // Grab only the first few of each of these to save on API calls
        val followersIterable = user.listFollowers().withPageSize(30).iterator()
        followers.value = if (followersIterable.hasNext()) followersIterable.nextPage() else emptyList()
        isFollowersLoading.value = false

        val followsIterable = user.listFollows().withPageSize(30).iterator()
        follows.value = if (followsIterable.hasNext()) followsIterable.nextPage() else emptyList()
        isFollowsLoading.value = false

        val repositoriesIterable = user.listRepositories().withPageSize(50).iterator()
        repositories.value = if (repositoriesIterable.hasNext()) repositoriesIterable.nextPage() else emptyList()
        isRepositoriesLoading.value = false

        // Language by amount of bytes
        val languageBytesMap: MutableMap<String, Long> = mutableMapOf()
        repositories.value.forEach { repo ->
            val langMap: Map<String, Long> = repo.listLanguages() // Explicit type needed due to casting issues
            langMap.forEach { (lang, amount) -> languageBytesMap[lang] = languageBytesMap[lang]?.plus(amount) ?: 0L }
        }
        val languageData: MutableList<DataList> = mutableListOf(DataList.Numbers(languageBytesMap.values))
        languageBytesData.value = PieChart.PieChartData(languageBytesMap.keys, languageData)

        pieDataFromProperty(forksCountPerRepoData, GHRepository::getForksCount)
        pieDataFromProperty(stargazersPerRepoData, GHRepository::getStargazersCount)
        pieDataFromProperty(openIssuesPerRepoData, GHRepository::getOpenIssueCount)
        pieDataFromProperty(watchersPerRepoData, GHRepository::getWatchersCount)
        pieDataFromProperty(repoSizeData, GHRepository::getSize)

        val commitsDataSets: MutableList<LineDataSet> = mutableListOf()

        // Line chart of commits per week for all users
        repositories.value.filter { it.statistics.participation.allCommits.any { it > 0 } }.forEach { repo ->
            commitsDataSets.add(LineDataSet(
                label = repo.name,
                dataList = DataList.Numbers(repo.statistics.participation.allCommits),
            )
            )
        }
        val commitsLabels: MutableList<String> = mutableListOf()
        for (i in 1..53) commitsLabels.add("Week $i")
        commitsPerWeek.value = LineChart.LineChartData(labels = commitsLabels, datasets = commitsDataSets)

        val weeklyCommitsAll = MutableList(52) { 0 }
        val weeklyCommitsOwner = MutableList(52) { 0 }
        repositories.value.filter { it.owner.id == user.id }.forEach { repo ->
            val commitsAll = repo.statistics.participation.allCommits
            val commitsOwner = repo.statistics.participation.ownerCommits
            for (i in commitsAll.indices) {
                weeklyCommitsAll[i] += commitsAll[i]
                weeklyCommitsOwner[i] += commitsOwner[i]
            }
        }
        val weeklyCommitsDataSets = listOf(
            LineDataSet(label = "Total", dataList = DataList.Numbers(weeklyCommitsAll), order = 0,
                backgroundColour = null
            ), LineDataSet(label = name.value, dataList = DataList.Numbers(weeklyCommitsOwner), order = 1)
        )
        commitsPerWeekAggregate.value =
            LineChart.LineChartData(labels = commitsLabels, datasets = weeklyCommitsDataSets)

        // Barchart of commits per weekday
        val commitsWeekDays = MutableList<Long>(7) { 0 } // A slot for each day of the week
        repositories.value.forEach { repo ->
            repo.statistics.commitActivity.toList().forEach { commitActivity ->
                val daysList = commitActivity.days
                // First day is actually Sunday, fix this abomination
                commitsWeekDays[6] = commitsWeekDays[6] + daysList[0]
                for (i in 1..6) commitsWeekDays[i - 1] = commitsWeekDays[i - 1] + daysList[i]
            }
        }


        commitsPerWeekDay.value = BarChart.BarChartData(
            labels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
            datasets = listOf(BarDataSet(data = commitsWeekDays))
        )

        // Stacked charts
        val stackedDataSets = mutableListOf<StackedBarDataSet>()
        stackedDataSets.add(StackedBarDataSet(label = "banana", dataList = DataList.DatePoints(
            listOf(DatePoint(Instant.now(), 15), DatePoint(Instant.now().minus(Period.ofWeeks(2)), 10)
            )
        ), stack = "wekjfhwie"
        )
        )
        stackedDataSets.add(StackedBarDataSet(label = "banana", dataList = DataList.DatePoints(
            listOf(DatePoint(Instant.now(), 15), DatePoint(Instant.now().minus(Period.ofWeeks(2)), 10)
            )
        ), stack = "wqjfiuehu"
        )
        )
        // For each repo, get commits per day
        // Dataset for each repo, with stack = repo name
        /*repositories.value.forEach { repo ->
            val dayActivity: MutableList<DatePoint> = mutableListOf()
            repo.statistics.commitActivity.toList().forEach { commitActivity ->
                // TODO also tally these up for week bar chart
                val weekStart = Instant.ofEpochSecond(commitActivity.week)
                dayActivity.add(DatePoint(weekStart,commitActivity.total))
            }
            val stackedDataList = DataList.DatePoints(dayActivity)
            val stackedDataSet = StackedBarDataSet(label = repo.name, dataList = stackedDataList, stack = repo.name)
            stackedDataSets.add(stackedDataSet)
        }
repositories.value.forEach { repo ->
    val dayActivity: MutableList<DatePoint> = mutableListOf()
    repo.statistics.commitActivity.toList().forEach { commitActivity ->
        // TODO also tally these up for week bar chart
        val weekStart = Instant.ofEpochSecond(commitActivity.week)
        val daysArray = commitActivity.days
        daysArray.forEach { commitCount ->
            val point = DatePoint(weekStart, commitCount)
            dayActivity.add(point)
            weekStart.plus(Period.ofDays(1))
            println("Point (${point.x},${point.y})")
        }
    }
    val stackedDataList = DataList.DatePoints(dayActivity)
    val stackedDataSet = StackedBarDataSet(label = repo.name, dataList = stackedDataList, stack = repo.name)
    stackedDataSets.add(stackedDataSet)
}
 */

        loading.value = false // Finished loading
    }

    private fun pieDataFromProperty(dataKVar: KVar<PieChart.PieChartData>, property: (GHRepository) -> Int) {
        val valueMap: MutableMap<String, Int> = mutableMapOf()
        repositories.value.filter { property(it) > 0 }.forEach { valueMap[it.name] = property(it) }
        dataKVar.value = PieChart.PieChartData(valueMap.keys, mutableListOf(DataList.Numbers(valueMap.values)))
    }
}

