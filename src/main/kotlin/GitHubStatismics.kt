package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.html.Document
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import org.kohsuke.github.GitHub

fun main() {
    GitHubStatismics()
}

class GitHubStatismics {
    private val github = GitHub.connect()
    private lateinit var document: Document

    private val plugins = listOf(fomanticUIPlugin)
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
                        p().innerHTML(
                            """
                            A simple GitHub statistics visualiser. Enter a username or repository below to view some statistics.
                            <p>
                            Authentication via a property file ~/.github is necessary. Please see <a href=https://github-api.kohsuke.org/index.html> here </a> for more details.
                            An OAuth Personal Access Token with no additional scopes is recommended.
                            """
                                .trimIndent()
                        )
                    }
                }

                div(fomantic.ui.center.aligned.vertical.segment) {
                    val watchedUser = WatchedUser()
                    val usernameKVar = watchedUser.name
                    usernameInput(watchedUser)

                    watchedUser.show.addListener { _, show ->
                        if (show) {
                            h1(fomantic.ui.centered.dividing.header).text(usernameKVar)
                            userStatsHeader(watchedUser)
                            userStats(watchedUser)
                        }
                    }
                }
            }
        }
    }

    private fun ElementCreator<*>.usernameInput(watchedUser: WatchedUser) {
        div(fomantic.ui.centered.action.input) {
            val input = input(type = InputType.text, placeholder = "Search")
            select(fomantic.ui.compact.selection.dropdown) {
                option().text("Username")
                option().text("Repository")
            }
            input.on.keypress { if (it.code == "Enter") handleChooseUsername(input, watchedUser) }
            button(fomantic.ui.button).text("Search").apply {
                on.click {
                    handleChooseUsername(input, watchedUser)
                }
            }
        }
    }

    private fun getUser(username: String) = github.getUser(username)

    private fun ElementCreator<*>.userStatsHeader(watchedUser: WatchedUser) {
        // 1 size 4 column, centre-aligned
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
                            a(fomantic.ui.image.label).text(repo.name)
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
        watchedUser.followers.value = emptyList()
        watchedUser.follows.value = emptyList()
        watchedUser.repositories.value = emptyList()

        try {
            val user = getUser(username)
            watchedUser.setValuesFromGHUser(user)
        } catch (e: Exception) {
            sendToast("error", "User load error", "Something went wrong loading the user!", "white")
            e.printStackTrace()
        }
    }

    private fun sendToast(className: String, title: String, message: String, progressColour: String) {
        document.body.execute(
            "$('body').toast({showProgress: 'bottom', classProgress: 'i$progressColour', message: '$message', class: '$className', title: '$title'});"
        )
    }
}


