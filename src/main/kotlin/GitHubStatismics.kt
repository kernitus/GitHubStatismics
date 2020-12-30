package main.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kweb.*
import kweb.html.Document
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import kweb.state.KVar
import org.kohsuke.github.GHPersonSet
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
                            A simple GitHub statistics visualiser. Enter a username below to see some statistics associated with the account.
                            <p>
                            Authentication via a property file ~/.github is necessary. Please see <a href=https://github-api.kohsuke.org/index.html> here </a> for more details.
                            OAuth Personal Access Token with no extra scopes is recommended.
                            """
                                .trimIndent()
                        )
                    }
                }

                div(fomantic.ui.center.aligned.vertical.segment) {
                    val usernameKVar = KVar("")
                    val watchedUser = WatchedUser()
                    usernameInput(usernameKVar, watchedUser)
                    h1(fomantic.ui.centered.dividing.header).text(usernameKVar)

                    watchedUser.show.addListener { _, show ->
                        if (show) {
                            userStatsHeader(watchedUser)
                            userStats(watchedUser)
                        }
                    }
                }
            }
        }
    }

    private fun ElementCreator<*>.usernameInput(usernameKVar: KVar<String>, watchedUser: WatchedUser) {
        div(fomantic.ui.centered.action.input) {
            val input = input(type = InputType.text, placeholder = "Search")
            select(fomantic.ui.compact.selection.dropdown) {
                option().text("Username")
                option().text("Repository")
            }
            input.value = usernameKVar
            input.on.keypress { ke ->
                if (ke.code == "Enter") {
                    handleChooseUsername(input, watchedUser)
                }
            }
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
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.followers.map { it.size.toString() })
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
                                span().text(follower.name ?: follower.login)
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
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.repositories.map { it.size.toString() })
                    span().text("Repositories")
                }
            }
        }
        val tableBody = tbody()

        tableBody.new {
            watchedUser.repositories.addListener { _, repositoryMap ->
                tableBody.removeChildren() // Clear all rows
                repositoryMap.forEach { (name, _) ->
                    tr().new {
                        td().new {
                            a(fomantic.ui.image.label).text(name)
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
                    div(fomantic.ui.teal.horizontal.label).text(watchedUser.follows.map { it.size.toString() })
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
                                span().text(follows.name ?: follows.login)
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

            watchedUser.followers.value = GHPersonSet()
            watchedUser.follows.value = GHPersonSet()
            watchedUser.repositories.value = emptyMap()

            try {
                val user = getUser(username)
                watchedUser.show.value = true
                watchedUser.name.value = user.name
                watchedUser.bio.value = user.bio
                watchedUser.location.value = user.location
                watchedUser.pageUrl.value = user.htmlUrl
                watchedUser.avatarUrl.value = user.avatarUrl
                watchedUser.followers.value = user.followers
                watchedUser.follows.value = user.follows
                watchedUser.repositories.value = user.repositories
            } catch (e: Exception) {
                sendToast("error", "User load error", "Something went wrong loading the user!", "white")
            }
        }
    }

    private fun sendToast(className: String, title: String, message: String, progressColour: String) {
        document.body.execute(
            "$('body').toast({showProgress: 'bottom', classProgress: 'i$progressColour', message: '$message', class: '$className', title: '$title'});"
        )
    }
}


