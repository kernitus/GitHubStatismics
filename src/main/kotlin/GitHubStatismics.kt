package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import kweb.state.KVar
import mu.KotlinLogging
import org.kohsuke.github.GitHub

fun main() {
    GitHubStatismics()
}

class GitHubStatismics {
    private val logger = KotlinLogging.logger {}

    val plugins = listOf(fomanticUIPlugin)
    val server = Kweb(port = 16097, debug = true, plugins = plugins, buildPage = {

        doc.head {
            title().text("GitHub Statismics")
        }

        doc.body {
            val usernameKVar = KVar("")

            pageBorderAndTitle(usernameKVar) {
                div(fomantic.content) {

                    div(fomantic.ui.action.input) {
                        h2(fomantic.ui.header).text("Please enter a GitHub username: ")
                        val input = input(type = InputType.text, placeholder = "Username")
                        input.value = usernameKVar
                        input.on.keypress { ke ->
                            if (ke.code == "Enter") {
                                handleChooseUsername(input)
                            }
                        }
                        button(fomantic.ui.button).text("Run").apply {
                            on.click {
                                handleChooseUsername(input)
                            }
                        }
                    }
                }
            }
        }
    })

    private fun ElementCreator<*>.pageBorderAndTitle(
        username: KVar<String>,
        content: ElementCreator<DivElement>.() -> Unit
    ) {
        div(fomantic.ui.main.container) {
            div(fomantic.column) {
                div(fomantic.ui.vertical.segment) {
                    div(fomantic.ui.message) {
                        p().innerHTML(
                            """
                            A simple GitHub statistics visualiser. Enter a username below to see some statistics associated with the account.
                            """
                                .trimIndent()
                        )
                    }
                }

                div(fomantic.ui.vertical.segment) {
                    h1(fomantic.ui.dividing.header).text(username)
                    content(this)
                }
            }
        }
    }

    private fun handleChooseUsername(input: InputElement) {
        GlobalScope.launch {
            val newItemText = input.getValue().await()
            println("New username: $newItemText")
            input.setValue("")
        }
    }

    fun getGitHub(): GitHub {
        val github = GitHub.connectAnonymously()

        println("Please enter a GitHub username:")
        val username = readLine()

        val user = github.getUser(username)
        println("Username: ${user.name}")
        println("Bio: ${user.bio}")
        println("Organisations: ${user.organizations}")
        println("Follower count: ${user.followersCount}")
        println("Following count: ${user.followingCount}")

        print("Followers: ")
        user.followers.forEach { follower ->
            if (follower.name != null)
                print("\"${follower.name}\" ")
        }
        println()

        return github
    }
}


