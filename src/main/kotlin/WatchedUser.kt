package main.kotlin

import kweb.state.KVar
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser
import java.net.URL

data class WatchedUser(
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

    // Graphs variables
    var languageBytesData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var forksCountPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var stargazersPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var subscribersPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var openIssuesPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var watchersPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var repoSizeData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var commitsPerWeek: KVar<ChartData> = KVar(ChartData(datasets = emptyList())),
    var commitsSizeScatterData: KVar<ChartData> = KVar(ChartData(datasets = emptyList())),
    var commitsPerWeekAggregate: KVar<ChartData> = KVar(ChartData(datasets = emptyList())),
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
        languageBytesData.value = PieChart.PieData(languageBytesMap.keys, languageData)

        pieDataFromProperty(forksCountPerRepoData, GHRepository::getForksCount)
        pieDataFromProperty(stargazersPerRepoData, GHRepository::getStargazersCount)
        pieDataFromProperty(openIssuesPerRepoData, GHRepository::getOpenIssueCount)
        pieDataFromProperty(subscribersPerRepoData, GHRepository::getSubscribersCount)
        pieDataFromProperty(watchersPerRepoData, GHRepository::getWatchersCount)
        pieDataFromProperty(repoSizeData, GHRepository::getSize)

        val commitsDataSets: MutableList<DataSet> = mutableListOf()

        // Line chart of commits per week for all users
        repositories.value.filter { it.statistics.participation.allCommits.any { it > 0 } }.forEach { repo ->
            commitsDataSets.add(
                LineDataSet(
                    label = repo.name,
                    dataList = DataList.Numbers(repo.statistics.participation.allCommits),
                )
            )
        }
        val commitsLabels: MutableList<String> = mutableListOf()
        for (i in 1..53) commitsLabels.add("Week $i")
        commitsPerWeek.value = ChartData(labels = commitsLabels, datasets = commitsDataSets)

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
            LineDataSet(
                label = "Total",
                dataList = DataList.Numbers(weeklyCommitsAll),
                order = 0,
                backgroundColour = null
            ),
            LineDataSet(label = name.value, dataList = DataList.Numbers(weeklyCommitsOwner), order = 1)
        )
        commitsPerWeekAggregate.value = ChartData(labels = commitsLabels, datasets = weeklyCommitsDataSets)

        // Scatter chart of number of commits over repo size
        /*
        val scatterDataPoints = mutableListOf<Point>()
        repositories.value.forEach { repo ->
            val commitAmount = repo.statistics.participation.allCommits.sum()
            var additions = 0L
            var deletions = 0L
            repo.statistics.codeFrequency.forEach { freq ->
                additions += freq.additions
                deletions += freq.deletions
            }
            val size = deletions - additions
            scatterDataPoints.add(Point(size,commitAmount))
        }
        commitsSizeScatterData.value = ChartData(datasets = listOf(
        ScatterDataSet(
            label = "repi",
            dataList = DataList.Points(scatterDataPoints)
        )))
        */

        // Done loading
        loading.value = false
    }

    private fun pieDataFromProperty(dataKVar: KVar<PieChart.PieData>, property: (GHRepository) -> Int) {
        val valueMap: MutableMap<String, Int> = mutableMapOf()
        repositories.value.filter { property(it) > 0 }.forEach { valueMap[it.name] = property(it) }
        dataKVar.value = PieChart.PieData(valueMap.keys, mutableListOf(DataList.Numbers(valueMap.values)))
    }
}

