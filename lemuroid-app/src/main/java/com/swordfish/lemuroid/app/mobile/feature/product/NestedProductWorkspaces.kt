package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R

/** Unmounted presentation contracts. Only real authorised services can supply actions. */
@Composable
internal fun ModernWorkspace(
    gameTitle: String,
    controllerScope: String,
    configurationStatus: String,
    verifiedActions: List<ProductAction>,
    modifier: Modifier = Modifier,
) {
    NestedWorkspace(
        mark = R.drawable.cartdeck_brand_modern_mark,
        title = "Modern",
        scope = gameTitle,
        details = listOf(controllerScope, configurationStatus),
        actions = verifiedActions,
        modifier = modifier,
    )
}

@Composable
internal fun VaultWorkspace(
    authorisedPlayer: String,
    accountStatus: String,
    verifiedSnapshotSummary: String,
    verifiedActions: List<ProductAction>,
    modifier: Modifier = Modifier,
) {
    NestedWorkspace(
        mark = R.drawable.cartdeck_brand_vault_mark,
        title = "Vault",
        scope = authorisedPlayer,
        details = listOf(accountStatus, verifiedSnapshotSummary),
        actions = verifiedActions,
        modifier = modifier,
    )
}

@Composable
private fun NestedWorkspace(
    mark: Int,
    title: String,
    scope: String,
    details: List<String>,
    actions: List<ProductAction>,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(mark), contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(scope, style = MaterialTheme.typography.titleMedium)
        details.forEach { Text(it) }
        actions.forEach { ProductActionCard(it) }
    }
}
