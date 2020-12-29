package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import kweb.state.KVal
import kweb.state.KVar
import kweb.state.property
import mu.KotlinLogging
import org.kohsuke.github.GitHub

fun main() {
    GitHubStatismics()
}

class GitHubStatismics {
    private val logger = KotlinLogging.logger {}
    private val github = GitHub.connect()

    private val plugins = listOf(fomanticUIPlugin)
    val server = Kweb(port = 16097, debug = true, plugins = plugins, buildPage = {

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
                        p().innerHTML(
                            """
                            A simple GitHub statistics visualiser. Enter a username below to see some statistics associated with the account.
                            <p>
                            Authentication via a property file ~/.github is necessary. Please see <a href=https://github-api.kohsuke.org/index.html> here </a> for more details.
                            OAuth Personal Access Token with no extra scopes is recommended.
                            """
                                .trimIndent()
                        )
                    }
                }

                div(fomantic.ui.vertical.segment) {
                    val usernameKVar = KVar("")
                    val watchedUser = WatchedUser()
                    usernameInput(usernameKVar, watchedUser)
                    h1(fomantic.ui.dividing.header).text(usernameKVar)
                    userStatsHeader(watchedUser)
                    userStatsTable(watchedUser)
                }
            }
        }
    }

    private fun ElementCreator<*>.usernameInput(usernameKVar: KVar<String>, watchedUser: WatchedUser) {
        //TODO use search input text field
        //TODO also allow searching for repos https://fomantic-ui.com/elements/input.html
        div(fomantic.content) {
            div(fomantic.ui.action.input) {
                h2(fomantic.ui.header).text("Please enter a GitHub username: ")
                val input = input(type = InputType.text, placeholder = "Username")
                input.value = usernameKVar
                input.on.keypress { ke ->
                    if (ke.code == "Enter") {
                        handleChooseUsername(input, watchedUser)
                    }
                }
                button(fomantic.ui.button).text("Run").apply {
                    on.click {
                        handleChooseUsername(input, watchedUser)
                    }
                }
            }
        }
    }

    private fun getUser(username: String) = github.getUser(username)

    data class WatchedUser(
        var name: KVar<String> = KVar(""),
        var bio: KVar<String> = KVar(""),
        var location: KVar<String> = KVar(""),
        var avatarUrl: KVar<String> = KVar(""),
        var followers: KVar<String> = KVar(""),
        var follows: KVar<String> = KVar(""),
        var repositories: KVar<String> = KVar("")
    )

    private fun ElementCreator<*>.userStatsHeader(watchedUser: WatchedUser){
        div(fomantic.content) {
            val image = img(fomantic.ui.medium.circular.image)
            image.setAttribute("src", watchedUser.avatarUrl)
        }
    }

    private fun ElementCreator<*>.userStatsTable(watchedUser: WatchedUser){

        table(fomantic.ui.celled.table).new {
            thead().new {
                tr().new {
                    th().text("Key")
                    th().text("Value")
                }
            }
            tbody().new {
                tr().new {
                    td().text("Name")
                    td().text(watchedUser.name)
                }
                tr().new {
                    td().text("Bio")
                    td().text(watchedUser.bio)
                }
                tr().new {
                    td().text("Location")
                    td().text(watchedUser.location)
                }
                tr().new {
                    td().text("Followers")
                    td().text(watchedUser.followers)
                }
                tr().new {
                    td().text("Follows")
                    td().text(watchedUser.follows)
                }
                tr().new {
                    td().text("Repositories")
                    td().text(watchedUser.repositories)
                }
            }
        }
    }

    private fun handleChooseUsername(input: InputElement,  watchedUser: WatchedUser) {
        GlobalScope.launch {
            val username = input.getValue().await()
            input.setValue("")

            val user = getUser(username)
            // TODO if user doesn't exist show error

            watchedUser.name.value = user.name
            watchedUser.bio.value = user.bio
            watchedUser.location.value = user.location
            watchedUser.avatarUrl.value = user.avatarUrl

            var followers = ""
            user.followers.forEach{followers += "${it.name?: ""} "}
            watchedUser.followers.value = followers

            var follows = ""
            user.follows.forEach{follows += "${it.name?: ""} "}
            watchedUser.follows.value = follows

            var repositories = ""
            user.repositories.keys.forEach{repositories += "$it " }
            watchedUser.repositories.value = repositories
        }
    }
}


