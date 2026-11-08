// NekoUiKit.kt  - drop-in UI kit for NekoRead (Jetpack Compose + Material 3)
// 1) Change the package line to match your project.
// 2) Call UiPrefs.init(applicationContext) once (Application.onCreate or MainActivity.onCreate).
// 3) See the usage notes at the bottom of this file.
package com.nekoread.ui.kit

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ */
/* Options                                                             */
/* ------------------------------------------------------------------ */

enum class CardWidth(val label: String, val size: Int) {
    Compact("Compact", 96), Dense("Dense", 112), Standard("Standard", 124),
    Balanced("Balanced", 136), Comfort("Comfort", 148), Large("Large", 160)
}

enum class CardRadius(val label: String, val size: Int) {
    Sharp("Sharp", 0), Subtle("Subtle", 6), Classic("Classic", 12),
    Rounded("Rounded", 18), Pill("Pill", 28)
}

enum class Glow(val label: String, val alpha: Float) {
    Subtle("Subtle", 0.30f), Balanced("Balanced", 0.50f), Bold("Bold", 0.75f)
}

enum class Sheen(val label: String, val alpha: Float) {
    Off("Off", 0f), Soft("Soft", 0.10f), Bright("Bright", 0.22f)
}

enum class EdgeCoverage(val label: String) {
    TopOnly("Top only"), Half("Half"), Full("Full outline")
}

/* ------------------------------------------------------------------ */
/* Saved settings (SharedPreferences, observable by Compose)           */
/* ------------------------------------------------------------------ */

object UiPrefs {
    private var sp: SharedPreferences? = null

    var glassUi by mutableStateOf(true); private set
    var glassStrength by mutableFloatStateOf(0.6f); private set
    var floatingNav by mutableStateOf(true); private set
    var fluidMotion by mutableStateOf(true); private set

    var cardWidth by mutableStateOf(CardWidth.Dense); private set
    var cardRadius by mutableStateOf(CardRadius.Rounded); private set
    var depthEffect by mutableStateOf(true); private set
    var glow by mutableStateOf(Glow.Subtle); private set
    var sheen by mutableStateOf(Sheen.Soft); private set
    var coverage by mutableStateOf(EdgeCoverage.TopOnly); private set

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("neko_ui", Context.MODE_PRIVATE)
        sp = p
        glassUi = p.getBoolean("glass", true)
        glassStrength = p.getFloat("glass_strength", 0.6f)
        floatingNav = p.getBoolean("floating_nav", true)
        fluidMotion = p.getBoolean("fluid", true)
        depthEffect = p.getBoolean("depth", true)
        cardWidth = readEnum(p, "card_w", CardWidth.Dense)
        cardRadius = readEnum(p, "card_r", CardRadius.Rounded)
        glow = readEnum(p, "glow", Glow.Subtle)
        sheen = readEnum(p, "sheen", Sheen.Soft)
        coverage = readEnum(p, "coverage", EdgeCoverage.TopOnly)
    }

    private inline fun <reified E : Enum<E>> readEnum(p: SharedPreferences, key: String, def: E): E =
        runCatching { enumValueOf<E>(p.getString(key, null) ?: def.name) }.getOrDefault(def)

    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        val e = sp?.edit() ?: return
        e.block()
        e.apply()
    }

    fun setGlassUi(v: Boolean) { glassUi = v; put { putBoolean("glass", v) } }
    fun setGlassStrength(v: Float) { glassStrength = v; put { putFloat("glass_strength", v) } }
    fun setFloatingNav(v: Boolean) { floatingNav = v; put { putBoolean("floating_nav", v) } }
    fun setFluidMotion(v: Boolean) { fluidMotion = v; put { putBoolean("fluid", v) } }
    fun setDepth(v: Boolean) { depthEffect = v; put { putBoolean("depth", v) } }
    fun setCardWidth(v: CardWidth) { cardWidth = v; put { putString("card_w", v.name) } }
    fun setCardRadius(v: CardRadius) { cardRadius = v; put { putString("card_r", v.name) } }
    fun setGlow(v: Glow) { glow = v; put { putString("glow", v.name) } }
    fun setSheen(v: Sheen) { sheen = v; put { putString("sheen", v.name) } }
    fun setCoverage(v: EdgeCoverage) { coverage = v; put { putString("coverage", v.name) } }

    fun resetCardStyle() {
        setCardWidth(CardWidth.Dense); setCardRadius(CardRadius.Rounded)
        setDepth(true); setGlow(Glow.Subtle); setSheen(Sheen.Soft); setCoverage(EdgeCoverage.TopOnly)
    }
}

/* ------------------------------------------------------------------ */
/* Fluid motion helpers                                                */
/* ------------------------------------------------------------------ */

fun <T> fluidSpec(): FiniteAnimationSpec<T> =
    if (UiPrefs.fluidMotion) spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow) else snap()

// Use these in NavHost: enterTransition = { fluidEnter() }, exitTransition = { fluidExit() } ...
fun fluidEnter(): EnterTransition =
    if (UiPrefs.fluidMotion) fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = spring(0.8f, Spring.StiffnessLow))
    else EnterTransition.None

fun fluidExit(): ExitTransition =
    if (UiPrefs.fluidMotion) fadeOut(tween(140)) + scaleOut(targetScale = 1.02f, animationSpec = tween(180))
    else ExitTransition.None

/** Clickable with a springy press-down scale. Use on cards, list rows, buttons. */
fun Modifier.fluidClickable(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && UiPrefs.fluidMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/* ------------------------------------------------------------------ */
/* Liquid-glass look                                                   */
/* ------------------------------------------------------------------ */
// Note: this is a "frosted" look (translucent fill + lit edge + soft shadow).
// For a real blur of what is behind the bar, add the Haze library and apply
// Modifier.hazeChild(state) next to this - the rest of this kit keeps working.

@Composable
fun Modifier.glass(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surface
): Modifier {
    if (!UiPrefs.glassUi) {
        return this
            .clip(shape)
            .background(tint)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    val s = UiPrefs.glassStrength.coerceIn(0f, 1f)
    return this
        .shadow(14.dp, shape, ambientColor = Color.Black.copy(0.35f), spotColor = Color.Black.copy(0.35f))
        .clip(shape)
        .background(tint.copy(alpha = 0.92f - 0.45f * s))
        .background(Brush.verticalGradient(listOf(Color.White.copy(0.04f + 0.14f * s), Color.White.copy(0.02f))))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(0.10f + 0.30f * s), Color.White.copy(0.04f))),
            shape
        )
}

/* ------------------------------------------------------------------ */
/* Poster card style                                                   */
/* ------------------------------------------------------------------ */

fun posterShape(): Shape = RoundedCornerShape(UiPrefs.cardRadius.size.dp)

/** Clips to the chosen corner radius and adds the lit top edge / sheen. Put it on the cover image box. */
@Composable
fun Modifier.cardDepth(shape: Shape = posterShape()): Modifier {
    val clipped = this.clip(shape)
    if (!UiPrefs.depthEffect) return clipped
    val glow = UiPrefs.glow.alpha
    val sheen = UiPrefs.sheen.alpha
    val edge: Brush = when (UiPrefs.coverage) {
        EdgeCoverage.TopOnly -> Brush.verticalGradient(0f to Color.White.copy(glow), 0.35f to Color.Transparent, 1f to Color.Transparent)
        EdgeCoverage.Half -> Brush.verticalGradient(0f to Color.White.copy(glow), 0.6f to Color.Transparent, 1f to Color.Transparent)
        EdgeCoverage.Full -> SolidColor(Color.White.copy(glow * 0.6f))
    }
    return clipped
        .drawWithContent {
            drawContent()
            if (sheen > 0f) {
                drawRect(Brush.verticalGradient(0f to Color.White.copy(sheen), 0.4f to Color.Transparent, 1f to Color.Transparent))
            }
        }
        .border(1.dp, edge, shape)
}

/* ------------------------------------------------------------------ */
/* Floating bottom bar                                                 */
/* ------------------------------------------------------------------ */

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val floating = UiPrefs.floatingNav
    val shape = RoundedCornerShape(if (floating) 32.dp else 0.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = if (floating) 22.dp else 0.dp, vertical = if (floating) 12.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(shape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = fluidSpec(), label = "navBg"
                )
                val fg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = fluidSpec(), label = "navFg"
                )
                Row(
                    modifier = Modifier
                        .animateContentSize(fluidSpec())
                        .clip(CircleShape)
                        .background(bg)
                        .fluidClickable { onSelect(item.route) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = fg)
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Text(
                            item.label,
                            color = fg,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Settings UI                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title (${label(selected)})", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                FilterChip(
                    selected = o == selected,
                    onClick = { onSelect(o) },
                    label = { Text(label(o)) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassSection(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

/**
 * Put this inside your existing Appearance section, under the theme color picker.
 * onOpenCardStyle should navigate to CardStyleScreen.
 */
@Composable
fun AppearanceExtras(onOpenCardStyle: () -> Unit) {
    GlassSection {
        Text("Style", style = MaterialTheme.typography.titleLarge)
        SwitchRow("Liquid glass UI", UiPrefs.glassUi) { UiPrefs.setGlassUi(it) }
        if (UiPrefs.glassUi) {
            Text("Glass intensity", style = MaterialTheme.typography.titleMedium)
            Slider(value = UiPrefs.glassStrength, onValueChange = { UiPrefs.setGlassStrength(it) })
        }
        SwitchRow("Floating menu bar", UiPrefs.floatingNav) { UiPrefs.setFloatingNav(it) }
        SwitchRow("Fluid animations", UiPrefs.fluidMotion) { UiPrefs.setFluidMotion(it) }
        Row(
            Modifier.fillMaxWidth().fluidClickable(onOpenCardStyle).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Poster card style", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun CardStyleScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp), // room for the floating bar
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Poster Card Style", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { UiPrefs.resetCardStyle() }) { Text("Reset") }
        }

        GlassSection {
            Text("Live preview", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val w = UiPrefs.cardWidth.size.dp
                Box(
                    Modifier
                        .width(w)
                        .height(w * 1.5f)
                        .cardDepth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column {
                    Text("Width: ${UiPrefs.cardWidth.size}dp")
                    Text("Corner radius: ${UiPrefs.cardRadius.size}dp")
                    Text("Height: ${(UiPrefs.cardWidth.size * 1.5f).toInt()}dp", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        GlassSection {
            ChipRow("Width", CardWidth.values().toList(), UiPrefs.cardWidth, { it.label }) { UiPrefs.setCardWidth(it) }
            ChipRow("Corner radius", CardRadius.values().toList(), UiPrefs.cardRadius, { it.label }) { UiPrefs.setCardRadius(it) }
        }

        GlassSection {
            Text("Card depth effect", style = MaterialTheme.typography.titleLarge)
            SwitchRow("Enable depth effect", UiPrefs.depthEffect) { UiPrefs.setDepth(it) }
            if (UiPrefs.depthEffect) {
                ChipRow("Edge glow", Glow.values().toList(), UiPrefs.glow, { it.label }) { UiPrefs.setGlow(it) }
                ChipRow("Top sheen", Sheen.values().toList(), UiPrefs.sheen, { it.label }) { UiPrefs.setSheen(it) }
                ChipRow("Edge coverage", EdgeCoverage.values().toList(), UiPrefs.coverage, { it.label }) { UiPrefs.setCoverage(it) }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* USAGE                                                               */
/* ------------------------------------------------------------------ */
/*
1) MainActivity.onCreate:   UiPrefs.init(applicationContext)

2) Replace your current bottom bar. Draw it OVER the content, not in Scaffold.bottomBar:

   Box(Modifier.fillMaxSize()) {
       NavHost(
           navController, startDestination = "library",
           enterTransition = { fluidEnter() }, exitTransition = { fluidExit() },
           popEnterTransition = { fluidEnter() }, popExitTransition = { fluidExit() }
       ) { ... }
       FloatingNavBar(
           items = listOf(
               NavItem("library", "Library", Icons.Filled.CollectionsBookmark),
               NavItem("history", "History", Icons.Filled.History),
               NavItem("explore", "Explore", Icons.Filled.Explore),
               NavItem("settings", "Settings", Icons.Filled.Settings)
           ),
           selectedRoute = currentRoute,
           onSelect = { navController.navigate(it) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
           modifier = Modifier.align(Alignment.BottomCenter)
       )
   }
   Give every screen's list/grid  contentPadding(bottom = 110.dp)  so the last row clears the bar.

3) Cards: width = UiPrefs.cardWidth.size.dp, then on the cover image:
   AsyncImage(..., modifier = Modifier.width(UiPrefs.cardWidth.size.dp).aspectRatio(2f/3f).cardDepth().fluidClickable { open() })

4) Settings > Appearance: call AppearanceExtras(onOpenCardStyle = { navController.navigate("card_style") })
   and add composable("card_style") { CardStyleScreen(onBack = { navController.popBackStack() }) }

5) Glass surfaces anywhere:  Modifier.glass(RoundedCornerShape(24.dp))
   (use it on the Library header pill, the category chips row, dialogs, the Explore FAB, etc.)
*/
