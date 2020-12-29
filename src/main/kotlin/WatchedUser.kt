package main.kotlin

import kweb.state.KVar
import org.kohsuke.github.GHPersonSet
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser

data class WatchedUser(
    var show: KVar<Boolean> = KVar(false),
    var name: KVar<String> = KVar(""),
    var bio: KVar<String> = KVar(""),
    var location: KVar<String> = KVar(""),
    var avatarUrl: KVar<String> = KVar(""),
    var followers: KVar<GHPersonSet<GHUser>> = KVar(GHPersonSet<GHUser>()),
    var follows: KVar<GHPersonSet<GHUser>> = KVar(GHPersonSet<GHUser>()),
    var repositories: KVar<Map<String, GHRepository>> = KVar(emptyMap())
)
