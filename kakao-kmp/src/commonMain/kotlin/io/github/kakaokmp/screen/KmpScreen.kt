package io.github.kakaokmp.screen

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaokmp.node.KNode
import io.github.kakaokmp.node.MatcherBuilder

abstract class KmpScreen<T : KmpScreen<T>>(
    internal val composeUiTest: ComposeUiTest,
) : SemanticsNodeInteractionsProvider by composeUiTest {

    fun KNode(matcherBlock: MatcherBuilder.() -> Unit): KNode =
        KNode(composeUiTest, MatcherBuilder().apply(matcherBlock).build())
}

open class KmpScreenFactory<T : KmpScreen<T>>(
    internal val create: (ComposeUiTest) -> T,
)
