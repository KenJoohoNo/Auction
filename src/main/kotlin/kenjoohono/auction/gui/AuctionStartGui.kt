package kenjoohono.auction.gui

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AuctionStartGui {
    fun openAuctionStartGui(player: Player) {
        val auctionInventory = Bukkit.createInventory(null, 27, TITLE)
        val blackGlass = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        blackGlass.itemMeta = blackGlass.itemMeta?.apply { setDisplayName(" ") }
        for (slot in 0..26) {
            auctionInventory.setItem(slot, blackGlass)
        }
        val bundle = ItemStack(Material.BLACK_BUNDLE)
        bundle.itemMeta = bundle.itemMeta?.apply {
            setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a등록할 아이템을 올려주세요"))
        }
        auctionInventory.setItem(13, bundle)
        player.openInventory(auctionInventory)
    }

    companion object {
        const val TITLE = "경매 등록"
    }
}