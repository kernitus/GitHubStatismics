package main.kotlin

import org.kohsuke.github.GitHub

fun main(){
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
}