// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.profile.ProfileAction

/** The profile tab over its mount: the account screen when mounted, else the header, the plan row and the Account row. */
@Composable
fun ProfileScreen(mount: ProfileMount, modifier: Modifier = Modifier) {
  val account by mount.account.collectAsState()
  account?.let { child ->
    AccountScreen(child, modifier)
    return
  }

  val state by mount.store.state.collectAsState()
  Column(modifier.fillMaxSize()) {
    Text(
      stringResource(R.string.profile_title),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
    Text(
      state.displayName,
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(horizontal = 24.dp),
    )
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    ListItem(
      headlineContent = { Text(stringResource(R.string.profile_plan)) },
      supportingContent = { Text(stringResource(state.entitlement.planLabelRes)) },
      modifier = Modifier.clickable { mount.store.send(ProfileAction.PlanTapped) },
    )
    HorizontalDivider()
    ListItem(
      headlineContent = { Text(stringResource(R.string.profile_account)) },
      supportingContent = { Text(stringResource(R.string.profile_account_detail)) },
      modifier = Modifier.clickable { mount.store.send(ProfileAction.AccountTapped) },
    )
    HorizontalDivider()
  }
}

/** The plan row's text, as a resource, from the projected entitlement. */
val Entitlement.planLabelRes: Int
  @StringRes
  get() =
    when (this) {
      Entitlement.Free -> R.string.plan_free
      is Entitlement.Premium ->
        when (plan) {
          Plan.Monthly -> R.string.plan_premium_monthly
          Plan.Yearly -> R.string.plan_premium_yearly
        }
    }
