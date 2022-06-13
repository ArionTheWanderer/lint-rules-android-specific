/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lint.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API

/*
 * The list of issues that will be checked when running <code>lint</code>.
 */
@Suppress("UnstableApiUsage")
class SampleIssueRegistry : IssueRegistry() {
    override val issues = listOf(
//        SampleCodeDetector.ISSUE,
        MimDetector.ISSUE_MEMBER_IGNORING_METHOD,
        DwDetector.ISSUE_DURABLE_WAKELOCK,
        IdsDetector.ISSUE_INEFFICIENT_DATA_STRUCTURE,
        IsDetector.ISSUE_INTERNAL_SETTER,
        IgDetector.ISSUE_INTERNAL_GETTER,
        NlmrDetector.ISSUE_NO_LOW_MEMORY_RESOLVER
    )

    override val api: Int
        get() = CURRENT_API

    override val minApi: Int
        get() = 8 // works with Studio 4.1 or later; see com.android.tools.lint.detector.api.Api / ApiKt
}
