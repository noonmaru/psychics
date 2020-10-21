# PSYCHICS
[![Build Status](https://travis-ci.org/noonmaru/psychics.svg?branch=master)](https://travis-ci.org/noonmaru/psychics)
[![](https://jitpack.io/v/noonmaru/psychics.svg)](https://jitpack.io/#noonmaru/psychics)
![GitHub](https://img.shields.io/github/license/noonmaru/psychics)

---
### 소개
* [**PaperMC**](https://papermc.io/) 기반의 초능력 플러그인입니다.
---

### 사용법 및 개발 문서
* [**Wiki**](https://github.com/noonmaru/psychics/wiki)
---
### Compile
* **./gradlew build**
  * Plugin = `./psychics-common/build/libs/Psychics.jar`
  * Ability = `./psychics-abilities/build/libs/GroupName.AbilityName.jar`
  
### Dependency plugin
  * Tap = [Tap Releases](https://github.com/noonmaru/tap/releases)
  * InvFx = [InvFx Releases](https://github.com/noonmaru/invfx/releases)
  * Kotlin Plugin = [Kotlin Plugin Releases](https://github.com/noonmaru/kotlin-plugin/releases)
  * ProtocolLib = [ProtocolLib LastSuccessfulBuild](https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar)
  * 위 의존성 플러그인을 설치하신 후 PaperMC 1.16.3 서버에서 플러그인을 넣고 구동하세요.
  * 추가 플러그인 = [WorldEdit](https://dev.bukkit.org/projects/worldedit/files)
