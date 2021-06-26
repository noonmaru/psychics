# PSYCHICS
[![Build Status](https://travis-ci.org/monun/psychics.svg?branch=master)](https://travis-ci.org/monun/psychics)
[![](https://jitpack.io/v/monun/psychics.svg)](https://jitpack.io/#monun/psychics)
![GitHub](https://img.shields.io/github/license/monun/psychics)

---
### 소개
* [**PaperMC**](https://papermc.io/) 기반의 초능력 플러그인입니다.
---

### 사용법 및 개발 문서
* [**Wiki**](https://github.com/monun/psychics/wiki)
---
### Compile
* **./gradlew build**
  * Plugin = `./psychics-common/build/libs/Psychics.jar`
  * Ability = `./psychics-abilities/build/libs/GroupName.AbilityName.jar`
  
### Dependency plugin
  * Tap = [Tap Releases](https://github.com/monun/tap/releases)
  * InvFx = [InvFx Releases](https://github.com/monun/invfx/releases)
  * Kotlin Plugin = [Kotlin Plugin Releases](https://github.com/monun/kotlin-plugin/releases)
  * ProtocolLib = [ProtocolLib LastSuccessfulBuild](https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar)
  * 위 의존성 플러그인을 설치하신 후 PaperMC 1.16.5 서버에서 플러그인을 넣고 구동하세요.
  * 추가 플러그인 = [WorldEdit](https://dev.bukkit.org/projects/worldedit/files)
