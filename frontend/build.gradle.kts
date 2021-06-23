tasks.register<Exec>("npmBuild") {
    commandLine("npm", "install")
    commandLine("npm", "run", "build")
}

tasks.register<Copy>("copyDistToRes") {
    dependsOn("npmBuild")
    from(file("dist"))
    into(project(":server").file("src/main/resources/dist"))
}

tasks.register("build") {
    dependsOn("copyDistToRes")
}