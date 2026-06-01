plugins {
  id("me.roundaround.allay")
}

allay {
  displayName.set("Custom Paintings")
  description.set("Add your own custom paintings to Minecraft.")
  authors.set(listOf("Roundaround"))
  license.set("MIT")
  homepage.set("https://modrinth.com/mod/custom-paintings-mod")
  repository.set("https://github.com/Roundaround/mc-fabric-custom-paintings")
  issues.set("https://github.com/Roundaround/mc-fabric-custom-paintings/issues")
  logoFile.set("assets/custompaintings/banner.png")

  modrinth {
    projectId.set("custom-paintings-mod")
  }

  // curseforge { projectId.set(...) }

  release {
    versionType.set("release")
    sourcesJar.set(true)
  }
}
