plugins {
  id("me.roundaround.allay")
}

allay {
  displayName.set("Custom Paintings")
  description.set("Add your own custom paintings to Minecraft.")
  authors.set(listOf("Roundaround"))
  license.set("MIT")
  homepage.set("https://modrinth.com/mod/custom-paintings-mod")
  repository.set("https://github.com/Roundaround/mc-custom-paintings")
  issues.set("https://github.com/Roundaround/mc-custom-paintings/issues")
  logoFile.set("assets/custompaintings/banner.png")

  gametest {
    // Acknowledge the Minecraft EULA for the throwaway worlds the headless
    // server game test spins up.
    eula.set(true)
  }

  modrinth {
    projectId.set("custom-paintings-mod")
  }

  curseforge {
    projectId.set(1560408)
  }

  release {
    versionType.set("release")
    minecraftVersions("26.1".."26.1.2")
    changelogDir.set(file("changelogs"))
  }
}
