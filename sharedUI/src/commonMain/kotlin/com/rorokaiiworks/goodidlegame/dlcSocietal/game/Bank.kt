package com.rorokaiiworks.goodidlegame.dlcSocietal.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln

enum class BankMode(val title: String) {
    CoinsToIsleBucks("Coins to Isle Bucks"),
    IsleBucksToCoins("Isle Bucks to Coins"),
}

@Serializable
data class BankSave(
    val pressure: Float,
    val lastTickMills: Long,
)

class Bank : KoinComponent {
    val timeProvider: ITimeProvider by inject()
    val playerInventory: PlayerInventory by inject()
    val cityInventory: CityInventory by inject()

    var bankMode by mutableStateOf(BankMode.CoinsToIsleBucks)

    /**
     * 市场压力值，范围 [0, 1]。
     * 越高代表市场越饱和，买入汇率越差。
     * 每次用 Coins 换 Isle Bucks 时增加，随时间自然衰减。
     */
    var pressure by mutableStateOf(0f)
        private set

    private var lastTickMills = 0L

    val feePercent = 0.05f

    /**
     * 买入汇率（Coins → Isle Bucks）：
     * 给出 1 枚 Coin 能换到多少 Isle Bucks。
     * 压力越高，换得越少。
     */
    val buyRate: Float
        get() {
            val base = isleBucksPerCoin()          // 基础比率（无压力时）
            val penalty = pressurePenalty()        // 压力惩罚系数 ∈ [0, 1]
            return base * (1f - feePercent) * (1f - penalty)
        }

    /**
     * 卖出汇率（Isle Bucks → Coins）：
     * 给出 1 枚 Isle Buck 能换到多少 Coins。
     * 反向交易不受压力影响（鼓励回流）。
     */
    val sellRate: Float
        get() {
            val coinsPerIB = 1f / isleBucksPerCoin()
            return coinsPerIB * (1f - feePercent)
        }

    fun tick(currentMills: Long) {
        if (lastTickMills == 0L) {
            lastTickMills = currentMills
            return
        }
        val deltaSeconds = (currentMills - lastTickMills) / 1000f
        lastTickMills = currentMills

        // 压力半衰期：12 小时 = 43200 秒
        // 公式：pressure *= e^(-λ * Δt)，λ = ln2 / halfLife
        val halfLifeSeconds = 12 * 3600f
        val lambda = ln(2f) / halfLifeSeconds
        pressure = (pressure * exp(-lambda * deltaSeconds)).coerceIn(0f, 1f)
    }

    fun calculateReceive(amount: Long): Long {
        return when (bankMode) {
            BankMode.CoinsToIsleBucks -> floor(amount * buyRate).toLong()
            BankMode.IsleBucksToCoins -> floor(amount * sellRate).toLong()
        }
    }

    //CoinsToIsleBucks 会增加压力；IsleBucksToCoins 会小幅释放压力。
    fun trade(amount: Long) {
        when (bankMode) {
            BankMode.CoinsToIsleBucks -> {
                val receive = calculateReceive(amount)
                if (playerInventory.coins < amount || receive <= 0) return
                playerInventory.spendCoins(amount)
                cityInventory.addIsleBucks(receive)
                // 压力增量 = 交易额 / 压力容量基数，单次最多 +0.4
                val pressureDelta = (amount.toFloat() / PRESSURE_CAPACITY).coerceAtMost(0.4f)
                pressure = (pressure + pressureDelta).coerceAtMost(1f)
            }
            BankMode.IsleBucksToCoins -> {
                val receive = calculateReceive(amount)
                if (cityInventory.isleBucks < amount || receive <= 0) return
                cityInventory.spendIsleBucks(amount)
                playerInventory.addCoins(receive)
                // 反向交易释放少量压力（最多 -0.1）
                val relief = (amount.toFloat() / PRESSURE_CAPACITY * 0.25f).coerceAtMost(0.1f)
                pressure = (pressure - relief).coerceAtLeast(0f)
            }
        }
    }

    fun getMaxAffordable(): Long = when (bankMode) {
        BankMode.CoinsToIsleBucks -> playerInventory.coins
        BankMode.IsleBucksToCoins -> cityInventory.isleBucks
    }

    fun toSave(): BankSave = BankSave(pressure, lastTickMills)

    fun fromSave(save: BankSave) {
        pressure = save.pressure
        lastTickMills = save.lastTickMills
    }

    // 基础汇率：1 Coin = X Isle Bucks（无压力、无手续费时）。
    private fun isleBucksPerCoin(): Float {
        val base = 100f
        val multiplier = 1f // TODO: tie it maybe
        return (base * multiplier).coerceAtLeast(0.0001f)
    }

    /**
     * 阶梯式压力惩罚系数，返回 [0, MAX_PENALTY]。
     *
     * pressure : 0.0 → 0.3  惩罚 0%  → 20%   （轻微，几乎无感）
     * pressure : 0.3 → 0.6  惩罚 20% → 55%   （明显恶化）
     * pressure : 0.6 → 1.0  惩罚 55% → 90%   （严重，接近无利可图）
     */
    private fun pressurePenalty(): Float {
        return when {
            pressure < 0.3f -> lerp(0f,    0.20f, pressure / 0.3f)
            pressure < 0.6f -> lerp(0.20f, 0.55f, (pressure - 0.3f) / 0.3f)
            else            -> lerp(0.55f, 0.85f, (pressure - 0.6f) / 0.4f)
        }.coerceIn(0f, MAX_PENALTY)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    companion object {
        // 达到单次 +40% 压力所需的交易量（单位：Coins）。
        const val PRESSURE_CAPACITY = 10_000f

        // 压力惩罚上限，汇率最多恶化到原来的 10%
        const val MAX_PENALTY = 0.9f

        const val MAX_TRADE_MONEY_LENGTH = 6
        const val MAX_TRADE_MONEY = 999999L
    }
}
