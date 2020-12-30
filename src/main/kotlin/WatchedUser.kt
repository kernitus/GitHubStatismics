package main.kotlin

import kweb.state.KVar
import org.kohsuke.github.GHPersonSet
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser
import java.net.URL

data class WatchedUser(
    var show: KVar<Boolean> = KVar(false),
    var name: KVar<String> = KVar(""),
    var bio: KVar<String> = KVar(""),
    var location: KVar<String> = KVar(""),
    var avatarUrl: KVar<String> = KVar(""),
    var followers: KVar<GHPersonSet<GHUser>> = KVar(GHPersonSet()),
    var follows: KVar<GHPersonSet<GHUser>> = KVar(GHPersonSet()),
    var repositories: KVar<Map<String, GHRepository>> = KVar(emptyMap()),
    var pageUrl: KVar<URL> = KVar(URL("http://0.0.0.0:16097"))
) {
    public fun setValuesFromGHUser(user: GHUser) {
        show.value = true
        name.value = user.name ?: user.login
        bio.value = user.bio ?: ""
        location.value = user.location ?: ""
        pageUrl.value = user.htmlUrl ?: URL("http://0.0.0.0:16097")
        avatarUrl.value = user.avatarUrl ?: ""
        followers.value = user.followers ?: GHPersonSet()
        follows.value = user.follows ?: GHPersonSet()
        repositories.value = user.repositories ?: emptyMap()
    }
}
