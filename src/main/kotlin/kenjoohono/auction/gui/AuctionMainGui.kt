package kenjoohono.auction.gui

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AuctionMainGui {
    fun openAuctionGui(player: Player) {
        val auctionInventory = Bukkit.createInventory(null, 54, TITLE)
        val blackGlass = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        blackGlass.itemMeta = blackGlass.itemMeta?.apply { setDisplayName(" ") }
        for (slot in 45..53) {
            auctionInventory.setItem(slot, blackGlass)
        }
        val auctionStartItem = ItemStack(Material.ANVIL)
        auctionStartItem.itemMeta = auctionStartItem.itemMeta?.apply {
            setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a경매 등록"))
        }
        auctionInventory.setItem(49, auctionStartItem)
        player.openInventory(auctionInventory)
    }

    companion object {
        const val TITLE = "경매"
    }
}