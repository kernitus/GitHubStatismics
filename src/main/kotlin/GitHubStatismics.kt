package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
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
                    val userKVar = KVar(WatchedUser())
                    usernameInput(usernameKVar, userKVar)
                    h1(fomantic.ui.dividing.header).text(usernameKVar)
                    userStatsTable(userKVar)
                }
            }
        }
    }

    private fun ElementCreator<*>.usernameInput(usernameKVar: KVar<String>, userKVar: KVar<WatchedUser>) {
        div(fomantic.content) {
            div(fomantic.ui.action.input) {
                h2(fomantic.ui.header).text("Please enter a GitHub username: ")
                val input = input(type = InputType.text, placeholder = "Username")
                input.value = usernameKVar
                input.on.keypress { ke ->
                    if (ke.code == "Enter") {
                        handleChooseUsername(input, userKVar)
                    }
                }
                button(fomantic.ui.button).text("Run").apply {
                    on.click {
                        handleChooseUsername(input, userKVar)
                    }
                }
            }
        }
    }

    private fun getUser(username: String) = github.getUser(username)

    data class WatchedUser(
        var name: String = "",
        var bio: String = "",
        var location: String = "",
        var hireable: Boolean = false,
        var followers: String = "",
        var follows: String = "",
        var repositories: String = ""
    )

    private fun ElementCreator<*>.userStatsTable(user: KVar<WatchedUser>) {

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
                    td().text(user.property(WatchedUser::name))
                }
                tr().new {
                    td().text("Bio")
                    td().text(user.property(WatchedUser::bio))
                }
                tr().new {
                    td().text("Location")
                    td().text(user.property(WatchedUser::location))
                }
                tr().new {
                    td().text("Is Hireable")
                    td().text(user.property(WatchedUser::hireable).value.toString())
                }
                tr().new {
                    td().text("Followers")
                    td().text(user.property(WatchedUser::followers))
                }
                tr().new {
                    td().text("Follows")
                    td().text(user.property(WatchedUser::follows))
                }
                tr().new {
                    td().text("Repositories")
                    td().text(user.property(WatchedUser::repositories))
                }
            }
        }
    }

    private fun handleChooseUsername(input: InputElement, userKVar: KVar<WatchedUser>) {
        GlobalScope.launch {
            val username = input.getValue().await()
            input.setValue("")

            val user = getUser(username)

            userKVar.property(WatchedUser::name).value = user.name ?: ""
            userKVar.property(WatchedUser::bio).value = user.bio ?: ""
            userKVar.property(WatchedUser::location).value = user.location ?: ""
            userKVar.property(WatchedUser::hireable).value = user.isHireable

            var followers = ""
            user.followers.forEach{followers += "${it.name?: ""} "}
            userKVar.property(WatchedUser::followers).value = followers

            var follows = ""
            user.follows.forEach{follows += "${it.name?: ""} "}
            userKVar.property(WatchedUser::follows).value = follows

            var repositories = ""
            user.repositories.keys.forEach{repositories += "$it " }
            userKVar.property(WatchedUser::repositories).value = repositories
        }
    }
}


