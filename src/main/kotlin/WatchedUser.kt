package main.kotlin

import kweb.state.KVar
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser
import java.net.URL

data class WatchedUser(
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
    var languagesPieChartData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var commitsPerRepoPieChartData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
) {
    fun setValuesFromGHUser(user: GHUser) {
        show.value = true
        name.value = user.name ?: user.login
        bio.value = user.bio ?: ""
        location.value = user.location ?: ""
        pageUrl.value = user.htmlUrl ?: URL("http://0.0.0.0:16097")
        avatarUrl.value = user.avatarUrl ?: ""
        followersCount.value = user.followersCount.toString()
        followingCount.value = user.followingCount.toString()
        repositoriesCount.value = user.publicRepoCount.toString()

        val followersIterable = user.listFollowers().withPageSize(50).iterator()
        followers.value = if (followersIterable.hasNext()) followersIterable.nextPage() else emptyList()
        val followsIterable = user.listFollows().withPageSize(50).iterator()
        follows.value = if (followsIterable.hasNext()) followsIterable.nextPage() else emptyList()
        val repositoriesIterable = user.listRepositories().withPageSize(50).iterator()
        repositories.value = if (repositoriesIterable.hasNext()) repositoriesIterable.nextPage() else emptyList()

        // Process data for graphs
        val languageBytesMap: MutableMap<String, Long> = mutableMapOf() // language name, amount of bytes
        val commitsPerRepo: MutableMap<GHRepository, Long> = mutableMapOf() // repo, # of commits

        repositories.value.forEach { repo ->
            repo.statistics.codeFrequency
            val langMap: Map<String, Long> = repo.listLanguages()
            langMap.forEach { (lang, amount) ->
                languageBytesMap[lang] = languageBytesMap[lang]?.plus(amount) ?: 0L
            }

            val commits: MutableList<GHCommit> = mutableListOf()
            repo.listCommits().withPageSize(100).iterator().forEachRemaining { commits.add(it) }

            commits.forEach { commit ->
                println(commit.author.id)
                if (commit.author.id == user.id) commitsPerRepo[repo] = commitsPerRepo[repo]?.inc() ?: 0L
            }
        }

        val languageDataList: DataList.Numbers = DataList.Numbers(*languageBytesMap.values.toTypedArray())
        val languageData: MutableList<DataList> = mutableListOf(languageDataList)
        languagesPieChartData.value = PieChart.PieData(languageBytesMap.keys, languageData)
    }
}

