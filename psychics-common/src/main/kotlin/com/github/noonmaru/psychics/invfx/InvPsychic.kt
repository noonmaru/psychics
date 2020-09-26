package com.github.noonmaru.psychics.invfx

import com.github.noonmaru.invfx.InvFX
import com.github.noonmaru.invfx.InvScene
import com.github.noonmaru.psychics.PsychicConcept
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.addItemNonDuplicate
import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object InvPsychic {

    private val previousItem =
        ItemStack(Material.END_CRYSTAL).apply { setDisplayName("${ChatColor.RESET}${ChatColor.BOLD}←") }
    private val nextItem =
        ItemStack(Material.END_CRYSTAL).apply { setDisplayName("${ChatColor.RESET}${ChatColor.BOLD}→") }

    fun create(psychicConcept: PsychicConcept, stats: (EsperStatistic) -> Double): InvScene {
        return InvFX.scene(1, "${ChatColor.BOLD}Psychic") {
            panel(0, 0, 9, 1) {
                onInit {
                    it.setItem(0, 0, psychicConcept.renderTooltip().toItemStack(ItemStack(Material.ENCHANTED_BOOK)))
                }
                listView(2, 0, 5, 1, true, psychicConcept.abilityConcepts) {
                    transform { it.renderTooltip(stats).toItemStack(it.wand ?: ItemStack(Material.BOOK)) }
                    onClickItem { _, _, _, clicked, event ->
                        event.whoClicked.inventory.addItemNonDuplicate(clicked.supplyItems)
                    }
                }.let { view ->
                    button(1, 0) {
                        onInit {
                            it.item = previousItem
                        }
                        onClick { _, _ ->
                            view.index--
                        }
                    }
                    button(7, 0) {
                        onInit { it.item = nextItem }

                        onClick { _, _ ->
                            view.index++
                        }
                    }
                }
            }.let { panel ->
                panel.setItem(8, 0, ItemStack(Material.DARK_OAK_SIGN).apply {
                    setDisplayName("${ChatColor.GREEN}${ChatColor.BOLD}도움말")
                    lore = listOf(
                        "${ChatColor.WHITE}능력과 스킬의 정보를 확인하세요.",
                        "${ChatColor.WHITE}좌 우 버튼을 눌러 스크롤하세요.",
                        "${ChatColor.WHITE}지급 아이템이 있는 경우 능력을 ",
                        "${ChatColor.WHITE}클릭하여 얻을 수 있습니다.",
                    )
                })
            }
        }
    }

    private fun ItemStack.setDisplayName(name: String) {
        itemMeta = itemMeta.apply { this.setDisplayName(name) }
    }
}