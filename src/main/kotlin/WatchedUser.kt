package main.kotlin

import kweb.state.KVar
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
    var languageBytesData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var forksCountPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
    var stargazersPerRepoData: KVar<PieChart.PieData> = KVar(PieChart.PieData(emptyList(), emptyList())),
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

        // Grab only the first 30 of each of these to save on API calls
        val followersIterable = user.listFollowers().withPageSize(30).iterator()
        followers.value = if (followersIterable.hasNext()) followersIterable.nextPage() else emptyList()
        val followsIterable = user.listFollows().withPageSize(30).iterator()
        follows.value = if (followsIterable.hasNext()) followsIterable.nextPage() else emptyList()
        val repositoriesIterable = user.listRepositories().withPageSize(30).iterator()
        repositories.value = if (repositoriesIterable.hasNext()) repositoriesIterable.nextPage() else emptyList()

        // Language by amount of bytes
        val languageBytesMap: MutableMap<String, Long> = mutableMapOf()
        var totalBytes = 0L
        repositories.value.forEach { repo ->
            val langMap: Map<String, Long> = repo.listLanguages() // Explicit type needed due to casting issues
            langMap.forEach { (lang, amount) ->
                languageBytesMap[lang] = languageBytesMap[lang]?.plus(amount) ?: 0L
                totalBytes += amount
            }
        }
        val languageData: MutableList<DataList> = mutableListOf(DataList.Numbers(languageBytesMap.values))
        languageBytesData.value = PieChart.PieData(languageBytesMap.keys, languageData)

        pieDataFromProperty(forksCountPerRepoData, GHRepository::getForksCount)
        pieDataFromProperty(stargazersPerRepoData, GHRepository::getStargazersCount)
    }

    private fun pieDataFromProperty(dataKVar: KVar<PieChart.PieData>, property: (GHRepository) -> Int) {
        val valueMap: MutableMap<String, Int> = mutableMapOf()
        repositories.value.filter { property(it) > 0 }.forEach { valueMap[it.name] = property(it) }
        dataKVar.value = PieChart.PieData(valueMap.keys, mutableListOf(DataList.Numbers(valueMap.values)))
    }

}

