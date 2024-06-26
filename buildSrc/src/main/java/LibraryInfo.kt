import org.gradle.api.JavaVersion

/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

object LibraryInfo {

    const val versionCode = 1

    const val compileSDK = 34
    const val buildTools = "34.0.0"
    const val minSDK = 24
    const val targetSDK = 34

    const val kotlinJvmTarget = "17"
    private val javaVersion: JavaVersion = JavaVersion.VERSION_17
    val sourceCompatibility = javaVersion
    val targetCompatibility = javaVersion

    const val libraryArtifactId = "billingz"
    const val coreArtifactId = "core"
    const val googleArtifactId = "google"
    const val amazonArtifactId = "amazon"

    const val namespace = "com.zuko.billingz"
    const val groupId = "com.zuko.billingz"

    const val archivesBaseName = groupId
}
